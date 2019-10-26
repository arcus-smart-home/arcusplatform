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
package com.iris.agent.zwave;

import com.iris.agent.zwave.code.Decoded;

/**
 * Wrapper for a block of decoded data.
 * 
 * @author Erik Larson
 */
public class ZWData {
   private final int nodeId;
   private final Decoded decoded;
   
   /**
    * Constructs the decoded data wrapper
    * 
    * @param nodeId the id the message is from
    * @param decoded the decoded message
    */
   public ZWData(int nodeId, Decoded decoded) {
      this.nodeId = nodeId;
      this.decoded = decoded;
   }

   /**
    * Gets the id of the node this data is from
    * 
    * @return node id
    */
   public int getNodeId() {
      return nodeId;
   }

   /**
    * Gets the decoded data block
    * 
    * @return decoded message
    */
   public Decoded getDecoded() {
      return decoded;
   }
}

