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
import com.iris.protocol.ipcd.message.model.GetDeviceInfoCommand;
import com.iris.protocol.ipcd.message.model.MessageType;

public class TestManGetDeviceInfo extends SerializerTest {
   
   @Test
   public void testDeserialize() throws Exception {
      GetDeviceInfoCommand cmd = serDe.fromJson(getJson("command-getdeviceinfo-1"), GetDeviceInfoCommand.class);
      System.out.println("Command: " + cmd.getCommand());
      System.out.println("Txnid: " + cmd.getTxnid());
   }
   
   @Test
   public void testSerialize() throws Exception {
      GetDeviceInfoCommand cmd = new GetDeviceInfoCommand();
      cmd.setTxnid("1234");
      
      String json = serDe.toJson(cmd);
      
      System.out.println(json);
   }
   
   @Test
   public void testDiscoverMessageType() throws Exception {
      MessageType actual = serDe.discoverMessageType(getJson("command-getdeviceinfo-1"));
      Assert.assertEquals(MessageType.command, actual);
   }
   
   @Test
   public void testParse() throws Exception {
      IpcdMessage ipcdMsg = serDe.parse(getJson("command-getdeviceinfo-1"));
      
      System.out.println("Message Type: " + ipcdMsg.getMessageType());
      
      if (ipcdMsg instanceof GetDeviceInfoCommand) {
         GetDeviceInfoCommand msg = (GetDeviceInfoCommand)ipcdMsg;
         System.out.println("Command: " + msg.getCommand());
         System.out.println("Txnid: " + msg.getTxnid());
      }
      else {
         System.out.println("Failed: " + ipcdMsg);
      }
   }
}

