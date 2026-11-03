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
import com.iris.core.dao.PlaceDAO;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceCapability.RemoveResponse;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.platform.pairing.ProductLoader;
import com.iris.protocol.mock.MockProtocol;
import com.iris.test.Mocks;

/**
 * A client chooses the destination of a remove request, so a device at another
 * place has to be refused. Requests without a place header are internal and keep working.
 */
@Mocks({ PlaceDAO.class, ProductLoader.class })
public class RemoveRequestHandlerScopeTest extends RemoveRequestHandlerTestCase {

   @Inject private DeviceDAO deviceDao;
   @Inject private InMemoryPlatformMessageBus platformBus;

   @Inject private RemoveRequestHandler removeHandler;
   @Inject private ForceRemoveRequestHandler forceRemoveHandler;

   private Device device;

   @Before
   public void initializeDevice() {
      device = Fixtures.createDevice();
      device.setPlace(UUID.randomUUID());
      device.setProtocol(MockProtocol.NAMESPACE);
      device.setProtocolid(ProtocolDeviceId.hashDeviceId("test").getRepresentation());
      device.setProtocolAddress(Address.protocolAddress(MockProtocol.NAMESPACE, ProtocolDeviceId.hashDeviceId("test")).getRepresentation());
      device.setState(Device.STATE_ACTIVE_SUPPORTED);
   }

   private PlatformMessage request(MessageBody payload, UUID placeHeader) {
      PlatformMessage.Builder builder =
            PlatformMessage
               .request(Address.fromString(device.getAddress()))
               .from(Fixtures.createClientAddress())
               .withPayload(payload);
      if(placeHeader != null) {
         builder.withPlaceId(placeHeader);
      }
      return builder.create();
   }

   @Test
   public void testRemoveFromAnotherPlaceIsRefused() throws Exception {
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      replay();

      try {
         removeHandler.handleMessage(request(DeviceCapability.RemoveRequest.builder().build(), UUID.randomUUID()));
         fail("expected a remove from another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }

      assertNull(platformBus.poll());
      verify();
   }

   @Test
   public void testForceRemoveFromAnotherPlaceIsRefused() throws Exception {
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      replay();

      try {
         forceRemoveHandler.handleMessage(request(DeviceCapability.ForceRemoveRequest.instance(), UUID.randomUUID()));
         fail("expected a force remove from another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }

      assertNull(platformBus.poll());
      verify();
   }

   @Test
   public void testRemoveAtTheDevicesPlace() throws Exception {
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      replay();

      MessageBody response = removeHandler.handleMessage(request(DeviceCapability.RemoveRequest.builder().build(), device.getPlace()));
      assertEquals(RemoveResponse.NAME, response.getMessageType());
      assertDeviceRemovedMessage(device, platformBus.take());

      assertNull(platformBus.poll());
      verify();
   }
}
