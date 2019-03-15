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
import static org.junit.Assert.fail;

import org.junit.Test;

import com.iris.platform.rule.catalog.condition.config.OrConfig;
import com.iris.platform.rule.catalog.condition.config.ReceivedMessageConfig;
import com.iris.serializer.sax.SAXTagHandlers;
import com.iris.validators.ValidationException;
import com.iris.validators.Validator;

/**
 * 
 */
public class TestOrConditionProcessor {
   ConditionsProcessor processor = new ConditionsProcessor(new Validator());

   @Test
   public void testEmptyTag() {
      try {
         SAXTagHandlers.parse("<or />", processor);
         fail();
      }
      catch(ValidationException e) {
         // expected
         e.printStackTrace(System.out);
      }
   }
   
   @Test
   public void testOneCondition() {
      try {
         SAXTagHandlers.parse(
               "<or>" +
               "  <received message=\"subspres:PlaceOccupied\" from=\"SERV:subspres:${_placeId}\" />" +
               "</or>", 
               processor);
         fail();
      }
      catch(ValidationException e) {
         // expected
         e.printStackTrace(System.out);
      }
   }
   
   @Test
   public void testSimpleTypes() throws Exception {
      SAXTagHandlers.parse(
            "<or>" +
            "  <received message=\"subspres:PlaceOccupied\" from=\"SERV:subspres:${_placeId}\" />" +
            "  <received message=\"subspres:PlaceOccupied\" from=\"SERV:subspres:${_placeId}\" />" +
            "</or>",
            processor
      );
      OrConfig config = (OrConfig) processor.getCondition();
      assertEquals(2, config.getConfigs().size());
      assertEquals(ReceivedMessageConfig.TYPE, config.getConfigs().get(0).getType());
      assertEquals(ReceivedMessageConfig.TYPE, config.getConfigs().get(1).getType());
   }
   
}

