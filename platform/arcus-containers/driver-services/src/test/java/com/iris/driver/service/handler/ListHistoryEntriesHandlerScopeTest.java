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
package com.iris.driver.service.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.DeviceDAO;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.platform.PagedResults;
import com.iris.platform.history.HistoryLogDAO;
import com.iris.platform.history.HistoryLogDAO.ListEntriesQuery;
import com.iris.platform.history.HistoryLogEntry;

public class ListHistoryEntriesHandlerScopeTest {

   private BeanAttributesTransformer<HistoryLogEntry> transformer;
   private HistoryLogDAO logDao;
   private DeviceDAO deviceDao;
   private ListHistoryEntriesHandler handler;
   private Device device;

   @SuppressWarnings("unchecked")
   @Before
   public void setUp() {
      transformer = EasyMock.createMock(BeanAttributesTransformer.class);
      logDao = EasyMock.createMock(HistoryLogDAO.class);
      deviceDao = EasyMock.createMock(DeviceDAO.class);
      handler = new ListHistoryEntriesHandler(transformer, logDao, deviceDao);

      device = Fixtures.createDevice();
      device.setPlace(UUID.randomUUID());
   }

   private PlatformMessage request(UUID placeHeader) {
      PlatformMessage.Builder builder =
            PlatformMessage
               .request(Address.fromString(device.getAddress()))
               .from(Fixtures.createClientAddress())
               .withPayload(DeviceCapability.ListHistoryEntriesRequest.builder().build());
      if(placeHeader != null) {
         builder.withPlaceId(placeHeader);
      }
      return builder.create();
   }

   private void replay() {
      EasyMock.replay(transformer, logDao, deviceDao);
   }

   private void verify() {
      EasyMock.verify(transformer, logDao, deviceDao);
   }

   @Test
   public void testHistoryFromAnotherPlaceIsRefused() throws Exception {
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      replay();

      try {
         handler.handleMessage(request(UUID.randomUUID()));
         fail("expected a device history read from another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
      verify();
   }

   @Test
   public void testHistoryAtTheDevicesPlace() throws Exception {
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      EasyMock.expect(logDao.listEntriesByQuery(EasyMock.anyObject(ListEntriesQuery.class)))
         .andReturn(PagedResults.newPage(ImmutableList.<HistoryLogEntry>of()));
      replay();

      MessageBody response = handler.handleMessage(request(device.getPlace()));
      assertEquals(DeviceCapability.ListHistoryEntriesResponse.NAME, response.getMessageType());
      verify();
   }
}
