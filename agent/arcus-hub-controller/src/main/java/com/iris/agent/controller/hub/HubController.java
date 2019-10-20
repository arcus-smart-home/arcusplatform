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
package com.iris.agent.controller.hub;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.Sets;
import com.iris.agent.addressing.HubAddr;
import com.iris.agent.addressing.HubAddressUtils;
import com.iris.agent.alarm.AlarmController;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.backup.BackupFinishedListener;
import com.iris.agent.backup.BackupService;
import com.iris.agent.controller.hub.lights.LEDConfig;
import com.iris.agent.device.HubDeviceService;
import com.iris.agent.exec.ExecService;
import com.iris.agent.fourg.FourgListener;
import com.iris.agent.fourg.FourgService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.hal.LEDState;
import com.iris.agent.hal.Model;
import com.iris.agent.hal.SounderMode;
import com.iris.agent.lifecycle.LifeCycle;
import com.iris.agent.lifecycle.LifeCycleListener;
import com.iris.agent.lifecycle.LifeCycleService;
import com.iris.agent.reflex.ReflexController;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.agent.router.Router;
import com.iris.agent.storage.StorageService;
import com.iris.agent.watchdog.WatchdogService;
import com.iris.agent.zigbee.ZigbeeController;
import com.iris.agent.zwave.ZWaveController;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.Hub4gCapability;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubAlarmCapability;
import com.iris.messages.capability.HubBackupCapability;
import com.iris.messages.capability.HubButtonCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubChimeCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.capability.HubDebugCapability;
import com.iris.messages.capability.HubKitCapability;
import com.iris.messages.capability.HubMetricsCapability;
import com.iris.messages.capability.HubNetworkCapability;
import com.iris.messages.capability.HubPowerCapability;
import com.iris.messages.capability.HubReflexCapability;
import com.iris.messages.capability.HubSoundsCapability;
import com.iris.messages.capability.HubVolumeCapability;
import com.iris.messages.capability.HubWiFiCapability;
import com.iris.messages.capability.HubZigbeeCapability;
import com.iris.messages.capability.HubZwaveCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.DeviceService;
import com.iris.protocol.ProtocolMessage;
import com.iris.util.IrisUUID;
import com.netflix.governator.annotations.WarmUp;

public class HubController implements PortHandler, LifeCycleListener, BackupFinishedListener, FourgListener {
   private static final Logger log = LoggerFactory.getLogger(HubController.class);
   private static final long HUB_LED_UPDATE_CHECK = TimeUnit.NANOSECONDS.convert(1, TimeUnit.MINUTES);

   private static final Set<String> CAPS = new HashSet<String>();

   @SuppressWarnings({ "unused", "rawtypes" })
   private static final HubAttributesService.Attribute<Set> caps = HubAttributesService.ephemeral(Set.class, Capability.ATTR_CAPS, null);

   @SuppressWarnings("null")
   private static final HubAttributesService.Attribute<UUID> account = HubAttributesService.persisted(UUID.class, HubCapability.ATTR_ACCOUNT, null);
   @SuppressWarnings("null")
   private static final HubAttributesService.Attribute<UUID> place = HubAttributesService.persisted(UUID.class, HubCapability.ATTR_PLACE, null);
   @SuppressWarnings({ "unused", "null" })
   private static final HubAttributesService.Attribute<String> name = HubAttributesService.persisted(String.class, HubCapability.ATTR_NAME, null);
   @SuppressWarnings({ "unused", "null" })
   private static final HubAttributesService.Attribute<String> image = HubAttributesService.persisted(String.class, HubCapability.ATTR_IMAGE, null);
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> hubid = HubAttributesService.ephemeral(String.class, HubCapability.ATTR_ID, IrisHal.getHubId());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> vendor = HubAttributesService.ephemeral(String.class, HubCapability.ATTR_VENDOR, IrisHal.getVendor());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> model = HubAttributesService.ephemeral(String.class, HubCapability.ATTR_MODEL, IrisHal.getModel());
   private static final HubAttributesService.Attribute<String> state = HubAttributesService.ephemeral(String.class, HubCapability.ATTR_STATE, HubCapability.STATE_NORMAL);
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> timezone = HubAttributesService.persisted(String.class, HubCapability.ATTR_TZ, "");
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<Long> time = HubAttributesService.computed(Long.class, HubCapability.ATTR_TIME, new Supplier<Long>() {
      @Override
      public Long get() {
         return System.currentTimeMillis();
      }
   });

   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> serial = HubAttributesService.ephemeral(String.class, HubAdvancedCapability.ATTR_SERIALNUM, IrisHal.getSerialNumber());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> mac = HubAttributesService.ephemeral(String.class, HubAdvancedCapability.ATTR_MAC, IrisHal.getMacAddress());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> mfginfo = HubAttributesService.ephemeral(String.class, HubAdvancedCapability.ATTR_MFGINFO, IrisHal.getManufacturingInfo());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> mfgBatchNumber = HubAttributesService.ephemeral(String.class, HubAdvancedCapability.ATTR_MFGBATCHNUMBER, IrisHal.getManufacturingBatchNumber());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<Date> mfgDate = HubAttributesService.ephemeral(Date.class, HubAdvancedCapability.ATTR_MFGDATE, IrisHal.getManufacturingDate());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<Integer> mfgFactoryID = HubAttributesService.ephemeral(Integer.class, HubAdvancedCapability.ATTR_MFGFACTORYID, IrisHal.getManufacturingFactoryID());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> hwver = HubAttributesService.ephemeral(String.class, HubAdvancedCapability.ATTR_HARDWAREVER, IrisHal.getHardwareVersion());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<Long> hwFlashSize = HubAttributesService.ephemeral(Long.class, HubAdvancedCapability.ATTR_HWFLASHSIZE, IrisHal.getHardwareFlashSize());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> osver = HubAttributesService.ephemeral(String.class, HubAdvancedCapability.ATTR_OSVER, IrisHal.getOperatingSystemVersion());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> bootver = HubAttributesService.ephemeral(String.class, HubAdvancedCapability.ATTR_BOOTLOADERVER, IrisHal.getBootloaderVersion());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> agentver = HubAttributesService.ephemeral(String.class, HubAdvancedCapability.ATTR_AGENTVER, IrisHal.getAgentVersion());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<UUID> lastReset = HubAttributesService.persisted(UUID.class, HubAdvancedCapability.ATTR_LASTRESET, null);
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<UUID> lastDeviceAddRemove = HubAttributesService.persisted(UUID.class, HubAdvancedCapability.ATTR_LASTDEVICEADDREMOVE, null);
   @SuppressWarnings({ "unused", "rawtypes" })
   private static final HubAttributesService.Attribute<Set> lastFailedWatchdogCheck = HubAttributesService.persisted(Set.class, HubAdvancedCapability.ATTR_LASTFAILEDWATCHDOGCHECKS, Collections.emptySet());
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<Long> lastFailedWatchdogCheckTime = HubAttributesService.persisted(Long.class, HubAdvancedCapability.ATTR_LASTFAILEDWATCHDOGCHECKSTIME, 0L);
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> lastRestartReason = HubAttributesService.persisted(String.class, HubAdvancedCapability.ATTR_LASTRESTARTREASON, HubAdvancedCapability.LASTRESTARTREASON_UNKNOWN);
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<Long> lastDbCheck = HubAttributesService.persisted(Long.class, HubAdvancedCapability.ATTR_LASTDBCHECK, 0L);
   @SuppressWarnings("unused")
   private static final HubAttributesService.Attribute<String> lastDbCheckrResults = HubAttributesService.persisted(String.class, HubAdvancedCapability.ATTR_LASTDBCHECKRESULTS, "");

   public static final HubAddr ADDRESS = HubAddressUtils.service("hub");

   private final Router router;
   private final AtomicBoolean wasFactoryReset = new AtomicBoolean(false);
   private final AtomicBoolean wasSoftReset = new AtomicBoolean(false);

   private Port port;
   private HubAttributeReporter attrReporter;
   private long lastPairingChange;

   enum Provisioned {
	   Ethernet,
	   WiFi;
   }
   
   private Provisioned provisionedBy;
   private boolean hasPlace;
   
   @Inject
   @SuppressWarnings("null")
   public HubController(Router router) {
      this.router = router;

      // Capabilities can depend on model
      CAPS.add(HubCapability.NAMESPACE);
      CAPS.add(HubAdvancedCapability.NAMESPACE);
      CAPS.add(HubConnectionCapability.NAMESPACE);
      CAPS.add(HubMetricsCapability.NAMESPACE);
      CAPS.add(HubNetworkCapability.NAMESPACE);
      CAPS.add(HubPowerCapability.NAMESPACE);
      CAPS.add(HubVolumeCapability.NAMESPACE);
      CAPS.add(HubZigbeeCapability.NAMESPACE);
      CAPS.add(HubZwaveCapability.NAMESPACE);
      CAPS.add(HubChimeCapability.NAMESPACE);
      CAPS.add(HubSoundsCapability.NAMESPACE);
      CAPS.add(HubBackupCapability.NAMESPACE);
      CAPS.add(HubDebugCapability.NAMESPACE);
      CAPS.add(Hub4gCapability.NAMESPACE);
      CAPS.add(HubReflexCapability.NAMESPACE);
      CAPS.add(HubAlarmCapability.NAMESPACE);      
      CAPS.add(HubKitCapability.NAMESPACE);

      // Wireless support depends on model
      if (IrisHal.isWirelessSupported()) {
         CAPS.add(HubWiFiCapability.NAMESPACE);
      }
      
      if (IrisHal.hasButton()) {
          CAPS.add(HubButtonCapability.NAMESPACE);
      }
      
      caps.set(Sets.<String>newTreeSet(CAPS));

      HubAttributesService.setAttributeConnections(account, place, lastReset, lastDeviceAddRemove, lastRestartReason);
      WatchdogService.connectAttributes(lastFailedWatchdogCheck, lastFailedWatchdogCheckTime);

      // Is no place a good mark for assuming this is the first start up?
      try {
          log.debug("checking for provisioning");
          File provisioned = StorageService.getFile("data:///config/provisioned");
          if (provisioned.exists()) {
              String howProvisioned = Files.readAllLines(provisioned.toPath()).get(0);
              provisionedBy = Provisioned.valueOf(howProvisioned);
              log.debug("provisioned by {}", provisionedBy);
          }
      } catch (Exception ex) {
          log.warn("exception while processing factory reset: {}", ex.getMessage(), ex);
       }
      
      String lastResetReason = null;
      try {
         File factory = StorageService.getFile("agent:///factory_reset");
         log.debug("checking for factory reset: {}", factory);
         if (factory.exists()) {
             place.set(null);
             place.persist();
            factory.delete();
            wasFactoryReset.set(true);
            lastReset.set(IrisUUID.timeUUID());
            lastResetReason = HubAdvancedCapability.LASTRESTARTREASON_FACTORY_RESET;
         }
      } catch (Exception ex) {
         log.warn("exception while processing factory reset: {}", ex.getMessage(), ex);
      }

      try {
         File soft = StorageService.getFile("agent:///soft_reset");
         log.debug("checking for soft reset: {}", soft);
         if (soft.exists()) {
            if (!soft.delete()) {
               log.warn("failed to delete soft rest file");
            }

            wasSoftReset.set(true);
            if (lastResetReason == null) {
               lastResetReason = HubAdvancedCapability.LASTRESTARTREASON_SOFT_RESET;
            }
         }
      } catch (Exception ex) {
         log.warn("exception while processing soft reset: {}", ex.getMessage(), ex);
      }

      try {
         File dbcheck = StorageService.getFile("tmp:///dbcheck.log");
         log.debug("checking for db check logs: {}", dbcheck);
         if (dbcheck.exists()) {
            lastDbCheck.set(System.currentTimeMillis());
            try (InputStream is = new BufferedInputStream(new FileInputStream(dbcheck))) {
               lastDbCheckrResults.set(IOUtils.toString(is));
            } catch (IOException ex) {
               log.warn("failed to read db check results:", ex);
            } finally {
               if (!dbcheck.delete()) {
                  log.warn("failed to delete dbcheck file");
               }
            }
         }
      } catch (Exception ex) {
         log.warn("exception while processing soft reset: {}", ex.getMessage(), ex);
      }

      if (lastResetReason == null && System.getenv("IRIS_AGENT_WATCHDOG_RESET") != null) {
         lastResetReason = HubAdvancedCapability.LASTRESTARTREASON_WATCHDOG;
      }

      if (lastResetReason != null) {
         HubAttributesService.setLastRestartReason(lastResetReason);
      }
   }

   @PostConstruct
   public void start() {
      Preconditions.checkState(this.port == null, "hub controller already started");

      Port port = router.connect("hub", ADDRESS, this);
      this.port = port;
      this.attrReporter = new HubAttributeReporter(this.port);

      LifeCycleService.addListener(this);

      MetricsHandler.INSTANCE.start(port);
      FirmwareUpdateHandler.INSTANCE.start(port);
      LoggingHandler.INSTANCE.start(port);
      ConfigHandler.INSTANCE.start(port);
      BackupHandler.INSTANCE.start(port);
      SoundHandler.INSTANCE.start(port);
      PowerHandler.INSTANCE.start(this, port);
      DebugHandler.INSTANCE.start(port);

      // Wireless support depends on model
      if (IrisHal.isWirelessSupported()) {
         WirelessHandler.INSTANCE.start(port);
      }
      if (IrisHal.hasButton()) {
          ButtonIrisHandler.INSTANCE.start(this,port);
          IrisHal.addButtonListener((state,duration) -> onIrisButton(state,duration));
      }

      BackupService.addListener(this);
      FourgService.addListener(this);

      hasPlace = (place.get() != null);
      if (!hasPlace) {
    	  log.debug("At first boot");
    	  // LED and Sound States Handled by the hubOS
    	  //IrisHal.setLedState(LEDState.FIRST_BOOTUP);
    	  //IrisHal.setSounderMode(SounderMode.FIRST_BOOTUP);
      } else {
    	  log.debug("At place {}", place.get());
      }
      
      log.info("hub setup info: account={}, place={}", account.get(), place.get());
      ExecService.periodic().scheduleAtFixedRate(() -> updateLEDState(), HUB_LED_UPDATE_CHECK, HUB_LED_UPDATE_CHECK, TimeUnit.NANOSECONDS);   
   }

@WarmUp
   public void checkHubReset() {
      if (wasFactoryReset.compareAndSet(true,false)) {
         LifeCycleService.fireHubReset(LifeCycleService.Reset.FACTORY);
      }

      if (wasSoftReset.compareAndSet(true,false)) {
         LifeCycleService.fireHubReset(LifeCycleService.Reset.SOFT);
      }
   }

   @PreDestroy
   @SuppressWarnings("null")
   public void finish() {
      if (this.port != null) {
         router.disconnect(this.port);
         this.port = null;
      }
   }

   @Override
   public void v1MigrationFinished(MessageBody report) {
      port.sendEvent(report);
   }
   
   @Override
   public void v1MigrationUpdate(MessageBody report) {
      port.sendEvent(report);
   }

   @SuppressWarnings("null")
   @Override
   public void lifeCycleStateChanged(LifeCycle oldState, LifeCycle newState) {
	   updateLEDState(newState);

	   // Starting to feel like place == null should be added to the state list :/
	   if (oldState != LifeCycle.CONNECTED && newState == LifeCycle.CONNECTED) {   	  

         // Check if started up with no place
	      log.debug("First connections");
	      if (!hasPlace && place.get() == null) {
	         IrisHal.setLedState(LEDState.CLOUD_SUCCESS);
	         IrisHal.setSounderMode(SounderMode.GREATNEWS_CONNECTED_CLOUD);
	      }
	   }
	   if (oldState != LifeCycle.AUTHORIZED && newState == LifeCycle.AUTHORIZED) {   	  

    	  // Check if started up with no place, then changes.
		  log.debug("First authorization");
		  if (!hasPlace && place.get() != null) {
			   hasPlace = true;
			   log.debug("Place was Null, Now authorized!");
			   // Make sure this sound plays - delay a bit to make sure last sound (above) has played
			   //  This is needed in case these events happen back-to-back...
			   ExecService.periodic().schedule(new Runnable() {
			      @Override
			      public void run() {
			         IrisHal.setLedState(LEDState.REGISTER_SUCCESS);
			         IrisHal.setSounderMode(SounderMode.REGISTER_SUCCESS);
		         }
		      }, 10, TimeUnit.SECONDS);
		 }
         Map<String,Object> attributes = HubAttributesService.asAttributeMap(true, false, true);
         if (attributes != null && !attributes.isEmpty()) {
            port.sendEvent(MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, attributes));
         }
         BackupService.checkCurrentMigrationPhase();
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

   @Override
   public void fourgStateChanged(FourgService.State oldState, FourgService.State newState) {
      if (oldState != newState) {
    	  updateLEDState(newState);
      }
   }
      
   synchronized void updateLEDState() {
	   // If alarm is triggered it will dominate.
	   // TODO:  Roll alarm and other LED states into one state machine.
	   String alarmStatus = (String) HubAttributesService.asAttributeMap().get(HubAlarmCapability.ATTR_ALARMSTATE);
	   // V2 Does not care about alarms for LED States.
	   if (Model.isV2(IrisHal.getModel()) ||			   
		   alarmStatus.equals(HubAlarmCapability.ALARMSTATE_INACTIVE) || 
		   alarmStatus.equals(HubAlarmCapability.ALARMSTATE_READY)) {
		   updateLEDState(LifeCycleService.getState());
	   }
   }

   synchronized void updateLEDState(LifeCycle state) {
	   	updateLEDState(IrisHal.isBatteryPowered(), state, FourgService.getState());
   }
   
   synchronized void updateLEDState(FourgService.State newState) {
       updateLEDState(IrisHal.isBatteryPowered(), LifeCycleService.getState(), newState);      	   
   }
   
   synchronized void updateLEDState(boolean isBatteryPowered, LifeCycle current, FourgService.State backup) {
	   LEDState newState = IrisHal.getLedState();
	   log.trace("Updating LED State: {}", newState);
	   if (Model.isV2(IrisHal.getModel())) {
           newState = getLEDStateV2(isBatteryPowered,current,backup);
       } else {
           newState = getLEDStateV3(isBatteryPowered,current,backup);
       }
       IrisHal.setLedState(newState);
   }
   
   synchronized LEDState getLEDStateV2(boolean isBatteryPowered, LifeCycle current, FourgService.State backup) {
      boolean connected = LifeCycleService.isConnectedState(current);
      boolean isAuthorized = LifeCycleService.isAuthorizedState(current);
      boolean isBackupConnection = FourgService.isAuthorizedState(backup);
      boolean pairing = HubCapability.STATE_PAIRING.equals(state.get());

      return getLEDStateV2(IrisHal.getLedState(),isBatteryPowered,connected,isAuthorized, isBackupConnection,pairing);      
   }
   
   public synchronized static LEDState getLEDStateV2 (LEDState curState, boolean isBatteryPowered, boolean connected, boolean isAuthorized, boolean isBackupConnection, boolean pairing) {
       return LEDConfig.get(Model.IH200.toString(),isBackupConnection, isBatteryPowered, isAuthorized, pairing, connected);
   }
   
   synchronized LEDState getLEDStateV3(boolean isBatteryPowered, LifeCycle current, FourgService.State backup) {
       boolean connected = LifeCycleService.isConnectedState(current);
       boolean isAuthorized = LifeCycleService.isAuthorizedState(current);
       boolean isBackupConnection = FourgService.isAuthorizedState(backup);
       boolean pairing = HubCapability.STATE_PAIRING.equals(state.get());

       // WIFI - Handled by hub os for commisioning?
       // All sorts of other fun stuff.       
       // Need to add alert states here.
       return LEDConfig.get(IrisHal.getModel(),isBackupConnection, isBatteryPowered, isAuthorized, pairing, connected);
   }

   @Override
   public void recv(Port port, ProtocolMessage message) {
      log.debug("hub controller doesn't support protocol messages: {}", message);
   }

   @Override
   public void recv(Port port, Object message) {
      throw new UnsupportedOperationException();
   }

   @Nullable
   @Override
   public Object recv(Port port, PlatformMessage message) throws Exception {
      String type = message.getMessageType();
      switch (type) {
      case HubCapability.PairingRequestRequest.NAME:
         return handlePairingRequest(port, message);

      case HubCapability.UnpairingRequestRequest.NAME:
         return handleUnpairingRequest(port, message);

      case HubAdvancedCapability.RestartRequest.NAME:
         return handleRestartRequest();

      case HubAdvancedCapability.RebootRequest.NAME:
         return handleRebootRequest();

      case HubAdvancedCapability.FactoryResetRequest.NAME:
         return handleFactoryResetRequest();

      case HubAdvancedCapability.DeregisterEvent.NAME:
         return handleDeregister(port, message);

      case HubAdvancedCapability.GetKnownDevicesRequest.NAME:
         return handleGetKnownDevices(port, message);

      case HubAdvancedCapability.GetDeviceInfoRequest.NAME:
         return handleGetDeviceInfo(port, message);

      // Need to make a more generalize get/set attribute that maps back to the models.
      case Capability.CMD_GET_ATTRIBUTES:
         return handleGetBaseAttributes(port, message);

      case Capability.CMD_SET_ATTRIBUTES:
         return handleSetBaseAttributes(port, message);

      // Handle camera snapshot events
      /*
      case HubAdvancedCapability.StartUploadingCameraPreviewsEvent.NAME:
      case HubAdvancedCapability.StopUploadingCameraPreviewsEvent.NAME:
         port.send(SercommCameraController.ADDRESS, message);
         return Port.HANDLED;
      */

      case com.iris.messages.ErrorEvent.MESSAGE_TYPE:
         log.warn("error received from platform: {}", message);
         return null;

      default:
         if (type.startsWith(HubZwaveCapability.NAMESPACE)) {
            port.send(ZWaveController.ADDRESS, message);
            return Port.HANDLED;
         } else if (type.startsWith(HubZigbeeCapability.NAMESPACE)
        		 || type.startsWith(HubKitCapability.NAMESPACE)
        		 ) {
            port.send(ZigbeeController.ADDRESS, message);
            return Port.HANDLED;
         }/* else if (type.startsWith(HubSercommCapability.NAMESPACE)) {
            port.send(SercommCameraController.ADDRESS, message);
            return Port.HANDLED;
         } else if (type.startsWith(Hub4gCapability.NAMESPACE)) {
            port.send(FourGController.ADDRESS, message);
            return Port.HANDLED;
         } else */ if (type.startsWith(HubReflexCapability.NAMESPACE) || 
                    type.startsWith(DeviceService.NAMESPACE)) {
            port.send(ReflexController.ADDRESS, message);
            return Port.HANDLED;
         } else if (type.startsWith(HubAlarmCapability.NAMESPACE)) {
            port.send(AlarmController.ADDRESS, message);

            // Only update LEDs here on messages which apply to local alarms...
            switch (message.getMessageType()) {
            case HubAlarmCapability.ActivateRequest.NAME:
            case HubAlarmCapability.SuspendRequest.NAME:
            case HubAlarmCapability.PanicRequest.NAME:
            case HubAlarmCapability.ArmRequest.NAME:
            case HubAlarmCapability.DisarmRequest.NAME:
            case HubAlarmCapability.ClearIncidentRequest.NAME:
            case HubAlarmCapability.VerifiedEvent.NAME:
               updateLEDState();
               break;
            default:
               break;
            }
            return Port.HANDLED;
         /*
         } else if (type.startsWith(HubHueCapability.NAMESPACE)) {
            port.send(HueController.ADDRESS, message);
            return Port.HANDLED;
         */
         } else {
            if (log.isTraceEnabled()) {
               log.warn("hub controller cannot handle message: {}", message);
            } else {
               log.trace("hub controller cannot handle message: {}", message.getMessageType());
            }

            return message.isResponseRequired()
               ? Errors.unsupportedMessageType(message.getMessageType())
               : null;
         }
      }
   }

   private Object handleRestartRequest() {
      ExecService.periodic().schedule(new Runnable() {
         @Override
         public void run() {
            HubAttributesService.setLastRestartReason(HubAdvancedCapability.LASTRESTARTREASON_REQUESTED);
            IrisHal.restart();
         }
      }, 10, TimeUnit.SECONDS);

      return HubAdvancedCapability.RestartResponse.instance();
   }

   private Object handleRebootRequest() {
      ExecService.periodic().schedule(new Runnable() {
         @Override
         public void run() {
            HubAttributesService.setLastRestartReason(HubAdvancedCapability.LASTRESTARTREASON_REQUESTED);
            IrisHal.reboot();
         }
      }, 10, TimeUnit.SECONDS);

      return HubAdvancedCapability.RebootResponse.instance();
   }

   private Object handleFactoryResetRequest() {
      ExecService.periodic().schedule(new Runnable() {
         @Override
         public void run() {
            IrisHal.factoryReset();
         }
      }, 10, TimeUnit.SECONDS);

      return HubAdvancedCapability.FactoryResetResponse.instance();
   }

   @Nullable
   private Object handleDeregister(final Port port, final PlatformMessage message) {
      // Runs in a separate thread because some tasks make take a while
      Thread thr = new Thread(new Runnable() {
         @Override
         public void run() {
            LifeCycleService.fireHubDeregistered();
            IrisHal.factoryReset();
         }
      });

      thr.setName("remv");
      thr.setDaemon(false);
      thr.start();

      return null;
   }

   //////////////////////////////////////////////////////////////////////////////
   //////////////////////////////////////////////////////////////////////////////

   @Nullable
   private Object handleGetKnownDevices(final Port port, final PlatformMessage message) {
      MessageBody msg = message.getValue();
      Set<String> protocols = HubAdvancedCapability.GetKnownDevicesRequest.getProtocols(msg);
      if (protocols == null || protocols.isEmpty()) {
         protocols = new LinkedHashSet<>(Arrays.asList("ZIGB", "ZWAV", "SCOM"));
      }

      List<String> devices = new ArrayList<>();
      for (String protocol : protocols) {
         HubDeviceService.DeviceProvider provider = HubDeviceService.find(protocol);
         List<String> addrs = StreamSupport.stream(provider.spliterator(), false)
            .map(info -> info.getProtocolAddress())
            .collect(Collectors.toList());

         devices.addAll(addrs);
      }

      return HubAdvancedCapability.GetKnownDevicesResponse.builder()
         .withDevices(devices)
         .build();
   }

   @Nullable
   private Object handleGetDeviceInfo(final Port port, final PlatformMessage message) {
      MessageBody msg = message.getValue();
      final String protocolAddress = HubAdvancedCapability.GetDeviceInfoRequest.getProtocolAddress(msg);
      if (protocolAddress == null) {
         throw new RuntimeException("protocolAddress cannot be null");
      }

      try {
         DeviceProtocolAddress addr = (DeviceProtocolAddress)Address.fromString(protocolAddress);
         final HubDeviceService.DeviceProvider provider = HubDeviceService.find(addr.getProtocolName());
         ExecService.backgroundIo().submit(new Runnable() {
            @Override
            public void run() {
               Optional<MessageBody> response = StreamSupport.stream(provider.spliterator(),false)
                  .filter(dev -> protocolAddress.equals(dev.getProtocolAddress()))
                  .map(dev -> dev.getDeviceInfo(true))
                  .filter(info -> info != null)
                  .findFirst();
                  
               if (response.isPresent()) {
                  port.send(message.getSource(), response.get());
               }
            }
         });
      } catch (Exception ex) {
         // If there was a problem getting the device information then we don't send anything
         // back up to the platform.
      }

      return Port.HANDLED;
   }

   //////////////////////////////////////////////////////////////////////////////
   // Attributes Support
   //////////////////////////////////////////////////////////////////////////////

   @SuppressWarnings("unchecked")
   private Object handleGetBaseAttributes(Port port, PlatformMessage message) {
      Map<String,Object> attributes = message.getValue().getAttributes();
      if (attributes.containsKey("names")) {
         List<String> requestedAttributes = (List<String>)attributes.get("names");
         return MessageBody.buildMessage(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, HubAttributesService.asAttributeMap(requestedAttributes));
      }

      return MessageBody.buildMessage(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, HubAttributesService.asAttributeMap());
   }

   @SuppressWarnings("null")
   @Nullable
   private Object handleSetBaseAttributes(Port port, PlatformMessage message) {
      Map<String,Object> map = message.getValue().getAttributes();
      try (AutoCloseable lock = attrReporter.setExpectedSetAttributes(map)) {
         HubAttributesService.updateAttributes(map);
      } catch (Exception ex) {
         log.warn("failed to process set attributes: ", ex);
      }

      return null;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Protocol requests
   /////////////////////////////////////////////////////////////////////////////

   @Nullable
   private Object handlePairingRequest(Port port, PlatformMessage message) {
      log.info("handling pairing request: {}", message);

      MessageBody body = message.getValue();
      String action = HubCapability.PairingRequestRequest.getActionType(body);

      switch (action) {
      case HubCapability.PairingRequestRequest.ACTIONTYPE_START_PAIRING:
         enterPairingMode(message, HubCapability.PairingRequestRequest.getTimeout(body));
         return null;

      case HubCapability.PairingRequestRequest.ACTIONTYPE_STOP_PAIRING:
         exitPairingMode(message);
         return null;

      default:
         throw new RuntimeException("unknown pairing request action: " + action);
      }

   }

   @Nullable
   private Object handleUnpairingRequest(Port port, PlatformMessage message) {
      log.info("handling unpairing request: {} -> {}", message, message.getValue());

      MessageBody body = message.getValue();
      String action = HubCapability.UnpairingRequestRequest.getActionType(body);

      switch (action) {
      case HubCapability.UnpairingRequestRequest.ACTIONTYPE_START_UNPAIRING:
         boolean force = HubCapability.UnpairingRequestRequest.getForce(body);
         long duration = HubCapability.PairingRequestRequest.getTimeout(body);
         enterUnpairingMode(message, duration, force);
         return null;

      case HubCapability.UnpairingRequestRequest.ACTIONTYPE_STOP_UNPAIRING:
         exitUnpairingMode(message);
         return null;

      default:
         throw new RuntimeException("unknown pairing request action: " + action);
      }
   }

   private synchronized void enterPairingMode(PlatformMessage message, long duration) {
      String oldState = state.getAndSet(HubCapability.STATE_PAIRING);
      if (HubCapability.STATE_UNPAIRING.equals(oldState)) {
         doExitUnpairingMode();
      } else if (HubCapability.STATE_PAIRING.equals(oldState)) {
         log.warn("dropping start pairing request, already in pairing mode");
         return;
      }

      final long thisPairingChange = System.nanoTime();
      this.lastPairingChange = thisPairingChange;
      ExecService.periodic().schedule(new Runnable() {
         @Override
         public void run() {
            if (lastPairingChange != thisPairingChange) {
               log.debug("cancelling automatic pairing mode exit");
               return;
            }

            if (state.compareAndSet(HubCapability.STATE_PAIRING, HubCapability.STATE_NORMAL)) {
               log.debug("exiting pairing mode due to timeout");
               doExitPairingMode();
            }
         }
      }, duration, TimeUnit.MILLISECONDS);

      updateLEDState();
       port.forward(ZWaveController.ADDRESS, message);
       port.forward(ZigbeeController.ADDRESS, message);
      // port.forward(SercommCameraController.ADDRESS, message);
      // port.forward(HueController.ADDRESS, message);
   }

   private synchronized void enterUnpairingMode(PlatformMessage message, long duration, boolean force) {
      String oldState = state.getAndSet(HubCapability.STATE_UNPAIRING);
      if (HubCapability.STATE_PAIRING.equals(oldState)) {
         doExitPairingMode();
      } else if (HubCapability.STATE_UNPAIRING.equals(oldState)) {
         if (force) {
             port.forward(ZWaveController.ADDRESS, message);
             port.forward(ZigbeeController.ADDRESS, message);
            // port.forward(SercommCameraController.ADDRESS, message);
         } else {
            log.warn("dropping start unpairing request, already in unpairing mode");
         }

         return;
      }


      final long thisPairingChange = System.nanoTime();
      this.lastPairingChange = thisPairingChange;
      ExecService.periodic().schedule(new Runnable() {
         @Override
         public void run() {
            if (lastPairingChange != thisPairingChange) {
               log.debug("cancelling automatic unpairing mode exit");
               return;
            }

            if (state.compareAndSet(HubCapability.STATE_UNPAIRING, HubCapability.STATE_NORMAL)) {
               log.debug("exiting unpairing mode due to timeout");
               doExitUnpairingMode();
            }
         }
      }, duration, TimeUnit.MILLISECONDS);

      updateLEDState(IrisHal.isBatteryPowered(), LifeCycleService.getState(), FourgService.getState());
       port.forward(ZWaveController.ADDRESS, message);
       port.forward(ZigbeeController.ADDRESS, message);
      // port.forward(SercommCameraController.ADDRESS, message);
      // port.forward(HueController.ADDRESS, message);
   }

   private synchronized void exitPairingMode(PlatformMessage messagel) {
      if (!state.compareAndSet(HubCapability.STATE_PAIRING, HubCapability.STATE_NORMAL)) {
         log.warn("dropping stop pairing request, not in pairing mode");
         return;
      }

      doExitPairingMode();
   }

   private synchronized void exitUnpairingMode(PlatformMessage message) {
      if (!state.compareAndSet(HubCapability.STATE_UNPAIRING, HubCapability.STATE_NORMAL)) {
         log.warn("dropping stop unpairing request, not in unpairing mode");
         return;
      }

      doExitUnpairingMode();
   }

   private void doExitPairingMode() {
      MessageBody stopPairing = HubCapability.PairingRequestRequest.builder()
         .withActionType(HubCapability.PairingRequestRequest.ACTIONTYPE_STOP_PAIRING)
         .withTimeout(0L)
         .build();

      PlatformMessage message = PlatformMessage.buildMessage(stopPairing, port.getSendPlatformAddress(), port.getSendPlatformAddress()).create();
       port.forward(ZWaveController.ADDRESS, message);
       port.forward(ZigbeeController.ADDRESS, message);
      // port.forward(SercommCameraController.ADDRESS, message);
      // port.forward(HueController.ADDRESS, message);

      updateLEDState();
   }

   private void doExitUnpairingMode() {
      MessageBody stopUnpairing = HubCapability.UnpairingRequestRequest.builder()
         .withActionType(HubCapability.UnpairingRequestRequest.ACTIONTYPE_STOP_UNPAIRING)
         .withTimeout(0L)
         .build();

      PlatformMessage message = PlatformMessage.buildMessage(stopUnpairing, port.getSendPlatformAddress(), port.getSendPlatformAddress()).create();
       port.forward(ZWaveController.ADDRESS, message);
       port.forward(ZigbeeController.ADDRESS, message);
      // port.forward(SercommCameraController.ADDRESS, message);
      // port.forward(HueController.ADDRESS, message);

      updateLEDState();
   }

   @Nullable
   public File doBackup() {
      return BackupService.doBackup();
   }

   public void doRestore(String path) {
      BackupService.doRestore(new File(path));

      log.info("hub restore completed successfully, restating in 10 seconds");
      ExecService.periodic().schedule(new Runnable() {
         @Override
         public void run() {
            HubAttributesService.setLastRestartReason(HubAdvancedCapability.LASTRESTARTREASON_BACKUP_RESTORE);
            IrisHal.restart();
         }
      }, 10, TimeUnit.SECONDS);
   }
   
   private void onIrisButton(String state, int duration) {
	   boolean isBattery = IrisHal.isBatteryPowered();
	   boolean connected = LifeCycleService.isConnected();
	   boolean isAuthorized = LifeCycleService.isAuthorized();
	   boolean isBackupConnection = FourgService.isAuthorized();

	   
	   log.debug("Button Pressed isBatt = {}, connected = {}, isAuth = {}, isBackup = {}", isBattery, connected, isAuthorized, isBackupConnection);

	   // LED has the full state expansion for the button pressed, sounder copies because the enums have the same values.
	   // TODO:  Full scenario state machine that return both a light and sound and incorperates all states including alarms.
	   LEDState ledState = LEDConfig.INSTANCE.getButtonPressed(isBackupConnection, isBattery, isAuthorized, connected );
	   SounderMode soundMode  = SounderMode.valueOf(ledState.toString());

	   // Special Case
	   if (place.get() == null) {
		   soundMode = SounderMode.BUTTON_PRESS_NOPLACE;
	   }
	   log.debug("Button pressed, place = {}, sound = {}", place.get(), soundMode);
	   IrisHal.setLedState(ledState);
       IrisHal.setSounderMode(soundMode);
       
	   // Revert to persistent state in 5 seconds.
	   ExecService.once().schedule(() -> updateLEDState(), 5, TimeUnit.SECONDS );
   }
}

