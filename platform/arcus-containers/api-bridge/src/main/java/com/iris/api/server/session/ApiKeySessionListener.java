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

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.netty.Constants;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionListener;
import com.iris.core.dao.ApiKeyDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.model.Place;
import com.iris.messages.model.PlaceDescriptor;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.apikey.ApiKey;
import com.iris.security.apikey.ApiKeyPrincipal;
import com.iris.security.principal.Principal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.ScheduledFuture;

@Singleton
public class ApiKeySessionListener implements SessionListener {

   private static final Logger logger = LoggerFactory.getLogger(ApiKeySessionListener.class);

   private final PlaceDAO placeDao;
   private final ApiKeyDAO apiKeyDao;

   @Inject
   public ApiKeySessionListener(PlaceDAO placeDao, ApiKeyDAO apiKeyDao) {
      this.placeDao = placeDao;
      this.apiKeyDao = apiKeyDao;
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

      watchKey(session, apiKeyPrincipal);
   }

   @Override
   public void onSessionDestroyed(Session session) {
      // no-op
   }

   /**
    * Ends the session once its key stops working. The realm checked the key before the upgrade, and
    * ApiKeyRevocationListener can only close sessions that are registered, so a revocation that came in
    * between would be missed. WebSocketServerHandler registers the session before it calls listeners,
    * and the check below runs as a later task on the channel's event loop, after the upgrade is done:
    * either the revocation reached the DAO before this check, or its event finds the session registered.
    * The expiry timer covers a client that only listens and so never reaches ApiMessageHandler's check.
    */
   private void watchKey(Session session, ApiKeyPrincipal key) {
      Channel channel = session.getChannel();
      channel.eventLoop().execute(() -> {
         if(!isStillValid(key)) {
            close(session, key, "was revoked, deleted or expired after the session logged in");
         }
      });

      if(key.getExpiresAt() != null) {
         long delayMs = Math.max(0, Duration.between(Instant.now(), key.getExpiresAt()).toMillis());
         ScheduledFuture<?> expiry = channel.eventLoop().schedule(() -> close(session, key, "expired"), delayMs, TimeUnit.MILLISECONDS);
         channel.closeFuture().addListener((ChannelFutureListener) (f) -> expiry.cancel(false));
      }
   }

   private boolean isStillValid(ApiKeyPrincipal key) {
      try {
         for(ApiKey current : apiKeyDao.findByPlace(key.getPlaceId())) {
            if(key.getKeyId().equals(current.getId())) {
               return !current.isExpired();
            }
         }
         // deleted
         return false;
      }
      catch(Exception e) {
         // the client can reconnect, and the realm checks the key again
         logger.warn("Unable to check API key [{}], closing its session", key.getKeyId(), e);
         return false;
      }
   }

   private static void close(Session session, ApiKeyPrincipal key, String reason) {
      logger.info("Closing the session for API key [{}], label [{}]: the key {}", key.getKeyId(), key.getLabel(), reason);
      session.disconnect(Constants.SESSION_EXPIRED_STATUS);
      session.destroy();
   }
}
