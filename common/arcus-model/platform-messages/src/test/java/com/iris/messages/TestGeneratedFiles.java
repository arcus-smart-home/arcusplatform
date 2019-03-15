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
package com.iris.messages;

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
   private DefinitionVerificationUtil verificationUtil = DefinitionVerificationUtil.instance();

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
         verificationUtil.assertCapabilityDefinitionMatches(expected);
         
      }
   }
   
   @Test
   public void testServiceDefinitions() throws Exception {
      // make sure they all load
      for(Definition definition: definitions) {
         if(!(definition instanceof ServiceDefinition)) {
            continue;
         }
         
         ServiceDefinition expected = (ServiceDefinition) definition;
         verificationUtil.assertServiceDefinitionMatches(expected);
      }
   }
   
   // TODO test nested classes?
}

