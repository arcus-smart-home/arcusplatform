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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.ApiKeyDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Place;
import com.iris.security.apikey.ApiKey;
import com.iris.security.authz.AuthzUtil;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({ApiKeyDAO.class, AccountDAO.class})
@Modules({InMemoryMessageModule.class})
public class TestRevokeApiKeyHandler extends IrisMockTestCase {

   @Inject ApiKeyDAO apiKeyDao;
   @Inject AccountDAO accountDao;
   @Inject InMemoryPlatformMessageBus messageBus;

   private RevokeApiKeyHandler handler;
   private Place place;
   private Account account;
   private UUID ownerId;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      handler = new RevokeApiKeyHandler(apiKeyDao, accountDao, messageBus);

      ownerId = UUID.randomUUID();

      account = new Account();
      account.setId(UUID.randomUUID());
      account.setOwner(ownerId);

      place = new Place();
      place.setId(UUID.randomUUID());
      place.setAccount(account.getId());
      place.setName("Test Place");
      place.setPopulation("general");
   }

   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

   @Test
   public void testRevokeSuccess() throws Exception {
      UUID keyId = UUID.randomUUID();
      ApiKey existing = createActiveKey(keyId);

      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      EasyMock.expect(apiKeyDao.findByPlace(place.getId())).andReturn(Collections.singletonList(existing));
      apiKeyDao.expire(EasyMock.eq(place.getId()), EasyMock.eq(keyId), EasyMock.eq("keyhash123"), EasyMock.isA(Instant.class));
      EasyMock.expectLastCall();
      replay();

      PlatformMessage msg = buildRequest(ownerId, keyId.toString());
      MessageBody response = handler.handleRequest(place, msg);

      assertEquals("apikey:RevokeResponse", response.getMessageType());

      // Verify broadcast event was sent
      PlatformMessage event = messageBus.take();
      assertNotNull(event);
      assertEquals(RevokeApiKeyHandler.EVENT_API_KEY_REVOKED, event.getMessageType());
      assertEquals(keyId.toString(), event.getValue().getAttributes().get("keyId"));
   }

   @Test
   public void testRevokeMissingId() {
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      replay();

      PlatformMessage msg = buildRequest(ownerId, null);

      try {
         handler.handleRequest(place, msg);
         fail("Expected ErrorEventException");
      } catch (ErrorEventException e) {
         assertEquals(Errors.CODE_MISSING_PARAM, e.getCode());
      }
   }

   @Test
   public void testRevokeInvalidUuid() {
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      replay();

      PlatformMessage msg = buildRequest(ownerId, "not-a-uuid");

      try {
         handler.handleRequest(place, msg);
         fail("Expected ErrorEventException");
      } catch (ErrorEventException e) {
         assertEquals(Errors.CODE_INVALID_PARAM, e.getCode());
      }
   }

   @Test
   public void testRevokeNotFound() {
      UUID keyId = UUID.randomUUID();

      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      EasyMock.expect(apiKeyDao.findByPlace(place.getId())).andReturn(Collections.emptyList());
      replay();

      PlatformMessage msg = buildRequest(ownerId, keyId.toString());

      try {
         handler.handleRequest(place, msg);
         fail("Expected ErrorEventException");
      } catch (ErrorEventException e) {
         assertEquals(Errors.CODE_NOT_FOUND, e.getCode());
      }
   }

   @Test
   public void testRevokeAlreadyRevoked() {
      UUID keyId = UUID.randomUUID();
      ApiKey existing = createExpiredKey(keyId);

      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      EasyMock.expect(apiKeyDao.findByPlace(place.getId())).andReturn(Collections.singletonList(existing));
      replay();

      PlatformMessage msg = buildRequest(ownerId, keyId.toString());

      try {
         handler.handleRequest(place, msg);
         fail("Expected ErrorEventException");
      } catch (ErrorEventException e) {
         assertEquals(Errors.CODE_INVALID_PARAM, e.getCode());
      }
   }

   @Test
   public void testRevokeNotOwner() {
      UUID nonOwnerId = UUID.randomUUID();
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      replay();

      PlatformMessage msg = buildRequest(nonOwnerId, UUID.randomUUID().toString());

      try {
         handler.handleRequest(place, msg);
         fail("Expected ErrorEventException");
      } catch (ErrorEventException e) {
         assertEquals(AuthzUtil.UNAUTHORIZED_CODE, e.getCode());
      }
   }

   private ApiKey createActiveKey(UUID keyId) {
      ApiKey key = new ApiKey();
      key.setId(keyId);
      key.setPlaceId(place.getId());
      key.setLabel("test-key");
      key.setKeyPrefix("arcus_sk_0a1b2c3d");
      key.setKeyHash("keyhash123");
      key.setCreated(Instant.now());
      // no expiresAt = not expired
      return key;
   }

   private ApiKey createExpiredKey(UUID keyId) {
      ApiKey key = createActiveKey(keyId);
      key.setExpiresAt(Instant.ofEpochMilli(System.currentTimeMillis() - 86400000L)); // expired yesterday
      return key;
   }

   private PlatformMessage buildRequest(UUID actorId, String keyId) {
      Map<String, Object> attrs = new HashMap<>();
      if (keyId != null) {
         attrs.put("id", keyId);
      }

      MessageBody body = MessageBody.buildMessage(RevokeApiKeyHandler.MESSAGE_TYPE, attrs);
      return PlatformMessage.buildMessage(
            body,
            Address.clientAddress("test", "1"),
            Address.fromString(place.getAddress()))
            .withActor(Address.platformService(actorId, "person"))
            .create();
   }
}
