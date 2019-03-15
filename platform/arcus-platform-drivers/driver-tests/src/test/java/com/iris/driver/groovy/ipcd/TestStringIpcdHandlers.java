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

import org.junit.Assert;
import org.junit.Test;

import com.iris.protocol.ipcd.message.model.StatusType;

public class TestStringIpcdHandlers extends IpcdHandlersTestCase {
   
   public TestStringIpcdHandlers() {
      super("IpcdMessageHandlerStrings.driver");
   }
   
   @Test
   public void testResponseExactMatch() throws Exception {
      sendMessage(IpcdFixtures.getDeviceInfoResponse(StatusType.success));
      Assert.assertEquals("ipcd response getdeviceinfo success", context.getVariable("match"));
   }
   
   @Test
   public void testResponseCommandMatch() throws Exception {
      sendMessage(IpcdFixtures.getDeviceInfoResponse(StatusType.warn));
      Assert.assertEquals("ipcd response getdeviceinfo", context.getVariable("match"));
   }
   
   @Test
   public void testResponseMatch() throws Exception {
      sendMessage(IpcdFixtures.getSetParameterValuesResponse(StatusType.success));
      Assert.assertEquals("ipcd response", context.getVariable("match"));
   }
   
   @Test
   public void testProtocolMatch() throws Exception {
      sendMessage(IpcdFixtures.getEvent());
      Assert.assertEquals("ipcd", context.getVariable("match"));
   }
}

