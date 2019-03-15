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
package com.iris.platform.rule.catalog.condition.config;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.trigger.ValueChangeTrigger;
import com.iris.io.json.JSON;
import com.iris.platform.rule.catalog.serializer.json.RuleConfigJsonModule;
import com.iris.platform.rule.catalog.template.TemplatedExpression;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({ RuleConfigJsonModule.class })
public class TestValueChangeConfig extends IrisTestCase {

   ValueChangeConfig createFullConfig() {
      ValueChangeConfig config = new ValueChangeConfig();
      config.setQueryExpression(new TemplatedExpression("base:address == '${address}'"));
      config.setAttributeExpression(new TemplatedExpression("${attribute}"));
      config.setOldValueExpression(new TemplatedExpression("${old}"));
      config.setNewValueExpression(new TemplatedExpression("${new}"));
      return config;
   }
   
   @Test
   public void testSerializeEmpty() {
      ValueChangeConfig empty = new ValueChangeConfig();
      String json = JSON.toJson(empty);
      System.out.println(json);
      assertEquals(empty, JSON.fromJson(json, ConditionConfig.class));
   }

   @Test
   public void testSerializeEverything() {
      ValueChangeConfig config = createFullConfig();
      String json = JSON.toJson(config);
      System.out.println(json);
      assertEquals(config, JSON.fromJson(json, ConditionConfig.class));
   }

   @Test
   public void testInvalidConfig() {
      ValueChangeConfig empty = new ValueChangeConfig();
      try {
         empty.generate(ImmutableMap.of());
         fail("Allowed to be creqted with missing attributes");
      }
      catch(IllegalStateException e) {
         // expected
      }
   }

   @Test
   public void testItsAllAlright() {
      ValueChangeConfig config = createFullConfig();
      Condition condition = config.generate(ImmutableMap.of(
            "address", "SERV:note:",
            "attribute", "base:ValueChange",
            "new", "new",
            "old", "old"
      ));
      
      assertTrue(condition instanceof ValueChangeTrigger);
   }
}

