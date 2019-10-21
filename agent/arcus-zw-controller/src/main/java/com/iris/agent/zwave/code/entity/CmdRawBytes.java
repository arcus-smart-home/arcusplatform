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

/**
 * Represents a Z-Wave Command as a bag of bytes.
 * 
 * @author Erik Larson
 */
public class CmdRawBytes extends AbstractZCmd {
   private final byte[] bytes;
   
   public CmdRawBytes(byte[] bytes) {
      super(0x00FF & bytes[0], 0x00FF & bytes[1]);
      this.bytes = Arrays.copyOf(bytes, bytes.length);
   }
   
   public CmdRawBytes(byte[] bytes, int offset) {
      super(0x00FF & bytes[offset], 0x00FF & bytes[offset + 1]);
      this.bytes = Arrays.copyOfRange(bytes, offset, bytes.length);
   }

   @Override
   public int byteLength() {
      return bytes.length;
   }

   //TODO: Practice-wise, it would be better to return a copy, but that adds overhead.
   @Override
   public byte[] bytes() {
      return bytes;
   }

   @Override
   public int cmdClass() {
      return 0x00FF & bytes[0];
   }

   @Override
   public int cmd() {
      return 0x00FF & bytes[1];
   }

}

