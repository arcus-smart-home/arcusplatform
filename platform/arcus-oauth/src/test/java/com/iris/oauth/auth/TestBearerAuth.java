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
package com.iris.oauth.auth;

import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.iris.oauth.dao.OAuthDAO;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.util.IrisUUID;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.easymock.EasyMock;
import org.junit.Test;

@Mocks({OAuthDAO.class})
public class TestBearerAuth extends IrisMockTestCase {

   @Inject
   private OAuthDAO oauthDao;

   private BearerAuth auth;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      auth = new BearerAuth(oauthDao, null, "app1");
   }

   @Test
   public void testIsAuthorizedNoHeader() {
      DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "http://localhost");
      assertFalse(auth.isAuthorized(null, req));
   }

   @Test
   public void testIsAuthorizedNotBearer() {
      DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "http://localhost");
      req.headers().add(HttpHeaders.AUTHORIZATION, "Basic foobar");
      assertFalse(auth.isAuthorized(null, req));
   }

   @Test
   public void testIsAuthorizedNoPersonMatchesToken() {
      EasyMock.expect(oauthDao.getPersonWithAccess("app1", "foobar")).andReturn(null);
      replay();

      DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "http://localhost");
      req.headers().add(HttpHeaders.AUTHORIZATION, "Bearer foobar");

      assertFalse(auth.isAuthorized(null, req));
   }

   @Test
   public void testIsAuthorized() {
      EasyMock.expect(oauthDao.getPersonWithAccess("app1", "foobar")).andReturn(new ImmutablePair<>(IrisUUID.randomUUID(), 5000));
      replay();

      DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "http://localhost");
      req.headers().add(HttpHeaders.AUTHORIZATION, "Bearer foobar");

      assertTrue(auth.isAuthorized(null, req));
   }
}

