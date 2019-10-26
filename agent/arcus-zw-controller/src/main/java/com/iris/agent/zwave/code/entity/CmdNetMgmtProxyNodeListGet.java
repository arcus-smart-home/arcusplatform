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
import com.iris.agent.zwave.code.cmdclass.NetMgmtProxyCmdClass;

/**
 * NetworkManagementProxy NodeListGet Cmd
 * 
 * 3 Bytes
 * 
 * 0    : CmdClass (0x52)
 * 1    : Cmd (0x01)
 * 2    : Sequence No
 * 
 * @author Erik Larson
 */
public class CmdNetMgmtProxyNodeListGet extends AbstractZCmd{
   public final static CmdNetMgmtProxyNodeListGetDecoder DECODER = new CmdNetMgmtProxyNodeListGetDecoder();
   private final static int BYTE_LENGTH = 3;

   private final int seqNo;
   
   public CmdNetMgmtProxyNodeListGet(int seqNo) {
      super(CmdClasses.NETWORK_MGMT_PROXY.intId(), NetMgmtProxyCmdClass.CMD_NODE_LIST_GET, BYTE_LENGTH);
      this.seqNo = seqNo;
   }
   
   public int getSeqNo() {
      return seqNo;
   }

   @Override
   public byte[] bytes() {
     return ByteUtils.ints2Bytes(
              cmdClass,
              cmd,
              seqNo
           );
   }
   
   public static class CmdNetMgmtProxyNodeListGetDecoder implements Decoder {
      
      private CmdNetMgmtProxyNodeListGetDecoder() { }

      @Override
      public Decoded decode(byte[] bytes, int offset) {
         int seqNo = 0x00FF & bytes[offset + 2];
         return new Decoded(new CmdNetMgmtProxyNodeListGet(seqNo));
      }
   }
   
}


