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
package com.iris.platform.services.ipcd.registry;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.protocol.ProtocolMessageBus;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.core.protocol.ipcd.exceptions.DeviceNotFoundException;
import com.iris.core.protocol.ipcd.exceptions.PlaceMismatchException;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Device;
import com.iris.messages.model.Place;
import com.iris.messages.service.BridgeService;
import com.iris.messages.services.PlatformConstants;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.partition.PartitionChangedEvent;
import com.iris.platform.partition.PartitionListener;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.PlatformPartition;
import com.iris.platform.services.ipcd.IpcdService;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.control.ControlProtocol;
import com.iris.protocol.control.DeviceOfflineEvent;
import com.iris.protocol.ipcd.IpcdDevice;
import com.iris.protocol.ipcd.IpcdDevice.ConnState;
import com.iris.protocol.ipcd.IpcdDeviceTypeRegistry;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.util.ThreadPoolBuilder;
import com.netflix.governator.annotations.WarmUp;

@Singleton
public class IpcdRegistry implements PartitionListener {

   private static final Logger logger = LoggerFactory.getLogger(IpcdRegistry.class);

   private static final Address ADDRESS = Address.platformService(com.iris.messages.service.IpcdService.NAMESPACE);
   private static final Address BRIDGES = Address.bridgeAddress(IpcdProtocol.NAMESPACE);

   private final IpcdRegistryConfig config;
   private final Executor executor;
   private final Clock clock;
   private final PlaceDAO placeDao;
   private final IpcdDeviceDao ipcdDeviceDao;
   private final DeviceDAO deviceDao;
   private final Partitioner partitioner;
   private final PlatformBusClient busClient;
   private final ProtocolMessageBus protocolBus;
   private final ScheduledExecutorService watchDogExecutor;
   private final ConcurrentMap<String, IpcdDeviceState> ipcdDevices = new ConcurrentHashMap<>();
   private final long offlineTimeoutMs;
   private final long timeoutIntervalMs;
   private final IpcdRegistryMetrics metrics;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public IpcdRegistry(
         IpcdRegistryConfig config,
         @Named(IpcdService.PROP_THREADPOOL) Executor executor,
         Clock clock,
         PlaceDAO placeDao,
         IpcdDeviceDao ipcdDeviceDao,
         DeviceDAO deviceDao,
         PlatformBusClient busClient,
         ProtocolMessageBus protocolBus,
         Partitioner partitioner,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      this.config = config;
      this.executor = executor;
      this.clock = clock;
      this.placeDao = placeDao;
      this.ipcdDeviceDao = ipcdDeviceDao;
      this.deviceDao = deviceDao;
      this.busClient = busClient;
      this.protocolBus = protocolBus;
      this.partitioner = partitioner;
      this.populationCacheMgr = populationCacheMgr;
      this.watchDogExecutor = ThreadPoolBuilder.newSingleThreadedScheduler("ipcd-heartbeat-watchdog");
      offlineTimeoutMs = TimeUnit.MILLISECONDS.convert(config.getOfflineTimeoutMin(), TimeUnit.MINUTES);
      timeoutIntervalMs = TimeUnit.MILLISECONDS.convert(config.getTimeoutIntervalSec(), TimeUnit.SECONDS);
      this.metrics = new IpcdRegistryMetrics(IrisMetrics.metrics("service.platform.ipcdregistry"));
   }

   @WarmUp
   public void start() {
      partitioner.addPartitionListener(this);
      watchDogExecutor.scheduleWithFixedDelay(() -> timeout(), timeoutIntervalMs, timeoutIntervalMs, TimeUnit.MILLISECONDS);
   }

   @PreDestroy
   public void stop() {
      watchDogExecutor.shutdownNow();
   }

   @Override
   public void onPartitionsChanged(PartitionChangedEvent event) {
      removeDevicesFromOldPartitions(event.getRemovedPartitions());
      addDevicesFromNewPartitions(event.getAddedPartitions());
   }

   public MessageBody registerDevice(MessageBody body, String placeId) {
      logger.trace("handling register request [{}] @ [{}]", body, placeId);

      Place place = loadPlace(BridgeService.RegisterDeviceRequest.NAME, placeId);
      List<com.iris.protocol.ipcd.message.model.Device> devices = createDevicesFromBody(body);

      String protocolAddress = null;
      DeviceNotFoundException lastDnfe = null;
      PlaceMismatchException lastPme = null;
      com.iris.protocol.ipcd.message.model.Device claimedDevice = null;

      boolean alreadyPairedAtPlace = false;
      for (com.iris.protocol.ipcd.message.model.Device device : devices) {
         try {
            protocolAddress = ipcdDeviceDao.claimAndGetProtocolAddress(device, place.getAccount(), place.getId());
            if (null != protocolAddress) {
               claimedDevice = device;
               break;
            }
         } catch (DeviceNotFoundException dnfe) {
            logger.debug("[{}:{}:{}]:  failed to claim, no record exists", device.getVendor(), device.getModel(), device.getSn(), dnfe);
            lastDnfe = dnfe;
         } catch (PlaceMismatchException pme) {
            if (!Objects.equals(place.getId(), pme.getActualPlaceId())) {
               logger.debug("[{}:{}:{}]:  failed to claim ipcd device @ [{}], it has already been claimed by [{}]", device.getVendor(), device.getModel(), device.getSn(), place.getId(), pme.getActualPlaceId(), pme);
               lastPme = pme;
            } else {
               logger.debug("[{}:{}:{}]:  has already been claimed by [{}] continuing with registration", device.getVendor(), device.getModel(), device.getSn(), place.getId());
               alreadyPairedAtPlace = true;
               claimedDevice = device;
               protocolAddress = IpcdProtocol.ipcdAddress(claimedDevice).getRepresentation();
               break;
            }
         }
      }

      if (null == claimedDevice) {
         if (null != lastDnfe) {
            return Errors.fromCode(Errors.CODE_NOT_FOUND, "The ipcd device with serial number " + devices.get(0).getSn() + " could not be detected. Is it online?");
         } else if (null != lastPme) {
            return Errors.invalidRequest("The ipcd device with device id " + devices.get(0).getSn() + " is already registered. If you feel this is in error, please contact technical support.");
         }

         throw new ErrorEventException(Errors.invalidRequest("The device failed to be claimed in an unexpected fashion. Claimed device was null."));
      }

      updatePartition(protocolAddress, place.getId());
      emitClaimed(protocolAddress, place.getAccount(), place.getId(), place.getPopulation());
      issueAddDevice(claimedDevice, protocolAddress, place);
      return BridgeService.RegisterDeviceResponse.builder().withAlreadyAddedAtPlace(alreadyPairedAtPlace).build();
   }

   public MessageBody unregisterDevice(MessageBody body, String placeId) {
      logger.trace("handling unregister request [{}] @ [{}]", body, placeId);

      if(StringUtils.isBlank(placeId)) {
         return Errors.invalidRequest(BridgeService.RemoveDeviceRequest.NAME + " did not have a place id");
      }
      UUID placeUuid = UUID.fromString(placeId);
      String protocolAddress = BridgeService.RemoveDeviceRequest.getId(body);
      IpcdDevice device = ipcdDeviceDao.findByProtocolAddress(protocolAddress);

      try {
         ipcdDeviceDao.delete(protocolAddress, placeUuid);
         emitUnregistered(protocolAddress, placeUuid);
         updatePartitionId(protocolAddress, 0);
         busClient.sendEvent(createRemovedEvent(device));
         return BridgeService.RemoveDeviceResponse.instance();
      } catch(DeviceNotFoundException dnfe) {
         return Errors.fromCode(Errors.CODE_NOT_FOUND, protocolAddress + " could not be found");
      } catch(PlaceMismatchException pme) {
         return Errors.invalidRequest(protocolAddress + " must be unregistered from " + placeId);
      }
   }

   public void forceUnregister(IpcdDevice ipcdDevice) {
      ipcdDeviceDao.delete(ipcdDevice);
      emitUnregistered(ipcdDevice.getProtocolAddress(), ipcdDevice.getPlaceId());
      updatePartitionId(ipcdDevice.getProtocolAddress(), 0);
   }

   public void onEvent(PlatformMessage message) {
      busClient.onEvent(message);
   }

   public void onErrorEvent(PlatformMessage message) {
      busClient.onErrorEvent(message);
   }

   public void onConnected(PlatformMessage message) {
      MessageBody body = message.getValue();
      String protocolAddress = com.iris.messages.service.IpcdService.DeviceConnectedEvent.getProtocolAddress(body);
      if(protocolAddress == null) {
         logger.debug("ignoring ipcd connected event with no protocol address");
         return;
      }
      Address actor = message.getActor();
      if(actor == null || actor.getId() == null) {
         logger.debug("ignoring ipcd connected event with no ipcd bridge actor");
         return;
      }
      logger.debug("[{}]:  handling online event: [{}]", protocolAddress, message);
      online(protocolAddress, partitioner.getPartitionForMessage(message).getId(), (String) actor.getId());
   }

   public void onHeartBeat(PlatformMessage message) {
      MessageBody body = message.getValue();
      logger.debug("handling heartbeat message: [{}]", body);
      Integer partitionId = com.iris.messages.service.IpcdService.DeviceHeartBeatEvent.getPartitionId(body);
      if(partitionId == null) {
         logger.warn("heartbeat did not contain a partition id, ignoring");
         return;
      }
      Set<String> protocolAddresses = com.iris.messages.service.IpcdService.DeviceHeartBeatEvent.getConnectedDevices(body);
      String bridgeId = (String) message.getSource().getId();
      for(String protocolAddress : protocolAddresses) {
         online(protocolAddress, partitionId, bridgeId);
      }
   }

   private void timeout() {
      logger.info("Checking for expired ipcd devices");
      long expirationTime = clock.millis() - offlineTimeoutMs;
      for(IpcdDeviceState state: ipcdDevices.values()) {
         if(state.lastHeartbeat < expirationTime) {
            if(ipcdDevices.remove(state.getProtocolAddress(), state)) {
               try {
                  onTimeout(state.getProtocolAddress());
               }
               catch(Exception e) {
                  logger.warn("Error sending timeout for [{}]", state.getProtocolAddress(), e);
               }
            }
         }
      }
   }

   private void onTimeout(String protocolAddress) {
      logger.debug("[{}]:  timed out", protocolAddress);
      metrics.onTimeout();
      ipcdDeviceDao.offline(protocolAddress);
      emitOffline(protocolAddress);
   }

   private void removeDevicesFromOldPartitions(Set<Integer> removedPartitions) {
      if(removedPartitions.isEmpty()) {
         return;
      }

      Iterator<IpcdDeviceState> it = ipcdDevices.values().iterator();
      while(it.hasNext()) {
         IpcdDeviceState state = it.next();
         int partitionId = state.getPartitionId();
         if(removedPartitions.contains(partitionId)) {
            it.remove();
         }
      }
   }

   private void addDevicesFromNewPartitions(Set<Integer> addedPartitions) {
      if(addedPartitions == null) {
         return;
      }

      ForkJoinPool pool = ForkJoinPool.commonPool();
      List<ForkJoinTask<?>> results = new ArrayList<>(addedPartitions.size());
      long ts = clock.millis();
      logger.trace("Initializing ipcd registry");
      for(Integer partitionId: addedPartitions) {
         ForkJoinTask<?> result = pool.submit(() -> {
            logger.trace("Loading ipcd devices for partition [{}]...", partitionId);
            ipcdDeviceDao
               .streamByPartitionId(partitionId)
               .forEach((ipcdDevice) -> onLoadIpcdDevice(ipcdDevice, partitionId, ts));
         });
         results.add(result);
      }
      for(ForkJoinTask<?> result: results) {
         try {
            result.join();
         }
         catch(Exception e) {
            logger.warn("Error loading ipcd devices", e);
         }
      }
      logger.debug("Ipcd registry loaded");
   }

   private void onLoadIpcdDevice(IpcdDevice ipcdDevice, int partitionId, long ts) {
      if(ConnState.OFFLINE == ipcdDevice.getConnState()) {
         // skip this
      } else {
         IpcdDeviceState newState = new IpcdDeviceState(ipcdDevice.getProtocolAddress(), partitionId, ts);
         IpcdDeviceState existingState = ipcdDevices.putIfAbsent(ipcdDevice.getProtocolAddress(), newState);
         if(existingState == null) {
            existingState = newState;
         }

         // handle the case where registration was partial due to platform services being restarted during registration flow
         if(ipcdDevice.getRegistrationState() == IpcdDevice.RegistrationState.PENDING_DRIVER) {
            logger.debug("[{}]:   ipcd device loaded from database is pending a driver, checking state of device table", ipcdDevice.getProtocolAddress());
            Place p = placeDao.findById(ipcdDevice.getPlaceId());
            if(p == null || !syncDevice(ipcdDevice.getProtocolAddress(), p)) {
               clearRegistration(ipcdDevice.getProtocolAddress(), ipcdDevice.getPlaceId());
            }
         }
      }
   }

   private PlatformMessage createRemovedEvent(IpcdDevice device) {
      MessageBody body = DeviceAdvancedCapability.RemovedDeviceEvent.builder()
            .withProtocol(IpcdProtocol.NAMESPACE)
            .withProtocolId(IpcdProtocol.ipcdProtocolId(device.getDevice()).getRepresentation())
            .withAccountId(String.valueOf(device.getAccountId()))
            .build();
      return PlatformMessage.buildEvent(body, IpcdProtocol.ipcdAddress(device.getDevice()))
            .withPlaceId(device.getPlaceId())
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(device.getPlaceId()))
            .create();
   }

   //WDS - Since A.O. Smith B2.00, multiple devices can have the same v1 device type attribute.  This method now returns all the
   //possible mappings from it.  See: https://eyeris.atlassian.net/browse/I2-761
   private List<com.iris.protocol.ipcd.message.model.Device> createDevicesFromBody(MessageBody body) {
      Map<String, String> attrs = readAttrs(body);

      String sn = attrs.get(IpcdProtocol.ATTR_SN);
      if(StringUtils.isBlank(sn)) {
         throw new ErrorEventException(Errors.invalidRequest(BridgeService.RegisterDeviceRequest.ATTR_ATTRS + " must contain " + IpcdProtocol.ATTR_SN));
      }
      sn = StringUtils.upperCase(sn);

      String devType = attrs.get(IpcdProtocol.ATTR_V1DEVICETYPE);
      List<com.iris.protocol.ipcd.message.model.Device> devices = IpcdDeviceTypeRegistry.INSTANCE.createDeviceForV1Type(devType, sn);
      if (null == devices || devices.isEmpty()) {
         throw new ErrorEventException(Errors.invalidRequest("The v1 device type [" + devType + "] and serial number [" + sn + "] failed to create any devices."));
      }

      return devices;
   }

   private void issueAddDevice(com.iris.protocol.ipcd.message.model.Device d, String protocolAddress, Place place) {
      ListenableFuture<PlatformMessage> future = busClient.request(createAddDevice(d, protocolAddress, place), config.getAddDeviceTimeoutSecs());
      Futures.addCallback(future, new FutureCallback<PlatformMessage>() {
         @Override
         public void onSuccess(PlatformMessage result) {
            completeRegistration(protocolAddress, place, result);
         }
         @Override
         public void onFailure(Throwable t) {
            failRegistration(protocolAddress, place, t);
         }
      }, executor);
   }

   private void completeRegistration(String protocolAddress, Place place, PlatformMessage msg) {
      MessageBody body = msg.getValue();
      logger.trace("[{}]:  handling add device response [{}]", protocolAddress, body);
      String driverAddress = (String) body.getAttributes().get(MessageConstants.ATTR_DEVICEADDRESS);

      try {
         ipcdDeviceDao.completeRegistration(protocolAddress, place.getId(), driverAddress);
         updatePartition(protocolAddress, place.getId());
         emitRegistered(protocolAddress, place.getAccount(), place.getId(), place.getPopulation(), driverAddress);
      } catch(DeviceNotFoundException dnfe) {
         logger.warn("[{}]:  failed to complete registration, no record exists", protocolAddress);
      } catch(PlaceMismatchException pme) {
         logger.warn("[{}]:  failed to complete registration, place [{}] does not match claimed address of [{}]", protocolAddress, place.getId(), pme.getActualPlaceId());
         updatePartition(protocolAddress, pme.getActualPlaceId());
      }
   }

   private void failRegistration(String protocolAddress, Place place, Throwable t) {
      logger.warn("[{}]:  add device request for failed", protocolAddress, t);
      if(!syncDevice(protocolAddress, place)) {
         logger.warn("[{}]:  device did not exist, clearing registration", protocolAddress);
         clearRegistration(protocolAddress, place.getId());
      }
   }

   private void clearRegistration(String protocolAddress, UUID placeId) {
      try {
         ipcdDeviceDao.clearRegistration(protocolAddress, placeId);
         emitUnregistered(protocolAddress, placeId);
         updatePartitionId(protocolAddress, 0);
      } catch(DeviceNotFoundException dnfe) {
         logger.warn("[{}]:  failed to unregister, no record exists", protocolAddress);
      } catch(PlaceMismatchException pme) {
         logger.warn("[{}]:  failed to unregister, place [{}] does not match claimed address of [{}]", protocolAddress, placeId, pme.getActualPlaceId());
         updatePartition(protocolAddress, pme.getActualPlaceId());
      }
   }

   private boolean syncDevice(String protocolAddress, Place place) {
      Device d = deviceDao.findByProtocolAddress(protocolAddress);
      if(d == null) {
         return false;
      }
      if(Objects.equals(place.getId(), d.getPlace())) {
         logger.debug("[{}]: had been created with the appropriate place, completing registration", protocolAddress);
         ipcdDeviceDao.completeRegistration(protocolAddress, place.getId(), d.getAddress());
         emitRegistered(protocolAddress, place.getAccount(), place.getId(), place.getPopulation(), d.getAddress());
         updatePartition(protocolAddress, place.getId());
      } else {
         logger.warn("[{}]: has been registered to [{}] instead of [{}], forcing state update", protocolAddress, d.getPlace(), place.getId());
         ipcdDeviceDao.forceRegistration(protocolAddress, d.getAccount(), d.getPlace(), d.getAddress());
         emitRegistered(protocolAddress, d.getAccount(), d.getPlace(), place.getPopulation(), d.getAddress());
         updatePartition(protocolAddress, d.getPlace());
      }
      return true;
   }

   private PlatformMessage createAddDevice(com.iris.protocol.ipcd.message.model.Device d, String protocolAddress, Place place) {
      Map<String, Object> attrs = new HashMap<>();
      attrs.put(MessageConstants.ATTR_PROTOCOLNAME, IpcdProtocol.NAMESPACE);
      attrs.put(MessageConstants.ATTR_ACCOUNTID, String.valueOf(place.getAccount()));
      attrs.put(MessageConstants.ATTR_PLACEID, String.valueOf(place.getId()));
      attrs.put(MessageConstants.ATTR_DEVICEID, IpcdProtocol.ipcdProtocolId(d).getRepresentation());

      AttributeMap attrMap = AttributeMap.newMap();
      attrMap.set(AttributeKey.create(IpcdProtocol.ATTR_VENDOR, String.class), d.getVendor());
      attrMap.set(AttributeKey.create(IpcdProtocol.ATTR_MODEL, String.class), d.getModel());
      attrMap.set(AttributeKey.create(IpcdProtocol.ATTR_SN, String.class), d.getSn());
      attrs.put(MessageConstants.ATTR_PROTOCOLATTRIBUTES, attrMap);

      MessageBody body = MessageBody.buildMessage(MessageConstants.MSG_ADD_DEVICE_REQUEST, attrs);
      return PlatformMessage.buildRequest(body, Address.bridgeAddress(IpcdProtocol.NAMESPACE), Address.platformService(PlatformConstants.SERVICE_DEVICES))
            .withCorrelationId(UUID.randomUUID().toString())
            .withPlaceId(place.getId())
            .withPopulation(place.getPopulation())
            .isRequestMessage(true)
            .create();
   }

   private void emitOffline(String protocolAddress) {
      IpcdDevice ipcdDevice = ipcdDeviceDao.findByProtocolAddress(protocolAddress);
      if(ipcdDevice == null || ipcdDevice.getPlaceId() == null) {
         return;
      }
      ProtocolMessage protocolMessage = ProtocolMessage.buildProtocolMessage(Address.fromString(protocolAddress), Address.broadcastAddress(), ControlProtocol.INSTANCE, DeviceOfflineEvent.create())
            .withPlaceId(ipcdDevice.getPlaceId())
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(ipcdDevice.getPlaceId()))
            .create();
      protocolBus.send(protocolMessage);
   }

   private void emitRegistered(String protocolAddress, UUID accountId, UUID placeId, String population, String driverAddress) {
      emitEvent(com.iris.messages.service.IpcdService.DeviceRegisteredEvent.builder()
            .withAccountId(String.valueOf(accountId))
            .withDriverAddress(driverAddress)
            .withPlaceId(String.valueOf(placeId))
            .withProtocolAddress(protocolAddress)
            .build(), placeId, population);
   }

   private void emitUnregistered(String protocolAddress, UUID placeId) {
      emitEvent(com.iris.messages.service.IpcdService.DeviceUnregisteredEvent.builder()
            .withProtocolAddress(protocolAddress)
            .build(), placeId, populationCacheMgr.getPopulationByPlaceId(placeId));
   }

   private void emitClaimed(String protocolAddress, UUID accountId, UUID placeId, String population) {
      emitEvent(com.iris.messages.service.IpcdService.DeviceClaimedEvent.builder()
            .withAccountId(String.valueOf(accountId))
            .withPlaceId(String.valueOf(placeId))
            .withProtocolAddress(protocolAddress)
            .build(), placeId, population);
   }

   private void emitEvent(MessageBody body, UUID placeId, String population) {
      PlatformMessage msg = PlatformMessage.buildMessage(body, ADDRESS, BRIDGES)
            .withPlaceId(placeId)
            .withPopulation(population)
            .create();
      busClient.sendEvent(msg);
   }

   private boolean isOnline(String protocolAddress) {
      return ipcdDevices.get(protocolAddress) != null;
   }

   private void online(String protocolAddress, int partitionId, String ipcdBridge) {
      ipcdDevices.computeIfAbsent(protocolAddress, (s) -> new IpcdDeviceState(protocolAddress, partitionId))
         .updateHeartbeat(ipcdBridge, clock.millis());
   }

   private void offline(String protocolAddress, String ipcdBridge) {
      IpcdDeviceState ipcdDevice = ipcdDevices.get(protocolAddress);
      if(ipcdDevice == null) {
         return;
      }
      if(ipcdDevice.offline(ipcdBridge, clock.millis() - offlineTimeoutMs) && ipcdDevices.remove(protocolAddress, ipcdDevice)) {
         logger.debug("[{}]:  IPCD device disconnected", protocolAddress);
         metrics.onDisconnected();
      }
   }

   private Map<String, String> readAttrs(MessageBody body) {
      Map<String, String> attrs = BridgeService.RegisterDeviceRequest.getAttrs(body);
      if(attrs == null || attrs.isEmpty()) {
         throw new ErrorEventException(Errors.missingParam(BridgeService.RegisterDeviceRequest.ATTR_ATTRS));
      }
      return attrs;
   }

   private Place loadPlace(String requestName, UUID placeId) {
      if(placeId == null) {
         throw new ErrorEventException(Errors.invalidRequest(requestName + " did not have a place id"));
      }
      Place place = placeDao.findById(placeId);
      if(place == null) {
         throw new ErrorEventException(Errors.notFound(Address.platformService(placeId, PlaceCapability.NAMESPACE)));
      }
      return place;
   }

   private Place loadPlace(String requestName, String placeId) {
      if(StringUtils.isBlank(placeId)) {
         throw new ErrorEventException(Errors.invalidRequest(requestName + " did not have a place id"));
      }
      return loadPlace(requestName, UUID.fromString(placeId));
   }

   private void updatePartition(String protocolAddress, UUID placeId) {
      PlatformPartition partition = partitioner.getPartitionForPlaceId(placeId);
      if(partition != null) {
         updatePartitionId(protocolAddress, partition.getId());
      }
   }

   private void updatePartitionId(String protocolAddress, int partitionId) {
      IpcdDeviceState state = ipcdDevices.get(protocolAddress);
      if(state != null) {
         state.setPartitionId(partitionId);
      }
   }

   private int getOnlineDevices() {
      return ipcdDevices.size();
   }

   private static class IpcdDeviceState {

      private final String protocolAddress;
      private volatile int partitionId;
      private volatile long lastHeartbeat;
      private final Map<String, Long> heartbeats;

      IpcdDeviceState(String protocolAddress, int partitionId) {
         this.protocolAddress = protocolAddress;
         this.partitionId = partitionId;
         this.heartbeats = new HashMap<>(4);
      }

      IpcdDeviceState(String protocolAddress, int partitionId, long ts) {
         this(protocolAddress, partitionId);
         this.lastHeartbeat = ts;
      }

      String getProtocolAddress() {
         return protocolAddress;
      }

      int getPartitionId() {
         return partitionId;
      }

      void setPartitionId(int id) {
         this.partitionId = partitionId;
      }

      void updateHeartbeat(String ipcdBridge, long ts) {
         this.lastHeartbeat = ts;
         synchronized(heartbeats) {
            this.heartbeats.put(ipcdBridge, ts);
         }
      }

      boolean offline(String ipcdBridge, long expirationTime) {
         synchronized(heartbeats) {
            this.lastHeartbeat = 0;
            this.heartbeats.remove(ipcdBridge);
            Iterator<Long> timestamps = this.heartbeats.values().iterator();
            while(timestamps.hasNext()) {
               long ts = timestamps.next();
               if(ts < expirationTime) {
                  timestamps.remove();
               }
               else if(ts > lastHeartbeat) {
                  lastHeartbeat = ts;
               }
            }
            for(long ts: this.heartbeats.values()) {
               if(this.lastHeartbeat < ts) {
                  this.lastHeartbeat = ts;
               }
            }
         }
         return this.lastHeartbeat == 0;
      }
   }

   private class IpcdRegistryMetrics {
      private final Counter timedout;
      private final Counter disconnected;

      IpcdRegistryMetrics(IrisMetricSet metrics) {
         disconnected = metrics.counter("disconnected");
         timedout = metrics.counter("timedout");
         metrics.gauge("online", (Supplier<Integer>) () -> getOnlineDevices());
      }

      public void onDisconnected() {
         disconnected.inc();
      }

      public void onTimeout() {
         timedout.inc();
      }

   }
}

