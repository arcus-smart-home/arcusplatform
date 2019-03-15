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
/**
 * 
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
import com.iris.driver.service.DeviceService;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceCapability.RemoveResponse;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.platform.pairing.ProductLoader;
import com.iris.protocol.mock.MockProtocol;
import com.iris.test.Mocks;

@Mocks({ PlaceDAO.class, ProductLoader.class })
public class TestRemoveRequestHandler extends RemoveRequestHandlerTestCase {

   @Inject private DeviceDAO deviceDao;
   @Inject private DeviceService service;
   @Inject private InMemoryPlatformMessageBus platformBus;
   
   @Inject RemoveRequestHandler handler;
   
   private Device device;
   
   @Before
   public void initializeDevice() {
      device = Fixtures.createDevice();
      device.setPlace(UUID.randomUUID());
      device.setProtocol(MockProtocol.NAMESPACE);
      device.setProtocolid(ProtocolDeviceId.hashDeviceId("test").getRepresentation());
      device.setProtocolAddress(Address.protocolAddress(MockProtocol.NAMESPACE, ProtocolDeviceId.hashDeviceId("test")).getRepresentation());
   }
   
   public PlatformMessage createRemoveRequest() {
      return
            PlatformMessage
               .request(Address.fromString(device.getAddress()))
               .from(Fixtures.createClientAddress())
               .withPayload(DeviceCapability.RemoveRequest.builder().build())
               .create();
   }
   
   /**
    * A normal delete should just send the protocol level
    * message and then quit.
    */
   @Test
   public void testRemoveActiveDevice() throws Exception {
      device.setState(Device.STATE_ACTIVE_SUPPORTED);
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      replay();

      MessageBody response = handler.handleMessage(createRemoveRequest());
      assertEquals(RemoveResponse.NAME, response.getMessageType());
      assertDeviceRemovedMessage(device, platformBus.take());
      
      assertNull(platformBus.poll());
      
      verify();
   }
   
   @Test
   public void testRemoveMissingDevice() throws Exception {
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(null);
      replay();

      try {
         handler.handleMessage(createRemoveRequest());
         fail();
      }
      catch(NotFoundException e) {
         // expected
      }
      
      assertNull(platformBus.poll());
      
      verify();
   }

   @Test
   public void testRemoveLostDevice() throws Exception {
      device.setState(Device.STATE_LOST_RECOVERABLE);
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      EasyMock.expect(service.delete(device)).andReturn(true);
      replay();

      MessageBody response = handler.handleMessage(createRemoveRequest());
      assertEquals(RemoveResponse.NAME, response.getMessageType());
      
      // NOTE device service is responsible for sending out the deleted event
      
      assertNull(platformBus.poll());
      
      verify();
   }

   /**
    * In this case we should just try deleting it again, why not?  It doesn't "un-tombstone"
    * the device.
    * @throws Exception
    */
   @Test
   public void testDeleteTombstonedDevice() throws Exception {
      device.setState(Device.STATE_TOMBSTONED);
      EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
      replay();

      MessageBody response = handler.handleMessage(createRemoveRequest());
      assertEquals(RemoveResponse.NAME, response.getMessageType());
      
      PlatformMessage message = platformBus.take();
      assertDeviceRemovedMessage(device, message);
      
      assertNull(platformBus.poll());
      
      verify();
   }

}

