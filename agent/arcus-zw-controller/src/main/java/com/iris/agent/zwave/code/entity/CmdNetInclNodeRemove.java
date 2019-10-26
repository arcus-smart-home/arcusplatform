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
import com.iris.agent.zwave.code.cmdclass.NetInclusionCmdClass;

/**
 * NetworkInclusion NodeRemove Cmd
 * 
 * 5 Bytes
 * 
 * 0     : CmdClass (0x34)
 * 1     : Cmd (0x03)
 * 2     : Sequence No
 * 3     : Reserved (0)
 * 4     : Mode
 *       0x01 Remove Node Any
 *       0x05 Remove Node Stop
 * 
 * 
 * @author Erik Larson
 */
public class CmdNetInclNodeRemove extends AbstractZCmd{
   public final static int REMOVE_NODE_ANY = 0x01;
   public final static int REMOVE_NODE_STOP = 0x05;
   private final static int BYTE_LENGTH = 5;
   
   private final int seqNo;
   private final int mode;
   
   public CmdNetInclNodeRemove(int seqNo, int mode) {
      super(CmdClasses.NETWORK_INCLUSION.intId(), NetInclusionCmdClass.CMD_NODE_REMOVE, BYTE_LENGTH);
      this.seqNo = seqNo;
      this.mode = mode;
   }
   
   public int getSeqNo() {
      return seqNo;
   }
   
   public int getMode() {
      return mode;
   }

   @Override
   public byte[] bytes() {
      return ByteUtils.ints2Bytes(
               cmdClass,
               cmd,
               seqNo,
               0,
               mode
            );
   }

   public static class CmdNetInclNodeRemoveDecoder implements Decoder {
      
      private CmdNetInclNodeRemoveDecoder() {}
      
      @Override
      public Decoded decode(byte[] bytes, int offset) {
         int seqNo = 0x00FF & bytes[offset + 2];
         int mode = 0x00FF & bytes[offset + 4];
         return new Decoded(new CmdNetInclNodeRemove(seqNo, mode));
      }
   }
}


