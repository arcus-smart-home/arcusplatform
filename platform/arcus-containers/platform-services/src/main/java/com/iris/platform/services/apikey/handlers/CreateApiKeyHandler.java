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
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.ApiKeyDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Place;
import com.iris.security.apikey.ApiKey;
import com.iris.security.apikey.ApiKeyGenerator;

@Singleton
public class CreateApiKeyHandler implements ContextualRequestMessageHandler<Place> {

   public static final String MESSAGE_TYPE = "apikey:Create";

   private final ApiKeyDAO apiKeyDao;

   @Inject
   public CreateApiKeyHandler(ApiKeyDAO apiKeyDao) {
      this.apiKeyDao = apiKeyDao;
   }

   @Override
   public String getMessageType() {
      return MESSAGE_TYPE;
   }

   @SuppressWarnings("unchecked")
   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      MessageBody body = msg.getValue();

      String label = (String) body.getAttributes().get("label");
      if (StringUtils.isBlank(label)) {
         throw new ErrorEventException(Errors.CODE_MISSING_PARAM, "label is required");
      }

      Object permsObj = body.getAttributes().get("permissions");
      if (permsObj == null) {
         throw new ErrorEventException(Errors.CODE_MISSING_PARAM, "permissions is required");
      }
      Set<String> permissions;
      if (permsObj instanceof Set) {
         permissions = (Set<String>) permsObj;
      } else if (permsObj instanceof java.util.Collection) {
         permissions = new java.util.HashSet<>((java.util.Collection<String>) permsObj);
      } else {
         throw new ErrorEventException(Errors.CODE_INVALID_PARAM, "permissions must be a set of strings");
      }

      if (permissions.isEmpty()) {
         throw new ErrorEventException(Errors.CODE_INVALID_PARAM, "permissions must not be empty");
      }

      String rawKey = ApiKeyGenerator.generate();
      String keyPrefix = ApiKeyGenerator.extractPrefix(rawKey);
      String keyHash = ApiKeyGenerator.hashKey(rawKey);

      UUID keyId = UUID.randomUUID();

      ApiKey apiKey = new ApiKey();
      apiKey.setId(keyId);
      apiKey.setPlaceId(context.getId());
      apiKey.setLabel(label);
      apiKey.setKeyPrefix(keyPrefix);
      apiKey.setKeyHash(keyHash);
      apiKey.setPersonId(UUID.fromString(msg.getActor().getId().toString()));
      apiKey.setAccountId(context.getAccount());
      apiKey.setPermissions(permissions);
      apiKey.setCreated(new Date());

      apiKeyDao.save(apiKey);

      return MessageBody.buildMessage(
            "apikey:CreateResponse",
            ImmutableMap.of(
                  "id", keyId.toString(),
                  "key", rawKey
            )
      );
   }
}
