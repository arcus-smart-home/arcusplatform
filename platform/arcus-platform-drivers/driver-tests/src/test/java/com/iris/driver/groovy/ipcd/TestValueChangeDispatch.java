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
package com.iris.driver.groovy.ipcd;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.iris.protocol.ipcd.message.model.IpcdEvent;
import com.iris.util.Comparisons;

public class TestValueChangeDispatch extends IpcdHandlersTestCase {

   public TestValueChangeDispatch() {
      super("IpcdValueChangeDispatchHandler.driver");
   }
   
   @Test
   public void testSwitchValueChange() {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(IpcdFixtures.getDevice());
      event.setEvents(Arrays.asList("OnValueChange"));
      event.setValueChanges(Arrays.asList(IpcdFixtures.createValueChange("bb.switch", "on")));
      sendMessage(event);
      
      Assert.assertEquals("Event Landed", context.getVariable("event"));
      Assert.assertEquals("bb.switch", context.getVariable("switch_param"));
      Assert.assertEquals("on", context.getVariable("switch_value"));
      Assert.assertEquals("onChange", context.getVariable("switch_rule"));
      Assert.assertEquals(null, context.getVariable("switch_rule_value"));
      
      Assert.assertEquals(null, context.getVariable("color_param"));
      Assert.assertEquals(null, context.getVariable("color_value"));
      Assert.assertEquals(null, context.getVariable("color_rule"));
      Assert.assertEquals(null, context.getVariable("color_rule_value"));
      
      Assert.assertEquals(null, context.getVariable("level_param"));
      Assert.assertEquals(null, context.getVariable("level_value"));
      Assert.assertEquals(null, context.getVariable("level_rule"));
      Assert.assertEquals(null, context.getVariable("level_rule_value"));
   }
   
   @Test
   public void testColorValueChange() {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(IpcdFixtures.getDevice());
      event.setEvents(Arrays.asList("OnValueChange"));
      event.setValueChanges(Arrays.asList(IpcdFixtures.createValueChange("bb.color", "maroon", "onEquals", "maroon")));
      sendMessage(event);
      
      Assert.assertEquals("Event Landed", context.getVariable("event"));
      Assert.assertEquals(null, context.getVariable("switch_param"));
      Assert.assertEquals(null, context.getVariable("switch_value"));
      Assert.assertEquals(null, context.getVariable("switch_rule"));
      Assert.assertEquals(null, context.getVariable("switch_rule_value"));
      
      Assert.assertEquals("bb.color", context.getVariable("color_param"));
      Assert.assertEquals("maroon", context.getVariable("color_value"));
      Assert.assertEquals("onEquals", context.getVariable("color_rule"));
      Assert.assertEquals("maroon", context.getVariable("color_rule_value"));
      
      Assert.assertEquals(null, context.getVariable("level_param"));
      Assert.assertEquals(null, context.getVariable("level_value"));
      Assert.assertEquals(null, context.getVariable("level_rule"));
      Assert.assertEquals(null, context.getVariable("level_rule_value"));
   }
   
   @Test
   public void testLevelValueChange() {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(IpcdFixtures.getDevice());
      event.setEvents(Arrays.asList("OnValueChange"));
      event.setValueChanges(Arrays.asList(IpcdFixtures.createValueChange("bb.level", 11, "onGreaterThan", 10)));
      sendMessage(event);
      
      Assert.assertEquals("Event Landed", context.getVariable("event"));
      Assert.assertEquals(null, context.getVariable("switch_param"));
      Assert.assertEquals(null, context.getVariable("switch_value"));
      Assert.assertEquals(null, context.getVariable("switch_rule"));
      Assert.assertEquals(null, context.getVariable("switch_rule_value"));
      
      Assert.assertEquals(null, context.getVariable("color_param"));
      Assert.assertEquals(null, context.getVariable("color_value"));
      Assert.assertEquals(null, context.getVariable("color_rule"));
      Assert.assertEquals(null, context.getVariable("color_rule_value"));
      
      Assert.assertEquals("bb.level", context.getVariable("level_param"));
      Assert.assertTrue(Comparisons.areNumericValuesEqual(11, (Number)context.getVariable("level_value")));
      Assert.assertEquals("onGreaterThan", context.getVariable("level_rule"));
      Assert.assertTrue(Comparisons.areNumericValuesEqual(10, (Number)context.getVariable("level_rule_value")));
   }
   
   @Test
   public void testLevelAndColorValueChange() {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(IpcdFixtures.getDevice());
      event.setEvents(Arrays.asList("OnValueChange"));
      event.setValueChanges(Arrays.asList(
            IpcdFixtures.createValueChange("bb.level", 11, "onGreaterThan", 10),
            IpcdFixtures.createValueChange("bb.color", "maroon", "onEquals", "maroon")));
      sendMessage(event);
      
      System.out.println("Dump Vars");
      for (String name : context.getVariableNames()) {
         System.out.println("   " + name + " -> " + context.getVariable(name));
      }
      
      Assert.assertEquals("Event Landed", context.getVariable("event"));
      Assert.assertEquals(null, context.getVariable("switch_param"));
      Assert.assertEquals(null, context.getVariable("switch_value"));
      Assert.assertEquals(null, context.getVariable("switch_rule"));
      Assert.assertEquals(null, context.getVariable("switch_rule_value"));
      
      Assert.assertEquals("bb.color", context.getVariable("color_param"));
      Assert.assertEquals("maroon", context.getVariable("color_value"));
      Assert.assertEquals("onEquals", context.getVariable("color_rule"));
      Assert.assertEquals("maroon", context.getVariable("color_rule_value"));
      
      Assert.assertEquals("bb.level", context.getVariable("level_param"));
      Assert.assertTrue(Comparisons.areNumericValuesEqual(11, (Number)context.getVariable("level_value")));
      Assert.assertEquals("onGreaterThan", context.getVariable("level_rule"));
      Assert.assertTrue(Comparisons.areNumericValuesEqual(10, (Number)context.getVariable("level_rule_value")));
   }

   @Test
   public void testLevelColorSwitchValueChange() {
      IpcdEvent event = new IpcdEvent();
      event.setDevice(IpcdFixtures.getDevice());
      event.setEvents(Arrays.asList("OnValueChange"));
      event.setValueChanges(Arrays.asList(
            IpcdFixtures.createValueChange("bb.switch", "on"),
            IpcdFixtures.createValueChange("bb.level", 11, "onGreaterThan", 10),
            IpcdFixtures.createValueChange("bb.color", "maroon", "onEquals", "maroon")));
      sendMessage(event);
      
      System.out.println("Dump Vars");
      for (String name : context.getVariableNames()) {
         System.out.println("   " + name + " -> " + context.getVariable(name));
      }
      
      Assert.assertEquals("Event Landed", context.getVariable("event"));
      Assert.assertEquals("bb.switch", context.getVariable("switch_param"));
      Assert.assertEquals("on", context.getVariable("switch_value"));
      Assert.assertEquals("onChange", context.getVariable("switch_rule"));
      Assert.assertEquals(null, context.getVariable("switch_rule_value"));
      
      Assert.assertEquals("bb.color", context.getVariable("color_param"));
      Assert.assertEquals("maroon", context.getVariable("color_value"));
      Assert.assertEquals("onEquals", context.getVariable("color_rule"));
      Assert.assertEquals("maroon", context.getVariable("color_rule_value"));
      
      Assert.assertEquals("bb.level", context.getVariable("level_param"));
      Assert.assertTrue(Comparisons.areNumericValuesEqual(11, (Number)context.getVariable("level_value")));
      Assert.assertEquals("onGreaterThan", context.getVariable("level_rule"));
      Assert.assertTrue(Comparisons.areNumericValuesEqual(10, (Number)context.getVariable("level_rule_value")));
   }
}

