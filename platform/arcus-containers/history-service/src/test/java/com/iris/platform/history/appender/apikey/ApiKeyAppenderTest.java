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
package com.iris.platform.history.appender.apikey;

import java.util.UUID;

import org.easymock.Capture;
import org.junit.After;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.service.ApiKeyService.CreatedEvent;
import com.iris.messages.service.ApiKeyService.RevokedEvent;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.HistoryLogEntryType;
import com.iris.platform.history.appender.EventAppenderTestCase;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.test.Mocks;

@Mocks({ HistoryAppenderDAO.class, ObjectNameCache.class })
public class ApiKeyAppenderTest extends EventAppenderTestCase {

   private static final String KEY_ID = UUID.randomUUID().toString();
   private static final String LABEL = "Home Assistant";

   @Inject ApiKeyAppender appender;

   private PlatformMessage apiKeyEvent(String event, Address actor) {
      return eventBuilder()
            .event(event)
            .from(placeAddress)
            .withActor(actor)
            .withAttribute(CreatedEvent.ATTR_KEYID, KEY_ID)
            .withAttribute(CreatedEvent.ATTR_LABEL, LABEL)
            .build();
   }

   private void assertEntry(HistoryLogEntry entry, HistoryLogEntryType type, String messageKey) {
      assertEquals(type, entry.getType());
      assertEquals(PLACE_ID, entry.getId());
      assertEquals(messageKey, entry.getMessageKey());
      assertEquals(placeAddress.getRepresentation(), entry.getSubjectAddress());
      assertEquals(LABEL, entry.getValues().get(0));
      assertEquals(PLACE_NAME, entry.getValues().get(1));
      assertEquals(PERSON_NAME, entry.getValues().get(2));
   }

   @After
   public void verify() {
      super.verify();
   }

   @Test
   public void testCreated() {
      expectFindPlaceName();
      expectFindPersonName();
      Capture<HistoryLogEntry> dashboard = expectAndCaptureAppend();
      Capture<HistoryLogEntry> details = expectAndCaptureAppend();
      replay();

      assertTrue(appender.append(apiKeyEvent(CreatedEvent.NAME, ACTOR_PERSON)));
      assertEntry(dashboard.getValue(), CRITICAL, "apikey.created");
      assertEntry(details.getValue(), PLACE, "apikey.created");
   }

   @Test
   public void testRevoked() {
      expectFindPlaceName();
      expectFindPersonName();
      Capture<HistoryLogEntry> dashboard = expectAndCaptureAppend();
      Capture<HistoryLogEntry> details = expectAndCaptureAppend();
      replay();

      assertTrue(appender.append(apiKeyEvent(RevokedEvent.NAME, ACTOR_PERSON)));
      assertEntry(dashboard.getValue(), CRITICAL, "apikey.revoked");
      assertEntry(details.getValue(), PLACE, "apikey.revoked");
   }

   @Test
   public void testNoActorIgnored() {
      replay();

      assertFalse(appender.append(apiKeyEvent(RevokedEvent.NAME, null)));
   }

   @Test
   public void testOtherEventIgnored() {
      replay();

      assertFalse(appender.append(apiKeyEvent("apikey:Other", ACTOR_PERSON)));
   }
}
