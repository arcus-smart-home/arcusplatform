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

import java.util.EnumSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.AttributeFlag;
import com.iris.model.type.StringType;

/**
 * 
 */
public class TestGroovyStringAttributeDefinition extends GroovyAttributeDefinitionTestCase {
   private AttributeDefinition definition =
         new AttributeDefinition(
               AttributeKey.create("test:attribute", String.class),
               EnumSet.<AttributeFlag>of(AttributeFlag.READABLE, AttributeFlag.OPTIONAL),
               "description",
               "",
               StringType.INSTANCE
         );
   private GroovyAttributeDefinition uut;            
   
   @Before
   public void setUp() throws Exception {
      super.setUp();
      uut = new GroovyAttributeDefinition(definition, binding);
   }

   @Override
   @After
   public void tearDown() throws Exception {
      super.tearDown();
   }
   
   @Test
   public void testSet() {
      assertEquals(null, uut.get());
      uut.set("A string");
      assertEquals("A string", uut.get());
      assertEquals(ImmutableSet.of(definition.getKey()), context.getDirtyAttributes().keySet());
      context.clearDirty();
      
      uut.set(true);
      assertEquals("true", uut.get());
      assertEquals(ImmutableSet.of(definition.getKey()), context.getDirtyAttributes().keySet());
      context.clearDirty();
   }

   @Test
   public void testAdd() {
      try {
         uut.add("another string");
         fail();
      }
      catch(IllegalArgumentException e) {
         // expected
      }
   }

   @Test
   public void testAddAll() {
      try {
         uut.addAll(ImmutableSet.of("set1", "set2"));
         fail();
      }
      catch(IllegalArgumentException e) {
         // expected
      }
   }

   @Test
   public void testRemove() {
      try {
         uut.remove("another string");
         fail();
      }
      catch(IllegalArgumentException e) {
         // expected
      }
   }

   @Test
   public void testRemoveAll() {
      try {
         uut.removeAll(ImmutableSet.of("set1", "set2"));
         fail();
      }
      catch(IllegalArgumentException e) {
         // expected
      }
   }

}

