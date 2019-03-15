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
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.bootstrap.ServiceLocator;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Device;
import com.iris.messages.services.PlatformConstants;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({
   DeviceDAO.class,
   HubDAO.class,
   IpcdDeviceDao.class,
   HubDAO.class,
   PersonDAO.class,
   PersonPlaceAssocDAO.class,
   DriverRegistry.class,
   DriverExecutorRegistry.class,
   PlaceDAO.class,
   PopulationDAO.class
})
@Modules({ InMemoryMessageModule.class })
public class TestDeviceServiceHandler extends IrisMockTestCase {

   @Inject private DeviceDAO devDao;
   @Inject private HubDAO hubDao;
   @Inject private IpcdDeviceDao ipcdDevDao;
   @Inject private PersonDAO personDao;
   @Inject private PersonPlaceAssocDAO personPlaceAssocDao;
   @Inject private DeviceService service;
   @Inject private DriverExecutorRegistry execReg;
   @Inject private InMemoryPlatformMessageBus platformBus;

   private DeviceServiceHandler handler;
   private UUID placeId;
   private List<Device> devices;

   @Override
   public void setUp() throws Exception {
      super.setUp();

      handler = new DeviceServiceHandler(platformBus, new DriverServiceConfig(), devDao, hubDao, ipcdDevDao, personDao, personPlaceAssocDao, service, null);

      placeId = UUID.randomUUID();
      devices = new ArrayList<>();
      Device d1 = new Device();
      d1.setId(UUID.randomUUID());
      d1.setHubId("ABC-1234");
      d1.setCreated(new Date());
      d1.setAddress(Address.platformDriverAddress(d1.getId()).getRepresentation());
      devices.add(d1);
      Device d2 = new Device();
      d2.setId(UUID.randomUUID());
      d2.setCreated(new Date());
      d2.setAddress(Address.platformDriverAddress(d2.getId()).getRepresentation());
      devices.add(d2);
   }

   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

   @Test
   public void testOnPlaceDeletedOnlyRemovesWithoutHubId() throws Exception {
      EasyMock.expect(devDao.listDevicesByPlaceId(placeId, true)).andReturn(devices);
      EasyMock.expect(execReg.delete(Address.fromString(devices.get(1).getAddress()))).andReturn(true);
      replay();

      handler.handleEvent(createPlaceDeleted());
   }

   private PlatformMessage createPlaceDeleted() {
      MessageBody body = MessageBody.buildMessage(Capability.EVENT_DELETED, ImmutableMap.of());
      return PlatformMessage.buildBroadcast(body, Address.platformService(placeId, PlatformConstants.SERVICE_PLACES))
            .withPlaceId(placeId)
            .create();
   }

}

