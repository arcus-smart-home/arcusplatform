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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.dao.ApiKeyDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.model.Place;
import com.iris.security.apikey.ApiKey;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({ApiKeyDAO.class})
@Modules({InMemoryMessageModule.class})
public class TestListApiKeysHandler extends IrisMockTestCase {

   @Inject ApiKeyDAO apiKeyDao;

   private ListApiKeysHandler handler;
   private Place place;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      handler = new ListApiKeysHandler(apiKeyDao);

      place = new Place();
      place.setId(UUID.randomUUID());
      place.setAccount(UUID.randomUUID());
      place.setName("Test Place");
      place.setPopulation("general");
   }

   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

   @Test
   public void testListEmpty() {
      EasyMock.expect(apiKeyDao.findByPlace(place.getId())).andReturn(Collections.emptyList());
      replay();

      PlatformMessage msg = buildRequest();

      MessageBody response = handler.handleRequest(place, msg);
      assertEquals("apikey:ListKeysResponse", response.getMessageType());

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> keys = (List<Map<String, Object>>) response.getAttributes().get("keys");
      assertNotNull(keys);
      assertTrue(keys.isEmpty());
   }

   @SuppressWarnings("unchecked")
   @Test
   public void testListMultipleKeys() {
      UUID key1Id = UUID.randomUUID();
      UUID key2Id = UUID.randomUUID();
      Instant created1 = Instant.ofEpochMilli(1700000000000L);
      Instant created2 = Instant.ofEpochMilli(1700100000000L);
      Instant lastUsed = Instant.ofEpochMilli(1700200000000L);
      Instant expiresAt = Instant.ofEpochMilli(1600000000000L); // in the past = expired

      ApiKey key1 = new ApiKey();
      key1.setId(key1Id);
      key1.setPlaceId(place.getId());
      key1.setLabel("integration-1");
      key1.setKeyPrefix("arcus_sk_0a1b2c3d");
      key1.setKeyHash("hash1");
      key1.setPermissions(new HashSet<>(Arrays.asList("device:*", "scene:*")));
      key1.setCreated(created1);

      ApiKey key2 = new ApiKey();
      key2.setId(key2Id);
      key2.setPlaceId(place.getId());
      key2.setLabel("integration-2");
      key2.setKeyPrefix("arcus_sk_4e5f6a7b");
      key2.setKeyHash("hash2");
      key2.setPermissions(new HashSet<>(Collections.singletonList("device:get")));
      key2.setCreated(created2);
      key2.setLastUsed(lastUsed);
      key2.setExpiresAt(expiresAt);

      EasyMock.expect(apiKeyDao.findByPlace(place.getId())).andReturn(Arrays.asList(key1, key2));
      replay();

      PlatformMessage msg = buildRequest();

      MessageBody response = handler.handleRequest(place, msg);
      assertEquals("apikey:ListKeysResponse", response.getMessageType());

      List<Map<String, Object>> keys = (List<Map<String, Object>>) response.getAttributes().get("keys");
      assertNotNull(keys);
      assertEquals(2, keys.size());

      Map<String, Object> map1 = keys.get(0);
      assertEquals(key1Id.toString(), map1.get("id"));
      assertEquals("integration-1", map1.get("label"));
      assertEquals("arcus_sk_0a1b2c3d", map1.get("keyPrefix"));
      assertEquals(created1.toEpochMilli(), map1.get("created"));
      assertNull(map1.get("lastUsed"));
      assertNull(map1.get("expiresAt"));
      assertEquals(false, map1.get("expired"));

      Map<String, Object> map2 = keys.get(1);
      assertEquals(key2Id.toString(), map2.get("id"));
      assertEquals("integration-2", map2.get("label"));
      assertEquals("arcus_sk_4e5f6a7b", map2.get("keyPrefix"));
      assertEquals(created2.toEpochMilli(), map2.get("created"));
      assertEquals(lastUsed.toEpochMilli(), map2.get("lastUsed"));
      assertEquals(expiresAt.toEpochMilli(), map2.get("expiresAt"));
      assertEquals(true, map2.get("expired"));
   }

   private PlatformMessage buildRequest() {
      MessageBody body = MessageBody.buildMessage(ListApiKeysHandler.MESSAGE_TYPE, Collections.emptyMap());
      return PlatformMessage.buildMessage(
            body,
            Address.clientAddress("test", "1"),
            Address.fromString(place.getAddress()))
            .create();
   }
}
