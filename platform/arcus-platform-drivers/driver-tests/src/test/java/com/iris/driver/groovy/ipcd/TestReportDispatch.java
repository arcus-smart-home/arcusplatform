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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.iris.protocol.ipcd.message.model.IpcdReport;
import com.iris.util.Comparisons;

public class TestReportDispatch extends IpcdHandlersTestCase {

   public TestReportDispatch() {
      super("IpcdReportDispatchHandler.driver");
   }
   
   @Test
   public void testSwitchReport() {
      IpcdReport event = new IpcdReport();
      event.setDevice(IpcdFixtures.getDevice());
      Map<String, Object> reports = new HashMap<>();
      reports.put("bb.switch", "on");
      event.setReport(reports);
      sendMessage(event);

      Assert.assertEquals("Report Landed", context.getVariable("report"));
      Assert.assertEquals("on", context.getVariable("switch"));
      Assert.assertEquals(null, context.getVariable("color"));
      Assert.assertEquals(null, context.getVariable("level"));
   }
   
   @Test
   public void testColorReport() {
      IpcdReport event = new IpcdReport();
      event.setDevice(IpcdFixtures.getDevice());
      Map<String, Object> reports = new HashMap<>();
      reports.put("bb.color", "pink");
      event.setReport(reports);
      sendMessage(event);

      Assert.assertEquals("Report Landed", context.getVariable("report"));
      Assert.assertEquals(null, context.getVariable("switch"));
      Assert.assertEquals("pink", context.getVariable("color"));
      Assert.assertEquals(null, context.getVariable("level"));
   }
   
   @Test
   public void testLevelReport() {
      IpcdReport event = new IpcdReport();
      event.setDevice(IpcdFixtures.getDevice());
      Map<String, Object> reports = new HashMap<>();
      reports.put("bb.level", 21);
      event.setReport(reports);
      sendMessage(event);

      Assert.assertEquals("Report Landed", context.getVariable("report"));
      Assert.assertEquals(null, context.getVariable("switch"));
      Assert.assertEquals(null, context.getVariable("color"));
      Assert.assertTrue(Comparisons.areNumericValuesEqual(21, (Number)context.getVariable("level")));
   }
   
   @Test
   public void testSwitchAndLevelReport() {
      IpcdReport event = new IpcdReport();
      event.setDevice(IpcdFixtures.getDevice());
      Map<String, Object> reports = new HashMap<>();
      reports.put("bb.level", 21);
      reports.put("bb.switch", "on");
      event.setReport(reports);
      sendMessage(event);

      Assert.assertEquals("Report Landed", context.getVariable("report"));
      Assert.assertEquals("on", context.getVariable("switch"));
      Assert.assertEquals(null, context.getVariable("color"));
      Assert.assertTrue(Comparisons.areNumericValuesEqual(21, (Number)context.getVariable("level")));
   }
}

