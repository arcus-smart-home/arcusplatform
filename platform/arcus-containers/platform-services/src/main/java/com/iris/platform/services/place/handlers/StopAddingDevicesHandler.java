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

import java.util.List;
import java.util.UUID;

import com.google.inject.Inject;
import com.iris.Utils;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.BridgeCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubCapability.PairingRequestRequest;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.PlaceCapability.StopAddingDevicesRequest;
import com.iris.messages.model.Device;
import com.iris.messages.model.Hub;
import com.iris.messages.model.Place;
import com.iris.messages.services.PlatformConstants;

public class StopAddingDevicesHandler implements ContextualRequestMessageHandler<Place> {

   private final HubDAO hubDao;
   private final PlatformMessageBus bus;
   private final DeviceDAO deviceDao;

   @Inject
   public StopAddingDevicesHandler(HubDAO hubDao, DeviceDAO deviceDao, PlatformMessageBus bus) {
      this.hubDao = hubDao;
      this.bus = bus;
      this.deviceDao = deviceDao;
   }

   @Override
   public String getMessageType() {
      return StopAddingDevicesRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      Utils.assertNotNull(context, "Place is required to stop adding devices");

      Hub hub = hubDao.findHubForPlace(context.getId());
      if(hub != null) {
         MessageBody pairingRequest =
               HubCapability.PairingRequestRequest
                  .builder()
                  .withActionType(PairingRequestRequest.ACTIONTYPE_STOP_PAIRING)
                  .build()
                  ;

         bus.send(PlatformMessage.buildMessage(
               pairingRequest,
               Address.platformService(PlatformConstants.SERVICE_PLACES),
               Address.hubService(hub.getId(),  "hub"))
               .withPlaceId(context.getId())
               .withPopulation(context.getPopulation())
               .create());
      }

      List<Device>devices=deviceDao.listDevicesByPlaceId(context.getId());
      for(Device device:devices){
         if(device.getCaps()!=null && device.getCaps().contains(BridgeCapability.NAMESPACE)){
            sendStopParing(context.getId(), device.getAddress());
         }
      }

      return PlaceCapability.StopAddingDevicesResponse.instance();
   }
   private void sendStopParing(UUID placeId, String deviceAddress){
      MessageBody body = BridgeCapability.StopPairingRequest.instance();
      bus.send(PlatformMessage.buildRequest(
            body,
            Address.platformService(placeId,PlatformConstants.SERVICE_PLACES),
            Address.fromString(deviceAddress)).create());      
   }
}

