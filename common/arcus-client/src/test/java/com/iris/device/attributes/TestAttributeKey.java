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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.ParameterizedType;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.junit.Test;

import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeTypes;

/**
 *
 */
public class TestAttributeKey {

   private byte[] write(Object o) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      try {
         oos.writeObject(o);
      }
      finally {
         oos.close();
      }
      return baos.toByteArray();
   }
   
   private Object read(byte[] bytes) throws IOException, ClassNotFoundException {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      ObjectInputStream ois = new ObjectInputStream(bais);
      try {
         return ois.readObject();
      }
      finally {
         ois.close();
      }
   }
   
   // TODO this isn't really legal...
   @Test
   public void testNameOnly() throws Exception {
      AttributeKey<String> key = AttributeKey.create("id", String.class);
      assertEquals("id", key.getName());
      assertEquals("", key.getNamespace());
      assertEquals("id", key.getId());
      assertFalse(key.isInstance());
      assertEquals(null, key.getInstance());
   }
   
   @Test
   public void testNamespaced() throws Exception {
      AttributeKey<String> key = AttributeKey.create("namespace:id", String.class);
      assertEquals("namespace:id", key.getName());
      assertEquals("namespace", key.getNamespace());
      assertEquals("id", key.getId());
      assertFalse(key.isInstance());
      assertEquals(null, key.getInstance());
   }
   
   @Test
   public void testInstance() throws Exception {
      AttributeKey<String> key = AttributeKey.create("namespace:id:instance", String.class);
      assertEquals("namespace:id:instance", key.getName());
      assertEquals("namespace", key.getNamespace());
      assertEquals("id", key.getId());
      assertTrue(key.isInstance());
      assertEquals("instance", key.getInstance());
   }
   
   @Test
   public void testHashCodeAndEquals() throws Exception {
      {
         AttributeKey<String> key1 = AttributeKey.create("base:key", String.class);
         AttributeKey<?> key2 = AttributeKey.createType("base:key", AttributeTypes.stringType().getJavaType());
         assertEquals(key1, key2);
         assertEquals(key1.hashCode(), key2.hashCode());
      }
      
      {
         AttributeKey<Set<String>> key1 = AttributeKey.createSetOf("base:key", String.class);
         AttributeKey<?> key2 = AttributeKey.createType("base:key", AttributeTypes.setOf(AttributeTypes.stringType()).getJavaType());
         assertEquals(key1, key2);
         assertEquals(key1.hashCode(), key2.hashCode());
      }

      {
         AttributeKey<?> key1 = AttributeKey.createType(
               "base:key", 
               TypeUtils.parameterize(Map.class, String.class, TypeUtils.parameterize(List.class, Object.class))
         );
         AttributeKey<?> key2 = AttributeKey.createType("base:key", AttributeTypes.mapOf(AttributeTypes.listOf(AttributeTypes.anyType())).getJavaType());
         assertEquals(key1, key2);
         assertEquals(key1.hashCode(), key2.hashCode());
      }
   }
   
   @Test
   public void testSerializeSimple() throws Exception {
      byte [] bytes = write(AttributeKey.create("string", String.class));
      AttributeKey<?> key = (AttributeKey<?>) read(bytes);
      assertEquals("string", key.getName());
      assertEquals(String.class, key.getType());
   }
   
   @Test
   public void testSerializeSet() throws Exception {
      byte [] bytes = write(AttributeKey.createSetOf("ByteSet", Byte.class));
      AttributeKey<?> key = (AttributeKey<?>) read(bytes);
      assertEquals("ByteSet", key.getName());
      assertEquals(TypeUtils.parameterize(Set.class, Byte.class), key.getType());
   }
   
   @Test
   public void testSerializeList() throws Exception {
      byte [] bytes = write(AttributeKey.createListOf("StringList", String.class));
      AttributeKey<?> key = (AttributeKey<?>) read(bytes);
      assertEquals("StringList", key.getName());
      assertEquals(TypeUtils.parameterize(List.class, String.class), key.getType());
   }
   
   @Test
   public void testSerializeMap() throws Exception {
      byte [] bytes = write(AttributeKey.createMapOf("DateMap", Date.class));
      AttributeKey<?> key = (AttributeKey<?>) read(bytes);
      assertEquals("DateMap", key.getName());
      assertEquals(TypeUtils.parameterize(Map.class, String.class, Date.class), key.getType());
   }
   
}

