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

import java.util.HashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.MobileDeviceDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.model.MobileDevice;
import com.iris.messages.model.Person;

@Singleton
public class RemoveMobileDeviceHandler implements ContextualRequestMessageHandler<Person> {

   private final PlatformMessageBus bus;
   private final MobileDeviceDAO dao;

   @Inject
   public RemoveMobileDeviceHandler(PlatformMessageBus bus, MobileDeviceDAO dao) {
      this.bus = bus;
      this.dao = dao;
   }

   @Override
   public String getMessageType() {
      return PersonCapability.RemoveMobileDeviceRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Person context, PlatformMessage msg) {
      MessageBody request = msg.getValue();
      Integer index = PersonCapability.RemoveMobileDeviceRequest.getDeviceIndex(request);

      if(index == null) {
         return ErrorEvent.fromCode("MissingArgument", "The mobile device index must be provided");
      }

      MobileDevice dev = dao.findOne(context.getId(), index);
      dao.delete(dev);

      MessageBody body = MessageBody.buildMessage(Capability.EVENT_DELETED, new HashMap<>());
      PlatformMessage deleted = PlatformMessage.buildBroadcast(body, Address.fromString(dev.getAddress()))
            .withCorrelationId(msg.getCorrelationId())
            .withPlaceId(msg.getPlaceId())
            .withPopulation(msg.getPopulation())
            .create();
      bus.send(deleted);

      PlatformMessage notify = Notifications.builder()
            .withPersonId(context.getId())
            .withSource(Address.platformService(PersonCapability.NAMESPACE))
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .withMsgKey(Notifications.MobiledeviceRemoved.KEY)
            .create();
      bus.send(notify);

      return PersonCapability.RemoveMobileDeviceResponse.instance();
   }
}

