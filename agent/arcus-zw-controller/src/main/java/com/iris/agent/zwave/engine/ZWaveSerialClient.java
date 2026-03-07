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
package com.iris.agent.zwave.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.zwave.ZWMsg;
import com.iris.agent.zwave.client.ZWClient;
import com.iris.agent.zwave.code.ZCmd;

/**
 * ZWClient implementation that sends Z-Wave commands to a specific node
 * via the serial engine. Commands are serialized as FUNC_ID_ZW_SEND_DATA
 * frames and queued on the engine's transmit pipeline.
 */
public class ZWaveSerialClient implements ZWClient {
   private static final Logger logger = LoggerFactory.getLogger(ZWaveSerialClient.class);

   private final int nodeId;
   private final ZWaveSerialEngine engine;
   private volatile boolean running = true;

   ZWaveSerialClient(int nodeId, ZWaveSerialEngine engine) {
      this.nodeId = nodeId;
      this.engine = engine;
   }

   @Override
   public void send(ZWMsg msg) {
      if (!running) {
         logger.warn("Client for node {} is not running, cannot send", nodeId);
         msg.error(new IllegalStateException("Client not running"));
         return;
      }

      ZCmd cmd = msg.getCommand();
      byte[] cmdBytes = cmd.bytes();

      try {
         if (nodeId == ZWMsg.NO_NODE) {
            // Controller command: send directly as a serial API request
            engine.sendControllerCommand(cmd, msg);
         } else {
            // Node command: wrap in FUNC_ID_ZW_SEND_DATA
            engine.sendNodeCommand(nodeId, cmdBytes, msg);
         }
      } catch (Exception e) {
         logger.error("Failed to send command to node {}", nodeId, e);
         msg.error(e);
      }
   }

   @Override
   public void start() {
      running = true;
   }

   @Override
   public void shutdown() {
      running = false;
   }

   @Override
   public boolean isRunning() {
      return running;
   }
}
