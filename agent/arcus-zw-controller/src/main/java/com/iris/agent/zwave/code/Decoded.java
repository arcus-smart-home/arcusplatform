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
package com.iris.agent.zwave.code;

/**
 * Encapsulates a block of data from a Z/IP packet.
 * 
 * @author Erik Larson
 */
public class Decoded {
   
   /**
    * Type of data block
    */
   public enum Type {
      // A ZWave Command
      CMD
   }
   
   private final Type type;
   private final int cmdClass;
   private final int cmd;
   private final int length;
   private final ZCmd decoded;
   
   /**
    * Construct from a ZWave command object
    * 
    * @param zcmd representation of a ZWave command
    */
   public Decoded(ZCmd zcmd) {
      this(
            zcmd.cmdClass(),
            zcmd.cmd(),
            zcmd.byteLength(),
            zcmd
            );
   }
   
   /**
    * Constructs an encapsulation of a ZWave command
    * 
    * @param cmdClass the command class of the command
    * @param cmd the command within the command class
    * @param length the byte length of the command
    * @param decoded the representation of the ZWave command
    */
   public Decoded(int cmdClass, int cmd, int length, ZCmd decoded) {
      this.type = Type.CMD;
      this.cmdClass = cmdClass;
      this.cmd = cmd;
      this.length = length;
      this.decoded = decoded;
   }
   
   /**
    * The type of decoded object. Either a ZWave Command, the Z/IP header extensions, or header option.
    * 
    * @return the type
    */
   public Type type() {
      return type;
   }
   
   /**
    * The command class if the decoded object is a ZWave command.
    * 
    * @return command class if this is encapsulating a ZWave command, otherwise 0
    */
   public int cmdClass() {
      return cmdClass;
   }
   
   /**
    * The command of the command class if the decoded object is a ZWave command.
    * 
    * @return command of the command class if this is encapsulating a ZWave command, otherwise 0
    */
   public int cmd() {
      return cmd;
   }
   
   /**
    * Byte length of the decoded object.
    * 
    * @return byte length of the decoded object.
    */
   public int length() {
      return length;
   }
   
   /**
    * Decoded object.
    * 
    * @return the decoded object.
    */
   public ZCmd decoded() {
      return decoded;
   }
   
}


