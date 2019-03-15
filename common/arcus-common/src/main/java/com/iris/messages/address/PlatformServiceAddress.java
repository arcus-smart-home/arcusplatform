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
import java.util.UUID;

import com.iris.messages.MessageConstants;

public class PlatformServiceAddress extends Address {

   private static final long serialVersionUID = -3709142458260768683L;

   private final Object contextId;
   private final Integer contextQualifier;
   private final String serviceName;
   // not final to support deserialization
   private transient String representation;

   PlatformServiceAddress(Object contextId, String serviceName, Integer contextQualifier) {
      this.contextId = contextId;
      this.serviceName = serviceName;
      this.contextQualifier = contextQualifier;
      this.representation = getRepresentation(getNamespace(), serviceName, contextId, contextQualifier);
   }

   public Object getContextId() {
      return contextId;
   }

   public String getServiceName() {
      return serviceName;
   }

   public Integer getContextQualifier() {
      return contextQualifier;
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
      return MessageConstants.SERVICE;
   }

   @Override
   public String getGroup() {
      return serviceName;
   }

   @Override
   public Object getId() {
      return contextId;
   }

   @Override
   public byte[] getBytes() {
      byte[] bytes = createBytes();
      writePrefix(getNamespace(), bytes);
      writeGroup(getGroup(), bytes);
      if(contextId instanceof String) {
         writeId((String) contextId, bytes);
      } else if(contextId instanceof UUID) {
         writeId((UUID) contextId, bytes);
      } else {
         throw new IllegalArgumentException("The context id for a platfrom service must be a UUID or string");
      }
      writeContextQualifier(contextQualifier, bytes);

      return bytes;
   }

   @Override
   public String getRepresentation() {
      return representation;
   }

   private static final String getRepresentation(String namespace, String service, Object id, Integer contextQualifier) {
      StringBuilder sb = new StringBuilder(namespace).append(":").append(service).append(":");
      if(id != null && !PLATFORM_GROUP.equals(id) && !ZERO_UUID.equals(id)) {
         sb.append(id);
         if(contextQualifier != null) {
            sb.append(".").append(contextQualifier);
         }
      }
      return sb.toString();
   }
   
   @Override
   public int hashCode() {
      // Base hashCode off of representation since two addresses with the same 
      // representation are equal addresses. 
      return getRepresentation().hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      // Base equals off of representation since two addresses with the same 
      // representation are equal addresses. 
      PlatformServiceAddress other = (PlatformServiceAddress) obj;
      return getRepresentation().equals(other.getRepresentation());
   }

   private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
      ois.defaultReadObject();
      this.representation = getRepresentation(getNamespace(), serviceName, contextId, contextQualifier);
   }

}

