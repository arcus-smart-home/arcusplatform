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
package com.iris.platform.rule.catalog.serializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.iris.platform.rule.catalog.selector.ListSelectorGenerator;
import com.iris.platform.rule.catalog.selector.PresenceSelectorGenerator;
import com.iris.platform.rule.catalog.selector.SelectorType;
import com.iris.serializer.sax.SAXTagHandlers;
import com.iris.validators.ValidationException;
import com.iris.validators.Validator;

/**
 * 
 */
public class TestSelectorProcessor {
   SelectorProcessor processor = new SelectorProcessor(new Validator());

   @Test
   public void testEmptyTag() {
      try {
         SAXTagHandlers.parse("<selector />", SelectorProcessor.TAG, processor);
         fail();
      }
      catch(ValidationException e) {
         // expected
         e.printStackTrace(System.out);
      }
   }
   
   @Test
   public void testSimpleTypes() throws Exception {
      {
         String xml = "<selector name='name' type='" + SelectorProcessor.TYPE_TIME + "' />";
         SAXTagHandlers.parse(xml, SelectorProcessor.TAG, processor);
         assertEquals("name", processor.getName());
         assertEquals(SelectorType.TIME_OF_DAY, processor.getSelectorGenerator().generate(null).getType());
      }
      {
         String xml = "<selector name='name' type='" + SelectorProcessor.TYPE_DAY + "' />";
         SAXTagHandlers.parse(xml, SelectorProcessor.TAG, processor);
         assertEquals("name", processor.getName());
         assertEquals(SelectorType.DAY_OF_WEEK, processor.getSelectorGenerator().generate(null).getType());
      }
      {
         String xml = "<selector name='name' type='" + SelectorProcessor.TYPE_DURATION + "' />";
         SAXTagHandlers.parse(xml, SelectorProcessor.TAG, processor);
         assertEquals("name", processor.getName());
         assertEquals(SelectorType.DURATION, processor.getSelectorGenerator().generate(null).getType());
      }
      {
         String xml = "<selector name='name' type='" + SelectorProcessor.TYPE_TIME_RANGE + "' />";
         SAXTagHandlers.parse(xml, SelectorProcessor.TAG, processor);
         assertEquals("name", processor.getName());
         assertEquals(SelectorType.TIME_RANGE, processor.getSelectorGenerator().generate(null).getType());
      }
   }
   
   @Test
   public void testPersonTypes() throws Exception {
      String xml = "<selector name='name' type='" + SelectorProcessor.TYPE_PERSON + "' />";
      SAXTagHandlers.parse(xml, SelectorProcessor.TAG, processor);
      assertEquals("name", processor.getName());
      ListSelectorGenerator generator = (ListSelectorGenerator) processor.getSelectorGenerator();
      assertNotNull(generator);
      // TODO make assertions about the state of the generator?
   }
   
   @Test
   public void testDeviceTypes() throws Exception {
      String xml = "<selector name='name' type='" + SelectorProcessor.TYPE_DEVICE + "' query='mot:motion is supported' />";
      SAXTagHandlers.parse(xml, SelectorProcessor.TAG, processor);
      assertEquals("name", processor.getName());
      ListSelectorGenerator generator = (ListSelectorGenerator) processor.getSelectorGenerator();
      assertNotNull(generator);
      // TODO make assertions about the state of the generator?
   }
   
   @Test
   public void testSceneTypes() throws Exception {
      String xml = "<selector name='name' type='" + SelectorProcessor.TYPE_SCENE + "' />";
      SAXTagHandlers.parse(xml, SelectorProcessor.TAG, processor);
      assertEquals("name", processor.getName());
      ListSelectorGenerator generator = (ListSelectorGenerator) processor.getSelectorGenerator();
      assertNotNull(generator);
      // TODO make assertions about the state of the generator?
   }
   
   @Test
   public void testPresenceType() throws Exception {
      String xml = "<selector name='name' type='" + SelectorProcessor.TYPE_PRESENCE + "' />";
      SAXTagHandlers.parse(xml, SelectorProcessor.TAG, processor);
      assertEquals("name", processor.getName());
      PresenceSelectorGenerator generator = (PresenceSelectorGenerator) processor.getSelectorGenerator();
      assertNotNull(generator);
   }

}

