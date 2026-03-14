/*
 * Copyright 2020 Arcus Project
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
package com.iris.agent.reflex.drivers;

import java.io.IOException;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.reflex.ReflexController;
import com.iris.agent.util.RxIris;
import com.iris.messages.address.Address;
import com.iris.protocol.zwave.Protocol;
import com.iris.protocol.zwave.ZWaveProtocol;

import io.netty.buffer.Unpooled;

public abstract class AbstractZWaveHubDriver extends AbstractHubDriver {
   private static final Logger log = LoggerFactory.getLogger(AbstractZWaveHubDriver.class);

   public AbstractZWaveHubDriver(ReflexController parent, Address addr) {
      super(parent, addr);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Protocol Message Handling
   /////////////////////////////////////////////////////////////////////////////

   /**
    * Handle a deserialized Z-Wave command message from the device.
    *
    * @param nodeId the source node ID
    * @param commandClassId the command class (e.g. 0x6F for Entry Control)
    * @param commandId the command within the class (e.g. 0x01 for notification)
    * @param payload the command payload bytes
    * @return true if the message was handled
    */
   protected boolean handleZWaveCommand(byte nodeId, byte commandClassId, byte commandId, byte[] payload) {
      return false;
   }

   /**
    * Handle a Z-Wave node info message.
    */
   protected void handleZWaveNodeInfo(byte nodeId, byte status, byte basic, byte generic, byte specific) {
      log.trace("zwave node info: node={}, status={}, basic={}, generic={}, specific={}",
         nodeId, status, basic, generic, specific);
   }

   @Override
   protected boolean handle(String type, byte[] msg) {
      if (!ZWaveProtocol.NAMESPACE.equals(type)) {
         return false;
      }

      try {
         Protocol.Message pmsg = Protocol.Message.serde().nettySerDe()
            .decode(Unpooled.wrappedBuffer(msg));

         switch (pmsg.getType()) {
         case Protocol.Command.ID:
            return handleCommand(pmsg);

         case Protocol.NodeInfo.ID:
            handleNodeInfo(pmsg);
            return true;

         default:
            log.trace("unhandled zwave message type: {}", pmsg.getType());
            return false;
         }
      } catch (Exception ex) {
         log.warn("failed to process zwave protocol message: ", ex);
         return false;
      }
   }

   private boolean handleCommand(Protocol.Message msg) throws IOException {
      Protocol.Command pcmd = Protocol.Command.serde().nettySerDe()
         .decode(Unpooled.wrappedBuffer(msg.getPayload()));

      return handleZWaveCommand(
         pcmd.rawNodeId(),
         pcmd.rawCommandClassId(),
         pcmd.rawCommandId(),
         pcmd.getPayload()
      );
   }

   private void handleNodeInfo(Protocol.Message msg) throws IOException {
      Protocol.NodeInfo ni = Protocol.NodeInfo.serde().nettySerDe()
         .decode(Unpooled.wrappedBuffer(msg.getPayload()));

      handleZWaveNodeInfo(
         ni.rawNodeId(),
         ni.rawStatus(),
         ni.rawBasic(),
         ni.rawGeneric(),
         ni.rawSpecific()
      );
   }

   /////////////////////////////////////////////////////////////////////////////
   // Z-Wave Driver APIs
   /////////////////////////////////////////////////////////////////////////////

   /**
    * Send a raw Z-Wave command to the device.
    *
    * @param commandClassId the command class byte
    * @param commandId the command byte
    * @param payload the payload bytes (may be empty)
    */
   protected void zwaveSend(byte commandClassId, byte commandId, byte... payload) {
      try {
         Protocol.Command pcmd = Protocol.Command.builder()
            .setNodeId((byte) 0)
            .setCommandClassId(commandClassId)
            .setCommandId(commandId)
            .setPayload(payload)
            .create();

         Protocol.Message pmsg = Protocol.Message.builder()
            .setType(Protocol.Command.ID)
            .setPayload(ByteOrder.BIG_ENDIAN, pcmd)
            .create();

         parent.zwave().send(addr, pmsg).subscribe(RxIris.SWALLOW_ALL);
      } catch (Exception ex) {
         log.warn("failed to send zwave command: cc={}, cmd={}", commandClassId, commandId, ex);
      }
   }

   /**
    * Send an Indicator Set command to control LEDs/sounds on the device.
    *
    * @param value the indicator value
    */
   protected void sendIndicatorSet(byte value) {
      zwaveSend((byte) 0x87, (byte) 0x01, value);
   }

   /**
    * Send an Indicator Set V3 command with indicator objects for advanced
    * features like countdown timers. Each indicator object is a triplet of
    * (indicator ID, property ID, value).
    *
    * @param objects indicator object bytes, must be a positive multiple of 3
    */
   protected void sendIndicatorSetV3(byte... objects) {
      if (objects.length == 0 || objects.length % 3 != 0) {
         log.warn("invalid indicator v3 objects: length must be a positive multiple of 3, got {}", objects.length);
         return;
      }
      int objectCount = objects.length / 3;
      byte[] payload = new byte[objects.length + 2];
      payload[0] = 0x00;                         // V1 indicator value (unused)
      payload[1] = (byte)(objectCount & 0x1F);   // object count (5 bits)
      System.arraycopy(objects, 0, payload, 2, objects.length);
      zwaveSend((byte) 0x87, (byte) 0x01, payload);
   }

   /**
    * Send a Configuration Set command.
    *
    * @param param the parameter number
    * @param size the parameter size in bytes
    * @param values the parameter value bytes
    */
   protected void sendConfigurationSet(byte param, byte size, byte... values) {
      byte[] payload = new byte[values.length + 2];
      payload[0] = param;
      payload[1] = size;
      System.arraycopy(values, 0, payload, 2, values.length);
      zwaveSend((byte) 0x70, (byte) 0x04, payload);
   }

   /**
    * Send a Battery Get command.
    */
   protected void sendBatteryGet() {
      zwaveSend((byte) 0x80, (byte) 0x02);
   }
}
