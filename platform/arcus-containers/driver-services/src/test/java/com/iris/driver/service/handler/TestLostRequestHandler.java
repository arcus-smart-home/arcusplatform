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

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.driver.service.DeviceService;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceDriverAddress;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({
   DeviceDAO.class,
   HubDAO.class,
   DeviceService.class
})
@Modules({ InMemoryMessageModule.class})
public class TestLostRequestHandler extends IrisMockTestCase {

   @Inject private DeviceDAO devDao;
   @Inject private DeviceService service;
   @Inject LostRequestHandler handler;
   
   private Device device;
   
   protected Device createDevice() {
      Device device = Fixtures.createDevice();
      return device;
   }
   
   @Test
   public void handleRequest() throws Exception {
      device = createDevice();
      EasyMock.expect(devDao.findById(device.getId())).andReturn(device);
      service.lostDevice(Address.fromString(device.getAddress()));
      EasyMock.expectLastCall();
      replay();
      
      MessageBody body = DeviceConnectionCapability.LostDeviceRequest.instance();
      PlatformMessage message = PlatformMessage.buildRequest(body,DeviceDriverAddress.fromString(device.getAddress()),DeviceDriverAddress.fromString(device.getAddress())).create();
      handler.handleMessage(message);
      
      verify();
   }
   
   @Test
   public void handleRequestAddressNotFound() throws Exception {
      device = createDevice();
      EasyMock.expect(devDao.findById(device.getId())).andReturn(null);
      replay();
      
      MessageBody body = DeviceConnectionCapability.LostDeviceRequest.instance();
      PlatformMessage message = PlatformMessage.buildRequest(body,DeviceDriverAddress.fromString(device.getAddress()),DeviceDriverAddress.fromString(device.getAddress())).create();
      try{
         handler.handleMessage(message);
         fail();
      }
      catch(NotFoundException e){
         
      }
      
      verify();
   }
}

