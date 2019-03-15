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
package com.iris.platform.rule.catalog.action.config;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.capability.definition.AttributeTypes;
import com.iris.common.rule.action.stateful.SetAndRestore;
import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.io.json.JSON;
import com.iris.platform.rule.catalog.serializer.json.RuleConfigJsonModule;
import com.iris.platform.rule.catalog.template.TemplatedExpression;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules( RuleConfigJsonModule.class )
public class TestSetAttributeActionConfig extends IrisTestCase {

   SetAttributeActionConfig createFullConfig() {
      SetAttributeActionConfig config = new SetAttributeActionConfig();
      config.setAddress(new TemplatedExpression("${device}"));
      config.setAttributeName("swit:state");
      config.setAttributeType(AttributeTypes.enumOf(Arrays.asList("ON", "OFF")));
      config.setAttributeValue(new TemplatedExpression("${state}"));
      config.setDuration(30);
      config.setUnit(TimeUnit.SECONDS);
      return config;
   }
   
   @Test
   public void testSerializeEmpty() {
      SetAttributeActionConfig empty = new SetAttributeActionConfig();
      String json = JSON.toJson(empty);
      System.out.println(json);
      assertEquals(empty, JSON.fromJson(json, ActionConfig.class));
   }

   @Test
   public void testSerializeEverything() {
      SetAttributeActionConfig config = createFullConfig();
      String json = JSON.toJson(config);
      System.out.println(json);
      ActionConfig deser = JSON.fromJson(json, ActionConfig.class);
      System.out.println(config);
      System.out.println(deser);
      assertEquals(config, deser);
   }

   @Test
   public void testCreateEmpty() {
      SetAttributeActionConfig empty = new SetAttributeActionConfig();
      try {
         empty.createAction(ImmutableMap.of());
         fail("Allowed to be creqted without required params");
      }
      catch(IllegalStateException e) {
         // expected
      }
   }

   @Test
   public void testCreateInvalidValue() {
      SetAttributeActionConfig config = createFullConfig();
      config.setAttributeValue(new TemplatedExpression("INVALIDENUM"));
      try {
         config.createAction(ImmutableMap.of());
         fail("Allowed to be creqted with an invalid enum");
      }
      catch(IllegalArgumentException e) {
         // expected
      }
   }

   @Test
   public void testCreateWithoutDuration() {
      SetAttributeActionConfig config = createFullConfig();
      config.setDuration(0);
      
      StatefulAction action = config.createAction(ImmutableMap.of());
      
      assertTrue(action instanceof SetAndRestore);
   }

   @Test
   public void testCreateWithDuration() {
      SetAttributeActionConfig config = createFullConfig();
      StatefulAction action = config.createAction(null);
      
      assertTrue(action instanceof SetAndRestore);
   }
}

