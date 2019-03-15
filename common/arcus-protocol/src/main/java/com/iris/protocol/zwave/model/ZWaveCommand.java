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
/*
 * ZwaveCommand
 *
 * A class to handle ZWave commands. All command classes have a set of commands.
 * Each command is represented by a byte id that determines the function to
 * perform. The byte id is then followed by an array of bytes presenting the
 * command function. A response from the device will also have an array bytes
 * that represent the actual response. For example in the command class
 * "Switch Binary" there is the command "Switch Binary Set" Name =
 * "Switch Binary Set"; commandClass = 0x25; // Switch Binary commandNumber =
 * 0x01; sendVariables = { "Value", 0xFF }; receivedVariables = {}; This can be
 * packaged in to a function message array: node, length, 0x25, 0x01, 0xFF,
 * funcID
 *
 * where node is the node ID to communicate to. length is the length of the
 * message to follow (in example this is 0x03) funcID is the assigned is for
 * keeping track of the various functions that could be out there.
 */

package com.iris.protocol.zwave.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.mutable.MutableByte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Jackson JSON Parsing
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
// import com.fasterxml.jackson.annotation.JsonTypeInfo;
// import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

// @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = As.PROPERTY,
// property = "@class")
public class ZWaveCommand implements Serializable {
   @JsonIgnore
   private static final Logger log = LoggerFactory.getLogger(ZWaveCommand.class); // Where to log information and debugging strings.
   private static final long serialVersionUID = -7739579696209437442L;

   @JsonIgnore
   public static final byte NO_RESPONSE = 0x00;

   @JsonProperty("name")
   public String commandName;   // Real Name of the command
   @JsonIgnore
   public byte   commandClass;  // Byte representation of the command class.
   @JsonProperty("id")
   public byte   commandNumber; // Byte Identifier for the command.
   @JsonProperty("response_id")
   public byte   response_id;   // ID of report/expected response from the command class.

   @JsonProperty("SendVars")
   final public ArrayList<String> sendNames;

   @JsonProperty("ReceiveVars")
   final public ArrayList<String> receiveNames;

   @JsonIgnore
   final public LinkedHashMap<String, MutableByte> sendVariables; // Variables to be sent as part of the command.
   @JsonIgnore
   final public LinkedHashMap<String, MutableByte> receiveVariables; // Variables to be received from issue of the command.

   @JsonIgnore
   public int responseStatus; // Status

   @JsonIgnore
   byte[] sendBytes;

   @JsonIgnore
   public byte[] recvBytes;

   @JsonCreator
   public ZWaveCommand() {
      commandName = "unknown";
      commandNumber = 0x00;
      response_id = NO_RESPONSE;     // None.

      this.sendNames = new ArrayList<String>();
      this.receiveNames = new ArrayList<String>();
      this.sendVariables    = new LinkedHashMap<String, MutableByte>();
      this.receiveVariables = new LinkedHashMap<String, MutableByte>();

      this.sendBytes = new byte[2];
   }

   public ZWaveCommand(String name, byte number) {
      commandName = name;
      commandNumber = number;

      this.sendNames = new ArrayList<String>();
      this.receiveNames = new ArrayList<String>();
      this.sendVariables    = new LinkedHashMap<String, MutableByte>();
      this.receiveVariables = new LinkedHashMap<String, MutableByte>();

      this.sendBytes = new byte[2];
   }

   public ZWaveCommand(byte commandClass, byte commandNumber, byte[] payload) {
      this.commandName = "unknown";
      this.commandClass = commandClass;
      this.commandNumber = commandNumber;
      this.response_id = NO_RESPONSE;

      this.sendNames = new ArrayList<>();
      this.receiveNames = new ArrayList<>();

      this.sendVariables = new LinkedHashMap<String, MutableByte>();
      this.receiveVariables = new LinkedHashMap<String, MutableByte>();

      if (payload != null) {
         int i = 0;
         for (byte val : payload) {
            this.sendVariables.put("v" + i, new MutableByte(val));
            sendNames.add("v" + i);
            i++;
         }
      }

      this.sendBytes = new byte[this.sendVariables.size() + 2];
   }

   public ZWaveCommand(ZWaveCommand clone) {
      this.commandName = clone.commandName;
      this.commandClass = clone.commandClass;
      this.commandNumber = clone.commandNumber;
      this.response_id = clone.response_id;

      this.sendNames = new ArrayList<>(clone.sendNames);
      this.receiveNames = new ArrayList<>(clone.receiveNames);

      this.sendVariables    = new LinkedHashMap<String, MutableByte>();
      if (clone.sendVariables != null) {
         for (Map.Entry<String,MutableByte> entry : clone.sendVariables.entrySet()) {
            this.sendVariables.put(entry.getKey(), new MutableByte(entry.getValue()));
         }
      }

      this.receiveVariables = new LinkedHashMap<String, MutableByte>();
      if (clone.receiveVariables != null) {
         for (Map.Entry<String,MutableByte> entry : clone.receiveVariables.entrySet()) {
            this.receiveVariables.put(entry.getKey(), new MutableByte(entry.getValue()));
         }
      }

      this.responseStatus = clone.responseStatus;
      this.sendBytes = (clone.sendBytes == null) ? null : Arrays.copyOf(clone.sendBytes, clone.sendBytes.length);
      this.recvBytes = (clone.recvBytes == null) ? null : Arrays.copyOf(clone.recvBytes, clone.recvBytes.length);
   }

   public void setSendNames(ArrayList<String> names) {
      sendNames.clear();
      sendVariables.clear();

      String name;
      Iterator<String> i = names.iterator();
      while (i.hasNext()) {
         name = i.next();
         addSendVariable(name);
      }
   }

   public ArrayList<String> getSendNames() {
      return sendNames;
   }

   public void setReceiveNames(ArrayList<String> names) {
      receiveNames.clear();
      receiveVariables.clear();

      String name;
      Iterator<String> i = names.iterator();
      while (i.hasNext()) {
         name = i.next();
         addReceiveVariable(name);
      }
   }

   public ArrayList<String> getReceiveNames() {
      return receiveNames;
   }

   public void addSendVariable(String name) {
      sendNames.add(scrub(name));
      sendVariables.put(scrub(name), new MutableByte((byte) 0x00));
      // Increase the byte buffer to match the variables being sent, where is realloc when you need it.. right in C.
      sendBytes = new byte[sendVariables.size() + 2]; // 2, one for class, one for command.
   }

   public void addReceiveVariable(String name) {
      receiveNames.add(scrub(name));
      receiveVariables.put(scrub(name), new MutableByte((byte) 0x00));
   }

   public boolean set(byte[] sendBytes) {
      Iterator<Entry<String, MutableByte>> i;
      Entry<String, MutableByte> entry;
      MutableByte mb;
      byte b;
      int j;

      i = sendVariables.entrySet().iterator();
      j = 0;
      while (i.hasNext()) {
         entry = i.next();
         mb = entry.getValue();
         mb.setValue(sendBytes[j]);
         j++;
      }
      return true;
   }

   public boolean set(String name, byte value) {
      return setSend(name, value);
   }

   public boolean setSend(String name, byte value) {
      MutableByte b = sendVariables.get(scrub(name));
      if (b == null)
         return false;

      b.setValue(value);
      return true;
   }

   public boolean setRecv(String name, byte value) {
      MutableByte b = receiveVariables.get(scrub(name));
      if (b == null)
         return false;

      b.setValue(value);
      return true;
   }

   public byte get(String name) {
      return getRecv(name);
   }

   public byte getRecv(String name) {
      MutableByte b = receiveVariables.get(scrub(name));
      return b.getValue();
   }

   public byte getSend(String name) {
      MutableByte b = sendVariables.get(scrub(name));
      return b.getValue();
   }

   // gets the proper bytes to send.
   public byte[] toBytes() {
      Iterator<Entry<String, MutableByte>> i;
      Entry<String, MutableByte> entry;
      MutableByte mb;
      byte b;
      int j;

      sendBytes[0] = commandClass;
      sendBytes[1] = commandNumber;

      i = sendVariables.entrySet().iterator();
      j = 2;
      while (i.hasNext()) {
         entry = i.next();
         mb = entry.getValue();
         b = mb.getValue();
         sendBytes[j] = b;
         j++;
      }

      return sendBytes; // The other side can clone if they want or just straight copy them.
   }

   public byte[] payload() {
      byte[] payload = new byte[sendVariables.size()];

      int idx = 0;
      for (Entry<String,MutableByte> entry : sendVariables.entrySet()) {
         payload[idx++] = entry.getValue().byteValue();
      }

      return payload;
   }

   // It does not know about ZWaveFrame.
   public boolean parseAppDataFrameBytes(byte[] bytes) {
      return parseBytes(bytes, 8);
   }

   // Parses the frame bytes into the variable bytes.
   public boolean parseBytes(byte[] bytes) {
      return parseBytes(bytes, 0, bytes.length);
   }

   //Parse bytes using the offset
   public boolean parseBytes(byte[] bytes, int offset) {
      return parseBytes(bytes, offset, receiveVariables.size() + 2);
   }

   // Parsed the bytes using only the given section as the bytes used.
   public boolean parseBytes(byte[] bytes, int offset, int length) {

      Iterator<Entry<String, MutableByte>> i; // Iterator over the received variables.
      Entry<String, MutableByte> entry; // Entry in the iterator
      MutableByte MB; // Entry value of the key/value pair.
      byte b; // Actual byte we are interested in.
      int j = offset;
      int k;

      if (bytes.length < offset + length)
         return false; // Index array bound exception.
      if (bytes[j] != commandClass)
         return false; // Should not be parsing this command.
      if (bytes[j + 1] != commandNumber)
         return false; // Should not be parsing this command.

      if ((recvBytes == null) || (recvBytes.length != length)) {
         recvBytes = new byte[length];
      }

      i = receiveVariables.entrySet().iterator();
      j += 2;
      k = 0;
      while (i.hasNext() && j < length) {
         b = bytes[j];
         entry = i.next();
         MB = entry.getValue();
         MB.setValue(b);
         recvBytes[k] = b;
         j++;
         k++;
      }

      // Copy the rest of the bytes over.
      int rest = bytes.length - j;
      if (rest > (recvBytes.length - k)) {
         rest = (recvBytes.length - k);
      }
      if (rest > 0) {
         if (log.isTraceEnabled()) {
            log.trace(String.format("Rest of Bytes Copy.bytes.length = %d;  j=%d;  recvBytes.length = %d; k=%d; rest = %d", bytes.length, j, recvBytes.length, k, rest));
         }
         System.arraycopy(bytes, j, recvBytes, k, rest);
      }
      return true; // Made it through without error.
   }

   public boolean parsePayload(byte[] bytes) {
      return parsePayload(bytes, 0, bytes.length);
   }

   public boolean parsePayload(byte[] bytes, int offset, int length) {

      Iterator<Entry<String, MutableByte>> i; // Iterator over the received variables.
      Entry<String, MutableByte> entry; // Entry in the iterator
      MutableByte MB; // Entry value of the key/value pair.
      byte b; // Actual byte we are interested in.
      int j = offset;
      int k;

      if (bytes.length < offset + length)
         return false; // Index array bound exception.

      if ((recvBytes == null) || (recvBytes.length != length)) {
         recvBytes = new byte[length];
      }

      i = receiveVariables.entrySet().iterator();
      k = 0;
      while (i.hasNext() && j < length) {
         b = bytes[j];
         entry = i.next();
         MB = entry.getValue();
         MB.setValue(b);
         recvBytes[k] = b;
         j++;
         k++;
      }

      // Copy the rest of the bytes over.
      int rest = bytes.length - j;
      if (rest > (recvBytes.length - k)) {
         rest = (recvBytes.length - k);
      }
      if (rest > 0) {
         if (log.isTraceEnabled()) {
            log.trace(String.format("Rest of Bytes Copy.bytes.length = %d;  j=%d;  recvBytes.length = %d; k=%d; rest = %d", bytes.length, j, recvBytes.length, k, rest));
         }
         System.arraycopy(bytes, j, recvBytes, k, rest);
      }
      return true; // Made it through without error.
   }

   @Override
   public String toString() {
      String string = "";
      ZWaveCommandClass cls = ZWaveAllCommandClasses.getClass(commandClass);
      String className = "unknown";
      if (cls != null) {
         className = cls.name;
      }
      string += String.format("Command Class %s(%02X);", className, commandClass);
      string += String.format("Command Name %s(%02X);", commandName, commandNumber);
      if (response_id != 0x00) {
         string += String.format("Has Response %02X;", response_id);
      }
      string += " Sent: ";
      string += varsToString(sendVariables);

      string += " Received: ";
      string += varsToString(receiveVariables);

      return string;
   }

   public String varsToString(LinkedHashMap<String, MutableByte> vars) {
      String string = "";
      String name;
      MutableByte b;

      for (Entry<String, MutableByte> entry : vars.entrySet()) {
         name = entry.getKey();
         b = entry.getValue();
         string += String.format("%S - %02X;", name, b.getValue());
      }
      return string;
   }

   public static String scrub(String str) {
      return str.toLowerCase().replace(' ', '_');
   }

}

