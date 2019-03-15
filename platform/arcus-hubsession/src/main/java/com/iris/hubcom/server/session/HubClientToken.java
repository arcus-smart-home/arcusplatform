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
package com.iris.hubcom.server.session;

import com.iris.bridge.server.session.ClientToken;
import com.iris.messages.address.Address;
import com.iris.util.HubID;

public class HubClientToken implements ClientToken {

   public static HubClientToken fromMac(String mac) {
      String hubId = HubID.fromMac(mac);
      if(hubId == null) {
         return null;
      }
      return new HubClientToken(hubId.toString());
   }

   public static HubClientToken fromAddress(Address address) {
      if(address == null || !address.isHubAddress()) {
         return null;
      }

      String hubId = address.getHubId();
      if(hubId == null) {
         return null;
      }
      return new HubClientToken(hubId.toString());
   }

   public static HubClientToken fromHubID(String hubId) {
      return new HubClientToken(hubId);
   }

	private final String key;

	protected HubClientToken(String key) {
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
      HubClientToken other = (HubClientToken) obj;
      if (key == null) {
         if (other.key != null)
            return false;
      } else if (!key.equals(other.key))
         return false;
      return true;
   }

	@Override
   public String toString() {
      return "HubClientToken [key=" + key + "]";
   }

}

