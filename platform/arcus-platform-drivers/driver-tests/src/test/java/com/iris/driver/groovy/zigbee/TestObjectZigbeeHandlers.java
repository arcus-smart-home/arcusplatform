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
import com.iris.protocol.zigbee.zcl.General;
import com.iris.protocol.zigbee.zcl.OnOff;
import com.iris.protocol.zigbee.zdp.Discovery;

public class TestObjectZigbeeHandlers extends ZigbeeHandlersTestCase {

   public TestObjectZigbeeHandlers() {
      super("ZigbeeMessageHandlerObjects.driver");
   }
   
   @Test
   public void testZclExactMatch() throws Exception {
      createAndSendZclMessage(OnOff.CLUSTER_ID, OnOff.On.ID, true);
      Assert.assertEquals("zcl exact cluster-specific", context.getVariable("match"));
   }
   
   @Test
   public void testZclExactGeneralMatch() throws Exception {
      createAndSendZclMessage(OnOff.CLUSTER_ID, 1, false);
      Assert.assertEquals("zcl exact general", context.getVariable("match"));
   }
   
   @Test 
   public void testZclMatchMessageMethod() throws Exception {
      createAndSendZclMessage(OnOff.CLUSTER_ID, OnOff.Off.ID, true);
      Assert.assertEquals("zcl exact off", context.getVariable("match"));
   }
   
   @Test
   public void testZclMatchMessageString() throws Exception {
      createAndSendZclMessage(Fan.CLUSTER_ID, General.ZclReadAttributesResponse.ID, false);
      Assert.assertEquals("zcl exact fan general", context.getVariable("match"));
   }
   
   @Test
   public void testZclClusterMatch() throws Exception {
      createAndSendZclMessage(OnOff.CLUSTER_ID, OnOff.Toggle.ID, true);
      Assert.assertEquals("zcl cluster", context.getVariable("match"));
   }
   
   @Test
   public void testZdpCommandMatch() throws Exception {
      createAndSendZdpMessage(Discovery.ZDP_IEEE_ADDR_REQ);
      Assert.assertEquals("zdp ieee", context.getVariable("match"));
   }
}

