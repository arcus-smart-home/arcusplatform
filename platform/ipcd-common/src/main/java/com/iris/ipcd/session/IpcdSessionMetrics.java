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

import java.util.Map;

import com.codahale.metrics.Gauge;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.metrics.IrisMetrics;

@Singleton
public class IpcdSessionMetrics {

   private final SessionRegistry registry;

   @Inject
   public IpcdSessionMetrics(SessionRegistry registry) {
      this.registry = registry;
      IrisMetrics.metrics("bridge.sessions").gauge("ipcd", (Gauge<Map<String, Long>>) () -> sample());
   }

   public Map<String, Long> sample() {
      long total = 0;
      long invalid = 0;
      long unregistered = 0;
      long pending_driver = 0;
      long registered = 0;

      for(Session session: registry.getSessions()) {
         if(!(session instanceof IpcdSession)) {
            invalid++;
            continue;
         }
         total++;
         IpcdSession ipcdSession = (IpcdSession) session;
         switch(ipcdSession.getRegistrationState()) {
         // these are all unauthorized states
         case REGISTERED: registered++; break;
         case PENDING_DRIVER: pending_driver++; break;
         case UNREGISTERED: unregistered++; break;
         default: invalid++; break;
         }
      }

      return ImmutableMap.<String,Long>builder()
            .put("total", total)
            .put("invalid", invalid)
            .put("registered", registered)
            .put("unregistered", unregistered)
            .put("pending_driver", pending_driver)
            .build();
   }
}

