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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.codahale.metrics.Gauge;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.messages.capability.HubNetworkCapability;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.Tag;
import com.iris.metrics.tag.TagValue;

@Singleton
public class HubSessionMetrics {

   private final SessionRegistry registry;
   private final Tag firmware = new Tag("firmware");

   @Inject
   public HubSessionMetrics(SessionRegistry registry) {
      this.registry = registry;
      IrisMetrics.metrics("bridge.sessions").gauge("hub", (Gauge<Map<String, Object>>) () -> sample());
   }

   public Map<String, Object> sample() {
      long total = 0;
      long cell = 0;
      long handshaking = 0;
      long below_min_fw = 0;
      long unregistered = 0;
      long registering = 0;
      long orphaned = 0;
      long invalid_account = 0;
      long invalid = 0;
      long authorized = 0;
      long bannedcell = 0;
      long unauthenticated = 0;
      Map<TagValue, Integer> firmwares = new LinkedHashMap<TagValue, Integer>(16);

      for(Session session: registry.getSessions()) {
         if(!(session instanceof HubSession)) {
            invalid++;
            continue;
         }
         total++;
         HubSession hubSession = (HubSession) session;
         if(Objects.equals(HubNetworkCapability.TYPE_3G, hubSession.getConnectionType())) {
            cell++;
         }
         
         TagValue tag = firmware.tag(hubSession.getFirmwareVersion());
         int count = firmwares.getOrDefault(tag, 0) + 1;
         firmwares.put(tag, count);
         
         switch(hubSession.getState()) {
         // these are all unauthorized states
         case CONNECTED:
         case PENDING_REG_ACK:
         case REGISTERED:
            switch(hubSession.getUnauthReason()) {
            case BELOW_MIN_FW: below_min_fw++; break;
            case UNREGISTERED: unregistered++; break;
            case REGISTERING: registering++; break;
            case ORPHANED: orphaned++; break;
            case INVALID_ACCOUNT: invalid_account++; break;
            case HANDSHAKING: handshaking++; break;
            case BANNED_CELL: bannedcell++; break;
            case UNAUTHENTICATED: unauthenticated++; break;
            default: invalid++;
            }
            break;
         case AUTHORIZED:
            authorized++;
            break;
         default:
            invalid++; // uhh... what happened?
         }
      }

      return ImmutableMap.<String,Object>builder()
            .put("total", total)
            .put("cellbackup", cell)
            .put("invalid", invalid)
            .put("unauth.below_min_fw", below_min_fw)
            .put("unauth.unregistered", unregistered)
            .put("unauth.registering", registering)
            .put("unauth.orphaned", orphaned)
            .put("unauth.invalid_account", invalid_account)
            .put("unauth.handshaking", handshaking)
            .put("unauth.banned_cell", bannedcell)
            .put("unauth.invalid_cert", unauthenticated)
            .put("authorized", authorized)
            .put("firmware", firmwares)
            .build();
   }
}

