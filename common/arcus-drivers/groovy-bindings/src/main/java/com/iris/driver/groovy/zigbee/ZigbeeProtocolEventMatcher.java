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
package com.iris.driver.groovy.zigbee;

import com.iris.driver.metadata.ProtocolEventMatcher;

public class ZigbeeProtocolEventMatcher extends ProtocolEventMatcher {
   private Byte messageType;
   private Short clusterOrMessageId;
   private Byte zclMessageId;
   private Byte group;
   
   public Byte getMessageType() {
      return messageType;
   }
   
   public void setMessageType(Byte messageType) {
      this.messageType = messageType;
   }
   
   public boolean matchesAnyMessageType() {
      return messageType == null;
   }
   
   public Short getClusterOrMessageId() {
      return clusterOrMessageId;
   }
   
   public void setClusterOrMessageId(Short clusterId) {
      this.clusterOrMessageId = clusterId;
   }
   
   public boolean matchesAnyClusterOrMessageId() {
      return clusterOrMessageId == null;
   }
   
   public Byte getZclMessageId() {
      return zclMessageId;
   }
   
   public void setZclMessageId(Byte zclMessageId) {
      this.zclMessageId = zclMessageId;
   }
   
   public boolean matchesAnyZclMessageId() {
      return zclMessageId == null;
   }
   
   public Byte getGroup() {
      return group;
   }

   public void setGroup(Byte group) {
      this.group = group;
   }
   
   public boolean matchesAnyGroup() {
      return group == null;
   }

   @Override
   public String toString() {
      return "ZigbeeProtocolEventMatcher [messageType=" + messageType
            + ", clusterOrMessageId=" + clusterOrMessageId + ", zclMessageId="
            + zclMessageId + ", group=" + group + "]";
   }
}

