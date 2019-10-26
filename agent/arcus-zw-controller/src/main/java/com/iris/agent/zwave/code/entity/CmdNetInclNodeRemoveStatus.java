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
 * 0     : CmdClass(0x34)
 * 1     : Cmd (0x04)
 * 2     : Sequence No
 * 3     : Status
 *       0x06 - Remove Node Done
 *       0x07 - Remove Node Status Failed
 * 4     : NodeId - Id of the node being removed.
 * 
 * @author Erik Larson
 */
public class CmdNetInclNodeRemoveStatus extends AbstractZCmd {
   public final static int REMOVE_NODE_STATUS_DONE = 0x06;
   public final static int REMOVE_NODE_STATUS_FAILED = 0x07;
   private final static int BYTE_LENGTH = 5;
   
   public final static CmdNetInclNodeRemoveStatusDecoder DECODER = new CmdNetInclNodeRemoveStatusDecoder();
   
   private final int seqNo;
   private final int status;
   private final int nodeId;
   
   public CmdNetInclNodeRemoveStatus(int seqNo, int status, int nodeId) {
      super(CmdClasses.NETWORK_INCLUSION.intId(), NetInclusionCmdClass.CMD_NODE_REMOVE_STATUS, BYTE_LENGTH);
      this.seqNo = seqNo;
      this.status = status;
      this.nodeId = nodeId;
   }
   
   public boolean isSuccess() {
      return status == REMOVE_NODE_STATUS_DONE;
   }
   
   public int getSeqNo() {
      return seqNo;
   }
   
   public int getStatus() {
      return status;
   }
   
   public int getNodeId() {
      return nodeId;
   }

   @Override
   public byte[] bytes() {
      return ByteUtils.ints2Bytes(
            cmdClass,
            cmd,
            seqNo,
            status,
            nodeId
         );
   }
   
   public static class CmdNetInclNodeRemoveStatusDecoder implements Decoder {
      
      private CmdNetInclNodeRemoveStatusDecoder() {}

      @Override
      public Decoded decode(byte[] bytes, int offset) {
        int seqNo = 0x00FF & bytes[offset + 2];
        int status = 0x00FF & bytes[offset + 3];
        int nodeId = 0x00FF & bytes[offset + 4];
        return new Decoded(new CmdNetInclNodeRemoveStatus(seqNo, status, nodeId));
      }
      
   }
}


