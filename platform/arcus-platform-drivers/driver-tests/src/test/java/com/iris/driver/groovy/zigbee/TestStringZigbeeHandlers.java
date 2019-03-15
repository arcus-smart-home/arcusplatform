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

import com.iris.protocol.zigbee.zcl.OnOff;
import com.iris.protocol.zigbee.zdp.Discovery;

public class TestStringZigbeeHandlers extends ZigbeeHandlersTestCase {

   public TestStringZigbeeHandlers() {
      super("ZigbeeMessageHandlerStrings.driver");
   }

   @Test
   public void testZclExactMatch() throws Exception {
      createAndSendZclMessage(OnOff.CLUSTER_ID, OnOff.On.ID, true);
      Assert.assertEquals("zcl exact cluster-specific", context.getVariable("match"));
   }
   
   @Test
   public void testZclClusterMatch() throws Exception {
      createAndSendZclMessage(OnOff.CLUSTER_ID, OnOff.Off.ID, true);
      Assert.assertEquals("zcl cluster", context.getVariable("match"));
   }
   
   @Test
   public void testZdpCommandMatch() throws Exception {
      createAndSendZdpMessage(Discovery.ZDP_NWK_ADDR_REQ);
      Assert.assertEquals("zdp command", context.getVariable("match"));
   }
   
}

