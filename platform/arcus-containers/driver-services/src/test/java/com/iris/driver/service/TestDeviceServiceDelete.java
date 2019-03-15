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
package com.iris.driver.service;

import java.util.Date;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.driver.DeviceErrors;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Device;
import com.iris.messages.model.DriverId;
import com.iris.messages.model.Fixtures;
import com.iris.model.Version;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 *
 */
@Mocks({ DeviceDAO.class, HubDAO.class, PersonDAO.class, PersonPlaceAssocDAO.class, PlaceDAO.class, PopulationDAO.class })
@Modules({ InMemoryMessageModule.class, TestDriverModule.class })
public class TestDeviceServiceDelete extends IrisMockTestCase {

   @Inject DeviceService uut;

   @Inject InMemoryPlatformMessageBus messages;
   @Inject DeviceDAO mockDeviceDao;

   Device device = Fixtures.createDevice();
   DeviceDriverStateHolder state = new DeviceDriverStateHolder();

   @Test
   public void testDeleteNull() throws Exception {
      try {
         uut.delete(null);
         fail();
      }
      catch(NullPointerException e) {
         // expected
      }

      assertNull(messages.poll());
   }

   @Test
   public void testDeleteUnpersistedDevice() throws Exception {
      replay();

      assertEquals(false, uut.delete(device));
      assertNull(messages.poll());

      verify();
   }

   @Test
   public void testDeleteDeletedDevice() throws Exception {
      device.setCreated(new Date()); // persisted is true

      expectDeviceNotFound();
      replay();

      assertEquals(false, uut.delete(device));
      assertNull(messages.poll());

      verify();
   }

   @Test
   public void testDeleteDeviceWithInvalidDriver() throws Exception {
      device.setCreated(new Date()); // persisted
      device.setDriverId(new DriverId("Invalid", new Version(10))); // invalid driver

      expectLoadDeviceAndState();
      mockDeviceDao.delete(device);
      EasyMock.expectLastCall().once();
      replay();

      assertEquals(true, uut.delete(device));

      {
         PlatformMessage message = messages.poll();
         assertEquals(Address.broadcastAddress(), message.getDestination());
         assertEquals(device.getAddress(), message.getSource().getRepresentation());
         assertEquals(Capability.EVENT_DELETED, message.getMessageType());
      }
      assertNull(messages.poll());

      verify();
   }

   @Test
   public void testDeleteDeviceOk() throws Exception {
      device.setCreated(new Date()); // persisted
      device.setDriverId(new DriverId("mock", new Version(1))); // valid driver

      expectLoadDeviceAndState();
      mockDeviceDao.delete(device);
      EasyMock.expectLastCall().once();
      replay();

      DriverExecutor executor = uut.getDriver(Address.fromString(device.getAddress()));

      // delete should by-pass the block and cancel out the messages
      assertEquals(true, uut.delete(device));

      {
         PlatformMessage message = messages.poll();
         assertEquals(Address.broadcastAddress(), message.getDestination());
         assertEquals(device.getAddress(), message.getSource().getRepresentation());
         assertEquals(Capability.EVENT_DELETED, message.getMessageType());
      }
      assertNull(messages.poll());

      verify();
   }

   @Test
   public void testDeleteDeviceWithPendingRequests() throws Exception {
      device.setCreated(new Date()); // persisted
      device.setDriverId(new DriverId("Invalid", new Version(10))); // invalid driver

      expectLoadDeviceAndState();
      mockDeviceDao.delete(device);
      EasyMock.expectLastCall().once();
      replay();

      DriverExecutor executor = uut.getDriver(Address.fromString(device.getAddress()));
      // blocked
      executor.context().setMessageContext(createRequest());
      // add some pending requests
      executor.fire(createRequest());
      executor.fire(createRequest());

      // delete should by-pass the block and cancel out the messages
      assertEquals(true, uut.delete(device));

      {
         PlatformMessage message = messages.take();
         assertEquals(Address.broadcastAddress(), message.getDestination());
         assertEquals(device.getAddress(), message.getSource().getRepresentation());
         assertEquals(Capability.EVENT_DELETED, message.getMessageType());
      }
      for(int i=0; i<3; i++) {
         PlatformMessage message = messages.take();
         assertFalse(message.getDestination().isBroadcast());
         assertEquals(device.getAddress(), message.getSource().getRepresentation());
         assertEquals(ErrorEvent.MESSAGE_TYPE, message.getMessageType());
         assertEquals(DeviceErrors.ERR_CODE_DELETED, ((ErrorEvent) message.getValue()).getCode());
      }
      assertNull(messages.poll());

      verify();
   }

   // TODO system test to for send message to just deleted driver?

   private PlatformMessage createRequest() {
      return
            PlatformMessage
               .builder()
               .from(Fixtures.createClientAddress())
               .to(Address.fromString(device.getAddress()))
               .isRequestMessage(true)
               .withPayload(MessageBody.ping())
               .create();
   }

   private void expectDeviceNotFound() {
      EasyMock.expect(mockDeviceDao.findById(device.getId())).andReturn(null).anyTimes();
   }

   private void expectLoadDeviceAndState() {
      EasyMock.expect(mockDeviceDao.findById(device.getId())).andReturn(device).once();
      EasyMock.expect(mockDeviceDao.loadDriverState(device)).andReturn(state).once();
   }

}

