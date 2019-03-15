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
package com.iris.capability.attribute.transform;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.inject.Inject;
import com.iris.device.attributes.AttributeMap;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules(AttributeMapTransformModule.class)
public class TestCapabilityDrivenAttributeMapTransformer extends IrisTestCase {

   @Inject
   private AttributeMapTransformer transformer;

   @Test
   public void testTransformToMap() {
      assertNull(transformer.transformFromAttributeMap(null));
      Map<String,Object> map = transformer.transformFromAttributeMap(AttributeMap.emptyMap());
      assertTrue(map.isEmpty());
      map = transformer.transformFromAttributeMap(AttributeMap.newMap());
      assertTrue(map.isEmpty());

      AttributeMap attributeMap = AttributeMap.newMap();
      attributeMap.set(ContactCapability.KEY_CONTACT, ContactCapability.CONTACT_OPENED);
      attributeMap.set(TemperatureCapability.KEY_TEMPERATURE, 32.2);

      map = transformer.transformFromAttributeMap(attributeMap);
      assertEquals(ContactCapability.CONTACT_OPENED, map.get(ContactCapability.ATTR_CONTACT));
      assertEquals(32.2, map.get(TemperatureCapability.ATTR_TEMPERATURE));
   }

   @Test
   public void testTransformToMapWithInstances() {
      assertNull(transformer.transformFromAttributeMap(null));
      Map<String,Object> map = transformer.transformFromAttributeMap(AttributeMap.emptyMap());
      assertTrue(map.isEmpty());
      map = transformer.transformFromAttributeMap(AttributeMap.newMap());
      assertTrue(map.isEmpty());

      AttributeMap attributeMap = AttributeMap.newMap();
      attributeMap.set(ContactCapability.KEY_CONTACT.instance("id1"), ContactCapability.CONTACT_OPENED);
      attributeMap.set(ContactCapability.KEY_CONTACT.instance("id2"), ContactCapability.CONTACT_CLOSED);
      attributeMap.set(TemperatureCapability.KEY_TEMPERATURE.instance("id3"), 32.2);

      map = transformer.transformFromAttributeMap(attributeMap);
      assertEquals(ContactCapability.CONTACT_OPENED, map.get(ContactCapability.KEY_CONTACT.instance("id1").getName()));
      assertEquals(ContactCapability.CONTACT_CLOSED, map.get(ContactCapability.KEY_CONTACT.instance("id2").getName()));
      assertEquals(32.2, map.get(TemperatureCapability.KEY_TEMPERATURE.instance("id3").getName()));
   }

   @Test
   public void testTransformFromMap() {
      assertNull(transformer.transformFromAttributeMap(null));
      AttributeMap map = transformer.transformToAttributeMap(Collections.<String,Object>emptyMap());
      assertTrue(map.isEmpty());

      Map<String,Object> attributes = new HashMap<>();
      attributes.put("cont:contact", "OPENED");
      attributes.put("temp:temperature", 32.2d);

      map = transformer.transformToAttributeMap(attributes);
      assertEquals(ContactCapability.CONTACT_OPENED, map.get(ContactCapability.KEY_CONTACT));
      assertEquals(Double.valueOf(32.2), map.get(TemperatureCapability.KEY_TEMPERATURE));
   }

   @Test
   public void testTransformFromMapWithInstances() {
      Map<String,Object> attributes = new HashMap<>();
      attributes.put("cont:contact:id1", "OPENED");
      attributes.put("cont:contact:id2", "CLOSED");
      attributes.put("temp:temperature:id3", 32.2);

      AttributeMap map = transformer.transformToAttributeMap(attributes);
      assertEquals(ContactCapability.CONTACT_OPENED, map.get(ContactCapability.KEY_CONTACT.instance("id1")));
      assertEquals(ContactCapability.CONTACT_CLOSED, map.get(ContactCapability.KEY_CONTACT.instance("id2")));
      assertEquals(Double.valueOf(32.2), map.get(TemperatureCapability.KEY_TEMPERATURE.instance("id3")));
   }

   @Test(expected=IllegalArgumentException.class)
   public void testTransformFromMapInvalidEnumValue() {
      Map<String,Object> attributes = new HashMap<>();
      attributes.put("cont:contact", "ON");
      attributes.put("temp:temperature", 32.2d);
      transformer.transformToAttributeMap(attributes);
   }

   @Test
   public void testTransformFromMapNoCapability() {
      Map<String,Object> attributes = new HashMap<>();
      attributes.put("foo:contact", "ON");
      AttributeMap map = transformer.transformToAttributeMap(attributes);
      // should be empty because foo doesn't exist and has been stripped out
      assertTrue(map.isEmpty());
   }

   @Test
   public void testTransformFromMapNoAttributeDefined() {
      Map<String,Object> attributes = new HashMap<>();
      attributes.put("cont:foo", "ON");
      AttributeMap map = transformer.transformToAttributeMap(attributes);
      // should be empty because cont:foo doesn't exist and has been stripped out
      assertTrue(map.isEmpty());
   }
}

