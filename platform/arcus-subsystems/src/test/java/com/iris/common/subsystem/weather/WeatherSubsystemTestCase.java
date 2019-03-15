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
package com.iris.common.subsystem.weather;

import java.util.Map;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ClasspathDefinitionRegistry;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.capability.WeatherSubsystemCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.WeatherSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.util.IrisCollections;

public class WeatherSubsystemTestCase extends SubsystemTestCase<WeatherSubsystemModel> {
   private static final Logger LOGGER = LoggerFactory.getLogger(WeatherSubsystemTestCase.class);

   protected WeatherSubsystem subsystem = new WeatherSubsystem();

   protected Model haloPlusDevice1 = null;
   protected Model haloPlusDevice2 = null;

   protected Model owner = null;

   protected boolean started = false;

   @Override
   protected WeatherSubsystemModel createSubsystemModel() {
      Map<String, Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, WeatherSubsystemCapability.NAMESPACE);
      return new WeatherSubsystemModel(new SimpleModel(attributes));
   }

   @SuppressWarnings("unchecked")
   @Before
   public void setUp() {
      super.setUp();
      started = false;
      subsystem.setDefinitionRegistry(ClasspathDefinitionRegistry.instance());

      haloPlusDevice1 = addModel(HaloFixtures.createAlertingOnWeatherHaloPlusFixture());
      haloPlusDevice2 = addModel(HaloFixtures.createAlertingOnWeatherHaloPlusFixture());
      owner = addModel(ModelFixtures.createPersonAttributes());

      placeModel.setAttribute(PlaceCapability.ATTR_SERVICELEVEL, ServiceLevel.PREMIUM.name());
      addModel(placeModel.toMap());

      accountModel.setAttribute(AccountCapability.ATTR_OWNER, owner.getId());
      addModel(accountModel.toMap());
   }

   @SuppressWarnings("unchecked")
   protected void initModelStore() {
      store.addModel(
            IrisCollections
                  .setOf(
                        haloPlusDevice1.toMap(),
                        haloPlusDevice2.toMap(),
                        placeModel.toMap(),
                        accountModel.toMap(),
                        owner.toMap()));
   }
   
   protected void removeDevice(Model deviceModel) {
      ModelRemovedEvent event = ModelRemovedEvent.create(deviceModel);
      subsystem.onDeviceRemoved(event, context);
   }

   protected void setAttributes(Map<String, Object> attributes) {
      MessageBody request = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attributes);
      PlatformMessage msg = PlatformMessage.buildRequest(request, Address.clientAddress("android", "1"), Address.platformService(placeId, WeatherSubsystemCapability.NAMESPACE))
         .withPlaceId(placeId)
         .create();
      subsystem.setAttributes(msg, context);
   }

   protected void startSubsystem() {
      subsystem.onEvent(
            SubsystemLifecycleEvent.added(context.model().getAddress()),
            context);
      subsystem.onEvent(
            SubsystemLifecycleEvent.started(context.model().getAddress()),
            context);
      store.addListener(new Listener<ModelEvent>() {
         @Override
         public void onEvent(ModelEvent event) {
            subsystem.onEvent(event, context);
         }
      });
      started = true;   
   }
   
   protected PlatformMessage weatherPlatformMessage(MessageBody body) {
      return weatherPlatformMessage(body, clientAddress);
   }
   
   protected PlatformMessage weatherPlatformMessage(MessageBody body, Address fromAddress) {
         PlatformMessage message =
               PlatformMessage
                     .request(model.getAddress())
                     .from(fromAddress)
                     .withActor(owner.getAddress())
                     .withPayload(body)
                     .create();
         return message;
      } 
}

