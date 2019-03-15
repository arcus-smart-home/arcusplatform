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
package com.iris.platform.services.place.handlers;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.BridgeCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Account;
import com.iris.messages.model.Device;
import com.iris.messages.model.Hub;
import com.iris.messages.model.Place;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({
   DeviceDAO.class,
   HubDAO.class
})
@Modules({
   InMemoryMessageModule.class
})
public class BasePlacePairingHandler extends IrisMockTestCase {

   protected static final Address clientAddress = Address.clientAddress("test", "test");

   @Inject DeviceDAO deviceDao;
   @Inject HubDAO hubDao;
   @Inject InMemoryPlatformMessageBus bus;

   protected Account account;
   protected Place place;
   protected Hub hub;
   protected Device bridgeDevice1;
   protected Device bridgeDevice2;
   protected Device nonBridgeDevice1;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      account = new Account();
      account.setId(UUID.randomUUID());

      place = new Place();
      place.setId(UUID.randomUUID());
      place.setAccount(account.getId());
      
      
      hub = new Hub();
      hub.setId("LWW-1234");
      hub.setAccount(account.getId());

      account.setPlaceIDs(ImmutableSet.of(place.getId()));
      
      bridgeDevice1 = new Device();
      bridgeDevice1.setHubId(hub.getId());
      bridgeDevice1.setId(UUID.randomUUID());
      bridgeDevice1.setCaps(ImmutableSet.of("bridge"));
      bridgeDevice1.setAddress("DRIV:dev:"+bridgeDevice1.getId());

      bridgeDevice2 = new Device();
      bridgeDevice2.setHubId(hub.getId());
      bridgeDevice2.setId(UUID.randomUUID());
      bridgeDevice2.setCaps(ImmutableSet.of("bridge"));
      bridgeDevice2.setAddress(Address.fromString("DRIV:dev:"+bridgeDevice2.getId()).getRepresentation());
      
      nonBridgeDevice1 = new Device();
   }
   
   protected void setDefaultMockHubandDeviceDAO(){
      EasyMock.expect(hubDao.findHubForPlace(place.getId())).andReturn(hub);
      EasyMock.expect(deviceDao.listDevicesByPlaceId(place.getId())).andReturn(ImmutableList.of(nonBridgeDevice1,bridgeDevice1,bridgeDevice2));
   }
   
   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

}

