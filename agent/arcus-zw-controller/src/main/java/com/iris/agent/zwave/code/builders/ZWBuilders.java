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
package com.iris.agent.zwave.code.builders;

import com.iris.agent.util.ByteUtils;
import com.iris.agent.zwave.ZWConfig;
import com.iris.agent.zwave.code.ZWSequence;
import com.iris.agent.zwave.code.entity.CmdBasicGet;
import com.iris.agent.zwave.code.entity.CmdManSpecificGet;
import com.iris.agent.zwave.code.entity.CmdNetMgmtBasicLearnModeSet;
import com.iris.agent.zwave.code.entity.CmdNetMgmtBasicNodeInfoSend;
import com.iris.agent.zwave.code.entity.CmdRawBytes;
import com.iris.protocol.zwave.Protocol;

/**
 * This is a collection of helper methods to simplify the creation of ZWave
 * commands.
 * 
 * @author Erik Larson
 */
public class ZWBuilders {
   
   public static NetMgmtProxyNodeListGetBuilder buildGetNodeList() {
      return new NetMgmtProxyNodeListGetBuilder();
   }
   
   public static CmdManSpecificGet getManufacturerSpecificGet() {
      return CmdManSpecificGet.COMMAND_MANUFACTURER_GET;
   }
   
   public static CmdBasicGet getBasicGet() {
      return CmdBasicGet.CMD_BASIC_GET;
   }
   
   /**
    * Creates a command to tell a node to either enter or leave learn mode. The
    * available modes are: Disable, Classic Learn Mode, and NWI Learn Mode.
    * 
    * For more details see @see com.iris.agent.zip.code.entity.CmdNetMgmtBasicLearnModeSet
    * 
    * 
    * @param mode - The learn mode to set
    * @return CmdNetMgmtBasicLearnModeSet instance.
    */
   public static CmdNetMgmtBasicLearnModeSet createLearnModeSet(int mode) {
      return new CmdNetMgmtBasicLearnModeSet(ZWSequence.next(), mode);
   }
   
   /**
    * Creates a command to tell a node to send a NIF (Node Information Frame).
    * The options set are to broadcast the NIF only to nodes within range and
    * not route to nodes beyond that. 
    * 
    * @return CmdNetMgmtBasicNodeInfoSend instance.
    */
   public static CmdNetMgmtBasicNodeInfoSend getNodeInfoSend() {
      return new CmdNetMgmtBasicNodeInfoSend(ZWSequence.next(), 
            ZWConfig.BROADCAST_NODE_ID, 
            CmdNetMgmtBasicNodeInfoSend.TRANSMIT_OPTION_NO_ROUTE);
            
   }
   
   public static CmdRawBytes buildRawBytesCmd(Protocol.Command protCmd) {
      int cmdClass = protCmd.getCommandClassId();
      int cmd = protCmd.getCommandId();
      byte[] base = ByteUtils.ints2Bytes(cmdClass, cmd);
      if (protCmd.getLength() > 0) {
         return new CmdRawBytes(ByteUtils.concat(base, protCmd.getPayload()));
      }
      else {
         return new CmdRawBytes(base);
      }
   }
}


