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

import java.util.Map;

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
      expectFindByDeviceId();
      // note state isn't updated in this case, because instead we just delete the whole thing
      
      Device dev2=device.copy();
      mockDeviceDao.delete(dev2);
      EasyMock.expectLastCall();
      replay();
      
      deviceService.lostDevice(Address.fromString(device.getAddress()));
      assertForceRemove(); // kind of weird, but anytime a tombstoned driver is loaded a ForceRemove is sent
      assertDeleted(); // since it was already tombstoned this isn't necessary, but it doesn't hurt either

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
   
   private void assertForceRemove() {
      PlatformMessage message = messages.poll();
      assertEquals(DeviceCapability.ForceRemoveRequest.NAME, message.getMessageType());
   }
   
   private void assertValueChange(Map<String,Object> attributes) {
      PlatformMessage message = messages.poll();
      assertEquals(Address.broadcastAddress(), message.getDestination());
      assertEquals(device.getAddress(), message.getSource().getRepresentation());
      assertEquals(Capability.EVENT_VALUE_CHANGE, message.getMessageType());
      assertEquals(attributes, message.getValue().getAttributes());
   }

   private void assertDeleted() {
      PlatformMessage message = messages.poll();
      assertEquals(Address.broadcastAddress(), message.getDestination());
      assertEquals(device.getAddress(), message.getSource().getRepresentation());
      assertEquals(Capability.EVENT_DELETED, message.getMessageType());
   }

}

