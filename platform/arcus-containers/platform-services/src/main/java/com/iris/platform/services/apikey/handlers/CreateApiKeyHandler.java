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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.permission.WildcardPermission;

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
import com.iris.messages.service.ApiKeyService;
import com.iris.messages.model.Place;
import com.iris.security.apikey.ApiKey;
import com.iris.security.apikey.ApiKeyGenerator;
import com.iris.security.authz.permission.PermissionCode;

@Singleton
public class CreateApiKeyHandler implements ContextualRequestMessageHandler<Place> {

   public static final String MESSAGE_TYPE = "apikey:Create";
   public static final int MAX_KEYS_PER_PLACE = 10;
   public static final int MAX_LABEL_LENGTH = 64;

   // The authorizer only ever asks for one of the PermissionCode operations, so a permission whose second
   // part names none of them, such as device:get, is accepted by Shiro but can never allow anything.
   private static final Set<String> OPERATIONS = Stream
         .concat(Stream.of("*"), Arrays.stream(PermissionCode.values()).map(PermissionCode::name))
         .collect(Collectors.toSet());

   private final ApiKeyDAO apiKeyDao;
   private final AccountDAO accountDao;
   private final PlatformMessageBus bus;

   @Inject
   public CreateApiKeyHandler(ApiKeyDAO apiKeyDao, AccountDAO accountDao, PlatformMessageBus bus) {
      this.apiKeyDao = apiKeyDao;
      this.accountDao = accountDao;
      this.bus = bus;
   }

   @Override
   public String getMessageType() {
      return MESSAGE_TYPE;
   }

   @SuppressWarnings("unchecked")
   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      AccountOwnerCheck.requireAccountOwner(accountDao, context, msg);

      MessageBody body = msg.getValue();

      String label = StringUtils.trimToNull((String) body.getAttributes().get("label"));
      if (label == null) {
         throw new ErrorEventException(Errors.CODE_MISSING_PARAM, "label is required");
      }
      if (label.length() > MAX_LABEL_LENGTH) {
         throw new ErrorEventException(Errors.CODE_INVALID_PARAM, "label must be " + MAX_LABEL_LENGTH + " characters or less");
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

      for (String perm : permissions) {
         if (StringUtils.isBlank(perm)) {
            throw new ErrorEventException(Errors.CODE_INVALID_PARAM, "permission strings must not be blank");
         }
         ParsedPermission parsed;
         try {
            parsed = new ParsedPermission(perm);
         } catch (IllegalArgumentException e) {
            throw new ErrorEventException(Errors.CODE_INVALID_PARAM, "invalid permission format: " + perm);
         }
         if (parsed.partCount() > 3) {
            throw new ErrorEventException(Errors.CODE_INVALID_PARAM,
                  "invalid permission " + perm + ": use at most three parts, namespace:operation:object id, as in dev:r:*");
         }
         if (!parsed.canMatch()) {
            throw new ErrorEventException(Errors.CODE_INVALID_PARAM,
                  "invalid permission " + perm + ": the second part must be *, r, w, x, c or d, as in dev:r:*");
         }
      }

      // Enforce per-place key limit
      List<ApiKey> existing = apiKeyDao.findByPlace(context.getId());
      if (existing.size() >= MAX_KEYS_PER_PLACE) {
         throw new ErrorEventException("apikey.limit_reached",
               "Maximum of " + MAX_KEYS_PER_PLACE + " API keys per place");
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
      apiKey.setPersonId((UUID) msg.getActor().getId());
      apiKey.setAccountId(context.getAccount());
      apiKey.setPermissions(permissions);
      apiKey.setCreated(Instant.now());

      // Optional expiration, in milliseconds since the epoch. A value that isn't one is an error, never a key
      // that doesn't expire.
      Date expiresAtDate;
      try {
         expiresAtDate = ApiKeyService.CreateRequest.getExpiresAt(body);
      } catch (IllegalArgumentException e) {
         throw new ErrorEventException(Errors.CODE_INVALID_PARAM, "expiresAt must be a time in milliseconds since the epoch");
      }
      if (expiresAtDate != null) {
         Instant expiresAt = expiresAtDate.toInstant();
         if (!expiresAt.isAfter(Instant.now())) {
            throw new ErrorEventException(Errors.CODE_INVALID_PARAM, "expiresAt must be in the future");
         }
         apiKey.setExpiresAt(expiresAt);
      }

      apiKeyDao.save(apiKey);

      // Broadcast so the history service can record who created the key
      emitApiKeyCreated(context, msg.getActor(), apiKey);

      return MessageBody.buildMessage(
            "apikey:CreateResponse",
            ImmutableMap.of(
                  "id", keyId.toString(),
                  "key", rawKey
            )
      );
   }

   private void emitApiKeyCreated(Place place, Address actor, ApiKey apiKey) {
      MessageBody body = ApiKeyService.CreatedEvent.builder()
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

   /** Parsed the way the authorizer parses a key's permissions. */
   @SuppressWarnings("serial")
   private static class ParsedPermission extends WildcardPermission {
      ParsedPermission(String permission) {
         super(permission);
      }

      // The authorizer always asks with three parts, and Shiro never matches a request against a longer
      // permission whose extra parts aren't *, so dev:r:*:x allows nothing.
      int partCount() {
         return getParts().size();
      }

      boolean canMatch() {
         List<Set<String>> parts = getParts();
         return parts.size() < 2 || parts.get(1).stream().anyMatch(OPERATIONS::contains);
      }
   }
}
