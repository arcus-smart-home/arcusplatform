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

import java.util.Collections;
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
public class DeleteApiKeyHandler implements ContextualRequestMessageHandler<Place> {

   public static final String MESSAGE_TYPE = "apikey:Delete";

   private final ApiKeyDAO apiKeyDao;
   private final AccountDAO accountDao;
   private final PlatformMessageBus bus;

   @Inject
   public DeleteApiKeyHandler(ApiKeyDAO apiKeyDao, AccountDAO accountDao, PlatformMessageBus bus) {
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

      if (!existing.isExpired()) {
         throw new ErrorEventException(Errors.CODE_INVALID_PARAM, "API key must be revoked before it can be deleted");
      }

      apiKeyDao.delete(context.getId(), keyId, existing.getKeyHash());

      // a session that missed the revocation would otherwise stay open with nothing left to end it
      emitApiKeyDeleted(context, msg.getActor(), existing);

      return MessageBody.buildMessage("apikey:DeleteResponse", Collections.emptyMap());
   }

   private void emitApiKeyDeleted(Place place, Address actor, ApiKey apiKey) {
      MessageBody body = ApiKeyService.DeletedEvent.builder()
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
