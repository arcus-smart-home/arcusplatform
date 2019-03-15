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
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveNode;

@SuppressWarnings("serial")
public class ZWaveScheduleMessage implements ZWaveMessage {
   public final static String TYPE = "Schedule";
   private final ZWaveNode node;
   private final ZWaveCommand[] commands;
   private final int seconds;

   public ZWaveScheduleMessage(ZWaveNode node, ZWaveCommand command, int seconds) {
      this(node, new ZWaveCommand[] { command }, seconds);
   }

   public ZWaveScheduleMessage(ZWaveNode node, ZWaveCommand[] commands, int seconds) {
      this.node = node;
      this.commands = commands;
      this.seconds = seconds;
   }

   @Override
   public String getMessageType() {
      return TYPE;
   }

   public ZWaveNode getNode() {
      return node;
   }

   public ZWaveCommand[] getCommands() {
      return commands;
   }

   public int getSeconds() {
      return seconds;
   }
}

