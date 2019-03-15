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
 * The address for a service running on the platform or the hub.
 * For platform service the group is always the null UUID.
 * For hub service the group is the hub id.
 */
public class HubServiceAddress extends Address {
   private static final long serialVersionUID = 8673741519097409404L;
   private final String hubId;
   private final String serviceName;
   // not final to support deserialization
   private transient String representation;

   HubServiceAddress(String hubId, String serviceName) {
      Utils.assertNotNull(hubId);
      Utils.assertNotNull(serviceName);
      this.hubId = hubId;
      this.serviceName = serviceName;
      this.representation = createRepresentation();
   }

   @Override
   public boolean isHubAddress() {
      return true;
   }

   @Override
   public String getHubId() {
      return hubId;
   }

   public String getServiceName() {
      return serviceName;
   }

   @Override
   public boolean isBroadcast() {
      return false;
   }

   @Override
   public String getNamespace() {
      return MessageConstants.SERVICE;
   }

   @Override
   public String getGroup() {
      return hubId;
   }

   @Override
   public String getId() {
      return serviceName;
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
      result = prime * result + ((hubId == null) ? 0 : hubId.hashCode());
      result = prime * result + ((serviceName == null) ? 0 : serviceName.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      HubServiceAddress other = (HubServiceAddress) obj;
      if (hubId == null) {
         if (other.hubId != null) return false;
      }
      else if (!hubId.equals(other.hubId)) return false;
      if (serviceName == null) {
         if (other.serviceName != null) return false;
      }
      else if (!serviceName.equals(other.serviceName)) return false;
      return true;
   }

   private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
      ois.defaultReadObject();
      this.representation = createRepresentation();
   }

}

