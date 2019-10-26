/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.agent.reflex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import com.iris.agent.zwave.ZWaveLocalProcessing;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zwave.ZWaveProtocol;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.iris.agent.addressing.HubAddr;
import com.iris.agent.addressing.HubAddressUtils;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.device.HubDeviceService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.lifecycle.LifeCycle;
import com.iris.agent.lifecycle.LifeCycleListener;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.agent.reflex.drivers.HubDriver;
import com.iris.agent.reflex.drivers.HubDrivers;
import com.iris.agent.reflexes.HubReflexVersions;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.agent.router.Router;
import com.iris.agent.router.SnoopingPortHandler;
import com.iris.agent.util.Backoff;
import com.iris.agent.util.Backoffs;
import com.iris.agent.util.RxIris;
import com.iris.agent.zigbee.ZigbeeLocalProcessing;
import com.iris.driver.reflex.ReflexAction;
import com.iris.driver.reflex.ReflexActionBuiltin;
import com.iris.driver.reflex.ReflexDefinition;
import com.iris.driver.reflex.ReflexDriver;
import com.iris.driver.reflex.ReflexJson;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubReflexCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.DeviceService;
import com.iris.messages.services.PlatformConstants;
import com.iris.messages.type.DegradedInfo;
import com.iris.messages.type.SyncDeviceInfo;
import com.iris.messages.type.SyncDeviceState;
import com.iris.model.Version;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.control.ControlProtocol;
import com.iris.protocol.control.DeviceOfflineEvent;
import com.iris.protocol.control.DeviceOnlineEvent;
import com.iris.protocol.reflex.ReflexProtocol;
import com.iris.util.IrisUUID;
import com.iris.util.TypeMarker;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

import rx.Completable;

public class ReflexController implements SnoopingPortHandler, LifeCycleListener {
   private static final Logger log = LoggerFactory.getLogger(ReflexController.class);
   public static final boolean DISABLE_LOCAL_PROCESSING = System.getenv("IRIS_AGENT_DISABLE_LOCAL_PROCESSING") != null;

   public static final HubAddr ADDRESS = HubAddressUtils.service("reflex");
   public static final HubAddr GATEWAY_ADDRESS = HubAddressUtils.service("gateway");

   public static final Address DEVICE_SERVICE = Address.platformService(PlatformConstants.SERVICE_DEVICES);

   private final Timer timer;
   private final Router router;

   private final ZigbeeLocalProcessing zigbee;
   private final ZWaveLocalProcessing zwave;

   /*
    * Disable protocols that use proprietary information.
    * 
   private final SercommLocalProcessing sercomm;
   */

   private Port port;
   private ReflexLocalProcessing localProcessing;

   @SuppressWarnings("unused")
   private final HubAttributesService.Attribute<Integer> dbVersionAttr = HubAttributesService.computed(Integer.class, HubReflexCapability.ATTR_VERSIONSUPPORTED, new Supplier<Integer>() {
      @Override
      public Integer get() {
         return HubReflexVersions.CURRENT;
      }
   });

   private final HubAttributesService.Attribute<Integer> numDriversAttr = HubAttributesService.computed(Integer.class, HubReflexCapability.ATTR_NUMDRIVERS, new Supplier<Integer>() {
      @Override
      public Integer get() {
         return numDrivers;
      }
   });

   private final HubAttributesService.Attribute<Integer> numDevicesAttr = HubAttributesService.computed(Integer.class, HubReflexCapability.ATTR_NUMDEVICES, new Supplier<Integer>() {
      @Override
      @SuppressWarnings("null")
      public Integer get() {
         return (processors == null) ? 0 : processors.size();
      }
   });

   private final HubAttributesService.Attribute<Integer> numPinsAttr = HubAttributesService.computed(Integer.class, HubReflexCapability.ATTR_NUMPINS, new Supplier<Integer>() {
      @Override
      @SuppressWarnings("null")
      public Integer get() {
         return (pinToUser == null) ? 0 : pinToUser.size();
      }
   });

   private final HubAttributesService.Attribute<String> dbHashAttr = HubAttributesService.computed(String.class, HubReflexCapability.ATTR_DBHASH, new Supplier<String>() {
      @Override
      public String get() {
         return dbHash;
      }
   });

   private Map<String,UUID> pinToUser;
   private Map<UUID,String> userToPin;

   private final Map<Address,ReflexProcessor> processors;
   private volatile UUID reflexSyncCurrent = IrisUUID.randomUUID();
   private int numDrivers = 0;
   private String dbHash = "";

   private final Backoff syncBackoff = Backoffs.exponential()
      .initial(90, TimeUnit.SECONDS)
      .delay(90, TimeUnit.SECONDS)
      .random(0.33)
      .max(15, TimeUnit.MINUTES)
      .build();

   @Inject
   @SuppressWarnings("null")
   public ReflexController(
      ReflexLocalProcessing localProcessing,
      ZigbeeLocalProcessing zigbee,
      ZWaveLocalProcessing zwave,
      /*
      SercommLocalProcessing sercomm,
      */
      Router router
   ) {
      this.timer = new HashedWheelTimer();
      this.router = router;

      this.zigbee = zigbee;
      this.zwave = zwave;
      /*
      this.sercomm = sercomm;
      */
      this.localProcessing = localProcessing;

      this.pinToUser = new HashMap<>();
      this.userToPin = new HashMap<>();
      this.processors = new HashMap<>();
   }

   @PostConstruct
   public void initialize() {
      ReflexDao.start();
      this.port = router.connect("rflx", this, ADDRESS, new PortHandler() {
         @Override @Nullable public Object recv(Port port, PlatformMessage message) throws Exception { return recvDirect(port,message); }
         @Override public void recv(Port port, ProtocolMessage message) { }
         @Override public void recv(Port port, Object message) { handleDeferredTask(message); }
      });

      log.info("starting hub reflex controller...");
      localProcessing.setReflexController(this);
      LifeCycleService.addListener(this);

      port.queue(new Runnable() {
         @Override
         public void run() {
            try {
               updateReflexPins(ReflexDao.getReflexDBPins());
            } catch (Exception ex) {
               log.warn("failed to load user pin codes during startup:", ex);
            }

            try {
               String driversBase64 = ReflexDao.getReflexDB();
               Map<Address,Map<String,String>> reflexStates = ReflexDao.getAllReflexStates();
               Map<Address,Map<String,String>> driverStates = ReflexDao.getAllDriverStates();
               if (driversBase64 != null) {
                  applyDeviceReflexes(reflexStates, driverStates, driversBase64);
               }
            } catch (Exception ex) {
               log.warn("failed to load device reflexes during startup:", ex);
            }
         }
      });
   }

   @PreDestroy
   public void shutdown() {
      log.info("stopping hub reflex controller...");
      LifeCycleService.removeListener(this);
      ReflexDao.shutdown();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Alarm Controller Interaction
   /////////////////////////////////////////////////////////////////////////////
   
   Collection<? extends ReflexDevice> getDevices() {
      return processors.values();
   }
   
   void deliver(MessageBody msg) {
      final Address source = Address.hubService(IrisHal.getHubId(), "rflx");
      final PlatformMessage message = PlatformMessage.buildEvent(msg, source).create();
      port.queue(new Runnable() {
         @Override
         public void run() {
            for (ReflexProcessor proc : processors.values()) {
               try {
                  proc.handle(message);
               } catch (Exception ex) {
                  log.warn("exception while updated alert states: ", ex);
               }
            }
         }
      });
   }

   /////////////////////////////////////////////////////////////////////////////
   // APIs for Drivers
   /////////////////////////////////////////////////////////////////////////////
   

   public ZigbeeLocalProcessing zigbee() {
      return this.zigbee;
   }

   public ZWaveLocalProcessing zwave() {
      return this.zwave;
   }
   /*
   public SercommLocalProcessing sercomm() {
      return this.sercomm;
   }
   */
   
   public @Nullable UUID verifyPinCode(String code) {
      String hashed;
      UUID placeid = HubAttributesService.getPlaceId();
      if (placeid == null) {
         return null;
      }

      String uuid = IrisUUID.toString(placeid);

      try {
         MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
         hashed = Base64.encodeBase64String(sha1.digest((uuid + code).getBytes(StandardCharsets.UTF_8)));
      } catch (Exception ex) {
         log.warn("could not hash pin code: ", ex);
         return null;
      }

      return pinToUser.get(hashed);
   }

   public void emit(Address device, MessageBody msg) {
      emit(device, msg, null);
   }

   public void emit(Address device, MessageBody msg, @Nullable Address actor) {
      ProtocolMessage rflxMsg = ProtocolMessage.builder()
         .withPayload(ReflexProtocol.INSTANCE, msg)
         .to(Address.broadcastAddress())
         .from(device)
         .withActor(actor)
         .withReflexVersion(HubReflexVersions.CURRENT)
         .create();

      port.send(GATEWAY_ADDRESS, rflxMsg);
   }
   
   public void submit(Address device, Runnable task) {
      schedule(device, task, 0, TimeUnit.NANOSECONDS);
   }

   public void schedule(Address device, Runnable task, long time, TimeUnit unit) {
      timer.newTimeout((to) -> {
         // We only run the task if the device still exists. This ensures that
         // we aren't running tasks for devices that have been removed.
         if (processors.containsKey(device)) {
            port.queue(task);
         }
      }, time, unit);
   }

   public void periodic(Address device, Runnable task, long delay, long period, TimeUnit unit) {
      Runnable delegate = new Runnable() {
         @Override
         public void run() {
            try {
               task.run();
               schedule(device, this, period, unit);
            } catch (CancelPeriodicException ex) {
               // ignore
            } catch (Throwable th) {
               log.warn("task terminated abnormally: ", th);
            }
         }
      };

      schedule(device, delegate, delay, unit);
   }

   public void periodic(Address device, Runnable task, Backoff backoff) {
      Runnable delegate = new Runnable() {
         @Override
         public void run() {
            try {
               task.run();
               schedule(device, this, backoff.nextDelay(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
            } catch (CancelPeriodicException ex) {
               // ignore
            } catch (Throwable th) {
               log.warn("task terminated abnormally: ", th);
            }
         }
      };

      schedule(device, delegate, backoff.nextDelay(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
   }

   public void cancelPeriodicTask() {
      throw CancelPeriodicException.INSTANCE;
   }

   private static final class CancelPeriodicException extends RuntimeException {
      private static final long serialVersionUID = 1L;
      private static final CancelPeriodicException INSTANCE = new CancelPeriodicException();
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Hub <-> Platform Reflex Synchronization
   /////////////////////////////////////////////////////////////////////////////
   
   private void doReflexSync() {
      reflexSyncCurrent = IrisUUID.randomUUID();
      final UUID syncToken = reflexSyncCurrent;

      syncBackoff.onSuccess();
      Completable.create(sub -> {
         if (!syncToken.equals(reflexSyncCurrent)) {
            sub.onCompleted();
            return;
         }

         UUID acc = HubAttributesService.getAccountId();
         UUID plc = HubAttributesService.getPlaceId();
         if (acc == null || plc == null) {
            sub.onError(new Exception("not registered"));
            return;
         }

         List<Map<String,Object>> devices = new ArrayList<>(); 
         for (Map.Entry<String,HubDeviceService.DeviceProvider> entry : HubDeviceService.devices().entrySet()) {
            HubDeviceService.DeviceProvider prov = entry.getValue();

            int numDevices = 0;
            for (HubDeviceService.DeviceInfo info : prov) {
               SyncDeviceInfo sdinfo = new SyncDeviceInfo();
               sdinfo.setProtocol(info.getProtocolAddress());

               Address addr = Address.fromString(info.getProtocolAddress());
               ReflexProcessor proc = processors.get(addr);

               if (proc != null) {
                  sdinfo.setDriver(proc.getDriver());
                  sdinfo.setVersion(proc.getVersion().getRepresentation());
                  sdinfo.setHash(proc.getHash());
                  sdinfo.setAttrs(proc.getSyncState());
               }

               Boolean online = info.isOnline();
               if (online != null) {
                  sdinfo.setOnline(online);
               }

               sdinfo.setDegraded(proc != null && proc.isDegraded());
               devices.add(sdinfo.toMap());
               numDevices++;
            }

            log.info("syncing hub reflexes: {} protocol reported {} devices", entry.getKey(), numDevices);
         }

         MessageBody msg;
         try {
            log.info("syncing hub reflexes: {} devices reported to platform", devices.size());
            String json = JSON.toJson(devices);
            String compressed = compressAsString(json.getBytes(StandardCharsets.UTF_8));

            msg = DeviceService.SyncDevicesRequest.builder()
               .withAccountId(acc.toString())
               .withPlaceId(plc.toString())
               .withDevices(compressed)
               .build();
         } catch (Exception ex) {
            msg = Errors.fromException(ex);
         }

         PlatformMessage sync = PlatformMessage.buildRequest(msg, port.getSendPlatformAddress(), DEVICE_SERVICE)
            .withCorrelationId(reflexSyncCurrent.toString())
            .create();

         port.send(sync);
         sub.onError(new Throwable("again"));
      })
      .retryWhen(RxIris.retry(syncBackoff))
      .subscribe();
   }

   private void handleReflexSyncResponse(PlatformMessage message) {
      try {
         UUID corr = IrisUUID.fromString(message.getCorrelationId());
         if (!reflexSyncCurrent.equals(corr)) {
            log.debug("ignoring stale sync device response");
            return;
         }

         MessageBody body = message.getValue();

         Map<String,String> pins = DeviceService.SyncDevicesResponse.getPins(body);
         String devicesBase64 = DeviceService.SyncDevicesResponse.getDevices(body);
         String driverBase64 = DeviceService.SyncDevicesResponse.getDrivers(body);
         ReflexDao.putReflexDB(driverBase64);

         reflexSyncCurrent = IrisUUID.randomUUID();

         Map<Address,Boolean> currentDegradedStates = getDegradedStates();

         updateReflexPinsFrom(pins);
         applyDeviceStates(devicesBase64, driverBase64);

         Map<Address,Boolean> updatedDegradedStates = getDegradedStates();
         Set<Address> changed = new HashSet<>();
         for (Map.Entry<Address,Boolean> updated : updatedDegradedStates.entrySet()) {
            Address addr = updated.getKey();
            if (!currentDegradedStates.containsKey(addr)) {
               continue;
            }

            boolean isDegraded = updated.getValue();
            boolean wasDegraded = currentDegradedStates.get(addr);
            if (isDegraded != wasDegraded) {
               changed.add(addr);
            }
         }

         if (!changed.isEmpty()) {
            List<DegradedInfo> degraded = new ArrayList<>();
            for (Address change : changed) {
               Boolean isDegraded = updatedDegradedStates.get(change);

               DegradedInfo info = new DegradedInfo();
               info.setProtocol(change.getRepresentation());
               info.setDegraded(isDegraded != null && isDegraded);
               degraded.add(info);
            }

            String json = JSON.toJson(degraded);
            String compressed = compressAsString(json.getBytes(StandardCharsets.UTF_8));

            UUID plc = HubAttributesService.getPlaceId();
            port.sendEvent(
               DeviceService.DevicesDegradedEvent.builder()
                  .withPlaceId(IrisUUID.toString(plc))
                  .withDevices(compressed)
                  .build()
            );
         }
      } catch (Exception ex) {
         log.warn("failed to parse device sync response:", ex);
      }
   }

   private Map<Address,Boolean> getDegradedStates() {
      Map<Address,Boolean> degraded = new HashMap<>();
      for (ReflexProcessor proc : processors.values()) {
         degraded.put(proc.getAddress(), proc.isDegraded());
      }

      return degraded;
   }

   private void applyDeviceStates(String devicesBase64, String driverBase64) {
      try {
         String devices = decompressAsString(devicesBase64);
         List<Map<String,Object>> states = (List<Map<String,Object>>)JSON.fromJson(devices, List.class);
         applyDeviceStates(states, driverBase64);
      } catch (Exception ex) {
         log.warn("failed to apply device states:", ex);
      }
   }

   private void applyDeviceStates(List<Map<String,Object>> states, String driverBase64) {
      try {
         dbHash = DigestUtils.sha1Hex(driverBase64);
         dbHashAttr.poke();

         String drivers = decompressAsString(driverBase64);
         List<JsonObject> reflexes = JSON.fromJson(drivers, new TypeMarker<List<JsonObject>>() {});
         applyDeviceStates(states, reflexes);
      } catch (Exception ex) {
         log.warn("failed to apply device states:", ex);
      }
   }

   private void applyDeviceStates(List<Map<String,Object>> states, List<JsonObject> reflexes) {
      try {
         Map<Address,Map<String,String>> existingReflexStates = ReflexDao.getAllReflexStates();
         Map<Address,Map<String,String>> reflexStates = new HashMap<>();
         for (Map<String,Object> statedef : states) {
            try {
               SyncDeviceState state = new SyncDeviceState(statedef);
               if (log.isTraceEnabled()) {
                  log.trace("sync device state {}: {} {}", state.getProtocol(), state.getDriver(), state.getVersion());
               }

               Address addr = Address.fromString(state.getProtocol());
               String driv = state.getDriver();
               Version vers = Version.fromRepresentation(state.getVersion());

               Map<String,String> existing = existingReflexStates.get(addr);
               String curState = (existing == null) ? null : existing.get(ReflexDao.REFLEX_STATE_STATE);
               if (curState == null) {
                  curState = ReflexProcessor.State.INITIAL.name();
               }

               if (!"Fallback".equals(driv)) {
                  reflexStates.put(addr, ImmutableMap.of(
                     ReflexDao.REFLEX_STATE_DRIVER, driv,
                     ReflexDao.REFLEX_STATE_VERSION, vers.getRepresentation(),
                     ReflexDao.REFLEX_STATE_STATE, curState
                  ));
               }
            } catch (Exception ex) {
               log.warn("failed to sync device: {}", statedef, ex);
            }
         }

         ReflexDao.putReflexStates(reflexStates);
         applyDeviceReflexes(reflexStates, null, reflexes);
      } catch (Exception ex) {
         log.warn("failed to parse device sync response:", ex);
      }
   }

   private void applyDeviceReflexes(Map<Address,Map<String,String>> reflexStates, @Nullable Map<Address,Map<String,String>> driverStates, @Nullable String driverBase64) {
      dbHash = DigestUtils.sha1Hex(driverBase64);
      dbHashAttr.poke();

      if (driverBase64 == null || driverBase64.trim().isEmpty()) {
         log.info("no local device reflexes");
         return;
      }

      try {
         String drivers = decompressAsString(driverBase64);
         List<JsonObject> reflexes = JSON.fromJson(drivers, new TypeMarker<List<JsonObject>>() {});
         applyDeviceReflexes(reflexStates, driverStates, reflexes);
      } catch (Exception ex) {
         log.warn("failed to apply device states:", ex);
      }
   }

   private void applyDeviceReflexes(Map<Address,Map<String,String>> reflexStates, @Nullable Map<Address,Map<String,String>> driverStates, List<JsonObject> reflexes) {
      numDrivers = reflexes == null ? 0 : reflexes.size();
      numDriversAttr.poke();

      Map<String,Map<Version,com.iris.driver.reflex.ReflexDriverDefinition>> driversByName = new HashMap<>();
      if (reflexes != null) {
         for (JsonObject reflex : reflexes) {
            com.iris.driver.reflex.ReflexDriverDefinition driver = ReflexJson.fromJsonObject(reflex);
            String name = driver.getName();
            Version vers = driver.getVersion();

            Map<Version,com.iris.driver.reflex.ReflexDriverDefinition> drvmap = driversByName.get(name);
            if (drvmap == null) {
               drvmap = new HashMap<>();
               driversByName.put(name,drvmap);
            }

            drvmap.put(vers,driver);
         }
      }

      Set<Address> existingDevices = new HashSet<>();
      for (HubDeviceService.DeviceProvider provider : HubDeviceService.devices().values()) {
         for (HubDeviceService.DeviceInfo info : provider) {
            existingDevices.add(Address.fromString(info.getProtocolAddress()));
         }
      }

      for (Map.Entry<Address,Map<String,String>> statedef : reflexStates.entrySet()) {
         try {
            Address addr = statedef.getKey();
            if (!existingDevices.contains(addr)) {
               log.info("device {} exists in reflexdb but is not a known device, disabling reflexes", addr);
               continue;
            }

            String driv = (String)statedef.getValue().get(ReflexDao.REFLEX_STATE_DRIVER);
            Version vers = Version.fromRepresentation(statedef.getValue().get(ReflexDao.REFLEX_STATE_VERSION));

            String curStateStr = statedef.getValue().get(ReflexDao.REFLEX_STATE_STATE);
            ReflexProcessor.State curState = (curStateStr == null)
               ? ReflexProcessor.State.INITIAL
               : ReflexProcessor.State.valueOf(curStateStr);

            Map<Version,com.iris.driver.reflex.ReflexDriverDefinition> drvmap = driversByName.get(driv);
            com.iris.driver.reflex.ReflexDriverDefinition drv = (drvmap != null) ? drvmap.get(vers) : null;
            if (drv == null || drv.getReflexes().isEmpty()) {
               if (processors.remove(addr) != null) {
                  log.info("hub local reflexes for {} no longer present after sync", addr);
               } else {
                  log.trace("no hub local reflexes for {}", addr);
               }

               continue;
            }

            if (isBuiltinDriver(drv.getReflexes())) {
               String name = drv.getName();

               HubDrivers.Factory factory = HubDrivers.getFactory(driv, vers);
               if (factory == null) {
                  log.warn("hub driver should be builtin but could not be found: {} {}", driv, vers);
                  continue;
               }

               ReflexProcessor existing = processors.get(addr);
               if (existing != null && existing instanceof HubDriver) {
                  // If there is an existing driver that matches the requested
                  // driver then we don't need to do anything. Otherwise
                  // we need to shutdown the old one and start the new one.
                  if (factory.driver().equals(driv) && factory.version().equals(vers)) {
                     log.trace("reflexes haven't changed: {}", addr);

                     // Built-in drivers aren't allowed to have reflexes other than
                     // the built-in reflex so we don't check they hash here, but we
                     // do need to update the offline timeout just in case that has
                     // been updated.
                     sendOfflineTimeout(addr, drv.getOfflineTimeout());

                     continue;
                  }

                  ((HubDriver)existing).shutdown();
               }

               Map<String,String> driverState = (driverStates != null)
                  ? driverStates.get(addr)
                  : ReflexDao.getDriverState(addr);

               if (driverState == null) {
                  driverState = ImmutableMap.of();
               }

               HubDriver driver = factory.create(this,addr);
               driver.start(driverState, curState);
               sendOfflineTimeout(addr, drv.getOfflineTimeout());
               processors.put(addr, driver);
               continue;
            }

            ReflexDriverProcessor rdp = (ReflexDriverProcessor)processors.get(addr);
            if (rdp != null && rdp.driver.getHash().equals(drv.getHash())) {
               log.trace("reflexes haven't changed: {}", addr);
               continue;
            }

            Map<String,String> reflexState = (driverStates != null)
               ? driverStates.get(addr)
               : ReflexDao.getDriverState(addr);

            if (reflexState == null) {
               reflexState = ImmutableMap.of();
            }

            log.info("hub local reflexes {}: {} reflexes defined", addr, drv.getReflexes().size());
            if (log.isTraceEnabled() && drv.getDfa() != null && drv.getDfa().getDfa() != null) {
               log.trace("hub local dfa {}:\n{}", addr, drv.getDfa().getDfa().toDotGraph());
            }

            ReflexDriver driver = ReflexDriverFactory.create(driv, vers, drv);
            ReflexDriverProcessor proc = ReflexDriverProcessor.create(this, addr, driver);

            proc.start(reflexState, curState);
            processors.put(addr, proc);
            sendOfflineTimeout(addr, drv.getOfflineTimeout());
         } catch (Exception ex) {
            log.warn("failed to process device state: ", ex);
         }
      }

      numDevicesAttr.poke();
      localProcessing.fireDevicesUpdated();
   }

   private void sendOfflineTimeout(Address protocol, long offlineTimeout) {
      if (offlineTimeout > 0) {
         switch (((DeviceProtocolAddress)protocol).getProtocolName()) {

         case ZigbeeProtocol.NAMESPACE:
            zigbee.setOfflineTimeout(protocol, offlineTimeout);
            break;
         case ZWaveProtocol.NAMESPACE:
            zwave.setOfflineTimeout(protocol, offlineTimeout);
            break;

         default:
            log.trace("cannot set offline timeout for unknown protocol: {}", protocol);
            break;
         }
      }
   }

   private static boolean isBuiltinDriver(List<ReflexDefinition> reflexes) {
      for (ReflexDefinition reflex : reflexes) {
         List<?> matchers = reflex.getMatchers();
         if (matchers != null && !matchers.isEmpty()) {
            continue;
         }

         List<ReflexAction> actions = reflex.getActions();
         if (actions == null || actions.size() != 1) {
            continue;
         }

         ReflexAction action = actions.get(0);
         if (action instanceof ReflexActionBuiltin) {
            return true;
         }
      }

      return false;
   }

   private void updateReflexPinsFrom(Map<String,String> userToPin) {
      ImmutableMap.Builder<UUID,String> pins = ImmutableMap.builder();
      for (Map.Entry<String,String> pin : userToPin.entrySet()) {
         try {
            pins.put(IrisUUID.fromString(pin.getKey()), pin.getValue());
         } catch (Exception ex) {
            log.warn("failed to process pin for user {}", pin.getKey(), ex);
         }
      }

      updateReflexPins(pins.build());
   }

   private void updateReflexPins(Map<UUID,String> userToPin) {
      if (this.userToPin.equals(userToPin)) {
         return;
      }

      Set<String> existing = new HashSet<>();
      ImmutableMap.Builder<String,UUID> pinToUser = ImmutableMap.builder();
      for (Map.Entry<UUID,String> entry : userToPin.entrySet()) {
         if (existing.contains(entry.getValue())) {
            // this is not logged because we don't want to reveal users that have conflicting pins
            continue;
         }

         existing.add(entry.getValue());
         pinToUser.put(entry.getValue(), entry.getKey());
      }

      this.userToPin = ImmutableMap.copyOf(userToPin);
      this.pinToUser = pinToUser.build();
      ReflexDao.putReflexDBPins(userToPin);

      log.info("updated authorized pins: {} authorizations", this.userToPin.size());
      numPinsAttr.poke();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Hub LifeCycle API
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public void lifeCycleStateChanged(LifeCycle oldState, LifeCycle newState) {
      if (oldState != LifeCycle.AUTHORIZED && newState == LifeCycle.AUTHORIZED) {
         log.info("hub became authorized, should sync hub reflexes...");
         port.queue(new DoReflexSyncTask());
      }
   }

   @Override
   public void hubAccountIdUpdated(@Nullable UUID oldAcc, @Nullable UUID newAcc) {
      // ignore
   }

   @Override
   public void hubReset(LifeCycleService.Reset type) {
      // ignore
   }

   @Override
   public void hubDeregistered() {
      // ignore
   }

   /////////////////////////////////////////////////////////////////////////////
   // Message handling
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public boolean isInterestedIn(PlatformMessage message) {
      switch (message.getMessageType()) {
      case MessageConstants.MSG_ADD_DEVICE_RESPONSE:
      case DeviceAdvancedCapability.RemovedDeviceEvent.NAME:
      case HubCapability.UnpairingRequestRequest.NAME:
         return true;

      default:
         return false;
      }
   }

   @Override
   public boolean isInterestedIn(ProtocolMessage message) {
      Address source = message.getSource();

      boolean isReflex = ReflexProtocol.NAMESPACE.equals(message.getMessageType());
      return (!isReflex && source.isHubAddress()) ||
             (isReflex && MessageConstants.DRIVER.equals(source.getNamespace()));
   }

   @Nullable
   public Object recvDirect(Port port, PlatformMessage message) throws Exception {
      switch (message.getMessageType()) {
      case DeviceService.SyncDevicesResponse.NAME:
         handleReflexSyncResponse(message);
         return null;

      case HubReflexCapability.SyncNeededEvent.NAME:
         doReflexSync();
         return null;

      default:
         return Errors.unsupportedMessageType(message.getMessageType());
      }
   }

   @Override
   @Nullable
   public Object recv(Port port, PlatformMessage message) throws Exception {
      switch (message.getMessageType()) {
      case MessageConstants.MSG_ADD_DEVICE_RESPONSE:
         log.info("recevied add device response, should sync hub reflexes...");
         doReflexSync();
         break;

      case DeviceAdvancedCapability.RemovedDeviceEvent.NAME:
         try {
            log.info("snooped removed device request, removing reflexes for given device...");

            MessageBody msg = message.getValue();
            String prot = DeviceAdvancedCapability.RemovedDeviceEvent.getProtocol(msg);
            String prid = DeviceAdvancedCapability.RemovedDeviceEvent.getProtocolId(msg);
            Address addr = Address.hubProtocolAddress(IrisHal.getHubId(), prot, ProtocolDeviceId.fromRepresentation(prid));

            ReflexProcessor proc = processors.remove(addr);
            if (proc != null) {
               proc.setCurrentState(ReflexProcessor.State.REMOVED);
            }

            ReflexDao.removeAllDriverAndReflexState(addr.getRepresentation());
         } catch (Exception ex) {
            log.warn("reflexes could not process removed device event:", ex);
         } finally {
            localProcessing.fireDevicesUpdated();
         }
         break;

      case HubCapability.UnpairingRequestRequest.NAME:
         try {
            MessageBody msg = message.getValue();
            Boolean force = HubCapability.UnpairingRequestRequest.getForce(msg);
            if (force == null || !force) {
               return null;
            }

            log.info("snooped force remove device, removing reflexes for given device...");

            String prot = HubCapability.UnpairingRequestRequest.getProtocol(msg);
            String prid = HubCapability.UnpairingRequestRequest.getProtocolId(msg);
            Address addr = Address.hubProtocolAddress(IrisHal.getHubId(), prot, ProtocolDeviceId.fromRepresentation(prid));

            ReflexProcessor proc = processors.remove(addr);
            if (proc != null) {
               proc.setCurrentState(ReflexProcessor.State.REMOVED);
            }

            ReflexDao.removeAllDriverAndReflexState(addr.getRepresentation());
         } catch (Exception ex) {
            // ignore
         } finally {
            localProcessing.fireDevicesUpdated();
         }
         break;

      default:
         break;
      }

      return null;
   }

   @Override
   public void recv(Port port, ProtocolMessage message) {
      if (DISABLE_LOCAL_PROCESSING) {
         this.port.forward(GATEWAY_ADDRESS, message);
         return;
      }

      Address device = null;
      PlatformMessage forwarded = null;
      if (ReflexProtocol.NAMESPACE.equals(message.getMessageType())) {
         Address source = message.getSource();
         if (MessageConstants.DRIVER.equals(source.getNamespace())) {
            device = message.getDestination();
            MessageBody msg = message.getValue(ReflexProtocol.INSTANCE);
            forwarded = PlatformMessage.builder()
               .from(source)
               .to(device)
               .withPayload(msg)
               .withTimestamp(message.getTimestamp())
               .withTimeToLive(message.getTimeToLive())
               .withActor(message.getActor())
               .create();
         } else {
            return;
         }
      }

      Address from = message.getSource();
      if (from.isHubAddress()) {
         device = from;
      }

      if (device == null) {
         return;
      }

      ReflexProcessor processor = processors.get(device);
      if (processor != null && ControlProtocol.NAMESPACE.equals(message.getMessageType())) {
         MessageBody msg = message.getValue(ControlProtocol.INSTANCE);
         switch (msg.getMessageType()) {
         case DeviceOnlineEvent.MESSAGE_TYPE:
            processor.setCurrentState(ReflexProcessor.State.CONNECTED);
            localProcessing.fireDeviceOnline(device);
            break;
         case DeviceOfflineEvent.MESSAGE_TYPE:
            processor.setCurrentState(ReflexProcessor.State.DISCONNECTED);
            localProcessing.fireDeviceOffline(device);
            break;
         default:
            // ignore
            break;
         }

         this.port.forward(GATEWAY_ADDRESS, message);
         return;
      }

      boolean consumed = false;
      if (processor != null) {
         if (forwarded != null) {
            processor.handle(forwarded);
            consumed = true;
         } else {
            consumed = processor.handle(message);
         }
      }

      if (!consumed) {
         this.port.forward(GATEWAY_ADDRESS, message);
      }
   }

   @Override
   public void recv(Port port, Object message) {
      throw new UnsupportedOperationException();
   }
   
   public void handleDeferredTask(Object task) {
      if (task instanceof Runnable) {
         try {
            ((Runnable)task).run();
         } catch (Exception ex) {
            log.info("task failed:", ex);
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation details
   /////////////////////////////////////////////////////////////////////////////
   
   private byte[] decompress(byte[] content) throws IOException {
      ByteArrayInputStream bais = new ByteArrayInputStream(content);
      try (GZIPInputStream is = new GZIPInputStream(bais)) {
         return IOUtils.toByteArray(is);
      }
   }
   
   private String decompressAsString(byte[] content) throws IOException {
      return new String(decompress(content), StandardCharsets.UTF_8);
   }
   
   private String decompressAsString(String content) throws IOException {
      return decompressAsString(Base64.decodeBase64(content));
   }

   private byte[] compress(byte[] content) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (GZIPOutputStream os = new GZIPOutputStream(baos)) {
         os.write(content);
      }

      return baos.toByteArray();
   }

   private String compressAsString(byte[] content) throws IOException {
      return Base64.encodeBase64String(compress(content));
   }

   private final class DoReflexSyncTask implements Runnable {
      @Override
      public void run() {
         ReflexController.this.doReflexSync();
      }
   }
}

