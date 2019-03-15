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
package com.iris.platform.services.person.handlers;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.MobileDeviceDAO;
import com.iris.core.dao.support.MobileDeviceSaveResult;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.MobileDevice;
import com.iris.messages.model.Person;

@Singleton
public class AddMobileDeviceHandler implements ContextualRequestMessageHandler<Person> {

   private final PlatformMessageBus bus;
   private final MobileDeviceDAO dao;
   private final BeanAttributesTransformer<MobileDevice> transformer;

   @Inject
   public AddMobileDeviceHandler(PlatformMessageBus bus, MobileDeviceDAO dao, BeanAttributesTransformer<MobileDevice> transformer) {
      this.bus = bus;
      this.dao = dao;
      this.transformer = transformer;
   }

   @Override
   public String getMessageType() {
      return PersonCapability.AddMobileDeviceRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Person context, PlatformMessage msg) {
      MessageBody request = msg.getValue();
      if(PersonCapability.AddMobileDeviceRequest.getOsType(request) == null) {
         return Errors.fromCode(Errors.CODE_MISSING_PARAM, "The mobile device osType is required");
      }
      if(PersonCapability.AddMobileDeviceRequest.getDeviceIdentifier(request) == null) {
         return Errors.fromCode(Errors.CODE_MISSING_PARAM, "The device identifier is required");
      }

      List<MobileDevice> devices = dao.listForPerson(context);
      String deviceIdentifier = PersonCapability.AddMobileDeviceRequest.getDeviceIdentifier(request);

      MobileDevice device = null;
      boolean newDevice = true;
      
      for(MobileDevice md : devices) {
         if(md.getDeviceIdentifier().equals(deviceIdentifier)) {
            device = md;
            newDevice = false;
            break;
         }
      }

      if(device == null) {
         device = new MobileDevice();
         newDevice = true;
      }

      device.setDeviceIdentifier(deviceIdentifier);
      device.setAppVersion(PersonCapability.AddMobileDeviceRequest.getAppVersion(request));
      device.setName(PersonCapability.AddMobileDeviceRequest.getName(request));
      device.setDeviceModel(PersonCapability.AddMobileDeviceRequest.getDeviceModel(request));
      device.setDeviceVendor(PersonCapability.AddMobileDeviceRequest.getDeviceVendor(request));
      device.setFormFactor(PersonCapability.AddMobileDeviceRequest.getFormFactor(request));

      Double latitude = PersonCapability.AddMobileDeviceRequest.getLastLatitude(request);
      Double longitude = PersonCapability.AddMobileDeviceRequest.getLastLongitude(request);
      if(latitude != null && longitude != null) {
         device.setLastLatitude(latitude);
         device.setLastLongitude(longitude);
         device.setLastLocationTime(new Date());
      }

      device.setNotificationToken(PersonCapability.AddMobileDeviceRequest.getNotificationToken(request));
      device.setOsType(PersonCapability.AddMobileDeviceRequest.getOsType(request));
      device.setOsVersion(PersonCapability.AddMobileDeviceRequest.getOsVersion(request));
      device.setPersonId(context.getId());
      device.setPhoneNumber(PersonCapability.AddMobileDeviceRequest.getPhoneNumber(request));
      device.setResolution(PersonCapability.AddMobileDeviceRequest.getResolution(request));

      MobileDeviceSaveResult saveResult = dao.save(device);
      MobileDevice saved = saveResult.getDevice();

      MessageBody body = MessageBody.buildMessage(Capability.EVENT_ADDED, transformer.transform(saved));
      PlatformMessage added = PlatformMessage.buildBroadcast(body, Address.fromString(saved.getAddress()))
            .withCorrelationId(msg.getCorrelationId())
            .withPlaceId(msg.getPlaceId())
            .withPopulation(msg.getPopulation())
            .create();
      bus.send(added);

      if (newDevice || saveResult.isOwnerChanged()) {
    	  notificationMobileDeviceAdded(context.getId());
      }
      
      // Notify Old Owner that device has been removed
      if (saveResult.isOwnerChanged()){
         notificationMobileDeviceRemoved(saveResult.getOldOwnerId());
      }

      return PersonCapability.AddMobileDeviceResponse.instance();
   }

   private void notificationMobileDeviceAdded(UUID personId) {
      PlatformMessage msg = Notifications.builder()
            .withPersonId(personId)
            .withSource(Address.platformService(PersonCapability.NAMESPACE))
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .withMsgKey(Notifications.MobiledeviceAdded.KEY)
            .create();
      bus.send(msg);
   }
   
   private void notificationMobileDeviceRemoved(UUID personId) {
      PlatformMessage msg = Notifications.builder()
            .withPersonId(personId)
            .withSource(Address.platformService(PersonCapability.NAMESPACE))
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .withMsgKey(Notifications.MobiledeviceRemoved.KEY)
            .create();
      bus.send(msg);
   }
}

