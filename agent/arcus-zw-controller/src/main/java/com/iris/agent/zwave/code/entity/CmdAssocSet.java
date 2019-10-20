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
 * Association Set Cmd
 * 
 * Variable Bytes
 * 
 * 0       : CmdClass (0x85)
 * 1       : Cmd (0x01)
 * 2       : Grouping Identifier
 * 3+      Nodes to association (1 byte each)
 * 
 * @author Erik Larson
 *
 */
public class CmdAssocSet extends AbstractZCmd {

   private final int groupingIdentifier;
   private final int[] nodes;
   
   public CmdAssocSet(int groupingIdentifier, int[] nodes) {
      super(CmdClasses.ASSOCIATION.intId(), AssociationCmdClass.CMD_ASSOCIATION_SET);
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
               ByteUtils.ints2Bytes(cmdClass, cmd, groupingIdentifier),
               ByteUtils.ints2Bytes(nodes)
            );
   }

   @Override
   public int byteLength() {
      return 3 + nodes.length;
   }
   
}


