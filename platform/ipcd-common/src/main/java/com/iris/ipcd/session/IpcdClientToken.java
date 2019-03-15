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
package com.iris.ipcd.session;

import com.iris.bridge.server.session.ClientToken;
import com.iris.messages.address.Address;

// TODO can this be collapsed with ProtocolDeviceId?
public class IpcdClientToken implements ClientToken {
   public static IpcdClientToken fromProtocolAddress(Address address) {
      return new IpcdClientToken(address.getRepresentation());
   }
   
   public static IpcdClientToken fromProtocolAddress(String address) {
      return new IpcdClientToken(address);
   }
   
   private final String key;
   
   public IpcdClientToken(String key) {
      this.key = key;
   }
   
   @Override
   public String getRepresentation() {
      return key;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((key == null) ? 0 : key.hashCode());
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
      IpcdClientToken other = (IpcdClientToken) obj;
      if (key == null) {
         if (other.key != null)
            return false;
      } else if (!key.equals(other.key))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "IpcdClientToken [key=" + key + "]";
   }

}

