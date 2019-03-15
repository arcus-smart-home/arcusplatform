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
package com.iris.driver.platform;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.messaging.MessageListener;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.platform.PlatformRequestDispatcher;
import com.iris.core.protocol.ProtocolMessageBus;
import com.iris.device.attributes.AttributeKey;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.DeviceDriverDefinition;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.reflex.ReflexDriverDFA;
import com.iris.driver.reflex.ReflexDriverDefinition;
import com.iris.driver.reflex.ReflexJson;
import com.iris.driver.reflex.ReflexRunMode;
import com.iris.driver.service.DriverConfig;
import com.iris.driver.service.DriverService;
import com.iris.driver.service.DriverServiceConfig;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.driver.service.handler.DriverServiceRequestHandler;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.messages.ErrorEvent;
import com.iris.messages.Message;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.Device;
import com.iris.messages.type.SyncDeviceState;
import com.iris.model.Version;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.control.ControlProtocol;
import com.iris.protocol.control.DeviceOfflineEvent;
import com.iris.protocol.control.DeviceOnlineEvent;
import com.iris.protocol.reflex.ReflexProtocol;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.netflix.governator.annotations.WarmUp;

/**
 * Reads messages from the protocol bus and
 * pushes them into the associated drivers.
 */
@Singleton
public class PlatformDriverService implements DriverService {
	private static final Logger LOGGER =
			LoggerFactory.getLogger(PlatformDriverService.class);

	private final ProtocolMessageBus protocolBus;
	private final PlatformMessageBus platformBus;
   private final DriverRegistry drivers;
   private final DriverExecutorRegistry driverRegistry;
   private final Set<AddressMatcher> platformMatchers;
   private final Set<AddressMatcher> protocolMatchers;

   private final PlatformRequestDispatcher platformHandler;
	private final ThreadPoolExecutor executor;
	private final Address deviceServiceAddress = Address.platformService(DeviceCapability.NAMESPACE);
	
	private final boolean restoreLostDevices;
	private final PlacePopulationCacheManager populationCacheMgr;
	
	@Inject
	public PlatformDriverService(
	      DriverConfig config,
	      DriverServiceConfig serviceConfig,
			ProtocolMessageBus protocolBus,
			PlatformMessageBus platformBus,
			DriverRegistry drivers,
			DriverExecutorRegistry driverRegistry,
			Set<DriverServiceRequestHandler> handlers,
			@Named(DriverConfig.NAMED_EXECUTOR) ThreadPoolExecutor executor,
			@Named("PlatformMatchers") Set<AddressMatcher> platformMatchers,
			@Named("ProtocolMatchers") Set<AddressMatcher> protocolMatchers,
			PlacePopulationCacheManager populationCacheMgr
	) {
	   this.platformHandler = new PlatformRequestDispatcher(platformBus, handlers);
	   this.platformHandler.init();
	   this.driverRegistry = driverRegistry;
		this.protocolBus = protocolBus;
		this.platformBus = platformBus;
		this.platformMatchers = platformMatchers;
		this.protocolMatchers = protocolMatchers;
		this.populationCacheMgr = populationCacheMgr;

      this.drivers = drivers;
      this.executor = executor;
      
      this.restoreLostDevices = serviceConfig.isRestoreLostDevices();
	}

	@PostConstruct
	public void init() {
	   LOGGER.debug("DriverService adding protocol listeners {} and platform listeners {}", protocolMatchers, platformMatchers);
		this.protocolBus.addMessageListener(protocolMatchers, new MessageListener<ProtocolMessage>() {
         @Override
         public void onMessage(ProtocolMessage message) {
            submit(message);
         }
		});
		this.platformBus.addMessageListener(platformMatchers, new MessageListener<PlatformMessage>(){
         @Override
         public void onMessage(PlatformMessage message) {
            submit(message);
         }
		});
	}

	@WarmUp
	public void startup() {
	   this.executor.prestartAllCoreThreads();
	}

	@PreDestroy
	public void shutdown() {
	   LOGGER.debug("Shutting down drivers...");
	   executor.shutdown();
	   // TODO await?
	}

	@Override
   public void submit(Message message) {
	   this.executor.submit(new DeliverMessageJob(message));
	}

	@Override
   public void handleProtocolMessage(ProtocolMessage message) {
	   LOGGER.trace("Received protocol message [{}]", message);
	   try {
	      if (ReflexProtocol.NAMESPACE.equals(message.getMessageType())) {
	         handleReflexProtocolMessage(message);
	         return;
	      }
	      String population = populationCacheMgr.getPopulationByPlaceId(message.getPlaceId());
	      if (message.getSource() instanceof DeviceProtocolAddress &&
	          ControlProtocol.NAMESPACE.equals(message.getMessageType())) {
	         MessageBody value = message.getValue(ControlProtocol.INSTANCE);
	         if (DeviceOnlineEvent.MESSAGE_TYPE.equals(value.getMessageType()) &&
	             DeviceOnlineEvent.MESSAGE_PAIRED.equals(value.getAttributes().get(DeviceOnlineEvent.ATTR_MESSAGE))) {
	            LOGGER.debug("Received a legacy PAIRED message for [{}], attempting to re-add", message.getSource());
	            sendGetDeviceInfo(message.getSource(), message.getPlaceId(), population);
	            return;
	         }

	         if (DeviceOfflineEvent.MESSAGE_TYPE.equals(value.getMessageType()) &&
	             DeviceOfflineEvent.MESSAGE_UNPAIRED.equals(value.getAttributes().get(DeviceOfflineEvent.ATTR_MESSAGE))) {
	            LOGGER.debug("Received a legacy UNPAIRED message for [{}], attempting to mark as lost", message.getSource());
	            DriverExecutor executor = driverRegistry.loadConsumer(message.getSource());
	            sendLostDevice(executor.context().getDriverAddress(), executor.context().getPlaceId(), population);
	            return;
	         }
	      }

         Address address = message.getDestination();
         if (address.isBroadcast()) {
            // we still allow broadcast messages from the protocol side...
            address = message.getSource();
         }

         // inner catch so that we don't trigger a restore when we get a legacy unpaired message
         try {
            driverRegistry
               .loadConsumer(address)
               .fire(message);
   	   } catch(NotFoundException e) {
   	      LOGGER.warn("Received protocol message for unrecognized address [{}]", message.getSource());
   	      tryRestoreLostDevice(message.getSource(), message.getPlaceId(), population);
   	   }
	   } catch(Throwable t) {
	      // TODO don't respond to errors with errors
	      LOGGER.warn("Error processing protocol message [{}]", message, t);
	      sendError(message, t);
	   }
	}

   public void handleReflexProtocolMessage(ProtocolMessage message) {
      try {
         Address address = message.getDestination();
         if (address.isBroadcast()) {
            // we still allow broadcast messages from the protocol side...
            address = message.getSource();
         }

         DriverExecutor exec = driverRegistry.loadConsumer(address);
         DeviceDriver drv = exec.driver();
         DeviceDriverDefinition def = drv.getDefinition();
         ReflexDriverDefinition rdef = def.getReflexes();

         List<?> reflexes = (rdef != null) ? rdef.getReflexes() : null;
         ReflexDriverDFA dfa = (rdef != null) ? rdef.getDfa() : null;
         if ((reflexes == null || reflexes.isEmpty()) &&
             (dfa == null || dfa.getDfa() == null)) {
            // This is a trace level message because we have some built-in drivers that
            // send reflexes regardless of what version of the driver is being used.
            LOGGER.trace("received reflex protocol message from driver with no reflexes, ignoring");
            return;
         }

         DeviceDriverContext ctx = exec.context();
         MessageBody msg = message.getValue(ReflexProtocol.INSTANCE);
         switch (msg.getMessageType()) {
         case Capability.CMD_SET_ATTRIBUTES:
            handleReflexAttributesSync(exec, ctx, msg.getAttributes(), message.getReflexVersion(), true);
            break;

         default:
            platformBus.send(PlatformMessage.buildEvent(msg, exec.context().getDriverAddress())
               .withPlaceId(exec.context().getPlaceId())
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(exec.context().getPlaceId()))
               .withActor(message.getActor())
               .create());
            break;
         }
      } catch (Exception ex) {
         LOGGER.warn("failed to process reflex protocol message: {}", message, ex);
      }
   }

   public void handleReflexAttributesSync(DriverExecutor exec, DeviceDriverContext ctx, Map<String,Object> attrs, Integer reflexVersion, boolean isDeviceMessage) {
      Map<String,AttributeKey<?>> supported = ctx.getSupportedAttributesByName();
      Map<AttributeKey<?>,Object> update = Maps.newHashMapWithExpectedSize(attrs.size());
      for (Map.Entry<String,Object> entry : attrs.entrySet()) {
         try {
            AttributeKey<?> key = supported.get(entry.getKey());
            if (key != null) {
               update.put(key,entry.getValue());
            } else {
               LOGGER.debug("device reported local attribute '{}' that is not supported.", entry.getKey());
            }
         } catch (Exception ex) {
            LOGGER.warn("error while processing attribute definition for {}: skipping attribute change event", entry.getKey(), ex);
         }
      }

      exec.fire(DriverEvent.createAttributesUpdated(update, reflexVersion, isDeviceMessage));
   }

   @Override
   public void handlePlatformMessage(PlatformMessage message) {
      LOGGER.trace("Received platform message [{}]", message);
	   platformHandler.onMessage(message);
   }

	protected boolean isExpired(Message message) {
	   if(message.getTimeToLive() > 0) {
	      return System.currentTimeMillis() > (message.getTimestamp().getTime() + message.getTimeToLive());
	   }
	   return false;
	}

   /*************************************************************************
    * Determine if a given device is connected through a hub.
    ************************************************************************/
	private static boolean isHubDevice(Device device) {
	   String prot = device.getProtocol();
	   return ZigbeeProtocol.NAMESPACE.equals(prot) ||
	          ZWaveProtocol.NAMESPACE.equals(prot);
	}

   /*************************************************************************
    * Given a protocol address in any form, turn that address into the
    * canonical platform representation for comparison.
    ************************************************************************/
   private static final @Nullable String canonicalProtocolAddress(String hubId, @Nullable String protocolAddress) {
      if (protocolAddress == null) {
         return null;
      }

      DeviceProtocolAddress address = (DeviceProtocolAddress) Address.fromString(protocolAddress);
      return Address.hubProtocolAddress(hubId, address.getProtocolName(), address.getId()).getRepresentation();
   }

   /*************************************************************************
    * Compute a map from protocol address to device given a list of devices.
    * The resulting map will only contain devices that should exist on the
    * hub.
    ************************************************************************/
	private static Map<String,Device> getHubDevicesByProtocol(String hubId, List<Device> devices) {
      Map<String,Device> devicesByProtocol = new HashMap<>((devices.size()+1)*4/3, 0.75f);
      for (Device device : devices) {
         if (!isHubDevice(device)) {
            continue;
         }

         String addr = canonicalProtocolAddress(hubId, device.getProtocolAddress());
         if (addr != null) {
            devicesByProtocol.put(addr, device);
         } else if (!device.isTombstoned()) {
            LOGGER.info("platform reported device with no protocol address: {}", device.getAddress());
         }
      }

      return devicesByProtocol;
	}

   /*************************************************************************
    * Update a device's degraded code based on the hub's view.
    ************************************************************************/
   private static boolean updateDegradedCode(Device device, boolean degraded) {
      String code = device.getDegradedCode();
      if (code == null) {
         code = Device.DEGRADED_CODE_NONE;
      }

      if (degraded && Device.DEGRADED_CODE_NONE.equals(code)) {
         device.setDegradedCode(Device.DEGRADED_CODE_HUB_FIRMWARE);
         return true;
      } else if (!degraded && Device.DEGRADED_CODE_HUB_FIRMWARE.equals(code)) {
         device.setDegradedCode(Device.DEGRADED_CODE_NONE);
         return true;
      }

      return false;
   }
      
   /*************************************************************************
    * Compute a set of devices that are unknown to the hub, a set
    * of devices that are unknown to the platform, a map from device to
    * its online state, and a map from device to its current attributes
    * given a map of devices by protocol address and a list of device
    * reports from the hub.
    *
    * This method also updates the degraded status of each device if
    * it is needed.
    ************************************************************************/
   private static void updateKnownAndUnknownDevices(
      String hubId,
      Map<String,Device> devicesByProtocol,
      List<Map<String,Object>> reported,
      Set<String> unknownToHub,
      Set<String> unknownToPlatform,
      Map<Device,Boolean> knownToBothOnline,
      Map<Device,Map<String,Object>> knownToBothAttrs,
      List<Device> updatedDegraded
   ) {
      for (Map<String,Object> report : reported) {
         MessageBody sync = MessageBody.buildMessage(com.iris.messages.type.SyncDeviceInfo.NAME, report);

         String reportedProtocol = com.iris.messages.type.SyncDeviceInfo.getProtocol(sync);
         String protocol = canonicalProtocolAddress(hubId, reportedProtocol);
         Boolean online = com.iris.messages.type.SyncDeviceInfo.getOnline(sync);
         Boolean degraded = com.iris.messages.type.SyncDeviceInfo.getDegraded(sync);
         Map<String,Object> attrs = com.iris.messages.type.SyncDeviceInfo.getAttrs(sync);
         Device device = devicesByProtocol.get(protocol);

         if (device != null) {
            unknownToHub.remove(protocol);
            knownToBothOnline.put(device,online);
            knownToBothAttrs.put(device,attrs);
            if (updateDegradedCode(device, degraded != null && degraded)) {
               updatedDegraded.add(device);
            }
         } else {
            unknownToPlatform.add(protocol);
         }
      }
   }

   /*************************************************************************
   * Reconcile the hub's reported state with the platform:
   *  * For devices that the hub knows about that we also know about
   *    we simply need to update the online/offline state and the
   *    current attributes.
   *
   *  * For devices that the hub knows about that we do not know about
   *    we need to go through the lost device restoration process
   *    by calling tryRestoreLostDevice()
   *
   *  * For devices that the hub does not know about that we do know about
   *    we need to mark the device as a lost device.
   *************************************************************************/
	private void reconcileHubDevicesWithPlatform(
	   UUID place, 
	   Integer reflexVersion,
      Map<String,Device> devicesByProtocol,
      Set<String> unknownToHub,
      Set<String> unknownToPlatform,
	   Map<Device,Boolean> knownToBothOnline,
	   Map<Device,Map<String,Object>> knownToBothAttrs
	) {
      for (Map.Entry<Device,Boolean> device : knownToBothOnline.entrySet()) {
         Device dev = device.getKey();
         Boolean online = device.getValue();

         try {
            Address addr = Address.fromString(dev.getAddress());
            DriverExecutor exec = driverRegistry.loadConsumer(addr);
            if (exec.context().isTombstoned()) {
           		exec.context().getLogger().debug("Sending ForceRemove request for tombstoned device [{}]", addr);
           		PlatformMessage request =
                 		PlatformMessage
                    		.request(addr)
                    		.from(Address.platformService(DeviceCapability.NAMESPACE))
                    		.withPayload(DeviceCapability.ForceRemoveRequest.instance())
                    		.create();
           		exec.context().sendToPlatform(request);
               continue;
            }

            if (online != null && online != exec.context().isConnected()) {
               if (online) {
                  exec.fire(
                     ProtocolMessage.builder()
                        .withPayload(ControlProtocol.INSTANCE, DeviceOnlineEvent.create("hub sync"))
                        .to(Address.broadcastAddress())
                        .from(exec.context().getProtocolAddress())
                        .withReflexVersion(reflexVersion)
                        .create()
                  );
               } else if (!online) {
                  exec.fire(
                     ProtocolMessage.builder()
                        .withPayload(ControlProtocol.INSTANCE, DeviceOfflineEvent.create("hub sync"))
                        .to(Address.broadcastAddress())
                        .from(exec.context().getProtocolAddress())
                        .withReflexVersion(reflexVersion)
                        .create()
                  );
               }
            } else if (online == null) {
               LOGGER.warn("device reported with unknown online/offline state: {}", dev.getAddress());
            }

            Map<String,Object> attrs = knownToBothAttrs.get(dev);
            if (attrs != null && !attrs.isEmpty()) {
               handleReflexAttributesSync(exec, exec.context(), attrs, reflexVersion, false);
            }
         } catch (Exception ex) {
            LOGGER.warn("failed to update attributes and set connected state for device {}:", dev.getId(), ex);
         }
      }

      String strPlaceId = place.toString();
      for (String protocol : unknownToPlatform) {
   	   tryRestoreLostDevice(Address.fromString(protocol), strPlaceId, populationCacheMgr.getPopulationByPlaceId(strPlaceId));
      }

      /*
      TODO: Fix this https://eyeris.atlassian.net/browse/ITWO-12241

      for (String protocol : unknownToHub) {
         Device device = devicesByProtocol.get(protocol);
         if (device != null) {
	         sendLostDevice(Address.fromString(device.getAddress()), place);
	      }
      }
      */
	}

   /*************************************************************************
    * Compute a map from driver name and version to DeviceDriverDefinition
    * given a set of devices.
    ************************************************************************/
   private Map<String,Map<Version,DeviceDriverDefinition>> getDriverDefinitionForDevices(
	   Set<Device> knownToBoth
   ) {
      Map<String,Map<Version,DeviceDriverDefinition>> definitions = new HashMap<>();
      for (Device device : knownToBoth) {
         try {
            String drvName = device.getDrivername();
            Version drvVersion = device.getDriverversion();

            Map<Version,DeviceDriverDefinition> versionToDef = definitions.get(drvName);
            DeviceDriverDefinition existing = (versionToDef != null) ? versionToDef.get(drvVersion) : null;
            if (existing == null) {
               DeviceDriver driver = drivers.loadDriverById(drvName, drvVersion);
               DeviceDriverDefinition definition = (driver != null) ? driver.getDefinition() : null;
               if (definition != null) {
                  Map<Version,DeviceDriverDefinition> v2d = definitions.computeIfAbsent(drvName, (unused) -> new HashMap<>(3));
                  v2d.put(drvVersion, definition);
               } else {
                  LOGGER.info("device has unknown driver definition: {}", device);
               }
            }
         } catch (Exception ex) {
            LOGGER.info("failed to load driver defiintions", ex);
         }
      }

      return definitions;
   }

   /*************************************************************************
    * Compute a list of ReflexDriverDefintion JSON objects given a map of 
    * driver name and version to DeviceDriverDefinition.
    ************************************************************************/
   private List<JsonObject> getSyncDriverReflexes(
      Map<String,Map<Version,DeviceDriverDefinition>> driverDefinitions
   ) {
      List<JsonObject> driverReflexes = new ArrayList<>();
      for (Map.Entry<String,Map<Version,DeviceDriverDefinition>> driverNameEntry : driverDefinitions.entrySet()) {
         for (Map.Entry<Version,DeviceDriverDefinition> driverVersionEntry : driverNameEntry.getValue().entrySet()) {
            DeviceDriverDefinition definition = driverVersionEntry.getValue();
            if(definition.getReflexes().getMode() == ReflexRunMode.PLATFORM) {
               continue;
            }
            try {
               ReflexDriverDefinition reflexDefs = definition.getReflexes();
               if (reflexDefs != null) {
                  JsonObject json = ReflexJson.toJsonObject(reflexDefs);
                  driverReflexes.add(json);
               }
            } catch (Exception ex) {
               LOGGER.info("failed to sync device", ex);
            }
         }
      }

      return driverReflexes;
   }

   /*************************************************************************
    * Compute a list of SyncDeviceState give a set of devices that are
    * known to both the hub and the platform and a map of driver name
    * and driver version to DeviceDriverDefinition.
    ************************************************************************/
   private List<Map<String,Object>> getSyncDeviceStates(
	   Set<Device> knownToBoth,
      Map<String,Map<Version,DeviceDriverDefinition>> driverDefinitions
   ) {
      List<Map<String,Object>> deviceStates = new ArrayList<>(knownToBoth.size());
      for (Device device : knownToBoth) {
         try {
            SyncDeviceState state = new SyncDeviceState();

            String drvName = device.getDrivername();
            Version drvVersion = device.getDriverversion();

            Map<Version,DeviceDriverDefinition> v2d = driverDefinitions.get(drvName);
            DeviceDriverDefinition definition = (v2d != null) ? v2d.get(drvVersion) : null;
            if (definition == null) {
               LOGGER.info("device has unknown driver definition: {}", device);
            }

            state.setProtocol(device.getProtocolAddress());
            state.setPlatform(device.getAddress());
            state.setDriver(drvName);
            state.setVersion(drvVersion.getRepresentation());
            state.setHash(definition != null ? definition.getHash() : "");
            deviceStates.add(state.toMap());
         } catch (Exception ex) {
            LOGGER.info("failed to sync device", ex);
         }
      }

      return deviceStates;
   }

   /*************************************************************************
    * Persists the degraded code and sends a value change indicating the
    * change given a list of devices that have had their degraded status
    * updated.
    ************************************************************************/

   private void dispatchDegradedCodeUpdates(List<Device> updated) {
      for (Device device : updated) {
         Address addr = Address.fromString(device.getAddress());
         MessageBody body = MessageBody.buildMessage(DeviceCapability.CMD_SET_ATTRIBUTES, ImmutableMap.of(
            DeviceAdvancedCapability.ATTR_DEGRADEDCODE, device.getDegradedCode()
         ));

         PlatformMessage update = PlatformMessage.buildRequest(body, deviceServiceAddress, addr).create();
         driverRegistry.loadConsumer(Address.fromString(device.getAddress()))
            .fire(update);
      }
   }

   /*************************************************************************
    * Synchronize the platform's view of the devices at a place with the
    * hub's view of devices at a place.
    ************************************************************************/
   public Pair<List<Map<String,Object>>,List<JsonObject>> syncDevices(String hubId, UUID place, Integer reflexVersion, List<Device> devices, List<Map<String,Object>> reported) {
      Map<String,Device> devicesByProtocol = getHubDevicesByProtocol(hubId, devices);
      Set<String> unknownToHub = new HashSet<>(devicesByProtocol.keySet());

      Set<String> unknownToPlatform = new HashSet<>();
      Map<Device,Boolean> knownToBothOnline = Maps.newHashMapWithExpectedSize(devicesByProtocol.size());
      Map<Device,Map<String,Object>> knownToBothAttrs = Maps.newHashMapWithExpectedSize(devicesByProtocol.size());
      List<Device> updatedDegraded = new ArrayList<>();
      updateKnownAndUnknownDevices(hubId, devicesByProtocol, reported, unknownToHub, unknownToPlatform, knownToBothOnline, knownToBothAttrs, updatedDegraded);

      reconcileHubDevicesWithPlatform(place, reflexVersion, devicesByProtocol, unknownToHub, unknownToPlatform, knownToBothOnline, knownToBothAttrs);

      Map<String,Map<Version,DeviceDriverDefinition>> driverDefinitions = getDriverDefinitionForDevices(knownToBothOnline.keySet());
      List<JsonObject> driverReflexes = getSyncDriverReflexes(driverDefinitions);
      List<Map<String,Object>> deviceStates = getSyncDeviceStates(knownToBothOnline.keySet(), driverDefinitions);

      if (!updatedDegraded.isEmpty()) {
         dispatchDegradedCodeUpdates(updatedDegraded);
      }

      return Pair.of(deviceStates, driverReflexes);
   }

   /*************************************************************************
    * Synchronize the platform's view of each device's degraded state
    * with the hub's view of each device's degraded state at a place.
    ************************************************************************/
   public void syncDegradedDevices(String hubId, UUID place, List<Device> devices, List<Map<String,Object>> reported) {
      Map<String,Device> devicesByProtocol = getHubDevicesByProtocol(hubId, devices);
      List<Device> updated = null;
      for (Map<String,Object> report : reported) {
         try {
            MessageBody sync = MessageBody.buildMessage(com.iris.messages.type.DegradedInfo.NAME, report);

            String reportedProtocol = com.iris.messages.type.DegradedInfo.getProtocol(sync);
            String protocol = canonicalProtocolAddress(hubId, reportedProtocol);
            Boolean degraded = com.iris.messages.type.DegradedInfo.getDegraded(sync);

            Device device = devicesByProtocol.get(protocol);
            if (device != null && updateDegradedCode(device, degraded != null && degraded)) {
               List<Device> u = updated;
               if (u == null) {
                  u = new ArrayList<>();
                  updated = u;
               }

               u.add(device);
            }
         } catch (Exception ex) {
            LOGGER.warn("failed to sync degraded devices: {}", report, ex);
         }
      }

      if (updated != null) {
         dispatchDegradedCodeUpdates(updated);
      }
   }

   private void tryRestoreLostDevice(Address address, String placeId, String population) {
      if(!restoreLostDevices) {
         LOGGER.debug("Ignoring unrecognized device because restoreLostDevices == false");
         return;
      }
      if(address.isHubAddress()) {
         sendGetDeviceInfo(address, placeId, population);
      }
      else {
         ProtocolMessage message =
               ProtocolMessage
                  .builder()
                  .from(deviceServiceAddress)
                  .to(address)
                  .withPlaceId(placeId)
                  .withPopulation(population)
                  .withPayload(ControlProtocol.INSTANCE, Errors.notFound(address))
                  .create();
         protocolBus.send(message);
      }
   }
   
   private void sendGetDeviceInfo(Address protocolAddress, String placeId, String population) {
      Preconditions.checkArgument(protocolAddress.getHubId() != null, "must be a hub device");
      MessageBody getDeviceInfo =
            HubAdvancedCapability.GetDeviceInfoRequest
               .builder()
               .withProtocolAddress(protocolAddress.getRepresentation())
               .build();
      PlatformMessage message =
            PlatformMessage
               .request(Address.hubService(protocolAddress.getHubId(), HubCapability.NAMESPACE))
               .from(deviceServiceAddress)
               .withPlaceId(placeId)
               .withPopulation(population)
               .withPayload(getDeviceInfo)
               .create();
      platformBus.send(message);
   }

   private void sendLostDevice(Address address, UUID placeId, String population) {
      MessageBody lostDevice = DeviceConnectionCapability.LostDeviceRequest.instance();
      PlatformMessage message =
            PlatformMessage
               .request(address)
               .from(deviceServiceAddress)
               .withPlaceId(placeId)
               .withPopulation(population)
               .withPayload(lostDevice)
               .create();
      platformBus.send(message);
   }

   private void sendError(ProtocolMessage message, Throwable t) {
      sendError(message, Errors.fromException(t));
   }

   private void sendError(ProtocolMessage message, ErrorEvent e) {
      LOGGER.error("Unable to deliver protocol error [{}], protocol errors not yet implemented", e);
   }

   class DeliverMessageJob implements Runnable {
      private final Message message;

      DeliverMessageJob(Message message) {
         this.message = message;
      }

      @Override
      public void run() {
         if(message == null) {
            return;
         }
         
         try(Closeable ctx = Message.captureAndInitializeContext(message)) {
            if(isExpired(message)) {
               LOGGER.debug("Dropping message [{}] because it has expired", message);
            }
            else if(message instanceof ProtocolMessage) {
               handleProtocolMessage((ProtocolMessage) message);
            }
            else if(message instanceof PlatformMessage) {
               handlePlatformMessage((PlatformMessage) message);
            }
            else {
               LOGGER.warn("Dropping unsupported message [{}]", message);
            }
         }
         catch(Throwable t) {
            LOGGER.warn("Unable to deliver message [{}]", message, t);
         }
      }
   }

}

