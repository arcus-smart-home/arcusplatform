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
package com.iris.capability.definition;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.iris.capability.reader.GenericDefinitionReader;

/**
 * Tests that all the capability definitions are valid.
 */
public class TestDefinitionsAreValid {

   private static final String COMMON_PATH = "../src/main/resources";
   private static final String INTERNAL_PATH = "src/main/resources";

   GenericDefinitionReader reader = new GenericDefinitionReader();

   @Test
   public void testDefinitionsAreValid() {
      Map<String, String> errors = new HashMap<String, String>();
      
      validate(new File(COMMON_PATH), errors);
      validate(new File(INTERNAL_PATH), errors);
      if(!errors.isEmpty()) {
         fail("Invalid XML files: " + errors);
      }
   }

   // [rlg] ensure uniqueness within a single directory but not across directories to allow for merging of public
   // and private APIs separated.
   @Test
   public void testCapabilityDefinitionsAreUnique() {
      Map<String, String> nameToFile = new HashMap<String, String>();
      Map<String, String> namespaceToFile = new HashMap<String, String>();
      Map<String, String> errors = new HashMap<String, String>();
      validateUnique(new File(COMMON_PATH + "/capability"), nameToFile, namespaceToFile, errors);
      if(!errors.isEmpty()) {
         fail("Invalid XML files: " + errors);
      }

      nameToFile.clear();
      namespaceToFile.clear();
      errors.clear();
      validateUnique(new File(INTERNAL_PATH + "/capability"), nameToFile, namespaceToFile, errors);
      if(!errors.isEmpty()) {
         fail("Invalid XML files: " + errors);
      }
   }

   @Test
   public void testServiceDefinitionsAreUnique() {
      Map<String, String> nameToFile = new HashMap<String, String>();
      Map<String, String> namespaceToFile = new HashMap<String, String>();
      Map<String, String> errors = new HashMap<String, String>();
      validateUnique(new File(COMMON_PATH + "/service"), nameToFile, namespaceToFile, errors);
      if(!errors.isEmpty()) {
         fail("Invalid XML files: " + errors);
      }

      nameToFile.clear();
      namespaceToFile.clear();
      errors.clear();
      validateUnique(new File(INTERNAL_PATH + "/service"), nameToFile, namespaceToFile, errors);
      if(!errors.isEmpty()) {
         fail("Invalid XML files: " + errors);
      }
   }

   @Test
   public void testProtocolDefinitionsAreUnique() {
      Map<String, String> nameToFile = new HashMap<String, String>();
      Map<String, String> namespaceToFile = new HashMap<String, String>();
      Map<String, String> errors = new HashMap<String, String>();
      validateUnique(new File(COMMON_PATH + "/protocol"), nameToFile, namespaceToFile, errors);
      if(!errors.isEmpty()) {
         fail("Invalid XML files: " + errors);
      }

      nameToFile.clear();
      namespaceToFile.clear();
      errors.clear();
      validateUnique(new File(INTERNAL_PATH + "/protocol"), nameToFile, namespaceToFile, errors);
      if(!errors.isEmpty()) {
         fail("Invalid XML files: " + errors);
      }
   }

   @Test
   public void testTypeDefinitionsAreUnique() {
      Map<String, String> nameToFile = new HashMap<String, String>();
      Map<String, String> errors = new HashMap<String, String>();
      validateUnique(new File(COMMON_PATH + "/type"), nameToFile, Collections.<String,String>emptyMap(), errors);
      if(!errors.isEmpty()) {
         fail("Invalid XML files: " + errors);
      }

      nameToFile.clear();
      errors.clear();
      validateUnique(new File(INTERNAL_PATH + "/type"), nameToFile, Collections.<String,String>emptyMap(), errors);
      if(!errors.isEmpty()) {
         fail("Invalid XML files: " + errors);
      }
   }

   private void validateUnique(
         File folder, 
         Map<String, String> nameToFile, 
         Map<String, String> namespaceToFile,
         Map<String, String> errors
   ) {
      if(!folder.exists()) {
         return;
      }
      for(File child: folder.listFiles()) {
         if(child.isDirectory()) {
            validateUnique(child, nameToFile, namespaceToFile, errors);
         }
         else if(child.getName().endsWith(".xsd")) {
            continue;
         }
         else {
            Definition d = reader.readDefinition(child);
            String name = d.getName();
            String namespace = null;
            if(d instanceof ObjectDefinition) {
               namespace = ((ObjectDefinition) d).getNamespace();
            }
            else if(d instanceof ProtocolDefinition) {
               namespace = ((ProtocolDefinition) d).getNamespace();
            }
            
            if(nameToFile.containsKey(name)) {
               errors.put(child.getPath(), "Re-defines name [" + name + "] previously defined in [" + nameToFile.get(name) +"]");
            }
            else {
               nameToFile.put(name, child.getPath());
            }
            
            if(namespace == null) {
               continue;
            }
            if(namespaceToFile.containsKey(namespace)) {
               errors.put(child.getPath(), "Re-defines namespace [" + namespace + "] previously defined in [" + namespaceToFile.get(name) +"]");
            }
            else {
               namespaceToFile.put(namespace, child.getPath());
            }
         }
      }
   }

   private void validate(File file, Map<String, String> errors) {
      for(File child: file.listFiles()) {
         if(child.isDirectory()) {
            validate(child, errors);
         }
         else if(file.getName().toLowerCase().endsWith(".xml")) {
            try {
               reader.readDefinition(child);
            }
            catch(Exception e) {
               errors.put(child.getAbsolutePath(), e.getMessage());
            }
         }
      }
   }
}

