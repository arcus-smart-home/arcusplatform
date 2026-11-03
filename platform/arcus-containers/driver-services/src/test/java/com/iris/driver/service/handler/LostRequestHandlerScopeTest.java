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

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.driver.service.DeviceService;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({
   DeviceDAO.class,
   HubDAO.class,
   DeviceService.class
})
@Modules({ InMemoryMessageModule.class })
public class LostRequestHandlerScopeTest extends IrisMockTestCase {

   @Inject private DeviceDAO deviceDao;
   @Inject private DeviceService service;
   @Inject private LostRequestHandler handler;

   private Device device;

   @Before
   public void initializeDevice() {
      device = Fixtures.createDevice();
      device.setPlace(UUID.randomUUID());
   }

   private PlatformMessage request(UUID placeHeader) {
      PlatformMessage.Builder builder =
            PlatformMessage
               .request(Address.fromString(device.getAddress()))
               .from(Fixtures.createClientAddress())
               .withPayload(DeviceConnectionCapability.LostDeviceRequest.instance());
      if(placeHeader != null) {
         builder.withPlaceId(placeHeader);
      }
      return builder.create();
   }

   @Test
   public void testLostFromAnotherPlaceIsRefused() throws Exception {
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      replay();

      try {
         handler.handleMessage(request(UUID.randomUUID()));
         fail("expected a lost device request from another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
      verify();
   }

   @Test
   public void testClientRequestToADeviceWithoutAPlaceIsRefused() throws Exception {
      device.setPlace(null);
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      replay();

      try {
         handler.handleMessage(request(UUID.randomUUID()));
         fail("expected a request with a place header to a device without a place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
      verify();
   }

   @Test
   public void testLostAtTheDevicesPlace() throws Exception {
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      service.lostDevice(Address.fromString(device.getAddress()));
      EasyMock.expectLastCall();
      replay();

      handler.handleMessage(request(device.getPlace()));
      verify();
   }
}
