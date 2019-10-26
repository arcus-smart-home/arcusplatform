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

import com.iris.agent.zwave.code.ZWSequence;
import com.iris.agent.zwave.code.entity.CmdNetInclNodeAdd;
import com.iris.agent.zwave.code.entity.CmdNetInclNodeRemove;

public class NetInclusionBuilders {

   public static CmdNetInclNodeAdd buildStartNodeAdd() {
      return buildStartNodeAdd(ZWSequence.next());
   }
   
   public static CmdNetInclNodeAdd buildStartNodeAdd(int seq) {
      // Not sure why txOptions should be 1, but that appears to be the value to use.
      return new CmdNetInclNodeAdd(seq, CmdNetInclNodeAdd.ADD_NODE_ANY_S2, 1);
   }
   
   public static CmdNetInclNodeAdd buildStopNodeAdd() {
      return buildStopNodeAdd(ZWSequence.next());
   }
   
   public static CmdNetInclNodeAdd buildStopNodeAdd(int seq) {
      return new CmdNetInclNodeAdd(seq, CmdNetInclNodeAdd.ADD_NODE_STOP, 0);
   }
   
   public static CmdNetInclNodeRemove buildStartNodeRemove() {
      return buildStartNodeRemove(ZWSequence.next());
   }
   
   public static CmdNetInclNodeRemove buildStartNodeRemove(int seq) {
      return new CmdNetInclNodeRemove(seq, CmdNetInclNodeRemove.REMOVE_NODE_ANY);
   }
   
   public static CmdNetInclNodeRemove buildStopNodeRemove() {
      return buildStopNodeRemove(ZWSequence.next());
   }
   
   public static CmdNetInclNodeRemove buildStopNodeRemove(int seq) {
      return new CmdNetInclNodeRemove(seq, CmdNetInclNodeRemove.REMOVE_NODE_STOP);
   }
}


