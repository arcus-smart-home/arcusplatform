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
/**
 * 
 */
package com.iris.driver.groovy.zwave;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableMap;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.message.ZWaveNodeInfoMessage;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveNode;

/**
 * 
 */
public class ZWaveFixtures {
   public static final byte BINARY_SWITCH = 37;
   public static final byte BINARY_SWITCH_REPORT = 3;
   public static final byte SET_COMMAND = 1;
   public static final byte GET_COMMAND = 2;

   
   public static ZWaveCommandMessage createCommandMessage(
         byte nodeId,
         byte commandClass,
         byte commandNumber,
         byte... recv
   ) {
      ZWaveCommandMessage message = new ZWaveCommandMessage();
      message.setDevice(createNode(nodeId));
      message.setCommand(createCommand(commandClass, commandNumber, recv));
      return message;
   }
   
   public static ZWaveCommandMessage createCommandMessage(
         byte nodeId,
         byte commandClass,
         byte commandNumber,
         Map<String, Byte> recv
   ) {
      ZWaveCommandMessage message = new ZWaveCommandMessage();
      message.setDevice(createNode(nodeId));
      message.setCommand(createCommand(commandClass, commandNumber, recv));
      return message;
   }
   
   public static ZWaveNodeInfoMessage createNodeInfoMessage(byte nodeId) {
      return createNodeInfoMessage(nodeId, (byte)0x04, (byte)0x08, (byte)0x10, (byte)0x80);
   }

   public static ZWaveNodeInfoMessage createNodeInfoMessage(byte nodeId, byte status, byte basic, byte generic, byte specific) {
      return new ZWaveNodeInfoMessage(nodeId, status, basic, generic, specific);
   }

   public static ZWaveNode createNode(byte id) {
      ZWaveNode node = new ZWaveNode(id);
      return node;
   }
   
   public static ZWaveCommand createCommand(byte commandClass, byte commandNumber) {
      return createCommand(commandClass, commandNumber, ImmutableMap.of());
   }
   
   public static ZWaveCommand createCommand(byte commandClass, byte commandNumber, byte... recv) {
      Map<String, Object> recvMap = new LinkedHashMap<>();
      for(int i=0; i<recv.length; i++) {
         recvMap.put("recv" + i, recv[i]);
      }
      return createCommand(commandClass, commandNumber);
   }
   public static ZWaveCommand createCommand(byte commandClass, byte commandNumber, Map<String, Byte> recv) {
      ZWaveCommand command = new ZWaveCommand();
      command.commandClass = commandClass; 
      command.commandNumber = commandNumber;
      command.setReceiveNames(new ArrayList<>(recv.keySet()));
      for(Map.Entry<String, Byte> entry: recv.entrySet()) {
         command.set(entry.getKey(), entry.getValue());
      }
      return command;
   }

}

