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

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
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
import com.iris.security.authz.AuthzUtil;
import com.iris.messages.model.Account;
import com.iris.messages.model.Place;
import com.iris.security.apikey.ApiKey;

@Singleton
public class RevokeApiKeyHandler implements ContextualRequestMessageHandler<Place> {

   public static final String MESSAGE_TYPE = "apikey:Revoke";
   public static final String EVENT_API_KEY_REVOKED = "apikey:Revoked";

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
      requireAccountOwner(context, msg);

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

      if (existing.isExpired()) {
         throw new ErrorEventException(Errors.CODE_INVALID_PARAM, "API key is already revoked");
      }

      // Expire the key immediately rather than deleting, preserving audit trail
      apiKeyDao.expire(context.getId(), keyId, existing.getKeyHash(), new Date());

      // Broadcast revocation so api-bridge instances can disconnect active sessions
      emitApiKeyRevoked(context, keyId);

      return MessageBody.buildMessage("apikey:RevokeResponse", java.util.Collections.emptyMap());
   }

   private void requireAccountOwner(Place place, PlatformMessage msg) {
      if (msg.getActor() == null) {
         throw new ErrorEventException(Errors.CODE_INVALID_REQUEST, "actor is required");
      }

      Account account = accountDao.findById(place.getAccount());
      if (account == null) {
         throw new ErrorEventException(Errors.CODE_NOT_FOUND, "account not found");
      }

      UUID actorId = (UUID) msg.getActor().getId();
      if (!Objects.equals(actorId, account.getOwner())) {
         throw new ErrorEventException(AuthzUtil.UNAUTHORIZED_CODE, "only the account owner can manage API keys");
      }
   }

   private void emitApiKeyRevoked(Place place, UUID keyId) {
      MessageBody body = MessageBody.buildMessage(EVENT_API_KEY_REVOKED,
            ImmutableMap.of("keyId", keyId.toString()));
      PlatformMessage event = PlatformMessage.buildBroadcast(body, Address.fromString(place.getAddress()))
            .withPlaceId(place.getId())
            .withPopulation(place.getPopulation())
            .create();
      bus.send(event);
   }
}
