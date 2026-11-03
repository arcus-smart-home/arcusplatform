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

import com.iris.core.dao.DeviceDAO;
import com.iris.driver.service.DeviceService;
import com.iris.driver.service.DeviceService.UpgradeDriverResponse;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Device;
import com.iris.messages.model.DriverId;
import com.iris.messages.model.Fixtures;
import com.iris.model.Version;

public class UpgradeDriverRequestHandlerScopeTest {

   private DeviceService service;
   private DeviceDAO deviceDao;
   private UpgradeDriverRequestHandler handler;
   private Device device;

   @Before
   public void setUp() {
      service = EasyMock.createMock(DeviceService.class);
      deviceDao = EasyMock.createMock(DeviceDAO.class);
      handler = new UpgradeDriverRequestHandler(service, deviceDao);

      device = Fixtures.createDevice();
      device.setPlace(UUID.randomUUID());
   }

   private PlatformMessage request(UUID placeHeader) {
      PlatformMessage.Builder builder =
            PlatformMessage
               .request(Address.fromString(device.getAddress()))
               .from(Fixtures.createClientAddress())
               .withPayload(DeviceAdvancedCapability.UpgradeDriverRequest.builder().build());
      if(placeHeader != null) {
         builder.withPlaceId(placeHeader);
      }
      return builder.create();
   }

   private void replay() {
      EasyMock.replay(service, deviceDao);
   }

   private void verify() {
      EasyMock.verify(service, deviceDao);
   }

   @Test
   public void testUpgradeFromAnotherPlaceIsRefused() throws Exception {
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      replay();

      try {
         handler.handleMessage(request(UUID.randomUUID()));
         fail("expected an upgrade from another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
      verify();
   }

   @Test
   public void testUpgradeOfAMissingDeviceIsNotFound() throws Exception {
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(null);
      replay();

      try {
         handler.handleMessage(request(UUID.randomUUID()));
         fail("expected an upgrade of a missing device to be not found");
      }
      catch(NotFoundException e) {
         // expected
      }
      verify();
   }

   @Test
   public void testUpgradeAtTheDevicesPlace() throws Exception {
      DriverId driverId = new DriverId("TestDriver", Version.fromRepresentation("1.0"));
      UpgradeDriverResponse upgraded = EasyMock.createMock(UpgradeDriverResponse.class);
      EasyMock.expect(upgraded.isUpgraded()).andReturn(true);
      EasyMock.expect(upgraded.getDriverId()).andReturn(driverId).anyTimes();

      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      EasyMock.expect(service.upgradeDriver(Address.fromString(device.getAddress()))).andReturn(upgraded);
      EasyMock.replay(upgraded);
      replay();

      MessageBody response = handler.handleMessage(request(device.getPlace()));
      assertEquals(DeviceAdvancedCapability.UpgradeDriverResponse.NAME, response.getMessageType());
      verify();
   }
}
