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
import com.iris.agent.zwave.code.cmdclass.CmdClasses;
import com.iris.agent.zwave.code.cmdclass.NetMgmtBasicNodeCmdClass;

/**
 * NetworkManagementBasic NodeInformationSend Cmd
 * 
 * 6 Bytes
 * 
 * 0    : CmdClass (0x4d)
 * 1    : Cmd (0x05)
 * 2    : Sequence Number
 * 3    : Reserved (must be 0)
 * 4    : Destination Node Id
 *        Note: This is the node the NIF is being sent to, not the node
 *        sending the NIF.
 * 5    : txOptions (Bitmask)
 *        0x00 - Transmit at normal power with no options.
 *        0x01 - Request ack from destination node and allow routing.
 *        0x02 - Transmit at low power (1/3 of normal range)
 *        0x10 - Send only in direct range, no routing.
 *        0x20 - Resolve new routes via explorer discovery.
 * 
 * @author Erik Larson
 */
public class CmdNetMgmtBasicNodeInfoSend extends AbstractZCmd {
   public final static int TRANSMIT_NORMAL = 0x00;
   public final static int TRANSMIT_OPTION_ACK = 0x01;
   public final static int TRANSMIT_OPTION_LOW_POWER = 0x02;
   public final static int TRANSMIT_OPTION_NO_ROUTE = 0x10;
   public final static int TRANSMIT_OPTION_EXPLORE = 0x20;
   
   private final static int BYTE_LENGTH = 6;
   
   private final int seqNum;
   private final int destNodeId;
   private final int txOptions;
   
   public CmdNetMgmtBasicNodeInfoSend(int seqNum, int destNodeId, int txOptions) {
      super(CmdClasses.NETWORK_MGMT_BASIC_NODE.intId(), NetMgmtBasicNodeCmdClass.CMD_NODE_INFO_SEND, BYTE_LENGTH);
      this.seqNum = seqNum;
      this.destNodeId = destNodeId;
      this.txOptions = txOptions;      
   }

   public int getSeqNum() {
      return seqNum;
   }

   public int getDestNodeId() {
      return destNodeId;
   }

   public int getTxOptions() {
      return txOptions;
   }
   
   @Override
   public byte[] bytes() {
      return ByteUtils.ints2Bytes(
               cmdClass,
               cmd,
               seqNum,
               0,
               destNodeId,
               txOptions
            );
   }
}


