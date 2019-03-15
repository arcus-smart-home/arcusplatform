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

import com.google.inject.Singleton;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.util.HubID;

@Singleton
public class HubClientFactory implements ClientFactory {
   private final String CN_PREFIX = "ih200-";
   // Matches ih200, ih30x
   private final String CN_MATCHER = "ih[2,3]0[0,x]-([0-9A-F]{2}:){5}[0-9A-F]{2}";
   private final int CN_MAC_START = CN_PREFIX.length();
   private final int CN_MAC_END = "ih200-00:00:00:00:00:00".length();

   @Override
   public Client create() {
      return HubClient.unauthenticated();
   }

   @Override
   public Client load(String principal) {
      if (principal == null) {
         return null;
      }

      int index = principal.indexOf("CN=");
      if (index == -1) {
    	  	return null;
      }
      index+=3;
      if (!principal.substring(index,index + CN_MAC_END).matches(CN_MATCHER)) {
    	  	return null;
      }

      String mac = principal.substring(index+CN_MAC_START, index+CN_MAC_END);
      String hubId = HubID.fromMac(mac);
      return new HubClient(hubId);
   }

}

