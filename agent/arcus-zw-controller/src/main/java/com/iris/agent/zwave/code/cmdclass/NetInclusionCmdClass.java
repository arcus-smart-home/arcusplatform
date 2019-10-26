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
package com.iris.agent.zwave.code.cmdclass;

import com.iris.agent.zwave.code.anno.Id;
import com.iris.agent.zwave.code.anno.Name;
import com.iris.agent.zwave.code.cmds.NetInclNodeAddCmd;
import com.iris.agent.zwave.code.cmds.NetInclNodeAddStatusCmd;
import com.iris.agent.zwave.code.cmds.NetInclNodeRemoveCmd;
import com.iris.agent.zwave.code.cmds.NetInclNodeRemoveStatusCmd;

@Id(0x34)
@Name("Network Inclusion")
public class NetInclusionCmdClass extends AbstractCmdClass {
   public static final int CMD_NODE_ADD = 0x01;
   public static final int CMD_NODE_ADD_STATUS = 0x02;
   public static final int CMD_NODE_REMOVE = 0x03;
   public static final int CMD_NODE_REMOVE_STATUS = 0x04;
   
   public static final int CMD_FAILED_NODE_REMOVE = 0x07;
   public static final int CMD_FAILED_NODE_REMOVE_STATUS = 0x08;
   public static final int CMD_FAILED_NODE_REPLACE = 0x09;
   public static final int CMD_FAILED_NODE_REPLACE_STATUS = 0x0a;
   
   public static final int CMD_NODE_NEIGHBOR_UPDATE_REQUEST = 0x0b;
   public static final int CMD_NODE_NEIGHBOR_UPDATE_STATUS = 0x0c;
   public static final int CMD_RETURN_ROUTE_ASSIGN = 0x0d;
   public static final int CMD_RETURN_ROUTE_ASSIGN_COMPLETE = 0x0e;
   public static final int CMD_RETURN_ROUTE_DELETE = 0x0f;
   public static final int CMD_RETURN_ROUTE_DELETE_COMPLETE = 0x10;
   
   public static final int CMD_NODE_ADD_KEYS_REPORT = 0x11;
   public static final int CMD_NODE_ADD_KEYS_SET = 0x12;
   public static final int CMD_NODE_ADD_DSK_REPORT = 0x13;
   public static final int CMD_NODE_ADD_DSK_SET = 0x14;
   
   public static final int CMD_INCLUDED_NIF_REPORT = 0x19;
   
   public static final int CMD_SMART_START_JOIN_STARTED = 0x15;
   
   NetInclusionCmdClass() {
      super(
            new NetInclNodeAddCmd(),
            new NetInclNodeAddStatusCmd(),
            new NetInclNodeRemoveCmd(),
            new NetInclNodeRemoveStatusCmd()
            );
   }
}

