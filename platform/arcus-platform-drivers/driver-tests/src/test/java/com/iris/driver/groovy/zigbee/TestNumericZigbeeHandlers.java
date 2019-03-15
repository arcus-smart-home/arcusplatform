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
package com.iris.driver.groovy.zigbee;

import org.junit.Assert;
import org.junit.Test;

import com.iris.protocol.zigbee.zcl.Fan;
import com.iris.protocol.zigbee.zcl.OnOff;

public class TestNumericZigbeeHandlers extends ZigbeeHandlersTestCase {

   public TestNumericZigbeeHandlers() {
      super("ZigbeeMessageHandlerNumeric.driver");
   }
   
   @Test
   public void testZclExactMatch() throws Exception {
      createAndSendZclMessage(OnOff.CLUSTER_ID, OnOff.On.ID, true);
      Assert.assertEquals("zcl exact cluster-specific", context.getVariable("match"));
   }
   
   @Test
   public void testZclGeneralMatch() throws Exception {
      createAndSendZclMessage(OnOff.CLUSTER_ID, OnOff.On.ID, false);
      Assert.assertEquals("zcl exact general", context.getVariable("match"));
   }
   
   @Test
   public void testZclClusterMatch() throws Exception {
      createAndSendZclMessage(OnOff.CLUSTER_ID, (byte)0xfe, false);
      Assert.assertEquals("zcl cluster", context.getVariable("match"));
   }
   
   @Test
   public void testZclTypeMatch() throws Exception {
      createAndSendZclMessage(Fan.CLUSTER_ID, (byte)0xfe, false);
      Assert.assertEquals("zcl type", context.getVariable("match"));
   }
   
   @Test
   public void testZdpExactMatch() throws Exception {
      createAndSendZdpMessage(3);
      Assert.assertEquals("zdp exact", context.getVariable("match"));
   }
   
   @Test
   public void testZdpTypeMatch() throws Exception {
      createAndSendZdpMessage(4);
      Assert.assertEquals("zdp type", context.getVariable("match"));
   }
   
   @Test
   public void testProtocolMatch() throws Exception {
      createAndSendBogusMessageType(31);
      Assert.assertEquals("protocol", context.getVariable("match"));
   }
}

