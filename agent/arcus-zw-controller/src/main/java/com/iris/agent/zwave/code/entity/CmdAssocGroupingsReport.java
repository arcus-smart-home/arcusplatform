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
import com.iris.agent.zwave.code.cmdclass.AssociationCmdClass;
import com.iris.agent.zwave.code.cmdclass.CmdClasses;

/**
 * Association Groupings Get Cmd
 * 
 * 3 Bytes
 * 
 * 0       : CmdClass (0x85)
 * 1       : Cmd (0x05)
 * 2       : Supported Groupings
 * 
 * @author Erik Larson
 *
 */
public class CmdAssocGroupingsReport extends AbstractZCmd {
   public final static CmdAssocGroupingsReportDecoder DECODER = new CmdAssocGroupingsReportDecoder();
   private final static int BYTE_LENGTH = 3;

   private final int supportedGroupings;
   
   public CmdAssocGroupingsReport(int supportedGroupings) {
      super(CmdClasses.ASSOCIATION.intId(), AssociationCmdClass.CMD_ASSOCIATION_GROUPINGS_REPORT, BYTE_LENGTH);
      this.supportedGroupings = supportedGroupings;
   }
   
   public int getSupportedGroupings() {
      return supportedGroupings;
   }
   
   @Override
   public byte[] bytes() {
      return ByteUtils.ints2Bytes(cmdClass, cmd, supportedGroupings);
   }
   
   public static class CmdAssocGroupingsReportDecoder implements Decoder {

      @Override
      public Decoded decode(byte[] bytes, int offset) {
         int supportedGroupings = 0x00FF & bytes[offset + 2];
         return new Decoded(new CmdAssocGroupingsReport(supportedGroupings));
      }
      
   }
}

