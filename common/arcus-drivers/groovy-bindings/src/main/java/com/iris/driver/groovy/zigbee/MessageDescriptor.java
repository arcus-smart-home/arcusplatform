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

public class MessageDescriptor {
   private final int id;
   private final boolean clusterSpecific;
   private final byte group;
   
   public MessageDescriptor(int id, boolean clusterSpecific, String groupName) {
      this.id = id;
      this.clusterSpecific = clusterSpecific;
      this.group = "server".equals(groupName) 
            ? ZigbeeContext.GROUP_SERVER 
            : "client".equals(groupName) ? ZigbeeContext.GROUP_CLIENT : ZigbeeContext.GROUP_GENERAL;
   }

   public byte getIdAsByte() {
      return (byte)id;
   }
   
   public short getIdAsShort() {
      return (short)id;
   }

   public boolean isClusterSpecific() {
      return clusterSpecific;
   }
   
   public byte getGroup() {
      return group;
   }

}

