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
package com.iris.protocol.zwave.message.controller;

import com.iris.protocol.zwave.message.ZWaveMessage;
import com.iris.protocol.zwave.model.ZWaveNode;

@SuppressWarnings("serial")
public class ZWaveSetOfflineTimeoutMessage implements ZWaveMessage {
   public final static String TYPE = "SetOfflineTimeout";
   private final ZWaveNode node;
   private final int seconds;

   public ZWaveSetOfflineTimeoutMessage(ZWaveNode node, int seconds) {
      this.node = node;
      this.seconds = seconds;
   }

   @Override
   public String getMessageType() {
      return TYPE;
   }

   public ZWaveNode getNode() {
      return node;
   }

   public int getSeconds() {
      return seconds;
   }
}

