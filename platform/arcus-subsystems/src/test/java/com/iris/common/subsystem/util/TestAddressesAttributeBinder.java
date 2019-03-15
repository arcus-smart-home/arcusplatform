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
/**
 * 
 */
package com.iris.common.subsystem.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.capability.util.Addresses;
import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.messages.model.test.ModelFixtures;

/**
 * 
 */
public class TestAddressesAttributeBinder extends SubsystemTestCase<SubsystemModel> {
   private String attributeName = "test:addresses";
   
   public Set<String> addAll() {
      return ImmutableSet.of(
            addPlace(),
            addPerson(),
            addSwitch(),
            addMotion()
      );
   }
   
   protected String addModelAndReturnAddress(Map<String, Object> attributes) {
      String val = store.addModel(attributes).getAddress().getRepresentation();
      return val;
   }
   
   protected void changeModel(String address, Map<String, Object> attributes) {
      context.models().update(
            PlatformMessage
               .createBroadcast(
                     MessageBody
                        .buildMessage(Capability.EVENT_VALUE_CHANGE, attributes), Address.fromString(address)
               )
      );
   }
   
   protected void removeModel(String address) {
      context.models().update(
            PlatformMessage
               .createBroadcast(
                     MessageBody
                        .buildMessage(Capability.EVENT_DELETED, ImmutableMap.<String, Object>of()), Address.fromString(address)
               )
      );
   }
   
   private String addPlace() {
      return addModelAndReturnAddress(ModelFixtures.buildServiceAttributes(context.getPlaceId(), PlaceCapability.NAMESPACE).create());
   }

   private String addPerson() {
      return addModelAndReturnAddress(ModelFixtures.createPersonAttributes());
   }

   private String addSwitch() {
      return addModelAndReturnAddress(ModelFixtures.createSwitchAttributes());
   }

   private String addMotion() {
      return addModelAndReturnAddress(ModelFixtures.buildMotionAttributes().create());
   }

   @Test
   public void testAddAllOnInit() {
      AddressesAttributeBinder binder = new AddressesAttributeBinder(Predicates.alwaysTrue(), attributeName);
      Set<String> addresses = addAll();
      
      binder.bind(context);
      
      assertEquals(addresses, context.model().getAttribute(attributeName));
   }

   @Test
   public void testRemoveAllOnInit() {
      AddressesAttributeBinder binder = new AddressesAttributeBinder(Predicates.alwaysTrue(), attributeName);
      
      // add addresses that aren't in the context
      Set<String> addresses = ImmutableSet.of(
            Addresses.toObjectAddress("dev", UUID.randomUUID().toString()),
            Addresses.toObjectAddress("dev", UUID.randomUUID().toString()),
            Addresses.toObjectAddress("dev", UUID.randomUUID().toString())
      );
      context.model().setAttribute(attributeName, addresses);
      
      binder.bind(context);
      
      assertEquals(ImmutableSet.of(), context.model().getAttribute(attributeName));
   }

   @Test
   public void testAddThenRemove() {
      AddressesAttributeBinder binder = new AddressesAttributeBinder(Predicates.alwaysTrue(), attributeName);
      binder.bind(context);
      
      assertEquals(ImmutableSet.of(), context.model().getAttribute(attributeName));
      
      Set<String> addresses = addAll();
      assertEquals(addresses, context.model().getAttribute(attributeName));
   }

   @Test
   public void testLoadThenRemove() {
      AddressesAttributeBinder binder = new AddressesAttributeBinder(Predicates.alwaysTrue(), attributeName);
      Set<String> addresses = addAll();
      
      binder.bind(context);
      
      assertEquals(addresses, context.model().getAttribute(attributeName));
      
      for(String address: addresses) {
         removeModel(address);
      }
      assertEquals(ImmutableSet.of(), context.model().getAttribute(attributeName));
   }
   
   @Test
   public void testAddFiltered() {
      AddressesAttributeBinder binder = new AddressesAttributeBinder(com.iris.model.predicate.Predicates.isA(SwitchCapability.NAMESPACE), attributeName);
      Set<String> addresses = new HashSet<>();
      addPlace();
      addPerson();
      addMotion();
      addresses.add(addSwitch());
      
      binder.bind(context);
      
      assertEquals(addresses, context.model().getAttribute(attributeName));
      
      // add motion, no change
      String motion = addMotion();
      assertEquals(addresses, context.model().getAttribute(attributeName));
      
      // add switch, should be another device
      String swit = addSwitch();
      addresses.add(swit);
      assertEquals(addresses, context.model().getAttribute(attributeName));
      
      // remove motion, no change
      removeModel(motion);
      assertEquals(addresses, context.model().getAttribute(attributeName));
      
      // remove switch, should remove an element
      removeModel(swit);
      addresses.remove(swit);
      assertEquals(addresses, context.model().getAttribute(attributeName));
   }

   @Test
   public void testChangeCapabilities() {
      // very odd case, shouldn't happen in practice, although there are other properties this might happen for
      AddressesAttributeBinder binder = new AddressesAttributeBinder(com.iris.model.predicate.Predicates.isA(SwitchCapability.NAMESPACE), attributeName);
      String swit = addSwitch();
      
      binder.bind(context);
      
      // initially added
      assertEquals(ImmutableSet.of(swit), context.model().getAttribute(attributeName));
      
      // transform into a motion sensor...
      changeModel(swit, ImmutableMap.<String, Object>of(Capability.ATTR_CAPS, ImmutableSet.of(Capability.NAMESPACE, DeviceCapability.NAMESPACE, MotionCapability.NAMESPACE)));
      assertEquals(ImmutableSet.of(), context.model().getAttribute(attributeName));
      
      // and back again
      changeModel(swit, ImmutableMap.<String, Object>of(Capability.ATTR_CAPS, ImmutableSet.of(Capability.NAMESPACE, DeviceCapability.NAMESPACE, SwitchCapability.NAMESPACE)));
      assertEquals(ImmutableSet.of(swit), context.model().getAttribute(attributeName));
   }

}

