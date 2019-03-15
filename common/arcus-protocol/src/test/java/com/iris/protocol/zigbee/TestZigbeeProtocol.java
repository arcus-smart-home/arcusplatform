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
package com.iris.protocol.zigbee;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.iris.protocol.zigbee.msg.ZigbeeMessage;

public class TestZigbeeProtocol {
   private final ZigbeeProtocol protocol = ZigbeeProtocol.INSTANCE;

   @Test
   public void testZclSerializeDeserializeAllFields() throws IOException {
      // Build Message and serialize
      ZigbeeMessage.Zcl.Builder zclBuilder = ZigbeeMessage.Zcl.builder();
      ZigbeeMessage.Zcl origZclMessage = zclBuilder.setClusterId(4)
                                          .setFlags(1)
                                          .setZclMessageId(8)
                                          .setProfileId(15)
                                          .setEndpoint(16)
                                          .setPayload(new byte[] { 0x17, 0x2a, (byte)0xfe })
                                          .create();

      ZigbeeMessage.Protocol origMessage = ZigbeeProtocol.packageMessage(origZclMessage);

      byte[] bytes = protocol.createSerializer().serialize(origMessage);

      // Deserialize message and check
      ZigbeeMessage.Protocol message = protocol.createDeserializer().deserialize(bytes);
      Assert.assertEquals(true, ZigbeeProtocol.isZcl(message));
      Assert.assertEquals(false, ZigbeeProtocol.isZdp(message));

      ZigbeeMessage.Zcl zclMessage = ZigbeeProtocol.getZclMessage(message);

      Assert.assertEquals(origZclMessage, zclMessage);
   }


   @Test
   public void testZclSerializeDeserializeNoPayload() throws IOException {
   // Build Message and serialize
      ZigbeeMessage.Zcl.Builder zclBuilder = ZigbeeMessage.Zcl.builder();
      ZigbeeMessage.Zcl origZclMessage = zclBuilder.setClusterId(4)
                                          .setFlags(1)
                                          .setZclMessageId(8)
                                          .setProfileId(15)
                                          .setEndpoint(16)
                                          .setPayload(new byte[0])
                                          .create();

      ZigbeeMessage.Protocol origMessage = ZigbeeProtocol.packageMessage(origZclMessage);

      byte[] bytes = protocol.createSerializer().serialize(origMessage);

      // Deserialize message and check
      ZigbeeMessage.Protocol message = protocol.createDeserializer().deserialize(bytes);
      Assert.assertEquals(true, ZigbeeProtocol.isZcl(message));
      Assert.assertEquals(false, ZigbeeProtocol.isZdp(message));

      ZigbeeMessage.Zcl zclMessage = ZigbeeProtocol.getZclMessage(message);

      Assert.assertEquals(origZclMessage, zclMessage);
   }

   @Test
   public void testZdplSerializeDeserializeAllFields() throws IOException {
   // Build Message and serialize
      ZigbeeMessage.Zdp.Builder zdpBuilder = ZigbeeMessage.Zdp.builder();
      ZigbeeMessage.Zdp origZdpMessage = zdpBuilder.setZdpMessageId(0x2334)
                                          .setPayload(new byte[] { 0x17, 0x2a, (byte)0xfe })
                                          .create();

      ZigbeeMessage.Protocol origMessage = ZigbeeProtocol.packageMessage(origZdpMessage);

      byte[] bytes = protocol.createSerializer().serialize(origMessage);

      // Deserialize message and check
      ZigbeeMessage.Protocol message = protocol.createDeserializer().deserialize(bytes);
      Assert.assertEquals(false, ZigbeeProtocol.isZcl(message));
      Assert.assertEquals(true, ZigbeeProtocol.isZdp(message));

      ZigbeeMessage.Zdp zdpMessage = ZigbeeProtocol.getZdpMessage(message);

      Assert.assertEquals(origZdpMessage, zdpMessage);
   }

   @Test
   public void testZdplSerializeDeserializeNoPayload() throws IOException {
   // Build Message and serialize
      ZigbeeMessage.Zdp.Builder zdpBuilder = ZigbeeMessage.Zdp.builder();
      ZigbeeMessage.Zdp origZdpMessage = zdpBuilder.setZdpMessageId(0x2334)
                                          .setPayload(new byte[0])
                                          .create();

      ZigbeeMessage.Protocol origMessage = ZigbeeProtocol.packageMessage(origZdpMessage);

      byte[] bytes = protocol.createSerializer().serialize(origMessage);

      // Deserialize message and check
      ZigbeeMessage.Protocol message = protocol.createDeserializer().deserialize(bytes);
      Assert.assertEquals(false, ZigbeeProtocol.isZcl(message));
      Assert.assertEquals(true, ZigbeeProtocol.isZdp(message));

      ZigbeeMessage.Zdp zdpMessage = ZigbeeProtocol.getZdpMessage(message);

      Assert.assertEquals(origZdpMessage, zdpMessage);
   }
}

