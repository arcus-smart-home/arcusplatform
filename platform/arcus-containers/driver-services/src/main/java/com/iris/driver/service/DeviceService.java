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
package com.iris.driver.service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceErrors;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.reflex.ReflexRunMode;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.HubReflexCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Device;
import com.iris.messages.model.DriverId;
import com.iris.messages.model.Hub;
import com.iris.messages.services.PlatformConstants;
import com.iris.platform.model.ModelEntity;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.protocol.Protocols;
import com.iris.util.IrisAttributeLookup;

// TODO should this be merged with DriverExecutorRegistry?
@Singleton
public class DeviceService {
   private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);
   
   public static final AttributeKey<String> DEVICE_CONN_STATE_KEY = AttributeKey.create(DeviceConnectionCapability.ATTR_STATE, String.class);

   public static final AttributeKey<String> DEVICE_ADV_PROTCOL_KEY = AttributeKey.create(DeviceAdvancedCapability.ATTR_PROTOCOL, String.class);
   public static final AttributeKey<String> DEVICE_ADV_SUBPROTCOL_KEY = AttributeKey.create(DeviceAdvancedCapability.ATTR_SUBPROTOCOL, String.class);
   public static final AttributeKey<String> DEVICE_ADV_PROTOCOLID_KEY = AttributeKey.create(DeviceAdvancedCapability.ATTR_PROTOCOLID, String.class);
   
   private static final Runnable NOOP = () -> { };
   
   private final PlatformMessageBus platformBus;
   private final HubDAO hubDao;
   private final DeviceDAO deviceDao;
   private final DriverRegistry drivers;
   private final DriverExecutorRegistry registry;
   private final DeviceInitializer initializer;
   private final PlacePopulationCacheManager populationCacheMgr;
   
   private final ConcurrentMap<Address, Boolean> addsInProgress;
   
   @Inject(optional = true) @Named("driver.event.timeoutSec")
   private final long eventTimeoutSec = 10;

   @Inject
   public DeviceService(
         PlatformMessageBus platformBus,
         HubDAO hubDao,
         DeviceDAO deviceDao,
         DriverRegistry drivers,
         DriverExecutorRegistry registry,
         DeviceInitializer initializer,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      this.platformBus = platformBus;
      this.hubDao = hubDao;
      this.deviceDao = deviceDao;
      this.drivers = drivers;
      this.registry = registry;
      this.initializer = initializer;
      this.populationCacheMgr = populationCacheMgr;
      
      this.addsInProgress = new ConcurrentHashMap<>();
   }
   
   public boolean isUpgradeInProgress(Address protocolAddress) {
      return addsInProgress.get(protocolAddress) != null;
   }
   
   public Device create(CreateDeviceRequest request, AttributeMap protocolAttributes) {
      Preconditions.checkNotNull(request.getProtocolName(), "must specify a protocol name");
      Preconditions.checkNotNull(request.getProtocolId(), "must specify a protocol id");

      final Integer reflexVersion = request.getReflexVersion();

      // unspecified means connected
      boolean connected = !DeviceConnectionCapability.STATE_OFFLINE.equals(request.getConnectionState());
      logger.debug("Attempting to create device with hub id: [{}] protocol: [{}] protocol id: [{}] online? [{}]", request.getHubId(), request.getProtocolName(), request.getProtocolId(), connected);
      
      ProtocolDeviceId protocolId = request.getProtocolId();
      String hubId = request.getHubId();
      Address address = getProtocolAddress(request);
      Address protocolAddress = getProtocolAddress(request);
      if(protocolAddress == null) {
         logger.warn("Unable to determine protocol address for request [{}]", request);
         throw new ErrorEventException(Errors.invalidRequest("Invalid protocol address"));
      }

      Device existingDevice = lockByProtocolAddress(address,protocolId,hubId);
      // this try block CAN NOT start any earlier, we only release the lock if we
      // successfully acquired it
      try {
         if(existingDevice != null) {
            if(existingDevice.isTombstoned()) {
               deviceDao.delete(existingDevice);
               registry.remove(Address.fromString(existingDevice.getAddress()));
            }
            else {
               handleExistingDeviceAdd(existingDevice,protocolAttributes, reflexVersion, connected);
               return existingDevice;
            }
         }
         // if we don't have a place id or an accountId from the message, try to get it from the hub object
         if((request.getPlaceId() == null || request.getAccountId() == null) && hubId != null) {
            Hub hub = hubDao.findById(hubId);
            // TODO should hub settings always override?
            request.setAccountId(hub.getAccount());
            request.setPlaceId(hub.getPlace());
         }
         
         
         Device device = new Device();
         device.setAccount(request.getAccountId());
         device.setPlace(request.getPlaceId());
         device.setState(Device.STATE_CREATED);
         device.setProtocolAttributes(protocolAttributes);
         device.setProtocol(request.getProtocolName());
         device.setProtocolid(request.getProtocolId().getRepresentation());
         device.setProtocolAddress(protocolAddress.getRepresentation());
   
         if(Boolean.TRUE.equals(request.getMigrated())) {
            Set<String> tags = device.getTags() == null ? new HashSet<String>() : new HashSet<String>(device.getTags());
            tags.add(MessageConstants.TAG_MIGRATED);
            device.setTags(tags);
         }
   
         if(hubId != null) {
            device.setHubId(hubId);
         }
   
         // create the initial device in order to get an id & address
         device.setId(UUID.randomUUID());
         device.setAddress(Address.platformDriverAddress(device.getId()).getRepresentation());
   
         DeviceDriver driver = findDriverFor(device, reflexVersion);
         DriverExecutor e = registry.associate(device, driver, (executor) -> {
            synchronized (executor.context()) {
               doInitialize(executor);
               
               // onAdded
               sendAssociated(executor, protocolAttributes, () -> {
                  updateState(executor, driver, protocolAttributes, true);
                  
                  // do after association to get around drivers that set the name in the onAdded method (primarily sercomm cameras)
                  if(request.getName() != null) {
                     executor.context().setAttributeValue(AttributeKey.create(DeviceCapability.ATTR_NAME, String.class), request.getName());
                  }
                  
                  if(connected) {
                     sendConnected(executor, reflexVersion, NOOP);
                  } else {
                     executor.context().setDisconnected();
                     sendDisconnected(executor, reflexVersion, NOOP);
                  }
               });
            }
         });
         return ((PlatformDeviceDriverContext) e.context()).getDevice();
      }
      finally {
         addsInProgress.remove(protocolAddress);
      }
   }

   private int getReflexVersionSupportedByHub(DriverExecutor executor) {
      int maxReflexVersion = 0;
      ModelEntity hubModel = hubDao.findHubModelForPlace(executor.context().getPlaceId());
      if (hubModel != null) {
         try {
            Object value = hubModel.getAttribute(HubReflexCapability.ATTR_VERSIONSUPPORTED);
            if (value != null) {
              maxReflexVersion = (int)IrisAttributeLookup.coerce(HubReflexCapability.ATTR_VERSIONSUPPORTED, value);
            }
         } catch (Exception ex) {
            executor.context().getLogger().warn("failed to parse supported hub reflex version:", ex);
         }
      }

      return maxReflexVersion;
   }
   
   public UpgradeDriverResponse upgradeDriver(Address address) {
      DriverExecutor executor = getDriver(address);
      if(executor == null) {
         throw new ErrorEventException(Errors.notFound(address));
      }

      int maxReflexVersion = getReflexVersionSupportedByHub(executor);
      DeviceDriver driver = findDriverFor(((PlatformDeviceDriverContext) executor.context()).getDevice(), maxReflexVersion);
      boolean connected = DeviceConnectionCapability.STATE_ONLINE.equals(executor.context().getAttributeValue(DEVICE_CONN_STATE_KEY));
      return doUpgrade(executor, driver, connected);
   }

   public UpgradeDriverResponse upgradeDriver(Address address, DriverId driverId) {
      DriverExecutor executor = getDriver(address);
      if(executor == null) {
         throw new ErrorEventException(Errors.notFound(address));
      }

      DeviceDriver driver = drivers.loadDriverById(driverId);
      if(driver == null) {
         throw new ErrorEventException("NoSuchDriver", "No driver found for id " + driverId);
      }

      if(driver.getDefinition().getReflexes().getMode() != ReflexRunMode.PLATFORM) {
         int maxReflexVersion = getReflexVersionSupportedByHub(executor);
         int reqReflexVersion = driver.getDefinition().getMinimumRequiredReflexVersion();
         if (reqReflexVersion > maxReflexVersion) {
            throw new ErrorEventException(Errors.CODE_DRIVER_NOT_SUPPORTED, "The driver for id " + driverId + " requires reflex support >= " + reqReflexVersion + " but the only version " + maxReflexVersion + " is currently supported");
         }
      }

      boolean connected = DeviceConnectionCapability.STATE_ONLINE.equals(executor.context().getAttributeValue(DEVICE_CONN_STATE_KEY));
      return doUpgrade(executor, driver, connected);
   }
   
   public void lostDevice(Address address) throws Exception {
      Preconditions.checkNotNull(address);
      logger.debug("Device with address: [{}] is being marked as lost", address);
      DriverExecutor executor = getDriver(address);
      synchronized(executor.context()) {
         if(executor.context().isTombstoned()) {
            logger.debug("Received a lost message for a tombstoned device: [{}], deleting tombstone", address);
            registry.delete(address);
            return;
         }
   
         executor.context().setAttributeValue(DeviceConnectionCapability.KEY_STATUS, DeviceConnectionCapability.STATUS_LOST);
         sendDisconnected(executor, null, () -> {
            String protocolName = executor.context().getAttributeValue(AttributeKey.create(DeviceAdvancedCapability.ATTR_PROTOCOL, String.class));
            boolean isTransient = Protocols.getProtocolByName(protocolName).isTransientAddress();
            Device device = ((PlatformDeviceDriverContext) executor.context()).getDevice();
            if(isTransient){
               device.setState(Device.STATE_LOST_UNRECOVERABLE);
               // clear out the protocol index
               device.setProtocolAddress(null);
            }
            else {
               device.setState(Device.STATE_LOST_RECOVERABLE);
            }
            executor.context().commit();
         });
      }
   }
   
   public DeviceDriver findDriverFor(Device device, Integer maximumReflexVersion) {
      AttributeMap discoveryAttributes = AttributeMap.copyOf(device.getProtocolAttributes());
      discoveryAttributes.set(DEVICE_ADV_PROTCOL_KEY, device.getProtocol());
      discoveryAttributes.set(DEVICE_ADV_SUBPROTCOL_KEY, device.getSubprotocol());
      discoveryAttributes.set(DEVICE_ADV_PROTOCOLID_KEY, device.getProtocolid());

      String populationName = getPopulationFromPlace(device.getPlace());
      return drivers.findDriverFor(populationName, discoveryAttributes, maximumReflexVersion);
   }   

   public DriverExecutor getDriver(Address address) {
      return registry.loadConsumer(address);
   }

   /**
    * Decommissions the driver and deletes the device from the
    * database.
    * @param device
    * @throws Exception
    */
   public boolean delete(Device device) throws Exception {
      Preconditions.checkNotNull(device);
      if(!device.isPersisted()) {
         return false;
      }
      return registry.delete(Address.fromString(device.getAddress()));
   }

   public void tombstone(Device device) throws Exception {
      registry.tombstone(Address.fromString(device.getAddress()));
   }

   private Address getProtocolAddress(CreateDeviceRequest request){
      String protocolName = request.getProtocolName();
      ProtocolDeviceId protocolId = request.getProtocolId();
      String hubId = request.getHubId();

      Address address = hubId == null ?
            Address.protocolAddress(protocolName, protocolId) :
            Address.hubProtocolAddress(hubId, protocolName, protocolId);
      return address;
   }
   
   protected Device lockByProtocolAddress(Address address, ProtocolDeviceId protocolId, String hubId) {
            
      if(addsInProgress.putIfAbsent(address, Boolean.TRUE) != null) {
         throw new ErrorEventException(DeviceErrors.deviceExists(protocolId.getRepresentation(), hubId));
      }

      boolean success = false;
      try {
         Device existing = deviceDao.findByProtocolAddress(address.getRepresentation());
         if(existing != null) {
               return existing;
         }
         
         success = true;
         return null;
      }
      finally {
         if(!success) {
            addsInProgress.remove(address);
         }
      }
   }
   
   protected void doInitialize(DriverExecutor executor) {
      try {
         initializer.intialize(executor.context());
      }
      catch(Exception e) {
         executor.context().getLogger().warn("Error initializing driver: [{}]", initializer, e);
      }
   }

   protected UpgradeDriverResponse doUpgrade(DriverExecutor executor, DeviceDriver driver, boolean connected) {
      if(driver.getDriverId().equals(executor.driver().getDriverId())) {
         return new UpgradeDriverResponse(false, driver.getDriverId());
      }
      String oldDriverName;
      Device device;
      
      synchronized(executor.context()) {
         device = ((PlatformDeviceDriverContext) executor.context()).getDevice();
         oldDriverName = device.getDrivername(); 
         registry.associate(device, driver, (newExecutor) -> {
            Runnable afterAdded = () -> {
               updateState(newExecutor, driver, device.getProtocolAttributes(), false);
               if(!connected) {
                  newExecutor.context().setDisconnected();
                  sendDisconnected(newExecutor, null, NOOP);
                  sendResyncToHub(newExecutor);
               } else {
                  // don't generate a connected event, but trigger the closure
                  newExecutor.context().setConnected();
                  sendConnected(newExecutor, null, NOOP);
                  sendResyncToHub(newExecutor);
               }
            };
            
            synchronized(newExecutor.context()) {
               if(!Objects.equals(oldDriverName, driver.getDriverId().getName())) {
                  logger.info("Device {} is changing drivers", device.getAddress());
                  // FIXME should clear all the variables and attributes in this case
                  sendAssociated(newExecutor, device.getProtocolAttributes(), afterAdded);
               }
               else {
                  afterAdded.run();
               }
            }
         });
      }
      
      return new UpgradeDriverResponse(true, driver.getDriverId());
   }

   protected void updateState(DriverExecutor executor, DeviceDriver driver, AttributeMap protocolAttributes, boolean create) {
      Device device = ((PlatformDeviceDriverContext) executor.context()).getDevice();
      if(!device.isActive()) {
         // the driver did not manually set itself to one of the active states
         // this is inherited behavior, go ahead and assume provisioning succeeded
         if(isFallback(driver)) {
            device.setState(Device.STATE_ACTIVE_UNSUPPORTED);
         }
         else {
            device.setState(Device.STATE_ACTIVE_SUPPORTED);
         }
      }
      if(create) {
         executor.context().create();
      } else {
         executor.context().commit();
      }
   }

   public void sendResyncToHub(DriverExecutor exec) {
      Hub hub = hubDao.findHubForPlace(exec.context().getPlaceId());
      if (hub == null) {
         return;
      }

      MessageBody sync = HubReflexCapability.SyncNeededEvent.instance();
      Address addr = Address.platformService(PlatformConstants.SERVICE_DEVICES);
      Address dst = Address.hubService(hub.getId(), PlatformConstants.SERVICE_HUB);
      platformBus.send(
         PlatformMessage.buildEvent(sync, addr)
            .to(dst)
            .withPlaceId(exec.context().getPlaceId())
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(exec.context().getPlaceId()))
            .create()
      );
   }

   protected void sendEvent(
         DriverExecutor executor, 
         DriverEvent event, 
         Runnable listener, 
         String errorMessage
   ) {
      final ListenableFuture<?> result = executor.fire(event);
      result.addListener(
            () -> {
               try {
                  result.get();
               }
               catch(Exception e) {
                  executor.context().getLogger().warn(errorMessage, e);
               }
               listener.run();
            },
            MoreExecutors.directExecutor()
      );
   }
   
   protected void sendAssociated(DriverExecutor executor, AttributeMap protocolAttributes, Runnable listener) {
      sendEvent(
            executor,
            DriverEvent.createAssociated(protocolAttributes),
            listener,
            "Error associating device with driver"
      );
   }

   protected void sendConnected(DriverExecutor executor, Integer reflexVersion, Runnable listener) {
      sendEvent(
         executor,
         DriverEvent.createConnected(reflexVersion),
         listener,
         "Error notifying driver of connected event"
      );
   }

   protected void sendDisconnected(DriverExecutor executor, Integer reflexVersion, Runnable listener) {
      sendEvent(
            executor,
            DriverEvent.createDisconnected(reflexVersion),
            listener,
            "Error notifying driver of disconnected event"
      );
   }

   protected void sendDisassociated(DriverExecutor executor, Runnable listener) {
      sendEvent(
            executor,
            DriverEvent.createDisassociated(),
            listener,
            "Error disassociating driver from device"
      );
   }

   private Device handleExistingDeviceAdd(Device device, AttributeMap protocolAttributes, Integer reflexVersion, boolean connected) {
      DriverExecutor executor = getDriver(Address.fromString(device.getAddress()));
      
      synchronized(executor.context()) {
         if(isFallback(executor.driver())) {
            logger.info("Received an add for [{}] with a fallback driver, attempting to discover a better driver", device.getProtocolAddress());
            device.setProtocolAttributes(protocolAttributes);
            DeviceDriver driver = findDriverFor(device, reflexVersion);
            if(isFallback(driver)) {
               logger.warn("Unable to find a better driver for [{}]", device.getProtocolAddress());
               // drop through in case this is a Lost Fallback device, strange but possible
            }
            else {
               logger.info("Attempting to upgrade [{}] from the fallback driver to [{}]", device.getProtocolAddress(), driver.getDriverId());
               doUpgrade(executor, driver, connected);
               return device;
            }
         }
         
         logger.info("Attempting to re-add an existing device [{}]", device.getProtocolAddress());
         // ONLINE really means healthy in this case, not necessarilly ONLINE
         executor.context().setAttributeValue(DeviceConnectionCapability.KEY_STATUS, DeviceConnectionCapability.STATUS_ONLINE);
         sendAssociated(executor, protocolAttributes, () -> {
            if(connected) {
               executor.context().setConnected();
               sendConnected(executor, reflexVersion, NOOP);
            } else {
               executor.context().setDisconnected();
               sendDisconnected(executor, reflexVersion, NOOP);
            }
         });
      }
      
      return device;
   }
   
   private boolean isFallback(DeviceDriver driver) {
      return driver.getDefinition().getName().contains("Fallback");
   }
   
   private String getPopulationFromPlace(UUID placeId) {
   	return populationCacheMgr.getPopulationByPlaceId(placeId);
   }

   public static final class CreateDeviceRequest {
      String protocolName;
      ProtocolDeviceId protocolId;
      String hubId;
      UUID placeId;
      UUID accountId;
      String name;
      String connectionState;
      Boolean migrated;
      Integer reflexVersion;

      public CreateDeviceRequest() {

      }

      public CreateDeviceRequest(MessageBody message) {
         // calling setters from the constructor is safe because this class is final
         setProtocolName((String) message.getAttributes().get(MessageConstants.ATTR_PROTOCOLNAME));
         setProtocolId((String) message.getAttributes().get(MessageConstants.ATTR_DEVICEID));
         setHubId((String) message.getAttributes().get(MessageConstants.ATTR_HUBID));
         setAccountId((String) message.getAttributes().get(MessageConstants.ATTR_ACCOUNTID));
         setPlaceId((String) message.getAttributes().get(MessageConstants.ATTR_PLACEID));
         setName((String) message.getAttributes().get(DeviceCapability.ATTR_NAME));
         setConnectionState((String) message.getAttributes().get(DeviceConnectionCapability.ATTR_STATE));
         setMigrated((Boolean) message.getAttributes().get(MessageConstants.ATTR_MIGRATED));

         Number rflxVersion = (Number) message.getAttributes().get(MessageConstants.ATTR_REFLEXVERSION);
         int rflx = (rflxVersion != null) ? rflxVersion.intValue() : 0;
         setReflexVersion(rflx);
      }

      /**
       * @return the reflexVersion
       */
      public Integer getReflexVersion() {
         return reflexVersion;
      }

      /**
       * @param reflexVersion the reflexVersion to set
       */
      public void setReflexVersion(Integer reflexVersion) {
         this.reflexVersion = reflexVersion;
      }

      /**
       * @return the protocolName
       */
      public String getProtocolName() {
         return protocolName;
      }

      /**
       * @param protocolName the protocolName to set
       */
      public void setProtocolName(String protocolName) {
         this.protocolName = protocolName;
      }

      /**
       * @return the protocolId
       */
      public ProtocolDeviceId getProtocolId() {
         return protocolId;
      }

      /**
       * @param protocolId the protocolId to set
       */
      public void setProtocolId(ProtocolDeviceId protocolId) {
         this.protocolId = protocolId;
      }

      public void setProtocolId(String protocolId) {
         if(protocolId == null) {
            this.protocolId = null;
         }
         else {
            this.protocolId = ProtocolDeviceId.fromRepresentation(protocolId);
         }
      }

      /**
       * @return the hubId
       */
      public String getHubId() {
         return hubId;
      }

      /**
       * @param hubId the hubId to set
       */
      public void setHubId(String hubId) {
         this.hubId = hubId;
      }

      /**
       * @return the placeId
       */
      public UUID getPlaceId() {
         return placeId;
      }

      /**
       * @param placeId the placeId to set
       */
      public void setPlaceId(UUID placeId) {
         this.placeId = placeId;
      }

      public void setPlaceId(String placeId) {
         if(placeId == null) {
            this.placeId = null;
         }
         else {
            this.placeId = UUID.fromString(placeId);
         }
      }

      /**
       * @return the accountId
       */
      public UUID getAccountId() {
         return accountId;
      }

      /**
       * @param accountId the accountId to set
       */
      public void setAccountId(UUID accountId) {
         this.accountId = accountId;
      }

      public void setAccountId(String accountId) {
         if(accountId == null) {
            this.accountId = null;
         }
         else {
            this.accountId = UUID.fromString(accountId);
         }
      }

      public String getName() {
         return name;
      }

      public void setName(String name) {
         this.name = name;
      }

      public String getConnectionState() {
         return connectionState;
      }

      public void setConnectionState(String connectionState) {
         this.connectionState = connectionState;
      }

      public Boolean getMigrated() {
         return migrated;
      }

      public void setMigrated(Boolean migrated) {
         this.migrated = migrated;
      }

      @Override
      public String toString() {
         return "CreateDeviceRequest [protocolName=" + protocolName
               + ", protocolId=" + protocolId + ", hubId=" + hubId
               + ", placeId=" + placeId + ", accountId=" + accountId
               + ", name=" + name + ", connectionState=" + connectionState
               + ", migrated=" + migrated
               + ", reflexVersion=" + reflexVersion + "]";
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result
               + ((accountId == null) ? 0 : accountId.hashCode());
         result = prime * result
               + ((connectionState == null) ? 0 : connectionState.hashCode());
         result = prime * result + ((hubId == null) ? 0 : hubId.hashCode());
         result = prime * result
               + ((migrated == null) ? 0 : migrated.hashCode());
         result = prime * result + ((name == null) ? 0 : name.hashCode());
         result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
         result = prime * result
               + ((protocolId == null) ? 0 : protocolId.hashCode());
         result = prime * result
               + ((protocolName == null) ? 0 : protocolName.hashCode());
         result = prime * result
               + ((reflexVersion == null) ? 0 : reflexVersion.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         CreateDeviceRequest other = (CreateDeviceRequest) obj;
         if (accountId == null) {
            if (other.accountId != null)
               return false;
         } else if (!accountId.equals(other.accountId))
            return false;
         if (connectionState == null) {
            if (other.connectionState != null)
               return false;
         } else if (!connectionState.equals(other.connectionState))
            return false;
         if (hubId == null) {
            if (other.hubId != null)
               return false;
         } else if (!hubId.equals(other.hubId))
            return false;
         if (migrated == null) {
            if (other.migrated != null)
               return false;
         } else if (!migrated.equals(other.migrated))
            return false;
         if (name == null) {
            if (other.name != null)
               return false;
         } else if (!name.equals(other.name))
            return false;
         if (placeId == null) {
            if (other.placeId != null)
               return false;
         } else if (!placeId.equals(other.placeId))
            return false;
         if (protocolId == null) {
            if (other.protocolId != null)
               return false;
         } else if (!protocolId.equals(other.protocolId))
            return false;
         if (protocolName == null) {
            if (other.protocolName != null)
               return false;
         } else if (!protocolName.equals(other.protocolName))
            return false;
         if (reflexVersion == null) {
            if (other.reflexVersion != null)
               return false;
         } else if (!reflexVersion.equals(other.reflexVersion))
            return false;
         return true;
      }

   }

   public static class UpgradeDriverResponse {
      private boolean upgraded;
      private DriverId driverId;

      public UpgradeDriverResponse() {

      }

      public UpgradeDriverResponse(boolean upgraded, DriverId driverId) {
         this.upgraded = upgraded;
         this.driverId = driverId;
      }

      /**
       * @return the upgraded
       */
      public boolean isUpgraded() {
         return upgraded;
      }

      /**
       * @param upgraded the upgraded to set
       */
      public void setUpgraded(boolean upgraded) {
         this.upgraded = upgraded;
      }

      /**
       * @return the driverId
       */
      public DriverId getDriverId() {
         return driverId;
      }

      /**
       * @param driverId the driverId to set
       */
      public void setDriverId(DriverId driverId) {
         this.driverId = driverId;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString() {
         return "UpgradeDriverResponse [upgraded=" + upgraded + ", driverId="
               + driverId + "]";
      }

      /* (non-Javadoc)
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result
               + ((driverId == null) ? 0 : driverId.hashCode());
         result = prime * result + (upgraded ? 1231 : 1237);
         return result;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if (this == obj) return true;
         if (obj == null) return false;
         if (getClass() != obj.getClass()) return false;
         UpgradeDriverResponse other = (UpgradeDriverResponse) obj;
         if (driverId == null) {
            if (other.driverId != null) return false;
         }
         else if (!driverId.equals(other.driverId)) return false;
         if (upgraded != other.upgraded) return false;
         return true;
      }

   }

}

