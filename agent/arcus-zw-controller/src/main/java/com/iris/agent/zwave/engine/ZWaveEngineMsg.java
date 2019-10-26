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

import java.util.Arrays;

public class ZWaveEngineMsg {
   final private int homeId;
   final private int nodeId;
   final private byte[] payload;
   
   public ZWaveEngineMsg(int homeId, int nodeId, byte[] payload) {
      this.homeId = homeId;
      this.nodeId = nodeId;
      this.payload = payload != null
            ? Arrays.copyOf(payload, payload.length)
            : new byte[0];
   }
   
   public int getHomeId() {
      return homeId;
   }
   
   public int getNodeId() {
      return nodeId;
   }
   
   public byte[] getPayload() {
      return Arrays.copyOf(payload, payload.length);
   }
}
