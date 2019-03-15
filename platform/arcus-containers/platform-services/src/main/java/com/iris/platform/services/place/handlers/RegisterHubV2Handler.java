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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.Utils;
import com.iris.client.model.device.ClientDeviceModel.Base;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.HubRegistrationDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubKitCapability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.PlaceCapability.RegisterHubV2Request;
import com.iris.messages.capability.PlaceCapability.RegisterHubV2Response;
import com.iris.messages.capability.PlaceCapability.RegisterHubV2Response.Builder;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Hub;
import com.iris.messages.model.HubRegistration;
import com.iris.messages.model.HubRegistration.RegistrationState;
import com.iris.messages.model.serv.PairingDeviceMockModel;
import com.iris.messages.model.serv.PairingDeviceModel;
import com.iris.messages.services.PlatformConstants;
import com.iris.messages.type.ZigbeeLinkKeyedDevice;
import com.iris.messages.model.Place;
import com.iris.platform.hub.registration.HubRegistrationConfig;
import com.iris.platform.manufacture.kitting.dao.ManufactureKittingDao;
import com.iris.platform.manufacture.kitting.kit.Kit;
import com.iris.platform.manufacture.kitting.kit.KitDevice;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;
import com.iris.platform.pairing.PairingDeviceMock;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.KitUtil;

@Singleton
public class RegisterHubV2Handler extends AbstractRegisterHubHandler implements ContextualRequestMessageHandler<Place> {
   private static final Logger logger = LoggerFactory.getLogger(RegisterHubV2Handler.class);

   public static final String HUB_ORPHANED_MSG = "Hub with Id [%s] is orphaned ";
   public static final String HUB_ALREADY_REGISTERED_MSG = "Hub with Id [%s] is already registered ";
   public static final String HUB_REGISTER_ACTIVEHUB_MSG = "The place [%s] already has a hub associated with it";
   
   private final long upgradeErrorMaxAge;

   private final ManufactureKittingDao kitDao;
   private final PairingDeviceDao pairingDeviceDao;

   @Inject
   public RegisterHubV2Handler(PlatformMessageBus bus, 
		   HubDAO hubDao,
		   ManufactureKittingDao kitDao,
		   PairingDeviceDao pairingDeviceDao,
		   HubRegistrationDAO hubRegistrationDao,
		   HubRegistrationConfig config,
		   PlacePopulationCacheManager populationCacheMgr) {
      super(bus, hubDao, hubRegistrationDao, populationCacheMgr);
      this.upgradeErrorMaxAge = TimeUnit.MINUTES.toMillis(config.getUpgradeErrorMaxAgeInMin());
      this.kitDao = kitDao;
      this.pairingDeviceDao = pairingDeviceDao;
   }

   @Override
   public String getMessageType() {
      return RegisterHubV2Request.NAME;
   }

   @Override
   public MessageBody handleRequest(Place place, PlatformMessage msg) {
      Utils.assertNotNull(place, "The place is required");

      MessageBody message = msg.getValue();
      String hubId = RegisterHubV2Request.getHubId(message);

      if(StringUtils.isBlank(hubId)) {
    	  throw new ErrorEventException(Errors.missingParam(RegisterHubV2Request.ATTR_HUBID));
      }else if(!Address.isHubId(hubId)) {
    	  throw new ErrorEventException(Errors.invalidParam(RegisterHubV2Request.ATTR_HUBID));
      }
      UUID accountId = place.getAccount();

      Hub hub = hubDao.findById(hubId);
      if(hub == null) {
    	  //look at hub_registration table to see if it's doing firmware upgrade, and build response from hub_registration entry
    	  HubRegistration curHubReg = hubRegistrationDao.findById(hubId);
    	  return buildResponse(curHubReg);    	     	 
      }
      if(HubCapability.REGISTRATIONSTATE_ORPHANED.equals(hub.getRegistrationState())) {
         throw new ErrorEventException(Errors.fromCode(RegisterHubV2Response.CODE_ERROR_REGISTER_ORPHANEDHUB, String.format(HUB_ORPHANED_MSG, hubId)));
      }

      if(hub.getAccount() == null) {
    	  //Make sure the place does not have an active hub already
    	  Hub existingHub = hubDao.findHubForPlace(place.getId());
    	  if(existingHub != null && !existingHub.getId().equals(hub.getId()) && HubCapability.REGISTRATIONSTATE_REGISTERED.equals(existingHub.getRegistrationState())) {
    		  String errMsg = String.format(HUB_REGISTER_ACTIVEHUB_MSG, place.getId());
    		  throw new ErrorEventException(Errors.fromCode(RegisterHubV2Response.CODE_ERROR_REGISTER_ACTIVEHUB, errMsg));
    	  }  
    	  hub.setAccount(accountId);
		  hub.setPlace(place.getId());
		  hub.setRegistrationState(HubCapability.REGISTRATIONSTATE_REGISTERED);
		  hubDao.save(hub);
		  updateHubRegistrationIfNecessary(hub);
		  sendHubRegistered(hub.getId(), accountId, place.getId(), place.getPopulation());
		  checkIfPartOfKit(hub, place.getPopulation());
		  return buildResponse(hub);

      } else if(!Objects.equals(accountId, hub.getAccount()) || !Objects.equals(place.getId(), hub.getPlace())) {
         String errMsg = String.format(HUB_ALREADY_REGISTERED_MSG, hubId);
    	 throw new ErrorEventException(Errors.fromCode(RegisterHubV2Response.CODE_ERROR_REGISTER_ALREADYREGISTERED, errMsg));
      } else {
    	 //TODO - not sure why we need to emit this hub added event here, but that's what V1 RegisterHub is doing.
         //emitAddedEvent(hubDao.findHubModel(hub.getId()), msg.getCorrelationId());
         //TODO - both the existing hub account and place matches with the current request.  Should we set hub.RegistrationState to REGISTRATIONSTATE_REGISTERED here?
         //hub.RegistrationState could be REGISTRATIONSTATE_UNREGISTERED here
    	 if(!HubCapability.REGISTRATIONSTATE_REGISTERED.equals(hub.getState())) {
    		 hub.setRegistrationState(HubCapability.REGISTRATIONSTATE_REGISTERED);
    		 hubDao.save(hub);
    	 }
         return buildResponse(hub);
      }

   }
   
   private MessageBody buildResponse(Hub hub) {
	   Builder builder = PlaceCapability.RegisterHubV2Response.builder();
	   builder.withState(RegisterHubV2Response.STATE_REGISTERED);
	   builder.withProgress(100);
	   builder.withHub(hubDao.findHubModel(hub.getId()).toMap());
	   return builder.build();
   }

   private MessageBody buildResponse(HubRegistration curHubReg) {
	   Builder builder = PlaceCapability.RegisterHubV2Response.builder();
	   if(curHubReg != null) {
		   if(curHubReg.getState() == RegistrationState.DOWNLOADING) {
			   builder.withProgress(curHubReg.getDownloadProgress());
		   }
		   builder.withState(getState(curHubReg.getState()));
		   if(StringUtils.isNotBlank(curHubReg.getUpgradeErrorCode()) && !RegistrationState.REGISTERED.equals(curHubReg.getState())) {
		      if(curHubReg.getUpgradeErrorTime() != null && curHubReg.getUpgradeErrorTime().after(new Date(System.currentTimeMillis()-this.upgradeErrorMaxAge))) {
		         throw new ErrorEventException(Errors.fromCode(RegisterHubV2Response.CODE_ERROR_FWUPGRADE_FAILED, curHubReg.getUpgradeErrorMessage()));
		      }
		   }
		   return builder.build();
	   }else{
		   return builder.withState(RegisterHubV2Response.STATE_OFFLINE).build();  //default is offline
	   }
   }
   
   private String getState(RegistrationState regState) {
	   if(regState != null) {
		   switch(regState) {
			   case OFFLINE : return RegisterHubV2Response.STATE_OFFLINE;
			   case APPLYING: return RegisterHubV2Response.STATE_APPLYING;
			   case DOWNLOADING: return RegisterHubV2Response.STATE_DOWNLOADING;
			   case REGISTERED: return RegisterHubV2Response.STATE_REGISTERED;
			   case ONLINE: return RegisterHubV2Response.STATE_ONLINE;
		   }
	   }
	   return RegisterHubV2Response.STATE_OFFLINE;
   }

   private void checkIfPartOfKit(Hub hub, String population) {
	   logger.debug("Checking if hub is part of a kit.");

	   if (hub==null) return;

	   String hubId = hub.getId();
	   UUID placeId = hub.getPlace();
	   Kit kit = kitDao.getKit(hubId);
	   if (kit==null) {
		   logger.debug("No kit for hub {}", hubId);
		   return;
	   }
	 
	   String type = kit.getType();
	   if (!HubKitCapability.SetKitRequest.TYPE_TYPE.asEnum().getValues().contains(type)) {
		   type = HubKitCapability.SetKitRequest.TYPE_TEST;
	   }
	   
	   logger.debug("Deploying kit {} to hub {}", kit.toString(), hubId );
	   
	   List<Map<String,Object>> devices = new ArrayList<>();
	   
	   for (KitDevice device : kit.getDevices()) {
		   Map<String,Object> map = new HashMap<>();
		   map.put(ZigbeeLinkKeyedDevice.ATTR_EUID, device.getEuid());
		   map.put(ZigbeeLinkKeyedDevice.ATTR_INSTALLCODE, device.getInstallCode());
		   map.put(ZigbeeLinkKeyedDevice.ATTR_TYPE, device.getType());
		   devices.add(map);
	   }

	   sendHubKitMessage(hubId, placeId, devices, type, population);
	   
	   // Write pairing devices into the DAO.
	   for (KitDevice device : kit.getDevices()) {
		   DeviceProtocolAddress address = Address.hubProtocolAddress(hubId, "ZIGB", ProtocolDeviceId.fromRepresentation(KitUtil.zigbeeIdToProtocolId(device.getEuid())));
		   String productId = KitUtil.getProductId(device.getType());
		   
		   PairingDevice pairDev = new PairingDevice();
		   pairDev.setPlaceId(placeId);
		   pairDev.setProtocolAddress(address);
		   PairingDeviceModel.setCustomizations(pairDev, ImmutableSet.of());
		   PairingDeviceModel.setPairingPhase(pairDev, PairingDeviceCapability.PAIRINGPHASE_JOIN);
		   PairingDeviceModel.setPairingState(pairDev, PairingDeviceCapability.PAIRINGSTATE_PAIRING);
		   PairingDeviceModel.setRemoveMode(pairDev, PairingDeviceCapability.REMOVEMODE_HUB_AUTOMATIC);		   
		   Set<String> tags = new HashSet<String>();
		   tags.add("KIT");
		   pairDev.setAttribute(Base.ATTR_TAGS, tags);
		   pairDev = pairingDeviceDao.save(pairDev);

		   Map<String,Object> attributes = pairDev.toMap();		   
		   Address source = pairDev.getAddress();
		   
		   sendCreatePairingDevice(attributes,source,productId,placeId,hubId,population);
	   }
   }
   
}

