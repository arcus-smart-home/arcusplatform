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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionListener;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.apikey.ApiKeyPrincipal;
import com.iris.security.principal.Principal;

@Singleton
public class ApiKeySessionListener implements SessionListener {

   private static final Logger logger = LoggerFactory.getLogger(ApiKeySessionListener.class);

   @Inject
   public ApiKeySessionListener() {
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

      MessageBody body = MessageBody.buildMessage(
            MessageConstants.MSG_SESSION_CREATED,
            ImmutableMap.of(
                  "placeId", apiKeyPrincipal.getPlaceId().toString(),
                  "keyId", apiKeyPrincipal.getKeyId().toString(),
                  "label", apiKeyPrincipal.getLabel()
            )
      );

      ClientMessage msg = ClientMessage.builder().withPayload(body).create();
      session.sendMessage(JSON.toJson(msg));
   }

   @Override
   public void onSessionDestroyed(Session session) {
      // no-op
   }
}
