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

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.bus.PlatformBusListener;
import com.iris.bridge.server.netty.Constants;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.messages.PlatformMessage;
import com.iris.security.apikey.ApiKeyPrincipal;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.principal.Principal;

@Singleton
public class ApiKeyRevocationListener implements PlatformBusListener {
   private static final Logger logger = LoggerFactory.getLogger(ApiKeyRevocationListener.class);

   public static final String EVENT_API_KEY_REVOKED = "apikey:Revoked";

   private final SessionRegistry sessionRegistry;

   @Inject
   public ApiKeyRevocationListener(SessionRegistry sessionRegistry) {
      this.sessionRegistry = sessionRegistry;
   }

   @Override
   public void onMessage(ClientToken ct, PlatformMessage msg) {
      if (!EVENT_API_KEY_REVOKED.equals(msg.getMessageType())) {
         return;
      }

      String keyIdStr = (String) msg.getValue().getAttributes().get("keyId");
      if (keyIdStr == null) {
         logger.warn("Received apikey:Revoked event without keyId");
         return;
      }

      UUID keyId;
      try {
         keyId = UUID.fromString(keyIdStr);
      } catch (IllegalArgumentException e) {
         logger.warn("Received apikey:Revoked event with invalid keyId: {}", keyIdStr);
         return;
      }

      logger.info("API key revoked [{}], disconnecting active sessions", keyId);

      for (Session session : sessionRegistry.getSessions()) {
         AuthorizationContext authCtx = session.getAuthorizationContext();
         if (authCtx == null) {
            continue;
         }

         Principal principal = authCtx.getPrincipal();
         if (!(principal instanceof ApiKeyPrincipal)) {
            continue;
         }

         ApiKeyPrincipal apiKeyPrincipal = (ApiKeyPrincipal) principal;
         if (keyId.equals(apiKeyPrincipal.getKeyId())) {
            logger.info("Disconnecting session for revoked API key [{}], label [{}]",
                  keyId, apiKeyPrincipal.getLabel());
            session.disconnect(Constants.SESSION_EXPIRED_STATUS);
            session.destroy();
         }
      }
   }
}
