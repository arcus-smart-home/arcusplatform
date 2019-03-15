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
package com.iris.protocol.zwave.message;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.iris.protocol.zwave.model.ZWaveNode;

public class ZWaveDelayedCommandsMessage implements ZWaveMessage {
   private static final long serialVersionUID = 9007518570270140392L;

   public final static String TYPE = "DelayedCommand";

   private final long delay;
   private final List<ZWaveMessage> commands;
   private final ZWaveNode node;

   public ZWaveDelayedCommandsMessage(ZWaveNode node, long delay, List<ZWaveMessage> commands) {
      this.delay = delay;
      this.node = node;
      this.commands = ImmutableList.copyOf(commands);
   }

   @Override
   public String getMessageType() {
      return TYPE;
   }

   public ZWaveNode getDevice() {
      return node;
   }

   public long getDelay() {
      return delay;
   }

   public List<ZWaveMessage> getCommands() {
      return commands;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "ZWaveDelayCommandMessage [delay=" + delay + ",commands=" + commands + ", node=" + node + "]";
   }

}

