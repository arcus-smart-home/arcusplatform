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
package com.iris.common.subsystem.safety;

import java.util.Map;
import java.util.UUID;

import org.junit.Before;

import com.google.common.base.Optional;
import com.iris.capability.util.Addresses;
import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.common.subsystem.weather.HaloFixtures;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ClasspathDefinitionRegistry;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SafetySubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.SafetySubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.util.IrisCollections;

public class SafetySubsystemTestCase extends SubsystemTestCase<SafetySubsystemModel> {
   //private static final Logger LOGGER = LoggerFactory.getLogger(SafetySubsystemTestCase.class);

   protected SafetySubsystem subsystem = new SafetySubsystem();

   protected Model haloDevice1 = null;
   protected Model haloDevice2 = null;
   protected Model haloDevice3 = null;
   
   protected Model owner = null;

   protected boolean started = false;

   @Override
   protected SafetySubsystemModel createSubsystemModel() {
      Map<String, Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, SafetySubsystemCapability.NAMESPACE);
      return new SafetySubsystemModel(new SimpleModel(attributes));
   }

   // setAttributes
   @Before
   public void setUp() {
      super.setUp();
      started = false;
      subsystem.setDefinitionRegistry(ClasspathDefinitionRegistry.instance());

      Optional<Map<String,Object>> attrs = Optional.absent();
      haloDevice1 = addModel(HaloFixtures.createHaloFixtureWithAttrs(attrs));
      haloDevice2 = addModel(HaloFixtures.createHaloFixtureWithAttrs(attrs));
      haloDevice3 = addModel(HaloFixtures.createHaloFixtureWithAttrs(attrs));
      
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
                        haloDevice1.toMap(),
                        haloDevice2.toMap(),
                        haloDevice3.toMap(),
                        placeModel.toMap(),
                        accountModel.toMap(),
                        owner.toMap()));
   }
   
   protected void removeDevice(Model deviceModel) {
      store.removeModel(deviceModel.getAddress());
   }

   protected void setAttributes(Map<String, Object> attributes) {
      MessageBody request = MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attributes);
      PlatformMessage message = 
      		PlatformMessage
      			.buildRequest(request, Address.clientAddress("android", UUID.randomUUID().toString()), Address.platformService(placeId, SafetySubsystemCapability.NAMESPACE))
      			.create();
      subsystem.onEvent(new MessageReceivedEvent(message), context);
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
   
   protected PlatformMessage safetyPlatformMessage(MessageBody body) {
      return safetyPlatformMessage(body, clientAddress);
   }
   
   protected PlatformMessage safetyPlatformMessage(MessageBody body, Address fromAddress) {
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

