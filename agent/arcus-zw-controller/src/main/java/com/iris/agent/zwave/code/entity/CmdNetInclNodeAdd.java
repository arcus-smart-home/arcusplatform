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
 * NetworkInclusion NodeAdd Cmd
 * 
 * 6 Bytes
 * 
 * 0     : CmdClass (0x34)
 * 1     : Cmd (0x01)
 * 2     : Sequence No
 * 3     : Reserved (0)
 * 4     : Mode
 *         0x01 Node Add Any 
 *         (Add any type of node to the network and allow Security 0 bootstrapping)
 *         0x05 Node Add Stop
 *         (Stop Add Mode)
 *         0x07 Node Add Any S2
 *         (Allow any type of node and allow Security 0 or 2 bootstrapping)
 * 5     : Tx Options (BitMask)
 *         0x00 (Transmit at normal power level)
 *         0x02 Transmit Option Low Power
 *         0x20 Transmit Option Explore
 * 
 * @author Erik Larson
 */
public class CmdNetInclNodeAdd extends AbstractZCmd {
   public final static int ADD_NODE_ANY = 0x01;
   public final static int ADD_NODE_STOP = 0x05;
   public final static int ADD_NODE_ANY_S2 = 0x07;
   
   public final static int TRANSMIT_OPTION_LOW_POWER = 0x02;
   public final static int TRANSMIT_OPTION_EXPLORE = 0x20;
   
   public final static CmdNetInclNodeAddDecoder DECODER = new CmdNetInclNodeAddDecoder();
   private final static int BYTE_LENGTH = 6;
      
   private final int seqNo;
   private final int mode;
   private final int txOptions;
   
   public CmdNetInclNodeAdd(int seqNo, int mode, int txOptions) {
      super(CmdClasses.NETWORK_INCLUSION.intId(), NetInclusionCmdClass.CMD_NODE_ADD, BYTE_LENGTH);
      this.seqNo = seqNo;
      this.mode = mode;
      this.txOptions = txOptions;
   }
   
   public int getSeqNo() {
      return seqNo;
   }
   
   public int getMode() {
      return mode;
   }
   
   // Should separate into booleans if values are actually needed.
   public int getTxOptions() {
      return txOptions;
   }
   
   @Override
   public byte[] bytes() {
      return ByteUtils.ints2Bytes(
               cmdClass,
               cmd,
               seqNo,
               0,
               mode,
               txOptions
            );
   }
   
   public static class CmdNetInclNodeAddDecoder implements Decoder {
      
      private CmdNetInclNodeAddDecoder() {}
      
      @Override
      public Decoded decode(byte[] bytes, int offset) {
         int seqNo = 0x00FF & bytes[offset + 2];
         int mode = 0x00FF & bytes[offset + 4];
         int txOptions = 0x00FF & bytes[offset + 5];         
         return new Decoded(new CmdNetInclNodeAdd(seqNo, mode, txOptions));
      }
   }
   
}


