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
package com.iris.agent.zwave.node;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ZWNodeBuilder {
   private int nodeId = ZWNode.INVALID_VALUE;
   private long homeId = ZWNode.INVALID_VALUE;
   private int basicType = ZWNode.INVALID_VALUE;
   private int genericType = ZWNode.INVALID_VALUE;
   private int specificType = ZWNode.INVALID_VALUE;
   private int manufacturerId = ZWNode.INVALID_VALUE;
   private int productTypeId = ZWNode.INVALID_VALUE;
   private int productId = ZWNode.INVALID_VALUE;
   private Set<Integer> cmdClassSet = new HashSet<>();
   private boolean online = true;
   private int offlineTimeout = 0;
   
   ZWNodeBuilder(int nodeId) {
      this.nodeId = nodeId;
   }
   
   public ZWNodeBuilder setHomeId(long homeId) {
      this.homeId = homeId;
      return this;
   }
   
   public ZWNodeBuilder setBasicType(int basicType) {
      this.basicType = basicType;
      return this;
   }
   
   public ZWNodeBuilder setGenericType(int genericType) {
      this.genericType = genericType;
      return this;
   }
   
   public ZWNodeBuilder setSpecificType(int specificType) {
      this.specificType = specificType;
      return this;
   }
   
   public ZWNodeBuilder setManufacturerId(int manufacturerId) {
      this.manufacturerId = manufacturerId;
      return this;
   }
   
   public ZWNodeBuilder setProductTypeId(int productTypeId) {
      this.productTypeId = productTypeId;
      return this;
   }
   
   public ZWNodeBuilder setProductId(int productId) {
      this.productId = productId;
      return this;
   }
   
   public ZWNodeBuilder addCmdClass(int cmdClass) {
      this.cmdClassSet.add(cmdClass);
      return this;
   }
   
   public ZWNodeBuilder addCmdClasses(Collection<Integer> cmdClasses) {
      this.cmdClassSet.addAll(cmdClasses);
      return this;
   }
   
   public ZWNodeBuilder addCmdClasses(byte[] bytes) {
      if (bytes != null) {
         for (byte b : bytes) {
            this.cmdClassSet.add(0x00FF & b);
         }
      }
      return this;
   }
   
   public ZWNodeBuilder setOnline(boolean online) {
      this.online = online;
      return this;
   }
   
   public ZWNodeBuilder setOnline(int online) {
      this.online = online != 0;
      return this;
   }
   
   public ZWNodeBuilder setOfflineTimeout(int offlineTimeout) {
      this.offlineTimeout = offlineTimeout;
      return this;
   }
   
   public boolean isReadyToBuild() {
      return nodeId != ZWNode.INVALID_VALUE 
            && homeId != ZWNode.INVALID_VALUE 
            && basicType != ZWNode.INVALID_VALUE 
            && genericType != ZWNode.INVALID_VALUE 
            && specificType != ZWNode.INVALID_VALUE
            && manufacturerId != ZWNode.INVALID_VALUE
            && productTypeId != ZWNode.INVALID_VALUE
            && productId != ZWNode.INVALID_VALUE;
   }
   
   public ZWNode build() {
      return new ZWNode(nodeId, homeId, basicType, genericType, specificType, manufacturerId, productTypeId, productId, cmdClassSet, online, offlineTimeout);
   }
}

