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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.iris.agent.util.BitMasks;
import com.iris.agent.util.ByteUtils;
import com.iris.agent.zwave.code.Decoded;
import com.iris.agent.zwave.code.Decoder;
import com.iris.agent.zwave.code.cmdclass.CmdClasses;
import com.iris.agent.zwave.code.cmdclass.NetMgmtProxyCmdClass;

/**
 * NetworkManagementProxy NodeListReport Cmd
 * 
 * 34 Bytes
 * 
 * 0      : CmdClass (0x52)
 * 1      : Cmd (0x02)
 * 2      : Sequence No
 * 3      : Status
 *          0x00 - List data is the latest updated node list
 *          0x01 - List data may be outdated
 * 4      : Node List Controller ID
 * 5  :7  : Node 8 (1 = Present, 0 = Absent)
 *    :6  : Node 7 ...
 *    :5  : Node 6 ...
 *    :4  : Node 5 ...
 *    :3  : Node 4 ...
 *    :2  : Node 3 ...
 *    :1  : Node 2 ...
 *    :0  : Node 1 ...
 *    
 * ...
 * 
 * 33 :7  : Node 232
 *    :6  : Node 231
 *    :5  : Node 230
 *    :4  : Node 229
 *    :3  : Node 228
 *    :2  : Node 227
 *    :1  : Node 226
 *    :0  : Node 225
 * 
 * @author Erik Larson
 */
public class CmdNetMgmtProxyNodeListReport extends AbstractZCmd {
   public final static CmdNetMgmtProxyNodeListReportDecoder DECODER = new CmdNetMgmtProxyNodeListReportDecoder();
   private final static int BYTE_LENGTH = 34;
   
   private final int seqNo;
   private final int status;
   private final int controllerId;
   private final Set<Integer> nodes;
   
   public CmdNetMgmtProxyNodeListReport(int seqNo, int status, int controllerId, Set<Integer> nodes) {
      super(CmdClasses.NETWORK_MGMT_PROXY.intId(), NetMgmtProxyCmdClass.CMD_NODE_LIST_REPORT, BYTE_LENGTH);
      this.nodes = Collections.unmodifiableSet(nodes);
      this.seqNo = seqNo;
      this.status = status;
      this.controllerId = controllerId;
   }
   
   public int getSequenceNumber() {
      return seqNo;
   }

   public boolean isLatestNodeList() {
      return status == 0;
   }
   
   public int getControllerId() {
      return controllerId;
   }
   
   public Set<Integer> getNodes() {
      return nodes;
   }

   @Override
   public byte[] bytes() {
      byte[] header = ByteUtils.ints2Bytes(
                                 cmdClass,
                                 cmd,
                                 seqNo,
                                 status,
                                 controllerId
                              );
      return ByteUtils.concat(header, nodes2bytes());
   }
   
   private byte[] nodes2bytes() {
      byte[] bytes = new byte[29];
      java.util.Arrays.fill(bytes, (byte)0);
      for (int nodeNumber : nodes) {
         // The node list is 1-based so we need to subtract 1 here.
         int i = nodeNumber - 1;
         int index = i / 8;
         bytes[index] = ByteUtils.setBit(bytes[index], i % 8);
      }
      return bytes;
   }

   public static class CmdNetMgmtProxyNodeListReportDecoder implements Decoder {
      
      private CmdNetMgmtProxyNodeListReportDecoder() { }

      @Override
      public Decoded decode(byte[] bytes, int offset) {
         int seqNo = 0x00FF & bytes[offset + 2];
         int status = 0x00FF & bytes[offset + 3];
         int controllerId = 0x00FF & bytes[offset + 4];
         Set<Integer> nodes = bytes2Nodes(bytes, offset + 5);
         CmdNetMgmtProxyNodeListReport nodeListReport = new CmdNetMgmtProxyNodeListReport(seqNo, status, controllerId, nodes);
         return new Decoded(CmdClasses.NETWORK_MGMT_PROXY.intId(), NetMgmtProxyCmdClass.CMD_NODE_LIST_REPORT, nodeListReport.byteLength(), nodeListReport);
      }
      
      private Set<Integer> bytes2Nodes(byte[] bytes, int offset) {
         Set<Integer> nodes = new HashSet<>();
         int baseNumber = 0;
         for (int i = 0; i < 29; i++) {
            if (ByteUtils.isSet(BitMasks.BIT_0, bytes[offset + i])) {
               nodes.add(baseNumber + 1);
            }
            if (ByteUtils.isSet(BitMasks.BIT_1, bytes[offset + i])) {
               nodes.add(baseNumber + 2);
            }
            if (ByteUtils.isSet(BitMasks.BIT_2, bytes[offset + i])) {
               nodes.add(baseNumber + 3);
            }
            if (ByteUtils.isSet(BitMasks.BIT_3, bytes[offset + i])) {
               nodes.add(baseNumber + 4);
            }
            if (ByteUtils.isSet(BitMasks.BIT_4, bytes[offset + i])) {
               nodes.add(baseNumber + 5);
            }
            if (ByteUtils.isSet(BitMasks.BIT_5, bytes[offset + i])) {
               nodes.add(baseNumber + 6);
            }
            if (ByteUtils.isSet(BitMasks.BIT_6, bytes[offset + i])) {
               nodes.add(baseNumber + 7);
            }
            if (ByteUtils.isSet(BitMasks.BIT_7, bytes[offset + i])) {
               nodes.add(baseNumber + 8);
            }
            baseNumber += 8;
         }
         return nodes;
      }
   }
   
}


