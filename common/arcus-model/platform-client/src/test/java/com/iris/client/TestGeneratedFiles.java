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
package com.iris.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.Definition;
import com.iris.capability.definition.ServiceDefinition;
import com.iris.capability.reader.GenericDefinitionReader;

/**
 * 
 */
public class TestGeneratedFiles {
   List<Definition> definitions;

   @Before
   public void setUp() throws Exception {
      GenericDefinitionReader capReader = new GenericDefinitionReader();
      definitions = capReader.readDefinitionsFromPath("../src/main/resources");
   }
   
   @Test
   public void testCapabilityDefinitions() throws Exception {
      for(Definition definition: definitions) {
         if(!(definition instanceof CapabilityDefinition)) {
            continue;
         }
         
         CapabilityDefinition expected = (CapabilityDefinition) definition;
         
         Class<?> cls = Class.forName("com.iris.client.capability." + definition.getName());
         Field field = cls.getField("DEFINITION");
         CapabilityDefinition actual = (CapabilityDefinition) field.get(null);
         assertNotNull(actual);
         assertEquals(expected.getName(), actual.getName());
         assertEquals(expected.getNamespace(), actual.getNamespace());
//         assertEquals(expected.getEnhances(), actual.getEnhances());
         // note these are not equal because the XML parsed version does
         // not include the namespace in the name
//         assertEquals(expected.getAttributes(), actual.getAttributes());
//         assertEquals(expected.getMethods(), actual.getMethods());
//         assertEquals(expected.getEvents(), actual.getEvents());
      }
   }
   
   @Test
   public void testServiceDefinitions() throws Exception {
      // make sure they all load
      for(Definition definition: definitions) {
         if(!(definition instanceof ServiceDefinition)) {
            continue;
         }
         
         Class<?> cls = Class.forName("com.iris.client.service." + definition.getName());
         Field field = cls.getField("DEFINITION");
         ServiceDefinition actual = (ServiceDefinition) field.get(null);
         assertNotNull(actual);
         assertEquals(definition.getName(), actual.getName());
         assertEquals(((ServiceDefinition)definition).getNamespace(), actual.getNamespace());
      }
   }
   
   @Test
   public void testTypes() {
      assertNotNull(Types.getCapabilities());
      assertNotNull(Types.getModels());
      assertNotNull(Types.getServices());
   }
   
   // TODO test nested classes?
}

