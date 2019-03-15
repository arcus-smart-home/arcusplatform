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
import static com.iris.messages.model.Fixtures.createPreferences;
import static com.iris.messages.type.CardPreference.ATTR_HIDECARD;
import static com.iris.messages.type.CardPreference.ATTR_SERVICENAME;
import static com.iris.messages.type.CardPreference.SERVICENAME_ALARMS;
import static com.iris.messages.type.CardPreference.SERVICENAME_CAMERAS;
import static com.iris.messages.type.CardPreference.SERVICENAME_CARE;
import static com.iris.messages.type.CardPreference.SERVICENAME_CLIMATE;
import static com.iris.messages.type.CardPreference.SERVICENAME_DOORS_N_LOCKS;
import static com.iris.messages.type.CardPreference.SERVICENAME_FAVORITES;
import static com.iris.messages.type.CardPreference.SERVICENAME_HISTORY;
import static com.iris.messages.type.CardPreference.SERVICENAME_HOME_N_FAMILY;
import static com.iris.messages.type.CardPreference.SERVICENAME_LAWN_N_GARDEN;
import static com.iris.messages.type.CardPreference.SERVICENAME_LIGHTS_N_SWITCHES;
import static com.iris.messages.type.CardPreference.SERVICENAME_SANTA_TRACKER;
import static com.iris.messages.type.CardPreference.SERVICENAME_WATER;
import static com.iris.messages.type.Preferences.ATTR_DASHBOARDCARDS;
import static com.iris.messages.type.Preferences.ATTR_HIDETUTORIALS;
import static com.iris.netty.server.message.GetPreferencesHandler.NAME_EXECUTOR;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.session.Session;
import com.iris.core.dao.PreferencesDAO;
import com.iris.messages.ClientMessage;
import com.iris.messages.service.SessionService.GetPreferencesResponse;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({Session.class, Client.class, PreferencesDAO.class})
public class TestGetPreferencesHandler extends IrisMockTestCase
{
   private static final UUID ACTIVE_PLACE_ID = UUID.fromString("e972a1b7-ec80-448e-8ae7-77da6a034577");
   private static final UUID PRINCIPAL_ID = UUID.fromString("101e036b-3a2b-4524-8f56-6428d0514572");
   private static final String PRINCIPAL_NAME = "Joe User";

   @Inject
   private Session mockSession;

   @Inject
   private Client mockClient;

   @Inject
   private PreferencesDAO mockPreferencesDao;

   @Inject
   private GetPreferencesHandler componentUnderTest;

   @Provides
   @Singleton
   @Named(NAME_EXECUTOR)
   public Executor executor()
   {
      return directExecutor();
   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      expect(mockSession.getActivePlace()).andReturn(ACTIVE_PLACE_ID.toString()).anyTimes();
      expect(mockSession.getClient()).andReturn(mockClient).anyTimes();

      expect(mockClient.getPrincipalName()).andReturn(PRINCIPAL_NAME).anyTimes();
      expect(mockClient.getPrincipalId()).andReturn(PRINCIPAL_ID).anyTimes();
   }

   @Test
   @SuppressWarnings("unchecked")
   public void testNoExistingPrefs()
   {
      test(null,
         ImmutableMap.of(
            ATTR_HIDETUTORIALS, false,
            ATTR_DASHBOARDCARDS, ImmutableList.of(
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_SANTA_TRACKER,     ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_FAVORITES,         ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_HISTORY,           ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_LIGHTS_N_SWITCHES, ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_ALARMS,            ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CLIMATE,           ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_DOORS_N_LOCKS,     ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CAMERAS,           ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CARE,              ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_HOME_N_FAMILY,     ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_LAWN_N_GARDEN,     ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_WATER,             ATTR_HIDECARD, false))));
   }

   @Test
   @SuppressWarnings("unchecked")
   public void testExistingPrefs()
   {
      test(createPreferences(),
         ImmutableMap.of(
            ATTR_HIDETUTORIALS, true,
            ATTR_DASHBOARDCARDS, ImmutableList.of(
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_LIGHTS_N_SWITCHES, ATTR_HIDECARD, true),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CLIMATE,           ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_SANTA_TRACKER,     ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_FAVORITES,         ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_HISTORY,           ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_ALARMS,            ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_DOORS_N_LOCKS,     ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CAMERAS,           ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CARE,              ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_HOME_N_FAMILY,     ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_LAWN_N_GARDEN,     ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_WATER,             ATTR_HIDECARD, false))));
   }

   private void test(Map<String, Object> existingPrefs, Map<String, Object> expectedPrefs)
   {
      expect(mockPreferencesDao.findById(PRINCIPAL_ID, ACTIVE_PLACE_ID)).andReturn(existingPrefs);

      replay();

      ClientMessage request = ClientMessage.builder().create();

      ClientMessage response = componentUnderTest.handle(request, mockSession);

      assertThat(GetPreferencesResponse.getPrefs(response.getPayload()), equalTo(expectedPrefs));
   }

   @Override
   public void tearDown() throws Exception
   {
      verify();

      reset();

      super.tearDown();
   }
}

