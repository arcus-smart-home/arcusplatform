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
 * NetworkManagementBasic LearnModeSet Cmd
 * 
 * 5 Bytes
 * 
 * 0    : CmdClass (0x4d)
 * 1    : Cmd (0x01)
 * 2    : Sequence Number
 * 3    : Reserved (Must be 0)
 * 4    : Mode
 *          0x00 ZW_SET_LEARN_MODE_DISABLE - Stops learn mode.
 *          0x01 ZW_SET_LEARN_MODE_CLASSIC - Allows inclusion in direct range
 *          0x02 ZW_SET_LEARN_MODE_NWI - Allows routed inclusion
 * 
 * @author Erik Larson
 */
public class CmdNetMgmtBasicLearnModeSet extends AbstractZCmd {
   public final static int MODE_DISABLE = 0x00;
   public final static int MODE_CLASSIC = 0x01;
   public final static int MODE_NWI = 0x02;
   
   private final static int BYTE_LENGTH = 5;
   
   private final int seqNum;
   private final int mode;
   
   public CmdNetMgmtBasicLearnModeSet(int seqNum, int mode) {
      super(CmdClasses.NETWORK_MGMT_BASIC_NODE.intId(), NetMgmtBasicNodeCmdClass.CMD_LEARN_MODE_SET, BYTE_LENGTH);
      this.seqNum = seqNum;
      this.mode = mode;
   }
   
   public int getSeqNum() {
      return seqNum;
   }
   
   public int getMode() {
      return mode;
   }

   @Override
   public byte[] bytes() {
      return ByteUtils.ints2Bytes(
               cmdClass,
               cmd,
               seqNum,
               0,
               mode
            );
            
   }
}


