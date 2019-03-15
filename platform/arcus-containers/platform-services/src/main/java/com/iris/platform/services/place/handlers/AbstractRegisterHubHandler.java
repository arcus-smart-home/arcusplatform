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
package com.iris.platform.services.place.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.HubRegistrationDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubKitCapability;
import com.iris.messages.capability.PairingSubsystemCapability;
import com.iris.messages.model.Hub;
import com.iris.messages.model.HubRegistration;
import com.iris.messages.model.Model;
import com.iris.messages.model.Place;
import com.iris.messages.model.HubRegistration.RegistrationState;
import com.iris.messages.model.hub.HubModel;
import com.iris.messages.model.serv.ProductModel;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.messages.service.SubsystemService;
import com.iris.messages.services.PlatformConstants;
import com.iris.population.PlacePopulationCacheManager;

public abstract class AbstractRegisterHubHandler implements ContextualRequestMessageHandler<Place> {
   private static final Logger logger = LoggerFactory.getLogger(AbstractRegisterHubHandler.class);
   protected final PlatformMessageBus bus;
   protected final HubDAO hubDao;
   protected final HubRegistrationDAO hubRegistrationDao;
   protected final PlacePopulationCacheManager populationCacheMgr;

   public AbstractRegisterHubHandler(PlatformMessageBus bus, 
   		HubDAO hubDao, 
   		HubRegistrationDAO hubRegistrationDao,
   		PlacePopulationCacheManager populationCacheMgr) {
      this.bus = bus;
      this.hubDao = hubDao;
      this.hubRegistrationDao = hubRegistrationDao;
      this.populationCacheMgr = populationCacheMgr;
   }

   public abstract String getMessageType() ;
   public abstract MessageBody handleRequest(Place context, PlatformMessage msg); 

   protected void emitAddedEvent(Model hub, String correlationId) {
      MessageBody body = MessageBody.buildMessage(Capability.EVENT_ADDED, hub.toMap());
      String placeId = HubModel.getPlace(hub);
      PlatformMessage added = PlatformMessage.buildBroadcast(body, hub.getAddress())
         .withCorrelationId(correlationId)
         .withPlaceId(placeId)
         .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
         .create();
      bus.send(added);
   }

   protected void sendHubRegistered(String hubId, UUID accountId, UUID placeId, String population) {
      Address hubAddress = Address.hubService(hubId, "hub");

      MessageBody valueChange =
            MessageBody
               .buildMessage(
                     Capability.EVENT_VALUE_CHANGE,
                     ImmutableMap.of(
                           HubCapability.ATTR_ACCOUNT, String.valueOf(accountId),
                           HubCapability.ATTR_PLACE, String.valueOf(placeId),
                           HubCapability.ATTR_REGISTRATIONSTATE, HubCapability.REGISTRATIONSTATE_REGISTERED
                     )
               );
      // don't populate the placeId header here, that way anything listening on an old partition
      // knows what was happening
      bus.send(PlatformMessage.createBroadcast(valueChange, hubAddress));

      Map<String,Object> attrs = new HashMap<>();
         attrs.put(HubCapability.ATTR_ACCOUNT, String.valueOf(accountId));
         attrs.put(HubCapability.ATTR_PLACE, String.valueOf(placeId));

      bus.send(PlatformMessage.buildMessage(
            MessageBody.buildMessage(MessageConstants.MSG_HUB_REGISTERED_REQUEST, attrs),
            Address.platformService(PlatformConstants.SERVICE_HUB),
            hubAddress)
            .withPlaceId(placeId)
            .withPopulation(population)
            .create());

   }
   //TODO - I can update the hub_registration here instead of mothership listening for registered messages.  But then HubRegistrationRegistry will have obsolete cache
   //Maybe I should use a short lived cache like Cache<Serializable, HubRegistration> that expires after 20 min?
   protected void updateHubRegistrationIfNecessary(Hub hub) {
	   if(HubCapability.REGISTRATIONSTATE_REGISTERED.equals(hub.getRegistrationState())) {
		   HubRegistration curHubReg = hubRegistrationDao.findById(hub.getId());
		   if(curHubReg != null) {
			   curHubReg.setState(RegistrationState.REGISTERED);
			   hubRegistrationDao.save(curHubReg);
		   }		   
	   }
   }
   
   protected void sendHubKitMessage(String hubId, UUID placeId, List<Map<String,Object>> devices, String type, String population) {
	   PlatformMessage setKitMessage = PlatformMessage.buildMessage(
			   HubKitCapability.SetKitRequest.builder()
			   	.withDevices(devices)
			   	.withType(type)
			   	.build(),
			   Address.platformService(PlatformConstants.SERVICE_HUB),
			   Address.hubService(hubId, HubCapability.NAMESPACE))
	            .withPlaceId(placeId)
	            .withPopulation(population)
	            .create();

	   bus.send(setKitMessage);
   }
   
   protected void sendCreatePairingDevice(Map<String,Object> attributes, Address address, String productId, UUID placeId, String hubId, String population) {
	   PlatformMessage added =
           PlatformMessage
           		.broadcast()
           		.from(address)
           	    .withPlaceId(placeId)
                .withPopulation(population)
           		.withPayload(MessageBody.buildMessage(Capability.EVENT_ADDED, attributes))
                .create();
	   bus.send(added);
   }
   
}

