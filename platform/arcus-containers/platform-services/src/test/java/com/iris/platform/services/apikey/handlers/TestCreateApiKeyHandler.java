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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.ApiKeyDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
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
public class TestCreateApiKeyHandler extends IrisMockTestCase {

   @Inject ApiKeyDAO apiKeyDao;
   @Inject AccountDAO accountDao;

   private CreateApiKeyHandler handler;
   private Place place;
   private Account account;
   private UUID ownerId;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      handler = new CreateApiKeyHandler(apiKeyDao, accountDao);

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
   public void testCreateSuccess() {
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      EasyMock.expect(apiKeyDao.findByPlace(place.getId())).andReturn(Collections.emptyList());
      Capture<ApiKey> savedKey = EasyMock.newCapture();
      apiKeyDao.save(EasyMock.capture(savedKey));
      EasyMock.expectLastCall();
      replay();

      Set<String> perms = new HashSet<>();
      perms.add("device:*");
      PlatformMessage msg = buildRequest(ownerId, "test-label", perms, null);

      MessageBody response = handler.handleRequest(place, msg);
      assertEquals("apikey:CreateResponse", response.getMessageType());
      assertNotNull(response.getAttributes().get("id"));
      assertNotNull(response.getAttributes().get("key"));
      String key = (String) response.getAttributes().get("key");
      assertTrue(key.startsWith("arcus_sk_"));

      ApiKey captured = savedKey.getValue();
      assertEquals("test-label", captured.getLabel());
      assertEquals(place.getId(), captured.getPlaceId());
      assertTrue(captured.getPermissions().contains("device:*"));
      assertNull(captured.getExpiresAt());
   }

   @Test
   public void testCreateWithExpiration() {
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      EasyMock.expect(apiKeyDao.findByPlace(place.getId())).andReturn(Collections.emptyList());
      Capture<ApiKey> savedKey = EasyMock.newCapture();
      apiKeyDao.save(EasyMock.capture(savedKey));
      EasyMock.expectLastCall();
      replay();

      Set<String> perms = new HashSet<>();
      perms.add("device:*");
      long futureTime = System.currentTimeMillis() + 86400000L; // 1 day from now
      PlatformMessage msg = buildRequest(ownerId, "expiring-key", perms, futureTime);

      MessageBody response = handler.handleRequest(place, msg);
      assertEquals("apikey:CreateResponse", response.getMessageType());

      ApiKey captured = savedKey.getValue();
      assertNotNull(captured.getExpiresAt());
      assertEquals(futureTime, captured.getExpiresAt().toEpochMilli());
   }

   @Test
   public void testCreateMissingLabel() {
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      replay();

      Set<String> perms = new HashSet<>();
      perms.add("device:*");
      PlatformMessage msg = buildRequest(ownerId, null, perms, null);

      try {
         handler.handleRequest(place, msg);
         fail("Expected ErrorEventException");
      } catch (ErrorEventException e) {
         assertEquals(Errors.CODE_MISSING_PARAM, e.getCode());
      }
   }

   @Test
   public void testCreateLabelTooLong() {
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      replay();

      Set<String> perms = new HashSet<>();
      perms.add("device:*");
      // 65-char label
      String longLabel = String.join("", Collections.nCopies(65, "a"));
      PlatformMessage msg = buildRequest(ownerId, longLabel, perms, null);

      try {
         handler.handleRequest(place, msg);
         fail("Expected ErrorEventException");
      } catch (ErrorEventException e) {
         assertEquals(Errors.CODE_INVALID_PARAM, e.getCode());
      }
   }

   @Test
   public void testCreateMissingPermissions() {
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      replay();

      PlatformMessage msg = buildRequest(ownerId, "test-label", null, null);

      try {
         handler.handleRequest(place, msg);
         fail("Expected ErrorEventException");
      } catch (ErrorEventException e) {
         assertEquals(Errors.CODE_MISSING_PARAM, e.getCode());
      }
   }

   @Test
   public void testCreateEmptyPermissions() {
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      replay();

      PlatformMessage msg = buildRequest(ownerId, "test-label", Collections.emptySet(), null);

      try {
         handler.handleRequest(place, msg);
         fail("Expected ErrorEventException");
      } catch (ErrorEventException e) {
         assertEquals(Errors.CODE_INVALID_PARAM, e.getCode());
      }
   }

   @Test
   public void testCreateInvalidPermission() {
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      replay();

      Set<String> perms = new HashSet<>();
      perms.add(""); // blank permission
      PlatformMessage msg = buildRequest(ownerId, "test-label", perms, null);

      try {
         handler.handleRequest(place, msg);
         fail("Expected ErrorEventException");
      } catch (ErrorEventException e) {
         assertEquals(Errors.CODE_INVALID_PARAM, e.getCode());
      }
   }

   @Test
   public void testCreateExceedsKeyLimit() {
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      List<ApiKey> existingKeys = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
         ApiKey key = new ApiKey();
         key.setId(UUID.randomUUID());
         existingKeys.add(key);
      }
      EasyMock.expect(apiKeyDao.findByPlace(place.getId())).andReturn(existingKeys);
      replay();

      Set<String> perms = new HashSet<>();
      perms.add("device:*");
      PlatformMessage msg = buildRequest(ownerId, "test-label", perms, null);

      try {
         handler.handleRequest(place, msg);
         fail("Expected ErrorEventException");
      } catch (ErrorEventException e) {
         assertEquals("apikey.limit_reached", e.getCode());
      }
   }

   @Test
   public void testCreateNotOwner() {
      UUID nonOwnerId = UUID.randomUUID();
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      replay();

      Set<String> perms = new HashSet<>();
      perms.add("device:*");
      PlatformMessage msg = buildRequest(nonOwnerId, "test-label", perms, null);

      try {
         handler.handleRequest(place, msg);
         fail("Expected ErrorEventException");
      } catch (ErrorEventException e) {
         assertEquals(AuthzUtil.UNAUTHORIZED_CODE, e.getCode());
      }
   }

   @Test
   public void testCreateExpiredInPast() {
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(account);
      EasyMock.expect(apiKeyDao.findByPlace(place.getId())).andReturn(Collections.emptyList());
      replay();

      Set<String> perms = new HashSet<>();
      perms.add("device:*");
      long pastTime = System.currentTimeMillis() - 86400000L; // 1 day ago
      PlatformMessage msg = buildRequest(ownerId, "test-label", perms, pastTime);

      try {
         handler.handleRequest(place, msg);
         fail("Expected ErrorEventException");
      } catch (ErrorEventException e) {
         assertEquals(Errors.CODE_INVALID_PARAM, e.getCode());
      }
   }

   private PlatformMessage buildRequest(UUID actorId, String label, Set<String> permissions, Long expiresAt) {
      Map<String, Object> attrs = new HashMap<>();
      if (label != null) {
         attrs.put("label", label);
      }
      if (permissions != null) {
         attrs.put("permissions", permissions);
      }
      if (expiresAt != null) {
         attrs.put("expiresAt", expiresAt);
      }

      MessageBody body = MessageBody.buildMessage(CreateApiKeyHandler.MESSAGE_TYPE, attrs);
      return PlatformMessage.buildMessage(
            body,
            Address.clientAddress("test", "1"),
            Address.fromString(place.getAddress()))
            .withActor(Address.platformService(actorId, "person"))
            .create();
   }
}
