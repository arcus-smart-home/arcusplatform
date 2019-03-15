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
package com.iris.protocol.ipcd.message.serialize;

import org.junit.Assert;
import org.junit.Test;

import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.IpcdReport;
import com.iris.protocol.ipcd.message.model.MessageType;

public class TestReport extends SerializerTest {

   @Test
   public void testDeserializeReport() throws Exception {
      IpcdReport rpt = serDe.fromJson(getJson("report-1"), IpcdReport.class);

      Assert.assertEquals("BlackBox",  rpt.getDevice().getVendor());
      Assert.assertEquals("Switch1",   rpt.getDevice().getModel());
      Assert.assertEquals("123456789", rpt.getDevice().getSn());
      Assert.assertEquals("0.3",       rpt.getDevice().getIpcdver());
      
      Assert.assertEquals(MessageType.report, rpt.getMessageType());
      
      Assert.assertEquals("on", rpt.getReport().get("bb.switch"));
   }
   
   @Test
   public void testParseReport() throws Exception {
      IpcdMessage ipcdMsg = serDe.parse(getJson("report-1"));
      
      Assert.assertNotNull(ipcdMsg);
      
      Assert.assertTrue(ipcdMsg instanceof IpcdReport);
      
      IpcdReport rpt = (IpcdReport)ipcdMsg;
      
      Assert.assertEquals("BlackBox",  rpt.getDevice().getVendor());
      Assert.assertEquals("Switch1",   rpt.getDevice().getModel());
      Assert.assertEquals("123456789", rpt.getDevice().getSn());
      Assert.assertEquals("0.3",       rpt.getDevice().getIpcdver());
      
      Assert.assertEquals(MessageType.report, rpt.getMessageType());
      
      Assert.assertEquals("on", rpt.getReport().get("bb.switch"));
   }
}

