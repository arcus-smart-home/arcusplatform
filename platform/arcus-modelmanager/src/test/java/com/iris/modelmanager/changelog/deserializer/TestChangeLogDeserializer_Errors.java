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
package com.iris.modelmanager.changelog.deserializer;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;

public class TestChangeLogDeserializer_Errors extends BaseChangeLogDeserializerTestCase {

   @Test(expected=XMLStreamException.class)
   public void testValidationFailureNoVersion() throws Exception {
      deserializer.deserialize("invalid-noversion.xml");
   }

   @Test(expected=XMLStreamException.class)
   public void testValidationFailureNoFileOnImport() throws Exception {
      deserializer.deserialize("invalid-nofileonimport.xml");
   }

   @Test(expected=XMLStreamException.class)
   public void testValidationFailureNoAuthor() throws Exception {
      deserializer.deserialize("invalid-noauthor.xml");
   }

   @Test(expected=XMLStreamException.class)
   public void testValidationFailureNoIdentifier() throws Exception {
      deserializer.deserialize("invalid-noidentifier.xml");
   }

   @Test(expected=XMLStreamException.class)
   public void testValidationFailureNoUpgrades() throws Exception {
      deserializer.deserialize("invalid-noupgrades.xml");
   }

   @Test(expected=XMLStreamException.class)
   public void testValidationFailureNoRollbacks() throws Exception {
      deserializer.deserialize("invalid-norollbacks.xml");
   }

   @Test(expected=XMLStreamException.class)
   public void testValidationFailureEmptyVersion() throws Exception {
      deserializer.deserialize("invalid-emptyversion.xml");
   }

   @Test(expected=XMLStreamException.class)
   public void testValidationFailureEmptyAuthor() throws Exception {
      deserializer.deserialize("invalid-emptyauthor.xml");
   }

   @Test(expected=XMLStreamException.class)
   public void testValidationFailureEmptyIdentifier() throws Exception {
      deserializer.deserialize("invalid-emptyidentifier.xml");
   }

   @Test(expected=XMLStreamException.class)
   public void testValidationFailureEmptyFileOnImport() throws Exception {
      deserializer.deserialize("invalid-emptyfileonimport.xml");
   }

   @Test(expected=XMLStreamException.class)
   public void testValidationFailureImportedFileInvalid() throws Exception {
      deserializer.deserialize("invalid-imported.xml");
   }

   @Test(expected=XMLStreamException.class)
   public void testValidationFailureMixImportAndChangeSets() throws Exception {
      deserializer.deserialize("invalid-miximportwithchangesets.xml");
   }

   @Test(expected=XMLStreamException.class)
   public void testValidationFailureDuplicateChangeSets() throws Exception {
      deserializer.deserialize("invalid-duplicatechangesets.xml");
   }
}

