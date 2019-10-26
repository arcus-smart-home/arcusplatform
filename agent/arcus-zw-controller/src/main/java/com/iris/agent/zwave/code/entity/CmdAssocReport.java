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
import com.iris.agent.zwave.code.Decoded;
import com.iris.agent.zwave.code.Decoder;
import com.iris.agent.zwave.code.cmdclass.AssociationCmdClass;
import com.iris.agent.zwave.code.cmdclass.CmdClasses;

/**
 * Association Report
 * 
 * Variable Bytes
 * 
 * 0      : CmdClass (0x85)
 * 1      : Cmd (0x03)
 * 2      : Grouping Identifier
 * 3      : Max Nodes Supported
 * 4      : Reports to Follow
 * 5+     : Node ID's (1 byte each)
 * 
 * 
 * @author Erik Larson
 */
public class CmdAssocReport extends AbstractZCmd {
   public final static CmdAssocReportDecoder DECODER = new CmdAssocReportDecoder();

   private final int groupingIdentifier;
   private final int maxNodesSupported;
   private final int[] nodes;
   
   public CmdAssocReport(int groupingIdentifier, int maxNodesSupported, int[] nodes) {
      super(CmdClasses.ASSOCIATION.intId(), AssociationCmdClass.CMD_ASSOCIATION_REPORT);
      this.groupingIdentifier = groupingIdentifier;
      this.maxNodesSupported = maxNodesSupported;
      this.nodes = Arrays.copyOf(nodes, nodes.length);
   }

   @Override
   public byte[] bytes() {
      return ByteUtils.concat(
               ByteUtils.ints2Bytes(cmdClass, cmd, groupingIdentifier, maxNodesSupported, nodes.length),
               ByteUtils.ints2Bytes(nodes)
            );
   }

   @Override
   public int byteLength() {
      return 5 + nodes.length;
   }
   
   public static class CmdAssocReportDecoder implements Decoder {
      
      @Override
      public Decoded decode(byte[] bytes, int offset) {
         int groupingIdentifier = 0x00FF & bytes[offset + 2];
         int maxNodesSupported = 0x00FF & bytes[offset + 3];
         int reportsToFollow = 0x00FF & bytes[offset + 4];
         int[] nodes = ByteUtils.byteArray2Ints(bytes, offset + 5, reportsToFollow);
         return new Decoded(new CmdAssocReport(groupingIdentifier, maxNodesSupported, nodes));
      }
   }
}

