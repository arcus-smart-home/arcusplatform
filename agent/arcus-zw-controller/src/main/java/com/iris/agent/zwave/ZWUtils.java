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
package com.iris.agent.zwave;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import com.iris.agent.hal.IrisHal;
import com.iris.agent.util.ByteUtils;
import com.iris.messages.address.ProtocolDeviceId;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ZWUtils {
   
   public static boolean isValidNodeId(int nid) {
      return (nid > 0 && nid <= 232);
   }
   
   public static ProtocolDeviceId getDeviceId(long homeIdAsLong, int nodeIdAsInt) {
      int homeId = ByteUtils.from32BitToInt(ByteUtils.to32Bits(homeIdAsLong));
      byte nodeId = (byte)nodeIdAsInt;
      byte[] hubId = ZWConfig.HAS_AGENT 
            ? IrisHal.getHubId().getBytes(StandardCharsets.UTF_8)
            : "LWW-1202".getBytes(StandardCharsets.UTF_8);
      ByteBuf buffer = Unpooled.buffer(hubId.length + 6, hubId.length + 6).order(ByteOrder.BIG_ENDIAN);
      buffer.writeByte(nodeId);
      buffer.writeBytes(hubId);
      buffer.writeInt(homeId);
      buffer.writeByte(nodeId);
      return ProtocolDeviceId.fromBytes(buffer.array());
   }
   
   public static int safeInt(Integer i, int def) {
      return i != null ? i : def;
   }
}
