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
package com.iris.messages.address;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.iris.Utils;
import com.iris.messages.MessageConstants;

public class BridgeAddress extends Address {
   private static final long serialVersionUID = 8309876542779355445L;
   
   private final String bridgeId;
   // not final to support deserialization
   private transient String representation;
   
   BridgeAddress(String bridgeId) {
      Utils.assertNotNull(bridgeId);
      this.bridgeId = bridgeId;
      this.representation = createRepresentation();
   }
   
   @Override
   public boolean isBroadcast() {
      return false;
   }

   @Override
   public boolean isHubAddress() {
      return false;
   }

   @Override
   public String getNamespace() {
      return MessageConstants.BRIDGE;
   }

   @Override
   public String getGroup() {
      return PLATFORM_GROUP;
   }

   @Override
   public String getId() {
      return bridgeId;
   }

   @Override
   public String getRepresentation() {
      return representation;
   }

   @Override
   public byte[] getBytes() {
      byte [] bytes = createBytes();
      writePrefix(getNamespace(), bytes);
      writeGroup(getGroup(), bytes);
      writeId(getId(), bytes);
      return bytes;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((bridgeId == null) ? 0 : bridgeId.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      BridgeAddress other = (BridgeAddress) obj;
      if (bridgeId == null) {
         if (other.bridgeId != null)
            return false;
      } else if (!bridgeId.equals(other.bridgeId))
         return false;
      return true;
   }
   
   private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
      ois.defaultReadObject();
      this.representation = createRepresentation();
   }
   
}

