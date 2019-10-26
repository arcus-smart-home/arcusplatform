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

import java.util.Arrays;

import com.iris.agent.util.ByteUtils;
import com.iris.agent.zwave.code.cmdclass.AssociationCmdClass;
import com.iris.agent.zwave.code.cmdclass.CmdClasses;

/**
 * Association Remove Cmd
 * 
 * Variable Bytes
 * 
 * 0     : CmdClass (0x85)
 * 1     : Cmd (0x04)
 * 2     : Grouping Id
 * 3+    : List of node ids to remove
 * 
 * @author Erik Larson
 */
public class CmdAssocRemove extends AbstractZCmd {
   private final int groupingIdentifier;
   private final int[] nodes;
   
   public CmdAssocRemove(int groupingIdentifier, int... nodes) {
      super(CmdClasses.ASSOCIATION.intId(), AssociationCmdClass.CMD_ASSOCIATION_REMOVE);
      this.groupingIdentifier = groupingIdentifier;
      this.nodes = Arrays.copyOf(nodes, nodes.length);
   }
   
   public int getGroupingIdentifier() {
      return groupingIdentifier;
   }

   public int[] getNodes() {
      return Arrays.copyOf(nodes, nodes.length);
   }

   @Override
   public byte[] bytes() {
      return ByteUtils.concat(
               ByteUtils.ints2Bytes(cmdClass, cmd),
               ByteUtils.ints2Bytes(nodes)
            );
            
   }
}

