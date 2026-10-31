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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;

public class ZigbeeContextTest {
   private Capture<ZigbeeMessage.Protocol> sent;
   private ZigbeeContext zigbee;

   @Before
   public void setUp() {
      sent = Capture.newInstance();
      DeviceDriverContext context = createNiceMock(DeviceDriverContext.class);
      context.sendToDevice(eq(ZigbeeProtocol.INSTANCE), capture(sent), eq(-1));
      expectLastCall();
      replay(context);

      GroovyContextObject.setContext(context);
      zigbee = new ZigbeeContext();
   }

   @After
   public void tearDown() {
      GroovyContextObject.clearContext();
   }

   @Test
   public void sendOrderedKeepsEveryMessage() throws Exception {
      zigbee.sendOrdered(offlineTimeout(60), offlineTimeout(120));

      assertEquals(ZigbeeMessage.Ordered.ID, sent.getValue().getType());
      ZigbeeMessage.Ordered ordered =
            ZigbeeMessage.Ordered.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, sent.getValue().getPayload());
      assertSeconds(new int[] { 60, 120 }, ordered.getPayload());
   }

   @Test
   public void sendDelayedKeepsEveryMessage() throws Exception {
      zigbee.sendDelayed(5, TimeUnit.SECONDS, offlineTimeout(60), offlineTimeout(120));

      assertEquals(ZigbeeMessage.Delay.ID, sent.getValue().getType());
      ZigbeeMessage.Delay delay =
            ZigbeeMessage.Delay.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, sent.getValue().getPayload());
      assertEquals(TimeUnit.SECONDS.toNanos(5), delay.getDelay());
      assertSeconds(new int[] { 60, 120 }, delay.getPayload());
   }

   private static ZigbeeMessage.SetOfflineTimeout offlineTimeout(int seconds) {
      return ZigbeeMessage.SetOfflineTimeout.builder().setSeconds(seconds).create();
   }

   private static void assertSeconds(int[] expected, ZigbeeMessage.Protocol[] msgs) throws Exception {
      assertEquals(expected.length, msgs.length);
      for (int i = 0; i < expected.length; i++) {
         assertEquals(ZigbeeMessage.SetOfflineTimeout.ID, msgs[i].getType());
         ZigbeeMessage.SetOfflineTimeout msg =
               ZigbeeMessage.SetOfflineTimeout.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, msgs[i].getPayload());
         assertEquals(expected[i], msg.getSeconds());
      }
   }
}
