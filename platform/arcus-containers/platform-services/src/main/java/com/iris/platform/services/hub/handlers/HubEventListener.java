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
package com.iris.platform.services.hub.handlers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubAttributesPersistenceFilter;
import com.iris.core.dao.HubDAO;
import com.iris.core.platform.AbstractPlatformMessageListener;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.platform.PlatformService;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.HubServiceAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceConnectionCapability.LostDeviceRequest;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubAdvancedCapability.GetKnownDevicesRequest;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Hub;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.services.hub.HubRegistry;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.IrisUUID;

@Singleton
public class HubEventListener
   extends AbstractPlatformMessageListener
   implements PlatformService // FIXME this is a hack to jump in the PlatformServiceDispatcher's thread pool
{
   public static final String PROP_THREADPOOL = "platform.service.hubevent.threadpool";
   private static final Logger logger = LoggerFactory.getLogger(HubEventListener.class);

   // don't route any real messages to me
   private final Address address = Address.platformService("hubevent");

   private final HubDAO hubDao;
   private final DeviceDAO deviceDao;

   private final HubRegistry hubRegistry;
   private final BeanAttributesTransformer<Hub> hubTransformer;
   private final Partitioner partitioner;
   private final HubAttributesPersistenceFilter filter = new HubAttributesPersistenceFilter();
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public HubEventListener(
         PlatformMessageBus platformBus,
         @Named(PROP_THREADPOOL) Executor executor,
         HubDAO hubDao,
         HubRegistry hubRegistry,
         BeanAttributesTransformer<Hub> hubTransformer,
         Partitioner partitioner,
         DeviceDAO deviceDao,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      // FIXME we need this to intercept hub service events, really we just want to run in the normal service pool
      super(platformBus, executor);
      this.hubDao = hubDao;
      this.hubRegistry = hubRegistry;
      this.hubTransformer = hubTransformer;
      this.partitioner = partitioner;
      this.deviceDao = deviceDao;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public Address getAddress() {
      return address;
   }

   @Override
   protected void onStart() {
      // snoop the hub service without stealing request handling
      addListeners(AddressMatchers.anyOf(Address.platformService(HubCapability.NAMESPACE)));
   }


   @Override
   public void handleMessage(PlatformMessage message) {
      super.handleMessage(message);
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#handleEvent(com.iris.messages.PlatformMessage)
    */
   @Override
   protected void handleEvent(PlatformMessage message) throws Exception {
      Address address = message.getSource();
      if(!address.isHubAddress()) {
         return;
      }

      HubServiceAddress hubAddress = (HubServiceAddress) address;

      // TODO do we really need to listen for these...
      String messageType = message.getMessageType();
      if(
            MessageConstants.MSG_HUB_CONNECTED_EVENT.equals(messageType) ||
            HubCapability.HubConnectedEvent.NAME.equals(messageType)
      ) {
         int partitionId = partitioner.getPartitionForMessage(message).getId();
         logger.debug("message {} on connected hub {}",message.getMessageType(), hubAddress);
         onConnected(hubAddress, partitionId, message.getActor(), message.getValue());
      }
      else if(HubCapability.HubDisconnectedEvent.NAME.equals(messageType)) {
         onDisconnected(hubAddress, message.getActor(), message.getValue());
      }
      // note this is named HubRegisteredRequest but its not actually sent as a request
      else if(MessageConstants.MSG_HUB_REGISTERED_REQUEST.equals(messageType)) {
         onHubRegistered(hubAddress, message.getActor(), message.getValue());
      }
      else if(Capability.EVENT_ADDED.equals(messageType)) {
         onAdded(hubAddress, message.getValue());
      }
      else if(Capability.EVENT_VALUE_CHANGE.equals(messageType)) {
         onValueChange(hubAddress, message.getValue());
      }
      else if(Capability.EVENT_REPORT.equals(messageType)) {
         onReport(hubAddress, message.getValue());
      }
      else if(Capability.EVENT_DELETED.equals(messageType)) {
         onDeleted(hubAddress, message.getValue());
      }
      else if(HubAdvancedCapability.GetKnownDevicesResponse.NAME.equals(messageType)) {
         onGetDeviceResponse(hubAddress, message.getValue());
      }
   }

   private void onConnected(HubServiceAddress hubAddress, int partitionId, Address hubBridge, MessageBody value) {
      if(hubBridge == null) {
         // this is the version sent by the hub with all the attributes
         Hub hub = hubDao.findById(hubAddress.getHubId());

         if(!devicesInSync(hub,value)){
            sendMessage(buildGetKnownDevicesRequest(hub));
         }
      }
      else {
         // this is the version sent by the hub-bridge when the hub is fully authorized
         // FIXME this should be HubAuthorized
         hubRegistry.online(hubAddress.getHubId(), partitionId, (String) hubBridge.getId());
      }
   }

   /*
    * If the platform hubLastDeviceAddRemove is the same as what is in the database, then don't sync
    */
   private boolean devicesInSync(Hub hub,MessageBody value){

      String hubLastDeviceAddRemove = HubAdvancedCapability.getLastDeviceAddRemove(value);
      UUID platformLastDeviceAddRemove = hub.getLastDeviceAddRemove();
      //check to see if the hub is sending that field yet.  if so skip the sync.
      if(hubLastDeviceAddRemove==null){
         return true;
      }

      if(platformLastDeviceAddRemove!=null && !hubLastDeviceAddRemove.equals(platformLastDeviceAddRemove.toString())){
         return false;
      }
      return true;
   }

   protected void onGetDeviceResponse(HubServiceAddress hubAddress,MessageBody response) {
      try {
         String hubId = hubAddress.getHubId();
         List<Device> platformDevices = deviceDao.findByHubId(hubAddress.getHubId());

         Map<String, Device> platformProtocalAddresses =
               platformDevices.stream()
               .filter(device->device.getProtocolAddress()!=null)
               .collect(Collectors.toMap(device->canonicalProtocolAddress(hubId,device.getProtocolAddress()), device -> device));

         Set<String> hubReportedDevices = 
            new HashSet<>(HubAdvancedCapability.GetKnownDevicesResponse.getDevices(response)).stream()
            .map((addr) -> canonicalProtocolAddress(hubId, addr))
            .collect(Collectors.toSet());

         Set<String> lostDevices = platformProtocalAddresses.keySet().stream()
               .filter(platformDevice -> !hubReportedDevices.contains(platformDevice))
               .collect(Collectors.toSet());

         Set<String> unknownDevices = hubReportedDevices.stream()
               .filter(hubDevice -> !platformProtocalAddresses.containsKey(hubDevice))
               .collect(Collectors.toSet());

         lostDevices.forEach(lostDeviceProtocolAddress -> {
            String lostDevicePlatformAddress = platformProtocalAddresses.get(lostDeviceProtocolAddress).getAddress();
            PlatformMessage lostDevice = PlatformMessage.buildRequest(LostDeviceRequest.instance(), address, Address.fromString(lostDevicePlatformAddress)).create();
            sendMessage(lostDevice);
         });

         unknownDevices.forEach(unknownDeviceAddress -> {
            logger.warn("Unknow device detected. Hub [{}] reports device [{}], but it does not exist on the platform",hubAddress,unknownDeviceAddress);
         });

      }
      catch(Exception e){
         logger.error("Unknow exception during lost device detection",e);
      }
   }

   private String canonicalProtocolAddress(String hubId, String protocolAddress) {
      DeviceProtocolAddress address = (DeviceProtocolAddress) Address.fromString(protocolAddress);
      return Address.hubProtocolAddress(hubId, address.getProtocolName(), address.getId()).getRepresentation();
   }

   private PlatformMessage buildGetKnownDevicesRequest(Hub hub) {
      logger.debug("sending get known device to hub {}",hub.getId());
      PlatformMessage message =
            PlatformMessage
               .request(Address.hubService(hub.getId(), HubCapability.NAMESPACE))
               .from(address)
               .withPlaceId(hub.getPlace())
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(hub.getPlace()))
               .withCorrelationId(IrisUUID.randomUUID().toString())
               .withPayload(GetKnownDevicesRequest.NAME, ImmutableMap.of())
               .create();

      return message;
   }

   private void onDisconnected(HubServiceAddress hubAddress, Address bridge, MessageBody value) {
      String hubId = hubAddress.getHubId();
      if(bridge != null) {
         // this is the version sent by hub-bridge, update the registry
         // if bridge is null its the version sent by the registry, don't double dip
         String hubBridge = (String) bridge.getId();
         hubRegistry.offline(hubId, hubBridge);
      }
   }

   private void onHubRegistered(HubServiceAddress hubAddress, Address hubBridge, MessageBody value) {
      // if its online in the db start tracking it here, otherwise do nothing
      String hubId = hubAddress.getHubId();
      Hub hub = hubDao.findById(hubId);
      if(hub == null) {
         logger.warn("Received a registered event for a hub with no db record, id: [{}]", hubId);
         return;
      }

      if(HubCapability.STATE_DOWN.equals(hub.getState())) {
         logger.debug("Ignoring registered message for offline hub: [{}]", hubId);
         return;
      }

      int partitionId = partitioner.getPartitionForPlaceId((String) value.getAttributes().get(HubCapability.ATTR_PLACE)).getId();
      hubRegistry.online(hubId, partitionId, (String) hubBridge.getId());
   }

   private void onAdded(HubServiceAddress hubAddress, MessageBody value) {
      // no-op?
   }

   private void onValueChange(HubServiceAddress hubAddress, MessageBody value) {
      String hubId = hubAddress.getHubId();
      Hub hub = hubDao.findById(hubId);
      if(hub == null) {
         logger.warn("Received ValueChange for unknown hub [{}]", hubId);
         return;
      }

      if(HubCapability.REGISTRATIONSTATE_REGISTERED.equals(value.getAttributes().get(HubCapability.ATTR_REGISTRATIONSTATE))) {
         // if the hub has just been registered, then its partition changes and we need to stop tracking here
         hubRegistry.remove(hubId);
      }
      else {
         updateHubAttributes(hub, value);
      }
   }

   private void onReport(HubServiceAddress hubAddress, MessageBody value) {
      String hubId = hubAddress.getHubId();
      Hub hub = hubDao.findById(hubId);
      if(hub == null) {
         logger.warn("Received AttrReport for unknown hub [{}]", hubId);
         return;
      }
      updateHubAttributes(hub, value);
   }

   private void updateHubAttributes(Hub hub, MessageBody value) {
      Map<String, Object> changed = hubTransformer.merge(hub, value.getAttributes());
      if(!changed.isEmpty()) {
         hubDao.save(hub);
      }
      Map<String, Object> attrs = filter.filter(value.getAttributes());
      if(!attrs.isEmpty()) {
         hubDao.updateAttributes(hub.getId(), attrs);
      }
   }

   private void onDeleted(HubServiceAddress hubAddress, MessageBody value) {
      // no-op?
   }
}

