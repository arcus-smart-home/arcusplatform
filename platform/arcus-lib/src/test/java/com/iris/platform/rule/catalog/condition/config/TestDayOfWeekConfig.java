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
import com.iris.common.rule.filter.DayOfWeekFilter;
import com.iris.io.json.JSON;
import com.iris.platform.rule.catalog.serializer.json.RuleConfigJsonModule;
import com.iris.platform.rule.catalog.template.TemplatedExpression;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({ RuleConfigJsonModule.class })
public class TestDayOfWeekConfig extends IrisTestCase {

   DayOfWeekConfig createFullConfig() {
      DayOfWeekConfig config = new DayOfWeekConfig();
      config.setCondition(new ReceivedMessageConfig(new TemplatedExpression("SERV:note:"), new TemplatedExpression("note:Send")));
      config.setDayExpression(new TemplatedExpression("MONDAY,TUESDAY,${day}"));
      return config;
   }
   
   @Test
   public void testSerializeEmpty() {
      DayOfWeekConfig empty = new DayOfWeekConfig();
      String json = JSON.toJson(empty);
      System.out.println(json);
      assertEquals(empty, JSON.fromJson(json, ConditionConfig.class));
   }

   @Test
   public void testSerializeEverything() {
      DayOfWeekConfig config = createFullConfig();
      String json = JSON.toJson(config);
      System.out.println(json);
      assertEquals(config, JSON.fromJson(json, ConditionConfig.class));
   }

   @Test
   public void testInvalidConfig() {
      DayOfWeekConfig empty = new DayOfWeekConfig();
      try {
         empty.generate(ImmutableMap.of("day", "FEBTOBER"));
         fail("Allowed to be creqted with missing attributes");
      }
      catch(IllegalStateException e) {
         // expected
      }
   }

   @Test
   public void testItsAllAlright() {
      DayOfWeekConfig config = createFullConfig();
      Condition condition = config.generate(ImmutableMap.of("day", "WEDNESDAY"));
      assertTrue(condition instanceof DayOfWeekFilter);
   }
}

