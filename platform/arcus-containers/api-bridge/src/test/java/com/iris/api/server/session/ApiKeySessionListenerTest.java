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
package com.iris.api.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.iris.bridge.server.netty.Constants;
import com.iris.bridge.server.session.Session;
import com.iris.core.dao.ApiKeyDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.security.apikey.ApiKey;
import com.iris.security.apikey.ApiKeyPrincipal;
import com.iris.security.authz.AuthorizationContext;

import io.netty.channel.embedded.EmbeddedChannel;

public class ApiKeySessionListenerTest {
   private final UUID keyId = UUID.randomUUID();
   private final UUID placeId = UUID.randomUUID();

   private EmbeddedChannel channel;
   private Session session;
   private ApiKeyDAO apiKeyDao;
   private ApiKeySessionListener listener;
   private final AtomicInteger disconnects = new AtomicInteger();
   private final AtomicInteger destroys = new AtomicInteger();

   @Before
   public void setUp() {
      channel = new EmbeddedChannel();
      channel.freezeTime();
      session = EasyMock.createMock(Session.class);
      apiKeyDao = EasyMock.createMock(ApiKeyDAO.class);
      PlaceDAO placeDao = EasyMock.createNiceMock(PlaceDAO.class);
      EasyMock.replay(placeDao);
      listener = new ApiKeySessionListener(placeDao, apiKeyDao);
   }

   @After
   public void tearDown() {
      channel.finishAndReleaseAll();
   }

   @Test
   public void testSessionClosesWhenItsKeyWasRevokedBeforeTheSessionRegistered() {
      AtomicBoolean checked = new AtomicBoolean();
      expectSession(principal(null));
      EasyMock.expect(apiKeyDao.findByPlace(placeId)).andAnswer(() -> {
         checked.set(true);
         return keys(Instant.now().minusSeconds(1));
      });
      replay();

      listener.onSessionCreated(session);
      // the bridge registers the session after the listeners return, so the check waits for that
      assertFalse(checked.get());

      channel.runPendingTasks();
      assertTrue(checked.get());
      assertClosed();
   }

   @Test
   public void testSessionClosesWhenItsKeyWasDeleted() {
      expectSession(principal(null));
      EasyMock.expect(apiKeyDao.findByPlace(placeId)).andReturn(Collections.emptyList());
      replay();

      listener.onSessionCreated(session);
      channel.runPendingTasks();

      assertClosed();
   }

   @Test
   public void testSessionClosesWhenTheKeyCannotBeChecked() {
      expectSession(principal(null));
      EasyMock.expect(apiKeyDao.findByPlace(placeId)).andThrow(new IllegalStateException("test"));
      replay();

      listener.onSessionCreated(session);
      channel.runPendingTasks();

      assertClosed();
   }

   @Test
   public void testSessionStaysOpenWhileItsKeyIsValid() {
      expectSession(principal(null));
      EasyMock.expect(apiKeyDao.findByPlace(placeId)).andReturn(keys(null));
      replay();

      listener.onSessionCreated(session);
      channel.runPendingTasks();
      channel.advanceTimeBy(365, TimeUnit.DAYS);
      channel.runScheduledPendingTasks();

      assertOpen();
   }

   @Test
   public void testSessionClosesWhenItsKeyExpires() {
      Instant expiresAt = Instant.now().plus(Duration.ofHours(1));
      expectSession(principal(expiresAt));
      EasyMock.expect(apiKeyDao.findByPlace(placeId)).andReturn(keys(expiresAt));
      replay();

      listener.onSessionCreated(session);
      channel.runPendingTasks();
      channel.advanceTimeBy(59, TimeUnit.MINUTES);
      channel.runScheduledPendingTasks();
      assertOpen();

      channel.advanceTimeBy(2, TimeUnit.MINUTES);
      channel.runScheduledPendingTasks();
      assertClosed();
   }

   @Test
   public void testExpiryTimerStopsWhenTheConnectionCloses() {
      Instant expiresAt = Instant.now().plus(Duration.ofHours(1));
      expectSession(principal(expiresAt));
      EasyMock.expect(apiKeyDao.findByPlace(placeId)).andReturn(keys(expiresAt));
      replay();

      listener.onSessionCreated(session);
      channel.runPendingTasks();
      channel.close();
      channel.advanceTimeBy(2, TimeUnit.HOURS);
      channel.runScheduledPendingTasks();

      assertOpen();
   }

   private ApiKeyPrincipal principal(Instant expiresAt) {
      return new ApiKeyPrincipal(keyId, "test", placeId, UUID.randomUUID(), UUID.randomUUID(), Collections.singleton("*:*:*"), expiresAt);
   }

   private List<ApiKey> keys(Instant expiresAt) {
      ApiKey key = new ApiKey();
      key.setId(keyId);
      key.setPlaceId(placeId);
      key.setExpiresAt(expiresAt);
      ApiKey other = new ApiKey();
      other.setId(UUID.randomUUID());
      other.setPlaceId(placeId);
      return Arrays.asList(other, key);
   }

   private void expectSession(ApiKeyPrincipal principal) {
      AuthorizationContext context = new AuthorizationContext(principal, null, Collections.emptyList());
      EasyMock.expect(session.getAuthorizationContext()).andReturn(context).anyTimes();
      EasyMock.expect(session.getChannel()).andReturn(channel).anyTimes();
      session.setActivePlace(placeId.toString());
      EasyMock.expectLastCall();
      session.sendMessage(EasyMock.anyString());
      EasyMock.expectLastCall();
      session.disconnect(Constants.SESSION_EXPIRED_STATUS);
      EasyMock.expectLastCall().andAnswer(() -> { disconnects.incrementAndGet(); return null; }).anyTimes();
      session.destroy();
      EasyMock.expectLastCall().andAnswer(() -> { destroys.incrementAndGet(); return null; }).anyTimes();
   }

   private void replay() {
      EasyMock.replay(session, apiKeyDao);
   }

   private void assertClosed() {
      assertEquals(1, disconnects.get());
      assertEquals(1, destroys.get());
      EasyMock.verify(session, apiKeyDao);
   }

   private void assertOpen() {
      assertEquals(0, disconnects.get());
      assertEquals(0, destroys.get());
   }
}
