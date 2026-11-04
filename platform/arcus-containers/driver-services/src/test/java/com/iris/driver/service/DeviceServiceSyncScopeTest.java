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
package com.iris.driver.service;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Fixtures;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * A device sync answers with the place's devices, driver reflexes and the hash of every pin at the
 * place, so it must only answer for the sender's own place. Every DAO is a strict mock: a refused
 * sync that looked anything up fails the test.
 */
@Mocks({
   DeviceDAO.class,
   HubDAO.class,
   IpcdDeviceDao.class,
   PersonDAO.class,
   PersonPlaceAssocDAO.class,
   DriverRegistry.class,
   DriverExecutorRegistry.class,
   PlaceDAO.class,
   PopulationDAO.class
})
@Modules({ InMemoryMessageModule.class })
public class DeviceServiceSyncScopeTest extends IrisMockTestCase {
   @Inject private DeviceDAO deviceDao;
   @Inject private HubDAO hubDao;
   @Inject private IpcdDeviceDao ipcdDeviceDao;
   @Inject private PersonDAO personDao;
   @Inject private PersonPlaceAssocDAO personPlaceAssocDao;
   @Inject private DeviceService service;
   @Inject private InMemoryPlatformMessageBus bus;

   private final UUID senderPlace = UUID.randomUUID();
   private final UUID otherPlace = UUID.randomUUID();

   private DeviceServiceHandler handler() {
      return new DeviceServiceHandler(bus, new DriverServiceConfig(), deviceDao, hubDao, ipcdDeviceDao, personDao, personPlaceAssocDao, service, null);
   }

   private PlatformMessage sync(String type, UUID bodyPlace, UUID placeHeader) {
      MessageBody body = MessageBody.buildMessage(type, java.util.Collections.<String, Object>singletonMap(
            com.iris.messages.service.DeviceService.SyncDevicesRequest.ATTR_PLACEID, bodyPlace.toString()));
      PlatformMessage.Builder builder = PlatformMessage
            .request(Address.platformService(DeviceServiceHandler.NAME))
            .from(Fixtures.createClientAddress())
            .withPayload(body);
      if(placeHeader != null) {
         builder.withPlaceId(placeHeader);
      }
      return builder.create();
   }

   private void assertSyncRefused(String type) throws Exception {
      replay();
      try {
         handler().handleRequest(sync(type, otherPlace, senderPlace));
         fail("expected " + type + " for another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
      verify();
   }

   @Test
   public void testSyncDevicesForAnotherPlaceIsRefused() throws Exception {
      assertSyncRefused(com.iris.messages.service.DeviceService.SyncDevicesRequest.NAME);
   }

   @Test
   public void testLegacySyncDevicesForAnotherPlaceIsRefused() throws Exception {
      assertSyncRefused(DeviceServiceHandler.LEGACY_DEVICESYNC_NAMESPACE);
   }

   @Test
   public void testSyncDevicesForTheSendersPlaceLooksUpItsHub() throws Exception {
      // no hub, so the sync stops there; reaching the lookup shows the check let it through
      EasyMock.expect(hubDao.findHubForPlace(senderPlace)).andReturn(null);
      replay();

      MessageBody response = handler().handleRequest(sync(com.iris.messages.service.DeviceService.SyncDevicesRequest.NAME, senderPlace, senderPlace));
      assertEquals(Errors.CODE_NOT_FOUND, response.getAttributes().get("code"));
      verify();
   }

   @Test
   public void testDegradedDevicesForAnotherPlaceAreRefused() throws Exception {
      replay();
      try {
         handler().handleEvent(sync(com.iris.messages.service.DeviceService.DevicesDegradedEvent.NAME, otherPlace, senderPlace));
         fail("expected a degraded devices report for another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
      verify();
   }

   @Test
   public void testDegradedDevicesForTheSendersPlaceLookUpItsHub() throws Exception {
      EasyMock.expect(hubDao.findHubForPlace(senderPlace)).andReturn(null);
      replay();

      handler().handleEvent(sync(com.iris.messages.service.DeviceService.DevicesDegradedEvent.NAME, senderPlace, senderPlace));
      verify();
   }
}
