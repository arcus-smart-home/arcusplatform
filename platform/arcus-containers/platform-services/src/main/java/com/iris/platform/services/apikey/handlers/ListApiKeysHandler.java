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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.ApiKeyDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.Place;
import com.iris.security.apikey.ApiKey;

@Singleton
public class ListApiKeysHandler implements ContextualRequestMessageHandler<Place> {

   public static final String MESSAGE_TYPE = "apikey:ListKeys";

   private final ApiKeyDAO apiKeyDao;

   @Inject
   public ListApiKeysHandler(ApiKeyDAO apiKeyDao) {
      this.apiKeyDao = apiKeyDao;
   }

   @Override
   public String getMessageType() {
      return MESSAGE_TYPE;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      List<ApiKey> keys = apiKeyDao.findByPlace(context.getId());

      List<Map<String, Object>> keyList = keys.stream()
            .map(this::toMap)
            .collect(Collectors.toList());

      return MessageBody.buildMessage(
            "apikey:ListKeysResponse",
            ImmutableMap.of("keys", keyList)
      );
   }

   private Map<String, Object> toMap(ApiKey key) {
      Map<String, Object> map = new HashMap<>();
      map.put("id", key.getId().toString());
      map.put("label", key.getLabel());
      map.put("keyPrefix", key.getKeyPrefix());
      map.put("permissions", key.getPermissions());
      map.put("created", key.getCreated() != null ? key.getCreated().getTime() : null);
      map.put("lastUsed", key.getLastUsed() != null ? key.getLastUsed().getTime() : null);
      map.put("expiresAt", key.getExpiresAt() != null ? key.getExpiresAt().getTime() : null);
      map.put("expired", key.isExpired());
      return map;
   }
}
