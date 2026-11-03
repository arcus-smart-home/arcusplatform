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
package com.iris.platform.subsystem.incident;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmIncidentCapability.ListHistoryEntriesRequest;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.ServiceLevel;
import com.iris.platform.PagedResults;
import com.iris.platform.alarm.incident.AlarmIncidentDAO;
import com.iris.platform.history.HistoryLogDAO;
import com.iris.platform.history.HistoryLogEntry;

public class ListIncidentHistoryEntriesHandlerTest {
   private final UUID place = UUID.randomUUID();
   private final UUID incidentId = UUID.randomUUID();
   private final Address incidentAddress = Address.platformService(incidentId, AlarmIncidentCapability.NAMESPACE);

   private HistoryLogDAO logDao;
   private AlarmIncidentDAO incidentDao;
   private ListIncidentHistoryEntriesHandler handler;

   @SuppressWarnings("unchecked")
   @Before
   public void setUp() {
      // strict, so reading the log of someone else's incident fails as an unexpected call
      logDao = EasyMock.createStrictMock(HistoryLogDAO.class);
      incidentDao = EasyMock.createMock(AlarmIncidentDAO.class);
      handler = new ListIncidentHistoryEntriesHandler(EasyMock.createNiceMock(BeanAttributesTransformer.class), logDao, incidentDao);
   }

   private PlatformMessage request(UUID placeHeader) {
      PlatformMessage.Builder builder = PlatformMessage
            .buildRequest(ListHistoryEntriesRequest.builder().build(), Address.clientAddress("android", "1"), incidentAddress);
      if(placeHeader != null) {
         builder.withPlaceId(placeHeader);
      }
      return builder.create();
   }

   private void expectLogRead() {
      EasyMock.expect(logDao.listEntriesByQuery(EasyMock.anyObject())).andReturn(PagedResults.<HistoryLogEntry>emptyPage());
   }

   private void handle(PlatformMessage message) {
      assertEquals(ListHistoryEntriesRequest.NAME + "Response", handler.handleRequest(message, message).getMessageType());
   }

   @Test
   public void testIncidentAtAnotherPlaceLooksLikeItDoesNotExist() {
      EasyMock.expect(incidentDao.findById(place, incidentId)).andReturn(null);
      EasyMock.replay(logDao, incidentDao);

      try {
         handle(request(place));
         fail("expected the incident not to be found");
      }
      catch(ErrorEventException e) {
         // expected
      }
      EasyMock.verify(logDao);
   }

   @Test
   public void testIncidentAtThePlaceIsListed() {
      EasyMock.expect(incidentDao.findById(place, incidentId)).andReturn(IncidentFixtures.createSmokeAlarm(ServiceLevel.BASIC));
      expectLogRead();
      EasyMock.replay(logDao, incidentDao);

      handle(request(place));
      EasyMock.verify(logDao, incidentDao);
   }

   @Test
   public void testMessagesWithoutAPlaceHeaderAreNotChecked() {
      expectLogRead();
      EasyMock.replay(logDao, incidentDao);

      handle(request(null));
      EasyMock.verify(logDao);
   }
}
