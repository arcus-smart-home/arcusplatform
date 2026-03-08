/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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

package com.iris.agent.zigbee.node;

public class ZBAttribute {
   private long id;
   private final long clusterDbId;
   private final int attributeId;
   private int attributeDt;
   private byte[] lastValue;

   public ZBAttribute(long id, long clusterDbId, int attributeId, int attributeDt, byte[] lastValue) {
      this.id = id;
      this.clusterDbId = clusterDbId;
      this.attributeId = attributeId;
      this.attributeDt = attributeDt;
      this.lastValue = lastValue;
   }

   public ZBAttribute(long clusterDbId, int attributeId) {
      this(0, clusterDbId, attributeId, 0, null);
   }

   public long getId() {
      return id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public long getClusterDbId() {
      return clusterDbId;
   }

   public int getAttributeId() {
      return attributeId;
   }

   public int getAttributeDt() {
      return attributeDt;
   }

   public void setAttributeDt(int attributeDt) {
      this.attributeDt = attributeDt;
   }

   public byte[] getLastValue() {
      return lastValue;
   }

   public void setLastValue(byte[] lastValue) {
      this.lastValue = lastValue;
   }
}
