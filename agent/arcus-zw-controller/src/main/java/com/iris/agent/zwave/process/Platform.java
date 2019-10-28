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
package com.iris.agent.zwave.process;

import com.iris.agent.zwave.code.ZCmd;
import com.iris.agent.zwave.code.ZWCmdHandler;
import com.iris.agent.zwave.code.cmdclass.CmdClasses;
import com.iris.agent.zwave.code.entity.CmdRawBytes;
import com.iris.agent.zwave.events.ZWEventDispatcher;
import com.iris.agent.zwave.events.ZWNodeCommandEvent;

public class Platform implements ZWCmdHandler {
   public final static Platform INSTANCE = new Platform();
   
   private Platform() { };

   @Override
   public boolean processCmd(int nodeId, ZCmd cmd) {
      // TODO: Get controller node id from someplace rather than hardcoding.
      if (nodeId > 1 && cmd instanceof CmdRawBytes) {
         int cmdClassId = cmd.cmdClass();
         if (!CmdClasses.isNetworkInclusion(cmdClassId)
            && !CmdClasses.isNetworkMgmtProxy(cmdClassId)) {
               
               ZWNodeCommandEvent event = new ZWNodeCommandEvent(nodeId, (CmdRawBytes)cmd);
               ZWEventDispatcher.INSTANCE.dispatch(event);
               return true;
            
         }
      }
      return false;
   }
   
   
}

