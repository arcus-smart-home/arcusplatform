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

import com.iris.agent.util.ByteUtils;
import com.iris.agent.zwave.code.Decoded;
import com.iris.agent.zwave.code.Decoder;
import com.iris.agent.zwave.code.cmdclass.BasicCmdClass;
import com.iris.agent.zwave.code.cmdclass.CmdClasses;

/**
 * Basic Report (v1)
 * 
 * 3 Bytes
 * 
 * 0    : CmdClass (0x20)
 * 1    : Cmd (0x03)
 * 2    : Value
 * 
 * @author Erik Larson
 */
public class CmdBasicReport extends AbstractZCmd {
   public final static CmdBasicReportDecoder DECODER = new CmdBasicReportDecoder(); 
   
   private final static int BYTE_LENGTH = 3;
   
   private final int value;
   
   public CmdBasicReport(int value) {
      super(CmdClasses.BASIC.intId(), BasicCmdClass.CMD_BASIC_REPORT, BYTE_LENGTH);
      this.value = value;
   }
   
   public int getValue() {
      return value;
   }
   
   @Override 
   public byte[] bytes() {
      return ByteUtils.ints2Bytes(cmdClass, cmd, value);
   }
   
   public static class CmdBasicReportDecoder implements Decoder {

      @Override
      public Decoded decode(byte[] bytes, int offset) {
         int value = 0x00FF & bytes[offset + 2];
         return new Decoded(new CmdBasicReport(value));
      }
      
   }
}


