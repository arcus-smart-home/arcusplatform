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
package com.iris.agent.gateway;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class GatewayDns {
   private static ConcurrentMap<String,InetAddress> resolved = new ConcurrentHashMap<>();
   private static final boolean denySiteLocal = System.getenv("IRIS_AGENT_GATEWAY_ALLOW_LOCAL") == null;

   private GatewayDns() {
   }

   public static InetAddress resolv(String host) throws UnknownHostException {
      try {
         InetAddress result = InetAddress.getByName(host);

         // NOTE: We check for a non-routable here and consider
         //       that a failed hostname lookup. The DNS server of the
         //       4G dongle will do DNS hijacking in some cases to
         //       redirect traffic to itself.
         if (denySiteLocal && (result.isSiteLocalAddress() || result.isLinkLocalAddress() || result.isLoopbackAddress())) {
            throw new UnknownHostException(host);
         }

         resolved.put(host, result);
         return result;
      } catch (Exception ex) {
         InetAddress previous = resolved.get(host);
         if (previous != null) {
            return previous;
         }

         throw ex;
      }
   }
}

