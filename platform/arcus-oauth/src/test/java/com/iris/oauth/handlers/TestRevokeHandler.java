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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.oauth.dao.OAuthDAO;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.util.IrisUUID;

@Mocks({OAuthDAO.class})
public class TestRevokeHandler extends IrisMockTestCase {

   private static final String AUTH_HEADER = "Basic YXBwMTphcHAx";
   private static final String token = IrisUUID.randomUUID().toString();
   private static final UUID person = IrisUUID.randomUUID();

   private RevokeHandler handler;
   @Inject private OAuthDAO mockDao;

   @Before
   public void setUp() throws Exception {
      super.setUp();
      handler = new RevokeHandler(null, null, mockDao, NoopPlaceSelectionHandler.INSTANCE);
   }

   @Test
   public void testTokenRequired() {
      FullHttpResponse response = handler.doRevoke(AUTH_HEADER, null);
      assertEquals(HttpResponseStatus.BAD_REQUEST, response.getStatus());
   }

   @Test
   public void testRemovedViaRefresh() {
      EasyMock.expect(mockDao.getPersonWithRefresh("app1", token)).andReturn(new ImmutablePair<>(person, (int) TimeUnit.DAYS.toSeconds(5)));
      EasyMock.expect(mockDao.getAttrs("app1", person)).andReturn(ImmutableMap.of()).anyTimes();
      mockDao.removePersonAndTokens("app1", person);
      EasyMock.expectLastCall();
      replay();
      FullHttpResponse response = handler.doRevoke(AUTH_HEADER, token);
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      verify();
   }

   @Test
   public void testRemovedViaAccess() {
      EasyMock.expect(mockDao.getPersonWithRefresh("app1", token)).andReturn(null);
      EasyMock.expect(mockDao.getPersonWithAccess("app1", token)).andReturn(new ImmutablePair<>(person, (int) TimeUnit.DAYS.toSeconds(5)));
      EasyMock.expect(mockDao.getAttrs("app1", person)).andReturn(ImmutableMap.of()).anyTimes();
      mockDao.removePersonAndTokens("app1", person);
      EasyMock.expectLastCall();
      replay();
      FullHttpResponse response = handler.doRevoke(AUTH_HEADER, token);
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      verify();
   }

   @Test
   public void testNoPersonFound() {
      EasyMock.expect(mockDao.getPersonWithRefresh("app1", token)).andReturn(null);
      EasyMock.expect(mockDao.getPersonWithAccess("app1", token)).andReturn(null);
      replay();
      FullHttpResponse response = handler.doRevoke(AUTH_HEADER, token);
      assertEquals(HttpResponseStatus.OK, response.getStatus());
      verify();
   }
}

