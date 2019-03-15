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
package com.iris.driver.groovy.zwave;

import java.util.Map;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.protocol.Protocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.message.ZWaveMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;
import com.iris.protocol.zwave.model.ZWaveNode;

class ZWaveUtil {
   //private static ZWaveAllCommandClasses commandClasses;

   static ZWaveCommandClass getCommandClassByName(String commandClassName) {
      ZWaveCommandClass commandClass = getCommandClasses().get(commandClassName);
      if(commandClass == null) {
         throw new IllegalArgumentException("No command class [" + commandClassName + "] is registered in the system, valid options are " + getCommandClasses().classesByName.keySet());
      }
      return commandClass;
   }

   static ZWaveCommand getCommandByName(ZWaveCommandClass commandClass, String commandName) {
      ZWaveCommand command = commandClass.commandsByName.get(commandName);
      if(command == null) {
         throw new IllegalArgumentException("No command [" + commandName + "] in class [" + commandClass.name + "], valid options are " + commandClass.commandsByName.keySet());
      }
      return command;
   }

   static ZWaveCommandClass getCommandClassByByte(byte commandClassByte) {
      ZWaveCommandClass commandClass = getCommandClasses().get(commandClassByte);
      if(commandClass == null) {
         throw new IllegalArgumentException("No command class [" + Integer.toHexString(commandClassByte) + "] is registered in the system.");
      }
      return commandClass;
   }

   static ZWaveCommand getCommandByByte(ZWaveCommandClass commandClass, byte commandNameByte) {
      ZWaveCommand command = commandClass.commandsByByte.get(commandNameByte);
      if(command == null) {
         throw new IllegalArgumentException("No command [" + Integer.toHexString(commandNameByte) + "] in class [" + commandClass.name + "].");
      }
      return command;
   }

   static void doSendZWaveCommand(Protocol<ZWaveMessage> protocol, byte commandClass, byte commandId, byte... sendVars) {
      ZWaveCommand command = new ZWaveCommand();
      command.commandClass = commandClass;
      command.commandNumber = commandId;
      for(int i=0; i<sendVars.length; i++) {
         String name = "send" + i;
         command.addSendVariable(name);
         command.setSend(name, sendVars[i]);
      }
      doSendZWaveCommand(protocol, command);
   }

   static void doSendZWaveCommand(Protocol<ZWaveMessage> protocol, ZWaveCommandClass commandClass, ZWaveCommand command, Map<String, ? extends Object> sendVars) {
      ZWaveCommand cmd = new ZWaveCommand();
      cmd.commandClass = command.commandClass;
      cmd.commandNumber = command.commandNumber;
      cmd.commandName = command.commandName;
      cmd.setSendNames(command.getSendNames());
      addSendVars(cmd, sendVars);
      doSendZWaveCommand(protocol, cmd);
   }

   static void doSendZWaveCommand(Protocol<ZWaveMessage> protocol, ZWaveCommand command) {
      DeviceDriverContext context = GroovyContextObject.getContext();

      ZWaveCommandMessage message = new ZWaveCommandMessage();
      message.setDevice(ZWaveUtil.extractNode(context));
      message.setCommand(command);
      context.sendToDevice(protocol, message, -1);
   }

   static ZWaveNode extractNode(DeviceDriverContext context) {
      try {
         DeviceProtocolAddress address = (DeviceProtocolAddress) context.getProtocolAddress();
         if(address == null) {
            throw new IllegalStateException("Protocol address is not configured can't send z-wave message");
         }
         ProtocolDeviceId deviceId = address.getProtocolDeviceId();
         return new ZWaveNode(deviceId.getBytes()[0]);
      } catch(ClassCastException | ArrayIndexOutOfBoundsException e) {
         throw new IllegalStateException("Protocol address [" + context.getProtocolAddress() + "] is not a Z-Wave address, can't send message", e);
      }
   }

   static byte getByte(Object value) {
      if(value == null) {
         return 0;
      }
      if(!(value instanceof Number)) {
         throw new IllegalArgumentException("Send variable must be a byte, not [" + value.getClass() + "]");
      }
      // TODO bounds checking?
      return ((Number) value).byteValue();
   }

   private static void addSendVars(ZWaveCommand command, Map<String, ? extends Object> variables) {
      for(Map.Entry<String, ? extends Object> variable: variables.entrySet()) {
         String name = variable.getKey();
         Object value = variable.getValue();
         if(value == null) {
            continue;
         }
         byte b = getByte(value);
         if(!command.setSend(name, b)) {
            throw new IllegalArgumentException("Unrecognized send variable [" + name + "] for command [" + command.commandName + "]");
         }
      }
   }

   public static ZWaveAllCommandClasses getCommandClasses() {
      return CommandClassHolder.INSTANCE.commandClasses;
   }

   private static class CommandClassHolder {
      private static final CommandClassHolder INSTANCE = new CommandClassHolder();
      private ZWaveAllCommandClasses commandClasses;

      public CommandClassHolder() {
         ZWaveAllCommandClasses.init();
         commandClasses = ZWaveAllCommandClasses.allClasses;
      }
   }
}

