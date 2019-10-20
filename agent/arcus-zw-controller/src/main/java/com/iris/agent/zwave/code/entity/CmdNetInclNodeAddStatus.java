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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.iris.agent.util.BitMasks;
import com.iris.agent.util.ByteUtils;
import com.iris.agent.zwave.code.Decoded;
import com.iris.agent.zwave.code.Decoder;
import com.iris.agent.zwave.code.cmdclass.CmdClasses;
import com.iris.agent.zwave.code.cmdclass.NetInclusionCmdClass;

/**
 * NetworkInclusion NodeAddStatus Cmd
 * 
 * Variable Bytes
 * --------------
 * Base Bytes
 * 0      : CmdClass (0x34)
 * 1      : Cmd (0x02)
 * 2      : Sequence No
 * 3      : Status
 * 4      : Reserved
 * 5      : New Node ID
 * 
 * Node Info Bytes
 * ---------------
 * 6      : Node Info Length
 *          Length of the following Node Info
 * 7      : Protocol Info
 * 8      : Protocol Info
 * 9      : Basic Device Class
 * 10     : Generic Device Class
 * 11     : Specific Device Class
 * 
 * 12 - n : Command Classes (n = 6 + node info length)
 * 
 * n+1    : Granted Keys (bitmask)
 *     :0   Indicates the Unauthenticated Security Class Key
 *     :1   Indicates the Authenticated Security Class Key
 *     :2   Indicates the Access Control Security Class Key
 *     :7   Indicates the Security 0 Network Key
 *     
 * n+2    : KEX Fail Type (S2 bootstrapping failed)
 *        0x00 - Success
 *        0x01 - Key failure - no match between requested/granted keys
 *        0x02 - Scheme failure - joining node specified an invalid scheme
 *        0x03 - Curve failure - joining node specified an invalid curve
 *        0x05 - Decrypt failure - node failed to decrypt received frame
 *        0x06 - Cancel - user has cancelled the S2 bootstrapping
 *        0x07 - Auth - the echo KEX frame did not match the earlier exchanged frame
 *        0x08 - Get - The joining node has requested a key which was not granted by the including node
 *        0x09 - Verify - including node failed to decrypt and hence verify the received frame
 *        0x0A - Report - The including node has transmitted a frame containing a different key
 * 
 * @author Erik Larson
 */
public class CmdNetInclNodeAddStatus extends AbstractZCmd {
   public final static int ADD_NODE_STATUS_DONE = 0x06;
   public final static int ADD_NODE_STATUS_FAILED = 0x07;
   public final static int ADD_NODE_STATUS_SECURITY_FAILED = 0x09;
   
   public final static CmdNetInclNodeAddStatusDecoder DECODER = new CmdNetInclNodeAddStatusDecoder();
      
   private final int seqNo;
   private final int status;
   private final int nodeId;
   private final int protocolInfo0;
   private final int protocolInfo1;
   private final int baseDeviceClass;
   private final int genericDeviceClass;
   private final int specificDeviceClass;
   private final int grantedKeys;
   private final int kexFailure;
   private final List<Integer> commandClasses;
   private final List<Integer> dsks;
   
   private CmdNetInclNodeAddStatus(int seqNo, 
         int status, 
         int nodeId,
         int protocolInfo0,
         int protocolInfo1,
         int baseDeviceClass,
         int genericDeviceClass,
         int specificDeviceClass,
         int grantedKeys,
         int kexFailure,
         List<Integer> commandClasses,
         List<Integer> dsks) {
      super(CmdClasses.NETWORK_INCLUSION.intId(), NetInclusionCmdClass.CMD_NODE_ADD_STATUS);
      this.seqNo = seqNo;
      this.status = status;
      this.nodeId = nodeId;
      this.protocolInfo0 = protocolInfo0;
      this.protocolInfo1 = protocolInfo1;
      this.baseDeviceClass = baseDeviceClass;
      this.genericDeviceClass = genericDeviceClass;
      this.specificDeviceClass = specificDeviceClass;
      this.grantedKeys = grantedKeys;
      this.kexFailure = kexFailure;
      this.commandClasses = Collections.unmodifiableList(commandClasses);
      this.dsks = Collections.unmodifiableList(dsks);
   }
   
   public int getSeqNo() {
      return seqNo;
   }
   
   public boolean isSuccess() {
      return status != 7 && status != 9;
   }
      
   public int getStatus() {
      return status;
   }
   
   public boolean isNodeAddedSuccessfully() {
      return status == ADD_NODE_STATUS_DONE;
   }
   
   public boolean isSecurityFailed() {
      return status == ADD_NODE_STATUS_SECURITY_FAILED;
   }

   public int getNodeId() {
      return nodeId;
   }

   public int getProtocolInfo0() {
      return protocolInfo0;
   }

   public int getProtocolInfo1() {
      return protocolInfo1;
   }

   public int getBaseDeviceClass() {
      return baseDeviceClass;
   }

   public int getGenericDeviceClass() {
      return genericDeviceClass;
   }

   public int getSpecificDeviceClass() {
      return specificDeviceClass;
   }

   public int getGrantedKeys() {
      return grantedKeys;
   }

   public int getKexFailure() {
      return kexFailure;
   }

   public List<Integer> getCommandClasses() {
      return commandClasses;
   }
   
   public List<Integer> getDsks() {
      return dsks;
   }

   @Override
   public int byteLength() {
      if (isSuccess()) {
         // Fixed length of 6 bytes at start of record.
         // Followed by the variable number of node info bytes
         // Followed 3 bytes for Granted Keys, KEX Fail Type, and DSK Length
         // Followed by variable number of bytes equals to DSK length
         return 6 + calcNodeInfoBytes() + 3 + dsks.size();
      }
      else {
         // Fixed length of 6 bytes at start of record.
         // Followed by 1 byte of node info (the length of the node info)
         // Followed by nothing.
         return 7;
      }
   }

   @Override
   public byte[] bytes() {
      int nodeInfoLength = calcNodeInfoBytes();
      byte[] base = ByteUtils.ints2Bytes(
            cmdClass,
            cmd,
            seqNo,
            status,
            0,
            nodeId,
            nodeInfoLength,
            protocolInfo0,
            protocolInfo1,
            baseDeviceClass,
            genericDeviceClass,
            specificDeviceClass
            );
      byte[] cmdClasses = ByteUtils.ints2Bytes(commandClasses);
      byte[] suffix = ByteUtils.ints2Bytes(grantedKeys, kexFailure, dsks.size());
      byte[] dskBytes = ByteUtils.ints2Bytes(dsks);
      return ByteUtils.concat(base, cmdClasses, suffix, dskBytes);
   }
   
   private int calcNodeInfoBytes() {
      // Node Info Length, Protocol Part 0, Protocol Part 1, Base Device Class, 
      // Generic Device Class, Specific Device Class (6 bytes)
      
      // Number of command classes      
      return 6 + commandClasses.size();
      
      // Granted Keys, Kex Failure Type, etc... is not part of this.
   }
   
   public static class CmdNetInclNodeAddStatusDecoder implements Decoder {
      
      private CmdNetInclNodeAddStatusDecoder() {}
      
      @Override
      public Decoded decode(byte[] bytes, int offset) {
         int seqNo = 0x00FF & bytes[offset + 2];
         int status = 0x00FF & bytes[offset + 3];
         // Offset + 4 is reserved
         int newNodeId = 0x00FF & bytes[offset + 5];
         
         int nodeInfoOffset = offset + 6;
         int nodeInfoLength = 0x00FF & bytes[nodeInfoOffset];
         int nodeInfoLimit = nodeInfoOffset + nodeInfoLength; 
         
         if (nodeInfoLength > 1) {
            nodeInfoOffset++;
            
            int protocolInfo0 = 0x00FF & bytes[nodeInfoOffset];
            nodeInfoOffset++;
            
            int protocolInfo1 = 0x00FF & bytes[nodeInfoOffset];
            nodeInfoOffset++;
            
            int baseDevice = 0x00FF & bytes[nodeInfoOffset];
            nodeInfoOffset++;
            
            int genericDevice = 0x00FF & bytes[nodeInfoOffset];
            nodeInfoOffset++;
            
            int specificDevice = 0x00FF & bytes[nodeInfoOffset];
            nodeInfoOffset++;
            
            List<Integer> commandClasses = new ArrayList<>();
            for (int i = nodeInfoOffset; i < nodeInfoLimit; i++) {
               commandClasses.add(0x00FF & bytes[i]);
            }
            
            int grantedKeys = 0x00FF & bytes[nodeInfoLimit];
            int kexFailure = 0x00FF & bytes[nodeInfoLimit + 1];
            int dskLength = BitMasks.MASK_5_BITS & bytes[nodeInfoLimit + 2];
            
            List<Integer> dsks = new ArrayList<>(dskLength);
            for (int i = 1; i <= dskLength; i++) {
               dsks.add(0x00FF & bytes[nodeInfoLimit + 2 + i]);
            }
            
            return new Decoded(new CmdNetInclNodeAddStatus(seqNo, 
                  status, 
                  newNodeId, 
                  protocolInfo0, 
                  protocolInfo1, 
                  baseDevice, 
                  genericDevice, 
                  specificDevice, 
                  grantedKeys, 
                  kexFailure, 
                  commandClasses,
                  dsks));
         }
         else {
            // In this case no node info was returned.
            return new Decoded(new CmdNetInclNodeAddStatus(seqNo,
                  status,
                  newNodeId,
                  0,
                  0,
                  0,
                  0,
                  0,
                  0,
                  0,
                  Collections.emptyList(),
                  Collections.emptyList()));
         }
         
        
      }
   }

}


