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
package com.iris.device.attributes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;

import org.junit.Test;

/**
 *
 */
public class TestAttributeMap {

   @Test
   public void testEmptyMap() throws Exception {
      AttributeMap map = AttributeMap.emptyMap();
      assertEquals(0, map.size());
      assertTrue(map.isEmpty());
      assertEquals(Collections.emptySet(), map.keySet());
      assertFalse(map.entries().iterator().hasNext());
      
      try {
         map.set(AttributeKey.create("test", String.class), "value");
         fail();
      }
      catch(UnsupportedOperationException e) {
         //expected
      }

      assertEquals(0, map.size());
      assertTrue(map.isEmpty());
      assertEquals(Collections.emptySet(), map.keySet());
      assertFalse(map.entries().iterator().hasNext());
   }

   @Test
   public void testMapOf() throws Exception {
      AttributeKey<String> testKey = AttributeKey.create("test", String.class);
      
      AttributeMap map = AttributeMap.mapOf();
      assertEquals(AttributeMap.emptyMap(), map);
      
      // modifiable
      map.set(testKey, "value");
      
      assertEquals(map, AttributeMap.mapOf(testKey.valueOf("value")));
   }

   @Test
   public void testUnmodifiableCopy() throws Exception {
      AttributeMap map = AttributeMap.newMap();
      AttributeMap copy = AttributeMap.unmodifiableCopy(map);
      
      assertEquals(map, copy);
      
      map.set(AttributeKey.create("test", String.class), "value");

      assertTrue(copy.isEmpty());
      assertFalse(map.isEmpty());
   }

   @Test
   public void testEquivalentKeys() throws Exception {
      AttributeMap map = AttributeMap.mapOf(AttributeKey.create("test", String.class).valueOf("value"));
      
      assertTrue(map.containsKey(AttributeKey.create("test", String.class)));
      assertEquals("value", map.get(AttributeKey.create("test", String.class)));
      assertEquals("value", map.set(AttributeKey.create("test", String.class), "value2"));
      assertEquals("value2", map.remove(AttributeKey.create("test", String.class)));
   }

   @Test
   public void testSimilarKeys() throws Exception {
      AttributeKey<String> stringKey = AttributeKey.create("test", String.class);
      AttributeKey<Integer> integerKey = AttributeKey.create("test", Integer.class);
      
      AttributeMap map = AttributeMap.mapOf(
            stringKey.valueOf("value"),
            integerKey.valueOf(10)
      );
      
      assertEquals(2, map.size());
      assertEquals("value", map.get(stringKey));
      assertEquals(Integer.valueOf(10), map.get(integerKey));
   }

}

