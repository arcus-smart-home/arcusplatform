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
public class DeviceProtocolAddress extends Address {
   private static final long serialVersionUID = -4983741624446033716L;

   private final String protocolName;
   private final String hubId;
   private final ProtocolDeviceId protocolDeviceId;
   // not final to support deserialization
   private transient String representation;

   DeviceProtocolAddress(String protocolName, String hubId, ProtocolDeviceId protocolDeviceId) {
      Utils.assertNotNull(protocolName);
      Utils.assertNotNull(hubId);
      Utils.assertNotNull(protocolDeviceId);
      Utils.assertTrue(protocolName.length() <= 4, "Protocol name must be 4 char or shorter");
      Utils.assertTrue(protocolDeviceId.getByteLength() <= 20, "Protocol device id must be 20 char or shorter");

      this.protocolName = protocolName;
      this.hubId = hubId;
      this.protocolDeviceId = protocolDeviceId;
      this.representation = createRepresentation();
   }

   @Override
   public boolean isHubAddress() {
      return hubId != null && !hubId.equals(PLATFORM_GROUP);
   }

   @Override
   public String getHubId() {
      return isHubAddress() ? hubId : null;
   }

   public String getProtocolName() {
      return protocolName;
   }

   public ProtocolDeviceId getProtocolDeviceId() {
      return protocolDeviceId;
   }

   @Override
   public boolean isBroadcast() {
      return false;
   }

   @Override
   public String getNamespace() {
      return MessageConstants.PROTOCOL;
   }

   @Override
   public String getGroup() {
      return isHubAddress() ? protocolName + "-" + hubId : protocolName;
   }

   @Override
   public ProtocolDeviceId getId() {
      return protocolDeviceId;
   }

   @Override
   public String getRepresentation() {
      return representation;
   }

   @Override
   public byte[] getBytes() {
      byte [] bytes = createBytes();
      writePrefix(getNamespace(), bytes);
      writeGroup(hubId, bytes);
      byte [] proto = protocolName.getBytes(Utils.UTF_8);
      if(proto.length > 4) {
         throw new IllegalStateException("Invalid protocol name [" + protocolName + "]. May be no longer than 4 characters");
      }
      System.arraycopy(proto, 0, bytes, 20, proto.length);

      protocolDeviceId.copyBytesTo(bytes, 24);

      return bytes;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((hubId == null) ? 0 : hubId.hashCode());
      result = prime * result
            + ((protocolDeviceId == null) ? 0 : protocolDeviceId.hashCode());
      result = prime * result
            + ((protocolName == null) ? 0 : protocolName.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      DeviceProtocolAddress other = (DeviceProtocolAddress) obj;
      if (hubId == null) {
         if (other.hubId != null) return false;
      }
      else if (!hubId.equals(other.hubId)) return false;
      if (protocolDeviceId == null) {
         if (other.protocolDeviceId != null) return false;
      }
      else if (!protocolDeviceId.equals(other.protocolDeviceId)) return false;
      if (protocolName == null) {
         if (other.protocolName != null) return false;
      }
      else if (!protocolName.equals(other.protocolName)) return false;
      return true;
   }

   private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
      ois.defaultReadObject();
      this.representation = createRepresentation();
   }

}

