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

import java.util.Date;

import org.easymock.EasyMock;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.driver.pin.PlatformPinManager;
import com.iris.driver.platform.PlatformDriverService;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({
   DeviceDAO.class,
   IpcdDeviceDao.class,
   HubDAO.class,
   PersonDAO.class,
   PersonPlaceAssocDAO.class,
   PlatformDriverService.class,
   DeviceService.class,
   PlatformPinManager.class,
   DriverExecutorRegistry.class,
   DriverRegistry.class
})
@Modules({ InMemoryMessageModule.class })
public class TestDeviceServiceHandler_DeviceRemoved extends IrisMockTestCase {

   // mocks
   @Inject DeviceDAO deviceDao;
   @Inject DeviceService service;
   @Inject PlatformDriverService driverService;
   @Inject InMemoryPlatformMessageBus platformBus; // not an EasyMock mock, but similar idea

   // unit under test
   @Inject DeviceServiceHandler handler;

   Device device;

   @Before
   public void initializeDevice() {
      device = Fixtures.createDevice();
      device.setCreated(new Date());
      device.setModified(new Date());
   }

   public void init() throws Exception {
      driverService.init();
      driverService.startup();
   }

   /*
    * Clean removal -> delete device
    */
   @Test
   public void testCleanRemovedActiveDevice() throws Exception {
      expectFindDeviceAndReturn(Device.STATE_ACTIVE_SUPPORTED);
      expectDelete();
      replay();

      init();
      handler.handleEvent(createCleanRemovedEvent());

      verify();
   }

   /*
    * Clean removal -> delete device
    * (although you shouldn't ever get a RemovedDevice for a lost device,
    * since it has already been removed at the protocol layer)
    */
   @Test
   public void testCleanRemovedLostDevice() throws Exception {
      expectFindDeviceAndReturn(Device.STATE_LOST_UNRECOVERABLE);
      expectDelete();
      replay();

      init();
      handler.handleEvent(createCleanRemovedEvent());

      verify();
   }

   /*
    * Got confirmation that the tombstoned device is removed
    * (although it should have had the status == FORCED) we
    * can fully delete the record
    */
   @Test
   public void testCleanRemovedTombstonedDevice() throws Exception {
      expectFindDeviceAndReturn(Device.STATE_TOMBSTONED);
      expectDelete();
      replay();

      init();
      handler.handleEvent(createCleanRemovedEvent());

      verify();
   }

   /*
    * Unexpected removal, mark the device as lost
    */
   @Test
   public void testSpontaneousRemovedActiveDevice() throws Exception {
      expectFindDeviceAndReturn(Device.STATE_ACTIVE_SUPPORTED);
      expectLost();
      replay();

      init();
      handler.handleEvent(createSpontaneousRemovedEvent());

      verify();
   }

   /*
    * A bit weird, don't know why we would get two, but go ahead
    * and make double sure its marked as lost
    */
   @Test
   public void testSpontaneousRemovedLostDevice() throws Exception {
      expectFindDeviceAndReturn(Device.STATE_LOST_RECOVERABLE);
      expectLost();
      replay();

      init();
      handler.handleEvent(createSpontaneousRemovedEvent());

      verify();
   }

   /*
    * A misbehaving device was manually unpaired, clear out the
    * tombstone.
    */
   @Test
   public void testSpontaneousRemovedTombstonedDevice() throws Exception {
      expectFindDeviceAndReturn(Device.STATE_TOMBSTONED);
      expectDelete();
      replay();

      init();
      handler.handleEvent(createSpontaneousRemovedEvent());

      verify();
   }

   /*
    * This shouldn't happen, the device should be marked as tombstoned
    * if it was triggered from a force delete. For the moment we delete anyway.
    * FIXME should this case be an error? mark the device as lost?
    */
   @Test
   public void testForceRemovedActiveDevice() throws Exception {
      expectFindDeviceAndReturn(Device.STATE_ACTIVE_SUPPORTED);
      expectDelete();
      replay();

      init();
      handler.handleEvent(createForceRemovedEvent());

      verify();
   }

   /*
    * This shouldn't happen, the device should be marked as tombstoned
    * if it was triggered from a force delete. For the moment we delete anyway.
    * FIXME should this case be an error? leave the device as lost?
    */
   @Test
   public void testForceRemovedLostDevice() throws Exception {
      expectFindDeviceAndReturn(Device.STATE_LOST_UNRECOVERABLE);
      expectDelete();
      replay();

      init();
      handler.handleEvent(createForceRemovedEvent());

      verify();
   }

   /*
    * This is the normal flow, the device was tombstoned, we told
    * the protocol to force remove it, and we got back confirmation
    */
   @Test
   public void testForceRemovedTombstonedDevice() throws Exception {
      expectFindDeviceAndReturn(Device.STATE_TOMBSTONED);
      expectDelete();
      replay();

      init();
      handler.handleEvent(createForceRemovedEvent());

      verify();
   }

   /*
    * This is really a misbehaving protocol, it should tell us what
    * the type of removal was.  For the moment we're considering
    * this to be the same as clean removal.
    * FIXME should this case be an error? mark the device as lost?
    */
   @Test
   public void testUnspecifiedRemovedActiveDevice() throws Exception {
      expectFindDeviceAndReturn(Device.STATE_ACTIVE_SUPPORTED);
      expectDelete();
      replay();

      init();
      handler.handleEvent(createUnspecifiedRemovedEvent());

      verify();
   }

   /*
    * This is really a misbehaving protocol, it should tell us what
    * the type of removal was.  For the moment we're considering
    * this to be the same as clean removal.
    * FIXME should this case be an error? leave the device as lost?
    */
   @Test
   public void testUnspecifiedRemovedLostDevice() throws Exception {
      expectFindDeviceAndReturn(Device.STATE_LOST_UNRECOVERABLE);
      expectDelete();
      replay();

      init();
      handler.handleEvent(createUnspecifiedRemovedEvent());

      verify();
   }

   /*
    * Removal of a tombstoned device in anyway allows us to clean up
    * the tombstoned record.
    */
   @Test
   public void testUnspecifiedRemovedTombstonedDevice() throws Exception {
      expectFindDeviceAndReturn(Device.STATE_TOMBSTONED);
      expectDelete();
      replay();

      init();
      handler.handleEvent(createUnspecifiedRemovedEvent());

      verify();
   }

   protected PlatformMessage createCleanRemovedEvent() {
      return createRemovedEvent(DeviceAdvancedCapability.RemovedDeviceEvent.STATUS_CLEAN);
   }

   protected PlatformMessage createForceRemovedEvent() {
      return createRemovedEvent(DeviceAdvancedCapability.RemovedDeviceEvent.STATUS_FORCED);
   }

   protected PlatformMessage createSpontaneousRemovedEvent() {
      return createRemovedEvent(DeviceAdvancedCapability.RemovedDeviceEvent.STATUS_SPONTANEOUS);
   }

   protected PlatformMessage createUnspecifiedRemovedEvent() {
      return createRemovedEvent(null);
   }

   protected PlatformMessage createRemovedEvent(@Nullable String status) {
      MessageBody event =
            DeviceAdvancedCapability.RemovedDeviceEvent
               .builder()
               .withProtocol(device.getProtocol())
               .withProtocolId(device.getProtocolid())
               .withStatus(status)
               .build();
      PlatformMessage message =
            PlatformMessage
               .createBroadcast(event, Address.fromString(device.getProtocolAddress()));
      return message;
   }

   protected void expectFindDeviceAndReturn(@Nullable String state) {
      if(state == null) {
         EasyMock.expect(deviceDao.findByProtocolAddress(device.getProtocolAddress())).andReturn(null);
      }
      else {
         device.setState(state);
         EasyMock.expect(deviceDao.findByProtocolAddress(device.getProtocolAddress())).andReturn(device);
      }
   }

   protected void expectDelete() throws Exception {
      EasyMock
         .expect(service.delete(device.copy()))
         .andReturn(true);
   }

   protected void expectLost() throws Exception {
      service.lostDevice(Address.fromString(device.getProtocolAddress()));
      EasyMock.expectLastCall();
   }


   protected void assertReceivedDeleted() throws Exception {
      PlatformMessage message = platformBus.take();

      assertEquals(Address.broadcastAddress(), message.getDestination());
      assertEquals(Address.fromString(device.getAddress()), message.getSource());
      assertEquals(Capability.EVENT_DELETED, message.getMessageType());
   }

   protected void assertBusEmpty() {
      assertNull(platformBus.poll());
   }

}

