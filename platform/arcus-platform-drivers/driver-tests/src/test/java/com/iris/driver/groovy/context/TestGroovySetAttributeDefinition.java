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
package com.iris.driver.groovy.context;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.AttributeFlag;
import com.iris.model.type.MapType;
import com.iris.model.type.SetType;
import com.iris.model.type.StringType;

/**
 * 
 */
public class TestGroovySetAttributeDefinition extends GroovyAttributeDefinitionTestCase {
   private AttributeDefinition definition =
         new AttributeDefinition(
               AttributeKey.createSetOf("test:attribute", String.class),
               EnumSet.<AttributeFlag>of(AttributeFlag.READABLE, AttributeFlag.OPTIONAL),
               "description",
               "",
               new SetType(StringType.INSTANCE)
         );
   
   private AttributeDefinition definitionForMapType =
	         new AttributeDefinition(
	                 AttributeKey.createMapOf("test:attribute", String.class),
	                 EnumSet.<AttributeFlag>of(AttributeFlag.READABLE, AttributeFlag.OPTIONAL),
	                 "description",
	                 "",
	                 new MapType(StringType.INSTANCE)
	           );
   private GroovyAttributeDefinition uut;         
   private GroovyAttributeDefinition uutForMapType;
   
   @Before
   public void setUp() throws Exception {
      super.setUp();
      uut = new GroovyAttributeDefinition(definition, binding);
      uutForMapType = new GroovyAttributeDefinition(definitionForMapType, binding);
   }

   @Override
   @After
   public void tearDown() throws Exception {
      super.tearDown();
   }

   @Test
   public void testSet() {
      assertEquals(null, uut.get());
      
      uut.set(ImmutableSet.of("A string"));
      assertEquals(ImmutableSet.of("A string"), uut.get());
      assertEquals(ImmutableSet.of(definition.getKey()), context.getDirtyAttributes().keySet());
      context.clearDirty();
      
      uut.set(ImmutableList.of(true));
      assertEquals(ImmutableSet.of("true"), uut.get());
      assertEquals(ImmutableSet.of(definition.getKey()), context.getDirtyAttributes().keySet());
      context.clearDirty();
   }
   
   @Test
   public void testSetAndPutForMapType() {
      assertEquals(null, uutForMapType.get());
      Map<String, String> expectedValue = ImmutableMap.<String, String> of("key1","value1","key2","value2","key3","value3");
      uutForMapType.set(expectedValue);
      Object value = uutForMapType.get();
      assertNotNull(value);
      assertTrue(value instanceof Map<?, ?>);
      assertTrue(Maps.difference(expectedValue, (Map<?, ?>)value).areEqual());
      assertEquals(ImmutableSet.of(definitionForMapType.getKey()), context.getDirtyAttributes().keySet());
      context.clearDirty();
      
      uutForMapType.put("key4", "value4");
      assertEquals(ImmutableSet.of(definitionForMapType.getKey()), context.getDirtyAttributes().keySet());
      ((Map<String, String>)uutForMapType.get()).containsKey("key4");
      context.clearDirty();
      
   }

   @Test
   public void testAdd() {
      assertEquals(null, uut.get());
      
      uut.add("tag1");
      assertEquals(ImmutableSet.of("tag1"), uut.get());
      assertEquals(ImmutableSet.of(definition.getKey()), context.getDirtyAttributes().keySet());
      context.clearDirty();
      
      uut.add("tag2");
      assertEquals(ImmutableSet.of("tag1", "tag2"), uut.get());
      assertEquals(ImmutableSet.of(definition.getKey()), context.getDirtyAttributes().keySet());
      context.clearDirty();
      
      // repeat, shouldn't be dirty
      uut.add("tag1");
      assertEquals(ImmutableSet.of("tag1", "tag2"), uut.get());
      assertEquals(ImmutableSet.of(), context.getDirtyAttributes().keySet());
   }

   @Test
   public void testAddAll() {
      assertEquals(null, uut.get());
      
      uut.addAll(ImmutableSet.of("tag1", "tag2"));
      assertEquals(ImmutableSet.of("tag1", "tag2"), uut.get());
      assertEquals(ImmutableSet.of(definition.getKey()), context.getDirtyAttributes().keySet());
      context.clearDirty();
      
      uut.addAll(ImmutableList.of("tag2", "tag3"));
      assertEquals(ImmutableSet.of("tag1", "tag2", "tag3"), uut.get());
      assertEquals(ImmutableSet.of(definition.getKey()), context.getDirtyAttributes().keySet());
      context.clearDirty();
      
      // repeat, shouldn't be dirty
      uut.addAll(ImmutableList.of("tag1", "tag2", "tag3"));
      assertEquals(ImmutableSet.of("tag1", "tag2", "tag3"), uut.get());
      assertEquals(ImmutableSet.of(), context.getDirtyAttributes().keySet());
   }

   @Test
   public void testRemove() {
      assertEquals(null, uut.get());
      
      // no values, remove shouldn't do anything
      uut.remove("tag1");
      assertEquals(null, uut.get());
      assertEquals(ImmutableSet.of(), context.getDirtyAttributes().keySet());
      
      // add some values
      context.setAttributeValue(definition, ImmutableSet.of("tag1", "tag2"));
      context.clearDirty();
      
      uut.remove("tag1");
      assertEquals(ImmutableSet.of("tag2"), uut.get());
      assertEquals(ImmutableSet.of(definition.getKey()), context.getDirtyAttributes().keySet());
      context.clearDirty();
      
      // already removed, can't remove again
      uut.remove("tag1");
      assertEquals(ImmutableSet.of("tag2"), uut.get());
      assertEquals(ImmutableSet.of(), context.getDirtyAttributes().keySet());
      context.clearDirty();
   }
   
   
   @Test
   public void testRemoveForMapType() {
      assertEquals(null, uutForMapType.get());
      
      // no values, remove shouldn't do anything
      uut.remove("key1");
      assertEquals(null, uut.get());
      assertEquals(ImmutableSet.of(), context.getDirtyAttributes().keySet());
      
      // add some values
      Map<String, String> expectedValue = ImmutableMap.<String, String> of("key1","value1","key2","value2","key3","value3");
      uutForMapType.set(expectedValue);
      context.clearDirty();
      
      uutForMapType.remove("key1");
      Map<String, String>returnValue = (Map<String, String>) uutForMapType.get();
      assertFalse(returnValue.containsKey("key1"));
      assertTrue(returnValue.containsKey("key2"));
      assertTrue(returnValue.containsKey("key3"));
      assertEquals(ImmutableSet.of(definitionForMapType.getKey()), context.getDirtyAttributes().keySet());
      context.clearDirty();
            
      // already removed, can't remove again
      uutForMapType.remove("key1");
      returnValue = (Map<String, String>) uutForMapType.get();
      assertFalse(returnValue.containsKey("key1"));
      assertTrue(returnValue.containsKey("key2"));
      assertTrue(returnValue.containsKey("key3"));
      assertEquals(ImmutableSet.of(), context.getDirtyAttributes().keySet());
      context.clearDirty();
      
   }

   @Test
   public void testRemoveAll() {
      assertEquals(null, uut.get());
      
      // no values, remove shouldn't do anything
      uut.removeAll(ImmutableSet.of("tag1", "tag2", "tag3"));
      assertEquals(null, uut.get());
      assertEquals(ImmutableSet.of(), context.getDirtyAttributes().keySet());
      
      // add some values
      context.setAttributeValue(definition, ImmutableSet.of("tag1", "tag2", "tag3"));
      context.clearDirty();
      
      uut.removeAll(ImmutableSet.of("tag1", "tag2"));
      assertEquals(ImmutableSet.of("tag3"), uut.get());
      assertEquals(ImmutableSet.of(definition.getKey()), context.getDirtyAttributes().keySet());
      context.clearDirty();
      
      uut.removeAll(ImmutableList.of("tag2", "tag3"));
      assertEquals(ImmutableSet.of(), uut.get());
      assertEquals(ImmutableSet.of(definition.getKey()), context.getDirtyAttributes().keySet());
      context.clearDirty();
      
      // already removed, can't remove again
      uut.removeAll(ImmutableSet.of("tag1", "tag2", "tag3"));
      assertEquals(ImmutableSet.of(), uut.get());
      assertEquals(ImmutableSet.of(), context.getDirtyAttributes().keySet());
      context.clearDirty();
   }

}

