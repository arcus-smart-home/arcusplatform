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
package com.iris.platform.rule;

import java.util.EnumSet;

import org.junit.Test;

import com.google.inject.Inject;
import com.iris.common.rule.condition.AlwaysFireCondition;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.condition.NeverFireCondition;
import com.iris.common.rule.filter.DayOfWeekFilter;
import com.iris.common.rule.filter.MatcherFilter;
import com.iris.common.rule.filter.TimeOfDayFilter;
import com.iris.common.rule.matcher.ModelPredicateMatcher;
import com.iris.common.rule.time.DayOfWeek;
import com.iris.common.rule.time.TimeOfDay;
import com.iris.common.rule.trigger.DurationTrigger;
import com.iris.common.rule.trigger.TimeOfDayTrigger;
import com.iris.common.rule.trigger.ValueChangeTrigger;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.model.predicate.Predicates;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

/**
 * 
 */
@Modules({ PlatformRuleModule.class })
public class TestConditionSerializer extends IrisTestCase {

   @Inject Serializer<Condition> serializer;
   @Inject Deserializer<Condition> deserializer;
   
   @Test
   public void testAlwaysFire() {
      Condition expected = AlwaysFireCondition.getInstance();
      Condition actual = deserializer.deserialize( serializer.serialize( expected ) );
      assertSame(expected, actual);
   }

   @Test
   public void testNeverFire() {
      Condition expected = NeverFireCondition.getInstance();
      Condition actual = deserializer.deserialize( serializer.serialize( expected ) );
      assertSame(expected, actual);
   }

   @Test
   public void testTimeOfDayTrigger() {
      TimeOfDayTrigger expected = new TimeOfDayTrigger(new TimeOfDay(20));
      Condition actual = deserializer.deserialize( serializer.serialize( expected ) );
      
      assertTrue(actual instanceof TimeOfDayTrigger);
      assertEquals(expected.toString(), actual.toString());
   }

   @Test
   public void testValueChangeTrigger() {
      ValueChangeTrigger expected = new ValueChangeTrigger("name", "oldValue", "newValue");
      Condition actual = deserializer.deserialize( serializer.serialize( expected ) );
      
      assertTrue(actual instanceof ValueChangeTrigger);
      assertEquals(expected, actual);
   }

   @Test
   public void testDurationTrigger() {
      DurationTrigger expected = new DurationTrigger(new ModelPredicateMatcher(Predicates.isDevice(), Predicates.attributeContains("name", 10)), 1000);
      Condition actual = deserializer.deserialize( serializer.serialize( expected ) );
      
      assertTrue(actual instanceof DurationTrigger);
      assertEquals(expected.toString(), actual.toString());
   }

   @Test
   public void testMatcherFilter() {
      MatcherFilter expected = new MatcherFilter(AlwaysFireCondition.getInstance(), new ModelPredicateMatcher(Predicates.isA("test"), Predicates.addressEquals("SERV:test:")));
      Condition actual = deserializer.deserialize( serializer.serialize( expected ) );
      
      assertTrue(actual instanceof MatcherFilter);
      assertEquals(expected.toString(), actual.toString());
   }

   @Test
   public void testTimeOfDayFilter() {
      Condition expected = new TimeOfDayFilter(AlwaysFireCondition.getInstance(), new TimeOfDay(9), new TimeOfDay(21));
      Condition actual = deserializer.deserialize( serializer.serialize( expected ) );
      
      assertTrue(actual instanceof TimeOfDayFilter);
      assertEquals(expected.toString(), actual.toString());
   }

   @Test
   public void testDayOfWeekFilter() {
      Condition expected = new DayOfWeekFilter(AlwaysFireCondition.getInstance(), EnumSet.allOf(DayOfWeek.class));
      Condition actual = deserializer.deserialize( serializer.serialize( expected ) );
      
      assertTrue(actual instanceof DayOfWeekFilter);
      assertEquals(expected.toString(), actual.toString());
   }

}

