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
package com.iris.api.server.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.UUID;

import org.apache.shiro.authc.IncorrectCredentialsException;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.iris.core.dao.ApiKeyDAO;
import com.iris.security.apikey.ApiKey;
import com.iris.security.apikey.ApiKeyGenerator;
import com.iris.security.apikey.ApiKeyPrincipal;

public class ApiKeyRealmTest {
   private final String rawKey = ApiKeyGenerator.generate();

   private ApiKeyDAO apiKeyDao;
   private ApiKeyRealm realm;

   @Before
   public void setUp() {
      apiKeyDao = EasyMock.createNiceMock(ApiKeyDAO.class);
      realm = new ApiKeyRealm(apiKeyDao);
   }

   @Test
   public void testSessionKnowsWhenItsKeyExpires() {
      Instant expiresAt = Instant.now().plus(Duration.ofHours(1)).truncatedTo(ChronoUnit.MILLIS);
      expectKey(expiresAt);

      assertEquals(expiresAt, login().getExpiresAt());
   }

   @Test
   public void testKeyWithoutAnExpiryNeverExpires() {
      expectKey(null);

      assertNull(login().getExpiresAt());
   }

   @Test(expected = IncorrectCredentialsException.class)
   public void testExpiredKeyIsRejected() {
      expectKey(Instant.now().minus(Duration.ofMinutes(1)));

      login();
   }

   private void expectKey(Instant expiresAt) {
      ApiKey key = new ApiKey();
      key.setId(UUID.randomUUID());
      key.setPlaceId(UUID.randomUUID());
      key.setPersonId(UUID.randomUUID());
      key.setAccountId(UUID.randomUUID());
      key.setLabel("test");
      key.setKeyHash(ApiKeyGenerator.hashKey(rawKey));
      key.setPermissions(Collections.singleton("*:*:*"));
      key.setExpiresAt(expiresAt);
      EasyMock.expect(apiKeyDao.findByKeyHash(key.getKeyHash())).andReturn(key);
      EasyMock.replay(apiKeyDao);
   }

   private ApiKeyPrincipal login() {
      return (ApiKeyPrincipal) realm.doGetAuthenticationInfo(new ApiKeyToken(rawKey)).getPrincipals().getPrimaryPrincipal();
   }
}
