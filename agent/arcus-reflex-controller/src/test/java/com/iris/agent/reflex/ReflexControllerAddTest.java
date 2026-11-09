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
package com.iris.agent.reflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Test;

import com.iris.agent.router.Router;
import com.iris.agent.zigbee.ZigbeeLocalProcessing;
import com.iris.agent.zigbee.node.ZBNode;
import com.iris.agent.zwave.ZWaveLocalProcessing;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.services.PlatformConstants;

public class ReflexControllerAddTest {
   private static final String HUB_ID = "TST-0000";
   private static final ProtocolDeviceId DEVICE_ID = ZBNode.computeDeviceId(0x000D6F0000AA0001L);
   private static final Address DEVICE = Address.hubProtocolAddress(HUB_ID, "ZIGB", DEVICE_ID);

   // The radio sends it to the device service, so this controller sees it by snooping
   @Test
   public void snoopsAddDeviceRequests() {
      ReflexController controller = new ReflexController(
         EasyMock.createNiceMock(ReflexLocalProcessing.class),
         EasyMock.createNiceMock(ZigbeeLocalProcessing.class),
         EasyMock.createNiceMock(ZWaveLocalProcessing.class),
         new Router()
      );

      PlatformMessage add = PlatformMessage
         .buildRequest(addRequest("ZIGB", DEVICE_ID), Address.hubService(HUB_ID, "zigb"), Address.platformService(PlatformConstants.SERVICE_DEVICES))
         .create();

      assertTrue(controller.isInterestedIn(add));
   }

   @Test
   public void zigbeeAddNamesItsDevice() {
      assertEquals(DEVICE, ReflexController.zigbeeDeviceAdded(HUB_ID, addRequest("ZIGB", DEVICE_ID)));
   }

   // As it arrives when the message has been serialized
   @Test
   public void zigbeeAddWithTheIdsRepresentationNamesItsDevice() {
      assertEquals(DEVICE, ReflexController.zigbeeDeviceAdded(HUB_ID, addRequest("ZIGB", DEVICE_ID.getRepresentation())));
   }

   @Test
   public void zwaveAddIsLeftAlone() {
      assertNull(ReflexController.zigbeeDeviceAdded(HUB_ID, addRequest("ZWAV", DEVICE_ID)));
   }

   // A migration adds every device again, which isn't a device paired again
   @Test
   public void migratedAddIsLeftAlone() {
      Map<String,Object> attrs = attributes("ZIGB", DEVICE_ID);
      attrs.put(MessageConstants.ATTR_MIGRATED, true);

      assertNull(ReflexController.zigbeeDeviceAdded(HUB_ID, MessageBody.buildMessage(MessageConstants.MSG_ADD_DEVICE_REQUEST, attrs)));
   }

   @Test
   public void addWithoutADeviceIdIsLeftAlone() {
      Map<String,Object> attrs = attributes("ZIGB", DEVICE_ID);
      attrs.remove(MessageConstants.ATTR_DEVICEID);

      assertNull(ReflexController.zigbeeDeviceAdded(HUB_ID, MessageBody.buildMessage(MessageConstants.MSG_ADD_DEVICE_REQUEST, attrs)));
   }

   private static MessageBody addRequest(String protocol, Object deviceId) {
      return MessageBody.buildMessage(MessageConstants.MSG_ADD_DEVICE_REQUEST, attributes(protocol, deviceId));
   }

   private static Map<String,Object> attributes(String protocol, Object deviceId) {
      Map<String,Object> attrs = new HashMap<>();
      attrs.put(MessageConstants.ATTR_PROTOCOLNAME, protocol);
      attrs.put(MessageConstants.ATTR_DEVICEID, deviceId);
      attrs.put("hubId", HUB_ID);
      return attrs;
   }
}
