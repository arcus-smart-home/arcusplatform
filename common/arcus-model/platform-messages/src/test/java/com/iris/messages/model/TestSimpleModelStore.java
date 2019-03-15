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
package com.iris.messages.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelEvent;

public class TestSimpleModelStore {
   private SimpleModelStore store;
   private List<ModelEvent> events;

   @Before
   public void setUp() {
      store = new SimpleModelStore();
      events = new ArrayList<ModelEvent>();
      store.addListener(new Listener<ModelEvent>() {
         @Override
         public void onEvent(ModelEvent event) {
            events.add(event);
         }         
      });
   }
   
   @Test
   public void testNoFilter() {
      store.update(baseAdded(DeviceCapability.NAMESPACE));
      store.update(baseAdded(RecordingCapability.NAMESPACE));
      
      assertEquals(2, store.getModels().size());
      assertEquals(2, events.size());
   }

   @Test
   public void testFilter() {
      store.setTrackedTypes(ImmutableSet.of(DeviceCapability.NAMESPACE));
      
      store.update(baseAdded(DeviceCapability.NAMESPACE));
      store.update(baseAdded(RecordingCapability.NAMESPACE));
      
      assertEquals(1, store.getModels().size());
      assertEquals(1, events.size());
   }

   private PlatformMessage baseAdded(String namespace) {
      UUID id = UUID.randomUUID();
      Address address = Address.platformService(id, namespace);
      Map<String, Object> attributes = ImmutableMap.<String, Object>of(
            Capability.ATTR_ID, id,
            Capability.ATTR_ADDRESS, address,
            Capability.ATTR_TYPE, namespace
      );
      return 
            PlatformMessage
               .broadcast()
               .from(address)
               .withPayload(Capability.EVENT_ADDED, attributes)
               .create();
   }
}

