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
/**
 * 
 */
package com.iris.messages.address;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.iris.Utils;
import com.iris.messages.MessageConstants;

/**
 *
 */
public class ClientAddress extends Address {
   private static final long serialVersionUID = 4823298215983862821L;

   // TODO change these to byte arrays?
   private final String serverId;
   private final String sessionId;
   // not final to support deserialization
   private transient String representation;
  
   ClientAddress(String serverId, String sessionId) {
      Utils.assertNotNull(sessionId);
      Utils.assertTrue(serverId.length() < GROUP_LENGTH, "Server id is too long");
      Utils.assertTrue(sessionId.length() < ID_LENGTH, "Session id is too long");
      this.serverId = serverId;
      this.sessionId = sessionId;
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
      return MessageConstants.CLIENT;
   }

   @Override
   public String getGroup() {
      return serverId;
   }

   @Override
   public String getId() {
      return sessionId;
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
      result = prime * result + ((serverId == null) ? 0 : serverId.hashCode());
      result = prime * result
            + ((sessionId == null) ? 0 : sessionId.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ClientAddress other = (ClientAddress) obj;
      if (serverId == null) {
         if (other.serverId != null) return false;
      }
      else if (!serverId.equals(other.serverId)) return false;
      if (sessionId == null) {
         if (other.sessionId != null) return false;
      }
      else if (!sessionId.equals(other.sessionId)) return false;
      return true;
   }

   private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
      ois.defaultReadObject();
      this.representation = createRepresentation();
   }

}

