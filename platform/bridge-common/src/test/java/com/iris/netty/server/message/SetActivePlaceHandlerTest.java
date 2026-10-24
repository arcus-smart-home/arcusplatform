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
package com.iris.netty.server.message;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.iris.netty.server.message.SetActivePlaceHandler.NAME_EXECUTOR;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.Session;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PreferencesDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.ClientMessage;
import com.iris.messages.service.SessionService;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

// places have no hub, so the camera preview events are skipped and only the session log is under test
@Mocks({Session.class, Client.class, ClientToken.class, HubDAO.class, PreferencesDAO.class, PlaceDAO.class, PlatformMessageBus.class})
public class SetActivePlaceHandlerTest extends IrisMockTestCase
{
   private static final UUID PERSON_ID = UUID.randomUUID();
   private static final UUID OLD_PLACE_ID = UUID.randomUUID();
   private static final UUID NEW_PLACE_ID = UUID.randomUUID();

   @Inject private Session mockSession;
   @Inject private Client mockClient;
   @Inject private ClientToken mockClientToken;
   @Inject private HubDAO mockHubDao;
   @Inject private PreferencesDAO mockPreferencesDao;

   @Inject private SetActivePlaceHandler componentUnderTest;

   private final AtomicReference<String> activePlace = new AtomicReference<>();
   private Logger sessionLogger;
   private Level originalLevel;
   private ListAppender<ILoggingEvent> appender;

   @Provides
   @Singleton
   @Named(NAME_EXECUTOR)
   public Executor executor()
   {
      return directExecutor();
   }

   @Provides
   @Named("video.preview.upload.url")
   public String videoPreviewUploadUrl()
   {
      return "https://preview.example.com/upload";
   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      sessionLogger = (Logger) LoggerFactory.getLogger("session");
      originalLevel = sessionLogger.getLevel();
      sessionLogger.setLevel(Level.INFO);
      appender = new ListAppender<>();
      appender.start();
      sessionLogger.addAppender(appender);

      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setPlaceId(NEW_PLACE_ID);
      grant.addPermissions("*:*:*");
      AuthorizationContext context = new AuthorizationContext(null, null, ImmutableList.of(grant));

      expect(mockSession.getClient()).andReturn(mockClient).anyTimes();
      expect(mockSession.getClientToken()).andReturn(mockClientToken).anyTimes();
      expect(mockSession.getAuthorizationContext(true)).andReturn(context).anyTimes();
      expect(mockSession.getChannel()).andReturn(null).anyTimes();
      expect(mockSession.getActivePlace()).andAnswer(activePlace::get).anyTimes();
      mockSession.setActivePlace(EasyMock.anyString());
      expectLastCall().andAnswer(() -> {
         activePlace.set(EasyMock.getCurrentArgument(0));
         return null;
      }).anyTimes();

      expect(mockClient.getPrincipalId()).andReturn(PERSON_ID).anyTimes();
      expect(mockClient.getPrincipalName()).andReturn("person").anyTimes();
      expect(mockClientToken.getRepresentation()).andReturn("token").anyTimes();
      expect(mockPreferencesDao.findById(PERSON_ID, NEW_PLACE_ID)).andReturn(ImmutableMap.of()).anyTimes();
      expect(mockHubDao.findHubForPlace(anyObject(UUID.class))).andReturn(null).anyTimes();
   }

   @Override
   public void tearDown() throws Exception
   {
      sessionLogger.detachAppender(appender);
      sessionLogger.setLevel(originalLevel);
      super.tearDown();
   }

   @Test
   public void testFirstSetLogsNewPlace()
   {
      replay();

      ClientMessage response = componentUnderTest.handle(request(NEW_PLACE_ID), mockSession);

      verify();
      assertEquals(SessionService.SetActivePlaceResponse.NAME, response.getType());
      assertEquals(NEW_PLACE_ID.toString(), activePlace.get());
      assertSessionLog("place:" + NEW_PLACE_ID, "message:Set active place");
      assertNoPreviousPlace();
   }

   @Test
   public void testSwitchLogsNewAndPreviousPlace()
   {
      activePlace.set(OLD_PLACE_ID.toString());
      replay();

      componentUnderTest.handle(request(NEW_PLACE_ID), mockSession);

      verify();
      assertSessionLog("place:" + NEW_PLACE_ID, "message:Set active place (was " + OLD_PLACE_ID + ")");
   }

   @Test
   public void testReselectSamePlaceDoesNotNamePreviousPlace()
   {
      activePlace.set(NEW_PLACE_ID.toString());
      replay();

      componentUnderTest.handle(request(NEW_PLACE_ID), mockSession);

      verify();
      assertSessionLog("place:" + NEW_PLACE_ID, "message:Set active place");
      assertNoPreviousPlace();
   }

   private static ClientMessage request(UUID placeId)
   {
      return ClientMessage.builder()
         .withPayload(SessionService.SetActivePlaceRequest.builder().withPlaceId(placeId.toString()).build())
         .create();
   }

   private void assertSessionLog(String... fields)
   {
      assertEquals(1, appender.list.size());
      String line = appender.list.get(0).getFormattedMessage();
      for (String field : fields)
      {
         assertTrue("expected [" + field + "] in [" + line + "]", line.contains(field));
      }
   }

   private void assertNoPreviousPlace()
   {
      assertFalse(appender.list.get(0).getFormattedMessage().contains("(was "));
   }
}
