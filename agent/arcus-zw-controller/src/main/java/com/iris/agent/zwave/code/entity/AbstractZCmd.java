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
package com.iris.agent.zwave.code.entity;

import java.util.Arrays;

import com.iris.agent.zwave.code.ZCmd;

public abstract class AbstractZCmd implements ZCmd {
   
   protected final int baseLength;
   protected final int cmdClass;
   protected final int cmd;
   
   protected AbstractZCmd(int cmdClass, int cmd) {
      this(cmdClass, cmd, 0);
   }
   
   protected AbstractZCmd(int cmdClass, int cmd, int baseLength) {
      this.baseLength = baseLength;
      this.cmdClass = cmdClass;
      this.cmd = cmd;
   }

   @Override
   public int byteLength() {
      return baseLength;
   }

   @Override
   public int cmdClass() {
      return cmdClass;
   }

   @Override
   public int cmd() {
      return cmd;
   }

   @Override
   public byte[] payload() {
      final byte[] bytes = bytes();
      return Arrays.copyOfRange(bytes, 2, bytes.length);      
   }

}


