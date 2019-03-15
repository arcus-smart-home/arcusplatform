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

import java.util.HashMap;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;

import com.google.inject.Provides;
import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.messaging.memory.InMemoryProtocolMessageBus;
import com.iris.device.attributes.AttributeMap;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.model.Fixtures;
import com.iris.protocol.Protocol;
import com.iris.protocol.Protocols;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.test.IrisTestCase;
import com.iris.test.MockModule;
import com.iris.test.Modules;

@Modules({
   InMemoryMessageModule.class,
   TestDriverModule.class,
   AttributeMapTransformModule.class
})
public class DriverServiceTestCase extends IrisTestCase {
   protected InMemoryPlatformMessageBus platformBus;
   protected InMemoryProtocolMessageBus protocolBus;

   protected MockModule mocks = new MockModule(DeviceDAO.class);
   protected DeviceDAO mockDeviceDao = mocks.get(DeviceDAO.class);

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      platformBus = ServiceLocator.getInstance(InMemoryPlatformMessageBus.class);
      protocolBus = ServiceLocator.getInstance(InMemoryProtocolMessageBus.class);
   }

   @Provides
   public DeviceDAO mockDeviceDao() {
      return mockDeviceDao;
   }

   protected UUID registerZWaveDevice(String clientId, byte nodeId, String driverId) {
      return registerDevice(Protocols.getProtocolByName(ZWaveProtocol.NAME), clientId, ProtocolDeviceId.fromBytes(new byte [] { 0, nodeId }), driverId);
   }

   protected UUID registerIpcdDevice(String clientId, String protocolDeviceId, String driverId) {
      return registerDevice(Protocols.getProtocolByName(IpcdProtocol.NAME), clientId, ProtocolDeviceId.hashDeviceId(protocolDeviceId), driverId);
   }

   protected UUID registerDevice(Protocol<?> protocol, String clientId, ProtocolDeviceId protocolDeviceId, String driverId) {
      com.iris.messages.model.Device device = Fixtures.createDevice();
      device.setId(UUID.randomUUID());
      device.setProtocol(protocol.getNamespace());
      device.setProtocolid(clientId);
      device.setDrivername(driverId);
      device.setAddress(Address.platformDriverAddress(device.getId()).getRepresentation());
      device.setProtocolAddress(Address.protocolAddress(protocol.getName(),  protocolDeviceId).getRepresentation());
      EasyMock
         .expect(mockDeviceDao.findByProtocolAddress(device.getProtocolAddress()))
         .andReturn(device)
         .anyTimes();
      EasyMock
         .expect(mockDeviceDao.findById(device.getId()))
         .andReturn(device)
         .anyTimes();
      EasyMock
         .expect(mockDeviceDao.loadDriverState(device))
         .andReturn(new DeviceDriverStateHolder(AttributeMap.newMap(), new HashMap<String,Object>()))
         .anyTimes();
      return device.getId();

   }

}

