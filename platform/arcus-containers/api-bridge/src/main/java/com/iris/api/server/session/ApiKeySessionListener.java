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
package com.iris.api.server.session;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionListener;
import com.iris.core.dao.PlaceDAO;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.model.Place;
import com.iris.messages.model.PlaceDescriptor;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.apikey.ApiKeyPrincipal;
import com.iris.security.principal.Principal;

@Singleton
public class ApiKeySessionListener implements SessionListener {

   private static final Logger logger = LoggerFactory.getLogger(ApiKeySessionListener.class);

   private final PlaceDAO placeDao;

   @Inject
   public ApiKeySessionListener(PlaceDAO placeDao) {
      this.placeDao = placeDao;
   }

   @Override
   public void onSessionCreated(Session session) {
      AuthorizationContext context = session.getAuthorizationContext();
      if (context == null) {
         logger.warn("Session created without authorization context");
         return;
      }

      Principal principal = context.getPrincipal();
      if (!(principal instanceof ApiKeyPrincipal)) {
         logger.warn("Session created with non-API-key principal: {}", principal);
         return;
      }

      ApiKeyPrincipal apiKeyPrincipal = (ApiKeyPrincipal) principal;

      // Auto-set the active place from the key
      session.setActivePlace(apiKeyPrincipal.getPlaceId().toString());

      logger.info("API key session created for key '{}' on place {}", apiKeyPrincipal.getLabel(), apiKeyPrincipal.getPlaceId());

      String placeId = apiKeyPrincipal.getPlaceId().toString();
      String accountId = apiKeyPrincipal.getAccountId().toString();

      String placeName = "";
      Place place = placeDao.findById(apiKeyPrincipal.getPlaceId());
      if (place != null) {
         placeName = place.getName();
      }

      PlaceDescriptor descriptor = new PlaceDescriptor(placeId, placeName, accountId, PlaceDescriptor.ROLE_OWNER);

      Map<String, Object> entries = new HashMap<>();
      entries.put("personId", apiKeyPrincipal.getPersonId().toString());
      entries.put("places", Collections.singleton(descriptor));
      entries.put("placeId", placeId);
      entries.put("keyId", apiKeyPrincipal.getKeyId().toString());
      entries.put("label", apiKeyPrincipal.getLabel());

      MessageBody body = MessageBody.buildMessage(
            MessageConstants.MSG_SESSION_CREATED,
            entries
      );

      ClientMessage msg = ClientMessage.builder().withPayload(body).create();
      session.sendMessage(JSON.toJson(msg));
   }

   @Override
   public void onSessionDestroyed(Session session) {
      // no-op
   }
}
