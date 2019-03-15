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
import java.util.UUID;

import com.iris.Utils;
import com.iris.messages.MessageConstants;

/**
 *
 */
public class DeviceDriverAddress extends Address {
   private static final long serialVersionUID = 3331507617223583402L;

   private final String groupId;
   private final UUID id;
   // not final to support deserialization
   private transient String representation;

   DeviceDriverAddress(String groupId, UUID id) {
      Utils.assertNotNull(groupId);
      Utils.assertNotNull(id);
      this.groupId = groupId;
      this.id = id;
      this.representation = createRepresentation();
   }

   @Override
   public boolean isHubAddress() {
      return groupId != null && !PLATFORM_GROUP.equals(groupId) && !PLATFORM_DRIVER_GROUP.equals(groupId);
   }

   @Override
   public String getHubId() {
      return isHubAddress() ? groupId : null;
   }

   public UUID getDeviceId() {
      return id;
   }

   @Override
   public boolean isBroadcast() {
      return false;
   }

   @Override
   public String getNamespace() {
      return MessageConstants.DRIVER;
   }

   @Override
   public String getGroup() {
      return groupId;
   }

   @Override
   public UUID getId() {
      return id;
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
      result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      DeviceDriverAddress other = (DeviceDriverAddress) obj;
      if (groupId == null) {
         if (other.groupId != null) return false;
      }
      else if (!groupId.equals(other.groupId)) return false;
      if (id == null) {
         if (other.id != null) return false;
      }
      else if (!id.equals(other.id)) return false;
      return true;
   }

   private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
      ois.defaultReadObject();
      this.representation = createRepresentation();
   }

}

