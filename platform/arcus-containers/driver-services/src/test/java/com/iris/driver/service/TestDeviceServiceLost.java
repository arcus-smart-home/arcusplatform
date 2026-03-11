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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.groovy.GroovyProtocolPluginModule;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({ DeviceDAO.class, HubDAO.class, PersonDAO.class, PersonPlaceAssocDAO.class, PlaceDAO.class, PopulationDAO.class })
@Modules({ InMemoryMessageModule.class, TestDriverModule.class, AttributeMapTransformModule.class, GroovyProtocolPluginModule.class })
public class TestDeviceServiceLost extends IrisMockTestCase {

   // unit under test
   @Inject
   private DeviceService deviceService;
   
   // mocks
   @Inject
   private InMemoryPlatformMessageBus messages;
   @Inject
   private DeviceDAO mockDeviceDao;

   // fixtures
   private Device device;
   private DeviceDriverStateHolder state;

   @Before
   public void setup(){
      device = Fixtures.createDevice();
      state = new DeviceDriverStateHolder(AttributeMap.mapOf(DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_ONLINE)));
   }

   @Test
   public void testLostDevice() throws Exception {
      expectFindByDeviceIdAndUpdateState();
      Device dev2=device.copy();
      dev2.setState(Device.STATE_LOST_RECOVERABLE);
      EasyMock.expect(mockDeviceDao.save(dev2)).andReturn(dev2);
      replay();
      
      deviceService.lostDevice(Address.fromString(device.getAddress()));
      // NOTE: connection state isn't updated because that is handled by the drivers themselves
      assertValueChange(ImmutableMap.<String, Object>of(DeviceConnectionCapability.ATTR_STATUS, DeviceConnectionCapability.STATUS_LOST));

      verify();
   }

   @Test
   public void testLostDeviceTransient() throws Exception {
      device = getDevice(ZWaveProtocol.NAME);
      device.setProtocolAddress("PROT:ZWAV:YWEK");
      expectFindByDeviceIdAndUpdateState();
      Device dev2=device.copy();
      dev2.setState(Device.STATE_LOST_UNRECOVERABLE);
      dev2.setProtocolAddress(null);
      EasyMock.expect(mockDeviceDao.save(dev2)).andReturn(dev2);
      replay();
      
      deviceService.lostDevice(Address.fromString(device.getAddress()));
      assertValueChange(ImmutableMap.<String, Object>of(DeviceConnectionCapability.ATTR_STATUS, DeviceConnectionCapability.STATUS_LOST));

      verify();
   }
   
   @Test
   public void testLostTombstonedDevice() throws Exception {
      device = getDevice(IpcdProtocol.NAME);
      device.setProtocolAddress("PROT:ZWAV:YWEK");
      device.setState(Device.STATE_TOMBSTONED);
      // Use anyTimes() because the ForceRemoveRequest sent by start() for
      // tombstoned devices is picked up by PlatformDriverService's listener
      // on a background thread, which may re-load the device from the DAO
      // if the executor cache has been invalidated by the time it runs.
      EasyMock.expect(mockDeviceDao.findById(device.getId())).andReturn(device).anyTimes();
      EasyMock.expect(mockDeviceDao.loadDriverState(device)).andReturn(state).anyTimes();
      // note state isn't updated in this case, because instead we just delete the whole thing

      Device dev2=device.copy();
      mockDeviceDao.delete(dev2);
      EasyMock.expectLastCall().atLeastOnce();
      replay();

      deviceService.lostDevice(Address.fromString(device.getAddress()));

      // Collect all messages from the bus. The ForceRemoveRequest sent by
      // start() for tombstoned devices is picked up by PlatformDriverService's
      // listener on a background thread, which asynchronously fires it through
      // the driver executor. Depending on thread scheduling, an error response
      // from the driver (which drops messages for tombstoned/deleted devices)
      // may appear on the message queue between the ForceRemoveRequest and the
      // Deleted event, causing non-deterministic ordering. Collect all messages
      // and verify the expected ones are present rather than relying on strict
      // order.
      List<PlatformMessage> received = collectTombstonedMessages();
      assertContainsMessageOfType(received, DeviceCapability.ForceRemoveRequest.NAME);
      assertContainsDeletedEvent(received);

      verify();
   }

   private Device getDevice(String protocol){
      Device device = Fixtures.createDevice();
      if(protocol!=null){
         device.setProtocol(protocol);
         device.setProtocolAddress(Fixtures.createProtocolAddress("zw").getRepresentation());
      }
      return device;
   }

   private void expectFindByDeviceIdAndUpdateState(){
      expectFindByDeviceId();
      expectUpdateDriverState();
   }

   private void expectFindByDeviceId(){
      EasyMock.expect(mockDeviceDao.findById(device.getId())).andReturn(device);
      EasyMock.expect(mockDeviceDao.loadDriverState(device)).andReturn(state).once();
   }

   private void expectUpdateDriverState(){
      AttributeMap attributes = AttributeMap.newMap();
      attributes.set(AttributeKey.create(DeviceConnectionCapability.ATTR_STATUS, String.class), DeviceConnectionCapability.STATUS_LOST);
      DeviceDriverStateHolder state = new DeviceDriverStateHolder(attributes);
      mockDeviceDao.updateDriverState(device, state);
      EasyMock.expectLastCall();
   }
   
   private void assertForceRemove() throws Exception {
      PlatformMessage message = messages.take();
      assertEquals(DeviceCapability.ForceRemoveRequest.NAME, message.getMessageType());
   }

   private void assertValueChange(Map<String,Object> attributes) throws Exception {
      PlatformMessage message = messages.take();
      assertEquals(Address.broadcastAddress(), message.getDestination());
      assertEquals(device.getAddress(), message.getSource().getRepresentation());
      assertEquals(Capability.EVENT_VALUE_CHANGE, message.getMessageType());
      assertEquals(attributes, message.getValue().getAttributes());
   }

   private void assertDeleted() throws Exception {
      PlatformMessage message = messages.take();
      assertEquals(Address.broadcastAddress(), message.getDestination());
      assertEquals(device.getAddress(), message.getSource().getRepresentation());
      assertEquals(Capability.EVENT_DELETED, message.getMessageType());
   }

   /**
    * Collects messages from the platform bus until both expected messages
    * (ForceRemoveRequest and Deleted event) have been found, or a timeout
    * is reached. This avoids sensitivity to message ordering caused by
    * async processing on background threads.
    */
   private List<PlatformMessage> collectTombstonedMessages() throws InterruptedException {
      List<PlatformMessage> received = new ArrayList<>();
      boolean foundForceRemove = false;
      boolean foundDeleted = false;
      long deadline = System.currentTimeMillis() + 10000; // 10 second overall deadline
      while((!foundForceRemove || !foundDeleted) && System.currentTimeMillis() < deadline) {
         long remaining = deadline - System.currentTimeMillis();
         if(remaining <= 0) break;
         PlatformMessage msg = messages.getMessageQueue().poll(remaining, TimeUnit.MILLISECONDS);
         if(msg == null) break;
         received.add(msg);
         if(DeviceCapability.ForceRemoveRequest.NAME.equals(msg.getMessageType())) {
            foundForceRemove = true;
         }
         if(Capability.EVENT_DELETED.equals(msg.getMessageType())) {
            foundDeleted = true;
         }
      }
      return received;
   }

   private void assertContainsMessageOfType(List<PlatformMessage> received, String messageType) {
      for(PlatformMessage msg : received) {
         if(messageType.equals(msg.getMessageType())) {
            return;
         }
      }
      fail("Expected message of type [" + messageType + "] but received: " + received);
   }

   private void assertContainsDeletedEvent(List<PlatformMessage> received) {
      for(PlatformMessage msg : received) {
         if(Capability.EVENT_DELETED.equals(msg.getMessageType())) {
            assertEquals(Address.broadcastAddress(), msg.getDestination());
            assertEquals(device.getAddress(), msg.getSource().getRepresentation());
            return;
         }
      }
      fail("Expected Deleted event but received: " + received);
   }

}

