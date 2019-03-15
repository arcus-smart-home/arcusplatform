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
package com.iris.protocol.zwave.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonTypeInfo;
//import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonProperty;

//@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = As.PROPERTY, property = "@class")
public class ZWaveCommandClass implements Serializable {
   private static final long serialVersionUID = 384833558421489848L;

   ////////////////////////////////////////////
   // Internal Variables.
   @JsonProperty("name")
   public String name;   // Name of the class
   @JsonProperty("id")
   public byte   number; // Number used to identify the class.
   @JsonProperty("version")
   public byte version; // Version of commands to use.

   @JsonProperty("Commands")
   final private ArrayList<ZWaveCommand> commands = new ArrayList<ZWaveCommand>(); // all of the commands related to this clas.

   @JsonIgnore
   final public LinkedHashMap<String, ZWaveCommand> commandsByName = new LinkedHashMap<String, ZWaveCommand>(); // All Command for this class listed by name.
   @JsonIgnore
   final public LinkedHashMap<Byte, ZWaveCommand>   commandsByByte = new LinkedHashMap<Byte, ZWaveCommand>();   // All Command for this class listed by byte number.

   @JsonCreator
   public ZWaveCommandClass() {
      name = "unknown";
      number = 0x00;
      version = 0x01;
   }

   public ZWaveCommandClass(String name, byte number) {
      this.name = name;
      this.number = number;
   }

   void setCommands(ArrayList<ZWaveCommand> commands) {
      Iterator<ZWaveCommand> i;
      ZWaveCommand command;

      this.commands.clear();
      commandsByName.clear();
      commandsByByte.clear();

      i = commands.iterator();
      while (i.hasNext()) {
         command = i.next();
         add(command);
      }
   }

   public void addZWaveCommand(ZWaveCommand command) {
      add(command);
   }

   public void add(ZWaveCommand command) {
      command.commandClass = number; // Set the command class number.
      commands.add(command);
      commandsByName.put(scrub(command.commandName), command);
      commandsByByte.put(command.commandNumber, command);
   }

   public ZWaveCommand get(String commandName) {
      ZWaveCommand pristineCommand = commandsByName.get(scrub(commandName));
      return (pristineCommand == null) ? null : new ZWaveCommand(pristineCommand);
   }

   public ZWaveCommand get(byte commandNumber) {
      ZWaveCommand pristineCommand = commandsByByte.get(Byte.valueOf(commandNumber));
      return (pristineCommand == null) ? null : new ZWaveCommand(pristineCommand);
   }

   // Setting values of specific command
   public boolean set(String commandName, String varname, byte value) {
      return setSend(commandName, varname, value);
   }

   public boolean setSend(String commandName, String varname, byte value) {
      ZWaveCommand cmd = get(commandName);
      if (cmd == null)
         return false;
      return cmd.set(varname, value);
   }

   public boolean setRecv(String commandName, String varname, byte value) {
      ZWaveCommand cmd = get(commandName);
      if (cmd == null)
         return false;
      return cmd.setRecv(varname, value);
   }

   public byte get(String commandName, String varname) {
      return getRecv(commandName, varname);
   }

   public byte getRecv(String commandName, String varname) {
      ZWaveCommand cmd = get(commandName);
      if (commandName == null)
         return 0x00; // TODO:  Handle this a little better, throw?

      return cmd.getRecv(varname);
   }

   public byte getSend(String commandName, String varname) {
      ZWaveCommand cmd = get(commandName);
      if (commandName == null)
         return 0x00; // TODO:  Handle this a little better, throw?

      return cmd.getSend(varname);
   }

   public static String scrub(String str) {
      return str.toLowerCase().replace(' ', '_');
   }

   @Override
   public String toString() {
      String string = "ZWaveCommandClass: ";
      Iterator<ZWaveCommand> i;
      ZWaveCommand command;

      i = commands.iterator();
      while (i.hasNext()) {
         command = i.next();
         string += command.toString();
      }

      return string;
   }
}

