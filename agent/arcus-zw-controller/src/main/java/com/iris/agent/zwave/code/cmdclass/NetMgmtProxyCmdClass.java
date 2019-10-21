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
import com.iris.agent.zwave.code.cmds.NetMgmtProxyNodeListCachedGet;
import com.iris.agent.zwave.code.cmds.NetMgmtProxyNodeListCachedReportCmd;
import com.iris.agent.zwave.code.cmds.NetMgmtProxyNodeListGetCmd;
import com.iris.agent.zwave.code.cmds.NetMgmtProxyNodeListReportCmd;

@Id(0x52)
@Name("Network Management Proxy")
public class NetMgmtProxyCmdClass extends AbstractCmdClass {
   public static final int CMD_NODE_LIST_GET = 0x01;
   public static final int CMD_NODE_LIST_REPORT = 0x02;
   public static final int CMD_NODE_CACHED_GET = 0x03;
   public static final int CMD_NODE_CACHED_REPORT = 0x04;
   
   NetMgmtProxyCmdClass() {
      super(
            new NetMgmtProxyNodeListGetCmd(),
            new NetMgmtProxyNodeListReportCmd(),
            new NetMgmtProxyNodeListCachedGet(),
            new NetMgmtProxyNodeListCachedReportCmd()
            );
   }
}

