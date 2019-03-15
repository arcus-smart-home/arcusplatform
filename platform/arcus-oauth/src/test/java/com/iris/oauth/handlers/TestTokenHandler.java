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
package com.iris.oauth.handlers;

import com.google.common.collect.ImmutableMap;
import com.iris.oauth.place.NoopPlaceSelectionHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.easymock.EasyMock;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.iris.oauth.OAuthConfig;
import com.iris.oauth.app.AppRegistry;
import com.iris.oauth.dao.OAuthDAO;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.util.IrisUUID;

@Mocks({OAuthDAO.class})
public class TestTokenHandler extends IrisMockTestCase {

   private static final String token = IrisUUID.randomUUID().toString();
   private static final UUID person = IrisUUID.randomUUID();

   private OAuthConfig config;
   private TokenHandler tokenHandler;
   @Inject private OAuthDAO mockDao;
   private Gson gson;

   public void setUp() throws Exception {
      super.setUp();
      config = new OAuthConfig();
      config.setAppsPath("classpath:///applications.json");
      tokenHandler = new TokenHandler(null, null, new AppRegistry(config), mockDao, config, NoopPlaceSelectionHandler.INSTANCE);
      gson = new GsonBuilder().create();
   }

   @Test
   public void testAuthNonMatchingRedirectBadRequest() throws Exception {
      FullHttpResponse response = tokenHandler.doHandleAuthorizationCode(token, "http://foobar", "app1");
      assertEquals(HttpResponseStatus.BAD_REQUEST, response.getStatus());
   }

   @Test
   public void testAuthNoPersonBadRequest() throws Exception {
      EasyMock.expect(mockDao.getPersonWithCode("app1", token)).andReturn(null);
      replay();
      FullHttpResponse response = tokenHandler.doHandleAuthorizationCode(token, "http://localhost", "app1");
      assertEquals(HttpResponseStatus.BAD_REQUEST, response.getStatus());
      verify();
   }

   @Test
   public void testAuthSuccess() throws Exception {
      EasyMock.expect(mockDao.getPersonWithCode("app1", token)).andReturn(new ImmutablePair<>(person, (int) TimeUnit.MINUTES.toSeconds(5)));
      mockDao.updateTokens(EasyMock.eq("app1"), EasyMock.anyString(), EasyMock.anyString(), EasyMock.eq(person));
      EasyMock.expect(mockDao.getAttrs("app1", person)).andReturn(ImmutableMap.of()).anyTimes();
      EasyMock.expectLastCall();
      mockDao.removeCode("app1", token);
      EasyMock.expectLastCall();
      replay();
      FullHttpResponse response = tokenHandler.doHandleAuthorizationCode(token, "http://localhost", "app1");
      String body = new String(response.content().array(), StandardCharsets.UTF_8).trim();
      TokenResponse tokRes = gson.fromJson(body, TokenResponse.class);
      assertNotNull(tokRes.getAccessToken());
      assertNotNull(tokRes.getRefreshToken());
      assertEquals(config.getAccessTtlMinutes(), TimeUnit.MINUTES.convert(tokRes.getExpiresIn(), TimeUnit.SECONDS));
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      verify();
   }

   @Test
   public void testRefreshNoPerson() throws Exception {
      EasyMock.expect(mockDao.getPersonWithRefresh("app1", token)).andReturn(null);
      replay();
      FullHttpResponse response = tokenHandler.doHandleRefreshToken(token, "app1");
      assertEquals(HttpResponseStatus.BAD_REQUEST, response.getStatus());
      verify();
   }

   @Test
   public void testRefreshSuccess() throws Exception {
      EasyMock.expect(mockDao.getPersonWithRefresh("app1", token)).andReturn(new ImmutablePair<>(person, (int) TimeUnit.DAYS.toSeconds(5)));
      mockDao.updateTokens(EasyMock.eq("app1"), EasyMock.anyString(), EasyMock.anyString(), EasyMock.eq(person));
      EasyMock.expectLastCall();
      replay();
      FullHttpResponse response = tokenHandler.doHandleRefreshToken(token, "app1");
      String body = new String(response.content().array(), StandardCharsets.UTF_8).trim();
      TokenResponse tokRes = gson.fromJson(body, TokenResponse.class);
      assertNotNull(tokRes.getAccessToken());
      assertNotNull(tokRes.getRefreshToken());
      assertEquals(config.getAccessTtlMinutes(), TimeUnit.MINUTES.convert(tokRes.getExpiresIn(), TimeUnit.SECONDS));
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      verify();
   }

}

