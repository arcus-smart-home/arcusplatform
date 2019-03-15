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
package com.iris.netty.server.message;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.bridge.server.session.Session;
import com.iris.messages.ClientMessage;
import com.iris.messages.ClientMessage.Builder;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.population.PlacePopulationCacheManager;

public class IrisNettyMessageUtil {
   public static final String MESSAGE_PREFIX_PROP = "message.prefix";

   @Inject
   @Named(MESSAGE_PREFIX_PROP)
   protected String prefix;

   public IrisNettyMessageUtil() {

   }

   public String buildId(String id) {
      return prefix + id;
   }

   public int parseId(String s) {
      int PREFIX_LENGTH = prefix.length();
      int ret = -1;

      if (s != null) {
         if (s.startsWith(prefix)) {
            ret = Integer.parseInt(s.substring(PREFIX_LENGTH));
         }
      }

      return ret;
   }

   public PlatformMessage convertClientToPlatform(ClientMessage clientMsg, Session session, Address actor, PlacePopulationCacheManager populationCacheMgr) {
      if (StringUtils.isBlank(clientMsg.getDestination())) {
         throw new IllegalArgumentException("Message cannot send broadcast messages and must provide a destination.");
      }

      PlatformMessage.Builder builder = PlatformMessage.buildMessage(
            clientMsg.getPayload(),
            Address.fromString(buildId(session.getClientToken().getRepresentation())),
            Address.fromString(clientMsg.getDestination()))
            .withPlaceId(session.getActivePlace())
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(session.getActivePlace()));

      if (!StringUtils.isBlank(clientMsg.getCorrelationId())) {
         builder.withCorrelationId(clientMsg.getCorrelationId());
      }
      builder.withActor(actor);
      
      builder.isRequestMessage(clientMsg.isRequest());
      builder.withTimestamp(new Date());
      return builder.create();
   }

   public ClientMessage convertPlatformToClient(PlatformMessage platformMsg) {
      Builder builder = new Builder()
         .withPayload(platformMsg.getValue())
         .withSource(platformMsg.getSource().getRepresentation())
         .isRequest(platformMsg.isRequest());

      if (!StringUtils.isBlank(platformMsg.getCorrelationId())) {
         builder.withCorrelationId(platformMsg.getCorrelationId());
      }

      return builder.create();
   }
}

