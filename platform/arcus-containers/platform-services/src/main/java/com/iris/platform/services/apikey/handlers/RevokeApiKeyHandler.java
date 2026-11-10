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
package com.iris.platform.services.apikey.handlers;

import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.ApiKeyDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.ApiKeyService;
import com.iris.messages.model.Place;
import com.iris.security.apikey.ApiKey;

@Singleton
public class RevokeApiKeyHandler implements ContextualRequestMessageHandler<Place> {

   public static final String MESSAGE_TYPE = "apikey:Revoke";
   public static final String EVENT_API_KEY_REVOKED = ApiKeyService.RevokedEvent.NAME;

   private final ApiKeyDAO apiKeyDao;
   private final AccountDAO accountDao;
   private final PlatformMessageBus bus;

   @Inject
   public RevokeApiKeyHandler(ApiKeyDAO apiKeyDao, AccountDAO accountDao, PlatformMessageBus bus) {
      this.apiKeyDao = apiKeyDao;
      this.accountDao = accountDao;
      this.bus = bus;
   }

   @Override
   public String getMessageType() {
      return MESSAGE_TYPE;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      AccountOwnerCheck.requireAccountOwner(accountDao, context, msg);

      MessageBody body = msg.getValue();

      String idStr = (String) body.getAttributes().get("id");
      if (StringUtils.isBlank(idStr)) {
         throw new ErrorEventException(Errors.CODE_MISSING_PARAM, "id is required");
      }

      UUID keyId;
      try {
         keyId = UUID.fromString(idStr);
      } catch (IllegalArgumentException e) {
         throw new ErrorEventException(Errors.CODE_INVALID_PARAM, "id must be a valid UUID");
      }

      // Find the key to get the keyHash for the secondary table delete
      ApiKey existing = null;
      for (ApiKey key : apiKeyDao.findByPlace(context.getId())) {
         if (key.getId().equals(keyId)) {
            existing = key;
            break;
         }
      }

      if (existing == null) {
         throw new ErrorEventException(Errors.CODE_NOT_FOUND, "API key not found");
      }

      // Expire the key immediately rather than deleting, preserving audit trail. A key that has already
      // expired keeps the time it stopped working.
      if (!existing.isExpired()) {
         apiKeyDao.expire(context.getId(), keyId, existing.getKeyHash(), Instant.now());
      }

      // Broadcast revocation so api-bridge instances can disconnect active sessions and the history service can
      // record who revoked the key. This is sent for a key that has already expired too, so that revoking it
      // again closes any session still using it, such as one whose key expired or whose first event was lost.
      emitApiKeyRevoked(context, msg.getActor(), existing);

      return MessageBody.buildMessage("apikey:RevokeResponse", java.util.Collections.emptyMap());
   }

   private void emitApiKeyRevoked(Place place, Address actor, ApiKey apiKey) {
      MessageBody body = ApiKeyService.RevokedEvent.builder()
            .withKeyId(apiKey.getId().toString())
            .withLabel(apiKey.getLabel())
            .build();
      PlatformMessage event = PlatformMessage.buildBroadcast(body, Address.fromString(place.getAddress()))
            .withPlaceId(place.getId())
            .withPopulation(place.getPopulation())
            .withActor(actor)
            .create();
      bus.send(event);
   }
}
