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

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.driver.groovy.context.GroovyCapabilityDefinition;
import com.iris.driver.groovy.reflex.ReflexMatchContext;
import com.iris.driver.groovy.reflex.ReflexUtil;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.protoc.runtime.ProtocUtil;
import com.iris.protocol.zwave.Protocol;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.message.ZWaveDelayedCommandsMessage;
import com.iris.protocol.zwave.message.ZWaveMessage;
import com.iris.protocol.zwave.message.ZWaveOrderedCommandsMessage;
import com.iris.protocol.zwave.message.controller.ZWaveScheduleMessage;
import com.iris.protocol.zwave.message.controller.ZWaveSetOfflineTimeoutMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;
import com.iris.protocol.zwave.model.ZWaveNode;

@Singleton
public class ZWaveContext extends GroovyObjectSupport {
   private static final Logger LOGGER = LoggerFactory.getLogger(ZWaveContext.class);
   private static Map<String, CommandClassObject> commandClasses;
   private final ZWaveConfigContext configContext;

   @Inject
   public ZWaveContext() {
      this(getAllZWaveCommandClasses());
   }

   public ZWaveContext(List<ZWaveCommandClass> commandClasses) {
      if (this.commandClasses == null) {
         this.commandClasses = convertToGroovy(commandClasses);
      }
      this.configContext = new ZWaveConfigContext();
   }

   public void processReflexes(DriverBinding binding) {
      configContext.processReflexes(binding);
   }

   // TODO getProperties
   // TODO setProperties should be read-only for everything

   public Map<String, Object> getProperties() {
      Map<String, Object> properties = new HashMap<String, Object>(commandClasses.size() + 1);
      properties.put("class", getClass());
      properties.putAll(commandClasses);
      return properties;
   }

   public void call(Closure<?> configClosure) {
      configClosure.setDelegate(configContext);
      configClosure.call();
   }

   public void call(GroovyCapabilityDefinition.ActionAndClosure action) {
      ZWaveActionContext ctx = new ZWaveActionContext(action.getClosure().getOwner(), this);
      action.getClosure().setDelegate(ctx);
      action.getClosure().call();
   }

   public void send(ReflexMatchContext.ProtocolClosureProcessor proc, Map<String,Object> payload) {
      byte commandClass = -1;
      if (payload.containsKey("commandClass") && !payload.containsKey("cc")) {
         commandClass = ((Number)payload.get("commandClass")).byteValue();
      } else if (!payload.containsKey("commandClass") && payload.containsKey("cc")) {
         commandClass = ((Number)payload.get("cc")).byteValue();
      } else {
         GroovyValidator.error("zigbee zcl message must contain either 'id' or 'command' to define the command identifier");
      }

      byte commandNumber = -1;
      if (payload.containsKey("id") && !payload.containsKey("command")) {
         commandNumber = ((Number)payload.get("id")).byteValue();
      } else if (!payload.containsKey("id") && payload.containsKey("command")) {
         commandNumber = ((Number)payload.get("command")).byteValue();
      } else {
         GroovyValidator.error("zigbee zcl message must contain either 'id' or 'command' to define the command identifier");
      }

      byte[] msg = ReflexUtil.extractAsByteArray(payload, "payload");
      ZWaveCommand cmd = new ZWaveCommand(commandClass, commandNumber, msg);

      Map<String,Object> args = createZWaveArguments(cmd);
      proc.process(cmd, args);
   }

   public void match(ReflexMatchContext.ProtocolClosureProcessor proc, Map<String,Object> payload) {
      // <TYPE> <LEN> <LEN> <LEN> <LEN> <NODE> <CCID> <CMDID> <LEN> <LEN> <LEN> <LEN> <PAYLOAD>...
      StringBuilder rex = new StringBuilder();
      rex.append(ProtocUtil.toHexString((byte)Protocol.Command.ID))
         .append(" . . . . .");

      rex.append(" ");
      if (payload.containsKey("commandClass") && !payload.containsKey("cc")) {
         rex.append(ProtocUtil.toHexString(((Number)payload.get("commandClass")).byteValue()));
      } else if (!payload.containsKey("commandClass") && payload.containsKey("cc")) {
         rex.append(ProtocUtil.toHexString(((Number)payload.get("cc")).byteValue()));
      } else {
         rex.append(".");
      }

      rex.append(" ");
      if (payload.containsKey("id") && !payload.containsKey("command")) {
         rex.append(ProtocUtil.toHexString(((Number)payload.get("id")).byteValue()));
      } else if (!payload.containsKey("id") && payload.containsKey("command")) {
         rex.append(ProtocUtil.toHexString(((Number)payload.get("command")).byteValue()));
      } else {
         rex.append(".");
      }

      List<String> msg = ReflexUtil.extractAsMatchList(payload, "payload");
      proc.process(new ZWaveReflex.ProtocolMatch(rex,msg), ImmutableMap.<String,Object>of());
   }

   private static Map<String,Object> createZWaveArguments(ZWaveCommand cmd) {
      Map<String,Object> args = new HashMap<>();
      for (String name : cmd.getSendNames()) {
         args.put(name, cmd.getSend(name));
      }

      return args;
   }

   @Override
   public Object getProperty(String propertyName) {
      Object commandClass = getCommandClassProperty(propertyName);
      if(commandClass != null) {
         return commandClass;
      }

      return super.getProperty(propertyName);
   }

   Object getCommandClassProperty(String propertyName) {
      return commandClasses.get(propertyName);
   }

   public void poll(int seconds, CommandClosure firstClosure, CommandClosure... closures) {
      List<ZWaveCommand> commands = new ArrayList<>();
      commands.add(firstClosure.getZWaveCommand());
      if (closures != null && closures.length > 0) {
         for (CommandClosure closure : closures) {
            commands.add(closure.getZWaveCommand());
         }
      }
      sendScheduleMessage(seconds, commands);
   }

   public void poll(int seconds, CommandClosure closure, Map<String,Object> args) {
      List<ZWaveCommand> commands = new ArrayList<>();
      ZWaveCommand cmd = closure.getZWaveCommand();
      if (args != null) {
         setPollArgs(cmd,args);
      }

      commands.add(cmd);
      sendScheduleMessage(seconds, commands);
   }

   public void poll(int seconds, String commandClass, String command, String... commandStrings) {
      List<ZWaveCommand> commands = new ArrayList<>();
      commands.add(ZWaveUtil.getCommandByName(ZWaveUtil.getCommandClassByName(commandClass), command));
      if (commandStrings != null && commandStrings.length > 0) {
         if (commandStrings.length % 2 != 0) {
            throw new IllegalArgumentException("Command class and command must be specified in pairs.");
         }
         for (int i = 0; i < commandStrings.length; i += 2) {
            commands.add(ZWaveUtil.getCommandByName(ZWaveUtil.getCommandClassByName(commandStrings[i]), commandStrings[i + 1]));
         }
      }
      sendScheduleMessage(seconds, commands);
   }

   public void poll(int seconds, byte commandClassByte, byte commandByte, byte... commandBytes) {
      List<ZWaveCommand> commands = new ArrayList<>();
      commands.add(ZWaveUtil.getCommandByByte(ZWaveUtil.getCommandClassByByte(commandClassByte), commandByte));
      if (commandBytes != null && commandBytes.length > 0) {
         if (commandBytes.length % 2 != 0) {
            throw new IllegalArgumentException("Command class and command must be specified in pairs.");
         }
         for (int i = 0; i < commandBytes.length; i += 2) {
            commands.add(ZWaveUtil.getCommandByByte(ZWaveUtil.getCommandClassByByte(commandBytes[i]), commandBytes[i + 1]));
         }
      }
      sendScheduleMessage(seconds, commands);
   }

   @SuppressWarnings("rawtypes")
   public void poll(int seconds, Map firstMap, Map... commandMaps) {
      List<ZWaveCommand> commands = new ArrayList<>();
      ZWaveCommand firstCommand = ZWaveUtil.getCommandByByte(
            ZWaveUtil.getCommandClassByByte(((Integer)firstMap.get("commandClass")).byteValue()),
            ((Integer)firstMap.get("command")).byteValue());
      setPollArgs(firstCommand, (Map<String,Object>)firstMap.get("args"));
      commands.add(firstCommand);
      if (commandMaps != null && commandMaps.length > 0) {
         for (Map map : commandMaps) {
            ZWaveCommand command = ZWaveUtil.getCommandByByte(
                  ZWaveUtil.getCommandClassByByte(((Integer)map.get("commandClass")).byteValue()),
                  ((Integer)map.get("command")).byteValue());
            setPollArgs(command, (Map<String,Object>)map.get("args"));
            commands.add(command);
         }
      }
      sendScheduleMessage(seconds, commands);
   }

   private void setPollArgs(ZWaveCommand command, Map<String,Object> args) {
      for (Map.Entry<String,Object> arg : args.entrySet()) {
         command.setSend(arg.getKey(), ((Number)arg.getValue()).byteValue());
      }
   }

   public void setOfflineTimeout(int seconds) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      ZWaveSetOfflineTimeoutMessage message = new ZWaveSetOfflineTimeoutMessage(ZWaveUtil.extractNode(context), seconds);
      context.sendToDevice(ZWaveProtocol.INSTANCE, message, -1);
   }

   public void updateFirmware(String url, String priority) {
      throw new UnsupportedOperationException("Z-Wave OTA updates not implemented");
   }

   public void updateFirmwareCancel() {
      throw new UnsupportedOperationException("Z-Wave OTA updates not implemented");
   }

   public void sendCommand(byte commandClass, byte commandId, byte... sendVars) {
      doSendZWaveCommand(commandClass, commandId, sendVars);
   }

   public void sendCommand(String commandClassName, String commandIdName, Map<String, Byte> sendVars) {
      ZWaveCommandClass commandClass = ZWaveAllCommandClasses.getClass(commandClassName);
      if(commandClass == null) {
         throw new IllegalArgumentException("Unrecognized commandClass [" + commandClassName + "]");
      }
      ZWaveCommand command = commandClass.get(commandIdName);
      if(command == null) {
         throw new IllegalArgumentException("Unrecognized commandId [" + commandIdName + "]");
      }
      doSendZWaveCommand(commandClass, command, sendVars);
   }

   public void sendCommand(Map<String, Object> args) {
      Object commandClassKey = args.remove("commandClass");
      Object commandKey = args.remove("command");
      if(commandClassKey == null || commandKey == null) {
         throw new IllegalArgumentException("Must specify 'commandClass' and 'command'");
      }

      ZWaveCommandClass commandClass = null;
      if(commandClassKey instanceof String) {
         commandClass = ZWaveAllCommandClasses.getClass((String) commandClassKey);
      }
      else if(commandClassKey instanceof Number) {
         commandClass = ZWaveAllCommandClasses.getClass(((Number) commandClassKey).byteValue());
      }
      else {
         throw new IllegalArgumentException("Invalid value for commandClass, must be a String name or byte");
      }
      if(commandClass == null) {
         throw new IllegalArgumentException("Unrecognized commandClass [" + commandClassKey + "]");
      }

      ZWaveCommand command = null;
      if(commandKey instanceof String) {
         command = commandClass.get((String) commandKey);
      }
      else if(commandKey instanceof Number) {
         command = commandClass.get(((Number) commandKey).byteValue());
      }
      else {
         throw new IllegalArgumentException("Invalid value for commandId, must be a String name or byte");
      }
      if(command == null) {
         throw new IllegalArgumentException("Unrecognized commandId [" + commandKey + "] for commandClass [" + commandClass.name + "]");
      }

      doSendZWaveCommand(commandClass, command, args);
   }

   public void sendOrdered(ZWaveCommand... commands) {
      sendOrdered(Arrays.asList(commands));
   }

   public void sendOrdered(List<ZWaveCommand> commands) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      ZWaveContext.doSendZWaveCommandsOrdered(context, commands);
   }

   public void sendDelayed(long delay, TimeUnit unit, List<ZWaveCommand> commands) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      ZWaveContext.doSendZWaveCommandsDelayed(context, delay, unit, commands);
   }

   @SuppressWarnings("rawtypes")
   private void setSendBytes(ZWaveCommand command, List bytes) {
      int i = 0;
      for(Object obj : bytes) {
         String name = "send" + i++;
         command.addSendVariable(name);
         command.setSend(name, ((Integer)obj).byteValue());
      }
   }

   private void sendScheduleMessage(int seconds, List<ZWaveCommand> commands) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      ZWaveScheduleMessage message = new ZWaveScheduleMessage(ZWaveUtil.extractNode(context),
            commands.toArray(new ZWaveCommand[0]), seconds);
      context.sendToDevice(ZWaveProtocol.INSTANCE, message, -1);
   }

   protected static void addSendVars(ZWaveCommand command, Map<String, ? extends Object> variables) {
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

   protected static byte getByte(Object value) {
      if(value == null) {
         return 0;
      }
      if(!(value instanceof Number)) {
         throw new IllegalArgumentException("Send variable must be a byte, not [" + value.getClass() + "]");
      }
      // TODO bounds checking?
      return ((Number) value).byteValue();
   }

   protected static ZWaveCommand doCreateZWaveCommand(byte commandClass, byte commandId, byte... sendVars) {
      ZWaveCommand command = new ZWaveCommand();
      command.commandClass = commandClass;
      command.commandNumber = commandId;
      for(int i=0; i<sendVars.length; i++) {
         String name = "send" + i;
         command.addSendVariable(name);
         command.setSend(name, sendVars[i]);
      }

      return command;
   }

   protected static ZWaveCommand doCreateZWaveCommand(ZWaveCommandClass commandClass, ZWaveCommand command, Map<String, ? extends Object> sendVars) {
      ZWaveCommand cmd = new ZWaveCommand();
      cmd.commandClass = command.commandClass;
      cmd.commandNumber = command.commandNumber;
      cmd.commandName = command.commandName;
      cmd.setSendNames(command.getSendNames());
      addSendVars(cmd, sendVars);
      return cmd;
   }

   protected static void doSendZWaveCommand(byte commandClass, byte commandId, byte... sendVars) {
      doSendZWaveCommand(doCreateZWaveCommand(commandClass,commandId,sendVars));
   }

   protected static void doSendZWaveCommand(ZWaveCommandClass commandClass, ZWaveCommand command, Map<String, ? extends Object> sendVars) {
      doSendZWaveCommand(doCreateZWaveCommand(commandClass, command, sendVars));
   }

   protected static void doSendZWaveCommand(ZWaveCommand command) {
      DeviceDriverContext context = GroovyContextObject.getContext();
      doSendZWaveCommand(GroovyContextObject.getContext(), command);
   }

   static void doSendZWaveCommand(DeviceDriverContext context, ZWaveCommand command) {
      ZWaveCommandMessage message = new ZWaveCommandMessage();
      message.setDevice(extractDevice(context));
      message.setCommand(command);
      context.sendToDevice(ZWaveProtocol.INSTANCE, message, -1);
   }

   static void doSendZWaveCommandsOrdered(DeviceDriverContext context, List<ZWaveCommand> commands) {
      if (commands == null || commands.isEmpty()) {
         return;
      }

      ZWaveNode node = extractDevice(context);
      List<ZWaveMessage> messages = new ArrayList<>(commands.size());
      for (ZWaveCommand cmd : commands) {
         messages.add(new ZWaveCommandMessage(node,cmd));
      }

      ZWaveOrderedCommandsMessage message = new ZWaveOrderedCommandsMessage(extractDevice(context), messages);
      context.sendToDevice(ZWaveProtocol.INSTANCE, message, -1);
   }

   static void doSendZWaveCommandsDelayed(DeviceDriverContext context, long delay, TimeUnit unit, List<ZWaveCommand> commands) {
      if (commands == null || commands.isEmpty()) {
         return;
      }

      ZWaveNode node = extractDevice(context);
      List<ZWaveMessage> messages = new ArrayList<>(commands.size());
      for (ZWaveCommand cmd : commands) {
         messages.add(new ZWaveCommandMessage(node,cmd));
      }

      ZWaveDelayedCommandsMessage message = new ZWaveDelayedCommandsMessage(extractDevice(context), unit.toNanos(delay), messages);
      context.sendToDevice(ZWaveProtocol.INSTANCE, message, -1);
   }

   private static List<ZWaveCommandClass> getAllZWaveCommandClasses() {
      ZWaveAllCommandClasses.init();
      return ZWaveAllCommandClasses.allClasses.commandClasses;
   }

   private static ZWaveNode extractDevice(DeviceDriverContext context) {
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

   private static Map<String, CommandClassObject> convertToGroovy(List<ZWaveCommandClass> commandClasses) {
      if(commandClasses == null || commandClasses.isEmpty()) {
         LOGGER.warn("No command classes loaded, all Z-Wave messages need to be sent as binary");
         return Collections.emptyMap();
      }

      Map<String, CommandClassObject> converted =
            new HashMap<String, CommandClassObject>((commandClasses.size() + 1)*4/3, 0.75f);
      for(ZWaveCommandClass commandClass: commandClasses) {
         converted.put(ZWaveCommand.scrub(commandClass.name), new CommandClassObject(commandClass));
      }
      return converted;
   }

   private static Map<String, CommandClosure> convertToGroovy(CommandClassObject owner, Collection<ZWaveCommand> commands) {
      if(commands == null || commands.isEmpty()) {
         // this is really common
         return Collections.emptyMap();
      }

      Map<String, CommandClosure> converted =
            new HashMap<String, CommandClosure>(commands.size());
      for(ZWaveCommand command: commands) {
         converted.put(command.commandName, new CommandClosure(owner, command));
      }
      return converted;
   }

   private static class CommandClassObject extends GroovyObjectSupport {
      private final ZWaveCommandClass commandClass;
      private final Map<String, CommandClosure> commandClosures;

      CommandClassObject(ZWaveCommandClass commandClass) {
         this.commandClass = commandClass;
         this.commandClosures = convertToGroovy(this, commandClass.commandsByName.values());
      }

      public String getName() {
         return commandClass.name;
      }

      public byte getNumber() {
         return commandClass.number;
      }

      public byte getVersion() {
         return commandClass.version;
      }

      @Override
      public Object getProperty(String propertyName) {
         CommandClosure command = commandClosures.get(propertyName);
         if(command != null) {
            return command;
         }
         return super.getProperty(propertyName);
      }

      @Override
      public Object invokeMethod(String name, Object args) {
         CommandClosure closure = commandClosures.get(name);
         if(closure != null) {
            return closure.call(args);
         }
         else {
            return super.invokeMethod(name, args);
         }
      }
   }

   static class CommandClosure extends Closure<Object> {
      private final ZWaveCommandClass commandClass;
      private final ZWaveCommand command;

      CommandClosure(CommandClassObject commandClassObject, ZWaveCommand command) {
         super(commandClassObject);
         this.commandClass = commandClassObject.commandClass;
         this.command = command;
      }

      ZWaveCommand getZWaveCommand() {
         return command;
      }

      public ZWaveCommand create(byte... sendVars) {
         return doCreateZWaveCommand(commandClass.number, command.commandNumber, sendVars);
      }

      public ZWaveCommand create(Object... sendVars) {
         byte[] values = new byte[sendVars.length];
         for(int i=0; i<sendVars.length; i++) {
            values[i] = getByte(sendVars[i]);
         }

         return create(values);
      }

      public ZWaveCommand create(Map<String, Byte> sendVars) {
         return doCreateZWaveCommand(commandClass, command, sendVars);
      }

      protected void doCall(byte... sendVars) {
         doSendZWaveCommand(commandClass.number, command.commandNumber, sendVars);
      }

      protected void doCall(Object... sendVars) {
         byte [] primitives = new byte[sendVars.length];
         for(int i=0; i<sendVars.length; i++) {
            primitives[i] = getByte(sendVars[i]);
         }
         doSendZWaveCommand(commandClass.number, command.commandNumber, primitives);
      }

      protected void doCall(Map<String, Byte> sendVars) {
         doSendZWaveCommand(commandClass, command, sendVars);
      }

   }
}

