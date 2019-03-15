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

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.oauth.OAuthConfig;
import com.iris.oauth.OAuthUtil;
import com.iris.oauth.app.AppRegistry;
import com.iris.oauth.dao.OAuthDAO;
import com.iris.resource.config.ResourceModule;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;
import com.iris.util.IrisUUID;

@Modules({ResourceModule.class})
@Mocks({OAuthDAO.class})
public class TestAuthorizeHandler extends IrisMockTestCase {

   private AuthorizeHandler handler;
   @Inject private OAuthDAO mockDao;

   @Before
   public void setUp() throws Exception {
      super.setUp();
      OAuthConfig config = new OAuthConfig();
      config.setAppsPath("classpath:///applications.json");
      handler = new AuthorizeHandler(null, null, null, mockDao, new AppRegistry(config));
   }

   @Test
   public void testMissingAttrsErrors() {
      FullHttpResponse response = handler.doRespond(IrisUUID.randomUUID(), ImmutableMap.of(
            OAuthUtil.ATTR_CLIENT_ID, "app2"
      ));
      assertEquals(HttpResponseStatus.FOUND, response.getStatus());
      assertEquals("http://localhost?error=invalid_request", response.headers().get("Location"));
   }

   @Test
   public void testMissingResponseCodeErrors() {
      FullHttpResponse response = handler.doRespond(IrisUUID.randomUUID(), ImmutableMap.of(
            OAuthUtil.ATTR_CLIENT_ID, "app1"
      ));
      assertEquals(HttpResponseStatus.FOUND, response.getStatus());
      assertEquals("http://localhost?error=unsupported_response_type", response.headers().get("Location"));
   }

   @Test
   public void testInvalidResponseCodeErrors() {
      FullHttpResponse response = handler.doRespond(IrisUUID.randomUUID(), ImmutableMap.of(
            OAuthUtil.ATTR_CLIENT_ID, "app1",
            OAuthUtil.ATTR_RESPONSETYPE, "wrong"
      ));
      assertEquals(HttpResponseStatus.FOUND, response.getStatus());
      assertEquals("http://localhost?error=unsupported_response_type", response.headers().get("Location"));
   }

   @Test
   public void testInvalidScopeErrors() {
      FullHttpResponse response = handler.doRespond(IrisUUID.randomUUID(), ImmutableMap.of(
            OAuthUtil.ATTR_CLIENT_ID, "app1",
            OAuthUtil.ATTR_RESPONSETYPE, OAuthUtil.ATTR_CODE,
            OAuthUtil.ATTR_SCOPE, "wrong"
      ));
      assertEquals(HttpResponseStatus.FOUND, response.getStatus());
      assertEquals("http://localhost?error=invalid_scope", response.headers().get("Location"));
   }


   @Test
   public void testInvalidRedirectErrors() {
      UUID personId = IrisUUID.randomUUID();
      UUID placeId = IrisUUID.randomUUID();

      replay();
      FullHttpResponse response = handler.doRespond(personId, ImmutableMap.<String,String>builder()
            .put(OAuthUtil.ATTR_CLIENT_ID, "app2")
            .put(OAuthUtil.ATTR_RESPONSETYPE, OAuthUtil.ATTR_CODE)
            .put(OAuthUtil.ATTR_STATE, "state")
            .put(OAuthUtil.ATTR_REDIRECT_URI, "http://foobar")
            .put(OAuthUtil.ATTR_SCOPE, "app2")
            .put("place", placeId.toString()
      ).build());
      assertEquals(HttpResponseStatus.BAD_REQUEST, response.getStatus());
      verify();
   }

   @Test
   public void testOk() {
      UUID personId = IrisUUID.randomUUID();
      UUID placeId = IrisUUID.randomUUID();

      Map<String,String> attrs = ImmutableMap.of("place", placeId.toString());

      mockDao.insertCode(EasyMock.eq("app2"), EasyMock.anyString(), EasyMock.eq(personId), EasyMock.eq(attrs));
      EasyMock.expectLastCall();
      replay();
      FullHttpResponse response = handler.doRespond(personId, ImmutableMap.<String,String>builder()
            .put(OAuthUtil.ATTR_CLIENT_ID, "app2")
            .put(OAuthUtil.ATTR_RESPONSETYPE, OAuthUtil.ATTR_CODE)
            .put(OAuthUtil.ATTR_STATE, "state")
            .put(OAuthUtil.ATTR_REDIRECT_URI, "http://localhost")
            .put(OAuthUtil.ATTR_SCOPE, "app2")
            .put("place", placeId.toString()
      ).build());
      assertEquals(HttpResponseStatus.FOUND, response.getStatus());
      assertTrue(Pattern.matches("http:\\/\\/localhost\\?state=state\\&code=.*", response.headers().get("Location")));
      verify();
   }

}

