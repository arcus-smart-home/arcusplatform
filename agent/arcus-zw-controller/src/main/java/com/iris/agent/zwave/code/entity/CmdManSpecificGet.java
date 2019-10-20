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
import com.iris.agent.zwave.code.cmdclass.CmdClasses;
import com.iris.agent.zwave.code.cmdclass.ManufacturerSpecificCmdClass;

/**
 * Manufacturer Specific Get Cmd
 * v1
 * 
 * 2 Bytes
 * 
 * 0     : CmdClass (0x72)
 * 1     : Cmd (0x04)
 * 
 * @author Erik Larson
 */
public class CmdManSpecificGet extends AbstractZCmd {
   public final static CmdManSpecificGetDecoder DECODER = new CmdManSpecificGetDecoder();
   public final static CmdManSpecificGet COMMAND_MANUFACTURER_GET = new CmdManSpecificGet(); 
   private final static int BYTE_LENGTH = 2;
      
   public CmdManSpecificGet() {
      super(CmdClasses.MANUFACTURER_SPECIFIC.intId(), ManufacturerSpecificCmdClass.CMD_MANUFACTURER_SPECIFIC_GET, BYTE_LENGTH);
   }

   @Override
   public byte[] bytes() {
      return ByteUtils.ints2Bytes(cmdClass, cmd);
   }
   
   public static class CmdManSpecificGetDecoder implements Decoder {

      @Override
      public Decoded decode(byte[] bytes, int offset) {
         return new Decoded(new CmdManSpecificGet());
      }
      
   }
}


