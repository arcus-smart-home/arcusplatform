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
package com.iris.api.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.netty.bus.IrisNettyPlatformBusListener;
import com.iris.netty.server.message.IrisNettyMessageUtil;
import com.iris.security.authz.Authorizer;

/**
 * Extends the default platform bus listener to scope responses to the
 * API key's single place.  For example, ListPlacesResponse is filtered
 * so that only the key's place is returned.
 */
@Singleton
public class ApiPlatformBusListener extends IrisNettyPlatformBusListener {

   private static final Logger logger = LoggerFactory.getLogger(ApiPlatformBusListener.class);

   private final SessionRegistry sessionRegistry;

   @Inject
   public ApiPlatformBusListener(Authorizer authorizer, IrisNettyMessageUtil messageUtil, SessionRegistry sessionRegistry) {
      super(authorizer, messageUtil, sessionRegistry);
      this.sessionRegistry = sessionRegistry;
   }

   @Override
   public void onMessage(ClientToken ct, PlatformMessage msg) {
      if (ct != null && isPlacesListResponse(msg.getMessageType())) {
         logger.debug("Intercepted {} for ct={}", msg.getMessageType(), ct);
         Session session = sessionRegistry.getSession(ct);
         if (session != null && session.getActivePlace() != null) {
            msg = filterListPlaces(msg, session.getActivePlace());
         }
      }
      super.onMessage(ct, msg);
   }

   private static boolean isPlacesListResponse(String messageType) {
      return AccountCapability.ListPlacesResponse.NAME.equals(messageType)
            || PersonCapability.ListAvailablePlacesResponse.NAME.equals(messageType);
   }

   @SuppressWarnings("unchecked")
   private PlatformMessage filterListPlaces(PlatformMessage msg, String activePlace) {
      // Both account:ListPlacesResponse and person:ListAvailablePlacesResponse
      // use "places" as the attribute key
      List<Map<String, Object>> places = (List<Map<String, Object>>)
            msg.getValue().getAttributes().get("places");
      if (places == null || places.isEmpty()) {
         return msg;
      }

      List<Map<String, Object>> filtered = places.stream()
            .filter(p -> activePlace.equals(Objects.toString(p.get("base:id"), null)))
            .collect(Collectors.toList());

      if (filtered.size() == places.size()) {
         return msg;
      }

      logger.debug("Filtered {} from {} to {} places for active place {}",
            msg.getMessageType(), places.size(), filtered.size(), activePlace);

      MessageBody body = MessageBody.buildMessage(
            msg.getMessageType(),
            Collections.singletonMap("places", filtered));

      return PlatformMessage.builder(msg)
            .withPayload(body)
            .create();
   }
}
