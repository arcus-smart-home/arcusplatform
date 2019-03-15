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

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.gson.IpcdJsonReader;
import com.iris.protocol.ipcd.message.model.Device;
import com.iris.protocol.ipcd.message.model.IpcdEvent;
import com.iris.protocol.ipcd.message.model.MessageType;
import com.iris.protocol.ipcd.message.model.ValueChange;

public class TestManEvent extends SerializerTest {
   
   @Test
   public void testDeserialize() throws Exception {
      IpcdEvent event = serDe.fromJson(json("{\"device\":{\"vendor\":\"BlackBox\",\"model\":\"ContactSensor1\",\"sn\":\"05102034A7B5\",\"ipcdver\":\"0.3\"},\"events\":[\"onBoot\",\"onConnect\"]}"), IpcdEvent.class);
      Device device = event.getDevice();
      List<String> events = event.getEvents();
      List<ValueChange> valueChanges = event.getValueChanges();
      
      Assert.assertEquals(MessageType.event, event.getMessageType());
      Assert.assertEquals("BlackBox", device.getVendor());
      Assert.assertEquals("ContactSensor1", device.getModel());
      Assert.assertEquals("05102034A7B5", device.getSn());
      Assert.assertEquals("0.3", device.getIpcdver());
      
      Assert.assertEquals("onBoot", events.get(0));
      Assert.assertEquals("onConnect", events.get(1));
      
      Assert.assertTrue(valueChanges == null || valueChanges.isEmpty());
   }
   
   @Test
   public void testParse() throws Exception {
      IpcdMessage msg = serDe.parse(json("{\"device\":{\"vendor\":\"BlackBox\",\"model\":\"ContactSensor1\",\"sn\":\"05102034A7B5\",\"ipcdver\":\"0.3\"},\"events\":[\"onBoot\",\"onConnect\"]}"));
      
      Assert.assertNotNull(msg);
      Assert.assertEquals(MessageType.event, msg.getMessageType());
      IpcdEvent event = (IpcdEvent)msg;
      Device device = event.getDevice();
      List<String> events = event.getEvents();
      List<ValueChange> valueChanges = event.getValueChanges();
      
      
      Assert.assertEquals("BlackBox", device.getVendor());
      Assert.assertEquals("ContactSensor1", device.getModel());
      Assert.assertEquals("05102034A7B5", device.getSn());
      Assert.assertEquals("0.3", device.getIpcdver());
      
      Assert.assertEquals("onBoot", events.get(0));
      Assert.assertEquals("onConnect", events.get(1));
      
      Assert.assertTrue(valueChanges == null || valueChanges.isEmpty());
   }

   @Test
   public void testIPCDNoStatus() throws Exception {
      IpcdSerDe serializer = new IpcdSerDe();
      InputStream is = this.getClass().getResourceAsStream("/com/iris/protocol/ipcd/message/ipcd-no-status.json");
      InputStreamReader reader = new InputStreamReader(is);
      IpcdMessage msg = serializer.parse(reader);
      assertTrue(msg != null);
   }

   @Test
   public void testGenieExtraComma() throws Exception {
      InputStream is = this.getClass().getResourceAsStream("/com/iris/protocol/ipcd/message/json-extra-comma.json");
      InputStreamReader reader = new InputStreamReader(is);
      IpcdJsonReader jsonReader = new IpcdJsonReader(reader);
      jsonReader.isLenient = true;
      JsonParser parser = new JsonParser();
      JsonElement root = parser.parse(jsonReader);
      
      /*
      IpcdSerDe serializer = new IpcdSerDe();
      InputStream is = this.getClass().getResourceAsStream("/com/iris/protocol/ipcd/message/json-extra-comma.json");
      InputStreamReader reader = new InputStreamReader(is);
      IpcdMessage msg = serializer.parse(reader);
      assertTrue(msg != null);
      */
   }


}

