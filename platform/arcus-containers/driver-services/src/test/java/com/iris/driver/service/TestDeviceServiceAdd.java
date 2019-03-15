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
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

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
import com.iris.driver.service.DeviceService.CreateDeviceRequest;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Hub;
import com.iris.messages.model.Place;
import com.iris.messages.type.Population;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 *
 */
@Mocks({ DeviceDAO.class, HubDAO.class, PersonDAO.class, PersonPlaceAssocDAO.class, PlaceDAO.class, PopulationDAO.class })
@Modules({ InMemoryMessageModule.class, TestDriverModule.class, AttributeMapTransformModule.class })
public class TestDeviceServiceAdd extends IrisMockTestCase {

   @Inject DeviceService uut;
   @Inject PopulationDAO populationDao;
   @Inject PlaceDAO placeDao;

   @Inject DriverRegistry registry;
   @Inject InMemoryPlatformMessageBus messages;
   @Inject DeviceDAO mockDeviceDao;
   @Inject HubDAO mockHubDao;

   Device device = Fixtures.createDevice();
   DeviceDriverStateHolder state = new DeviceDriverStateHolder();

   String hubId = "LWW-1234";
   UUID accountId = UUID.randomUUID();
   Place place;
   UUID placeId = UUID.randomUUID();
   
   String protocolName = "TEST";
   ProtocolDeviceId protocolId = ProtocolDeviceId.fromBytes(new byte [] { 1, 2, 3, 4, 5 });
   Address protocolAddress = Address.hubProtocolAddress(hubId, protocolName, protocolId);
   AttributeMap protocolAttributes = AttributeMap.newMap();

   Hub hub;
   private Population population;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      hub = Fixtures.createHub();
      hub.setId(hubId);
      hub.setAccount(accountId);
      hub.setPlace(placeId);
      
      place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      place.setAccount(accountId);
      
      //EasyMock.expect(populationDao.listPopulations()).andReturn(ImmutableList.<Population>of());
      population = Fixtures.createPopulation();
      place.setPopulation(population.getName());
   }
   
   private void expectFindPopulation() {
      //EasyMock.expect(populationDao.findByName(place.getPopulation())).andReturn(population);
      EasyMock.expect(placeDao.getPopulationById(placeId)).andReturn(place.getPopulation());
   }

   @Test
   public void testCreateWithHubAndPlaceId() throws Exception {
      expectFindPopulation();
      expectFindByProtocolNotFound();
      expectSuccessfulCreate();
      replay();

      CreateDeviceRequest request = createRequest();
      Device response = uut.create(request, protocolAttributes);
      assertNotNull(response);
      assertFalse(uut.isUpgradeInProgress(protocolAddress));

      {
         assertNotNull(response.getId());
         assertEquals(registry.getFallback().getDriverId(), response.getDriverId());
         assertEquals(protocolAttributes, response.getProtocolAttributes());
         assertEquals(Device.STATE_ACTIVE_UNSUPPORTED, response.getState());
         assertEquals(protocolName, response.getProtocol());
         assertEquals(protocolId.getRepresentation(), response.getProtocolid());
      }
      assertAdded(response.getAddress());
      assertConnected(response.getAddress());
      assertNull(messages.poll());

      verify();
   }

   @Test
   public void testCreateWithHubAndNoPlaceId() throws Exception {
      expectFindPopulation();
      expectFindByProtocolNotFound();
      expectFindHub();
      expectSuccessfulCreate();
      replay();

      CreateDeviceRequest request = createRequestWithNoPlaceId();
      Device response = uut.create(request, protocolAttributes);
      assertNotNull(response);
      assertFalse(uut.isUpgradeInProgress(protocolAddress));

      {
         assertNotNull(response.getId());
         assertEquals(registry.getFallback().getDriverId(), response.getDriverId());
         assertEquals(protocolAttributes, response.getProtocolAttributes());
         assertEquals(Device.STATE_ACTIVE_UNSUPPORTED, response.getState());
         assertEquals(protocolName, response.getProtocol());
         assertEquals(protocolId.getRepresentation(), response.getProtocolid());
      }
      assertAdded(response.getAddress());
      assertConnected(response.getAddress());
      assertNull(messages.poll());

      verify();
   }

   @Test
   public void testCreateWithNoHubAndPlaceId() throws Exception {
      hubId = null;
      expectFindPopulation();
      expectFindByProtocolNotFound();
      expectSuccessfulCreate();
      replay();

      CreateDeviceRequest request = createRequest();
      Device response = uut.create(request, protocolAttributes);
      assertNotNull(response);
      assertFalse(uut.isUpgradeInProgress(protocolAddress));

      {
         assertNotNull(response.getId());
         assertEquals(registry.getFallback().getDriverId(), response.getDriverId());
         assertEquals(protocolAttributes, response.getProtocolAttributes());
         assertEquals(Device.STATE_ACTIVE_UNSUPPORTED, response.getState());
         assertEquals(protocolName, response.getProtocol());
         assertEquals(protocolId.getRepresentation(), response.getProtocolid());
      }
      assertAdded(response.getAddress());
      assertConnected(response.getAddress());
      assertNull(messages.poll());

      verify();
   }

   @Test
   public void testCreateWithNoHubAndNoPlaceId() throws Exception {
      hubId = null;
      placeId = null;
      expectFindByProtocolNotFound();
      expectSuccessfulCreate();
      replay();

      CreateDeviceRequest request = createRequestWithNoPlaceId();
      Device response = uut.create(request, protocolAttributes);
      assertNotNull(response);
      assertFalse(uut.isUpgradeInProgress(protocolAddress));

      {
         assertNotNull(response.getId());
         assertEquals(registry.getFallback().getDriverId(), response.getDriverId());
         assertEquals(protocolAttributes, response.getProtocolAttributes());
         assertEquals(Device.STATE_ACTIVE_UNSUPPORTED, response.getState());
         assertEquals(protocolName, response.getProtocol());
         assertEquals(protocolId.getRepresentation(), response.getProtocolid());
      }
      assertAdded(response.getAddress());
      assertConnected(response.getAddress());
      assertNull(messages.poll());

      verify();
   }

   @Test
   public void testAddDuplicateProtocolId() {
      Device device = Fixtures.createDevice();
      device.setHubId(hubId);
      device.setProtocol(protocolName);
      device.setProtocolid(protocolId.getRepresentation());
      EasyMock.expect(mockDeviceDao.findById(device.getId())).andReturn(device);
      expectFindByProtocolAndReturn(device);
      expectLostDeviceState(device, false);
      expectUpdateConnectionState();
      replay();

      CreateDeviceRequest request = createRequest();
      uut.create(request, protocolAttributes);
      assertFalse(uut.isUpgradeInProgress(protocolAddress));

      verify();
   }

   @Test
   public void testAddLostDevice() {
      Device device = Fixtures.createDevice();
      device.setId(UUID.randomUUID());
      device.setAddress(Address.platformDriverAddress(device.getId()).getRepresentation());
      device.setCreated(new Date());
      device.setModified(new Date());
      expectFindByProtocolAndReturn(device);
      EasyMock.expect(mockDeviceDao.findById(device.getId())).andReturn(device);
      expectLostDeviceState(device,true);
      expectUpdateDriverState(device, DeviceConnectionCapability.STATUS_ONLINE);
      replay();

      CreateDeviceRequest request = createRequest();
      uut.create(request, protocolAttributes);
      verify();
   }
   
   @Test
   public void testAddTombstonedDevice() throws Exception {
      expectFindPopulation();
      Device device = Fixtures.createDevice();
      device.setState(Device.STATE_TOMBSTONED);
      expectFindByProtocolAndReturn(device);
      mockDeviceDao.delete(device);
      EasyMock.expectLastCall();
      expectSuccessfulCreate();
      replay();

      CreateDeviceRequest request = createRequest();
      Device response = uut.create(request, protocolAttributes);
      assertNotNull(response);
      assertFalse(uut.isUpgradeInProgress(protocolAddress));

      {
         assertNotNull(response.getId());
         assertEquals(registry.getFallback().getDriverId(), response.getDriverId());
         assertEquals(protocolAttributes, response.getProtocolAttributes());
         assertEquals(Device.STATE_ACTIVE_UNSUPPORTED, response.getState());
         assertEquals(protocolName, response.getProtocol());
         assertEquals(protocolId.getRepresentation(), response.getProtocolid());
      }
      assertAdded(response.getAddress());
      assertConnected(response.getAddress());
      assertNull(messages.poll());

      verify();
   }

   private void expectUpdateDriverState(Device device,String status){
      AttributeMap attributes = AttributeMap.mapOf(
         DeviceConnectionCapability.KEY_STATE.valueOf(DeviceConnectionCapability.STATE_ONLINE),
         DeviceConnectionCapability.KEY_LASTCHANGE.coerceToValue(device.getAdded())
      );
      attributes.set(AttributeKey.create(DeviceConnectionCapability.ATTR_STATUS, String.class),status);
      DeviceDriverStateHolder state = new DeviceDriverStateHolder(attributes);
      mockDeviceDao.updateDriverState(device, state);
      EasyMock.expectLastCall();
   }

   private void expectUpdateConnectionState(){
      mockDeviceDao.updateDriverState(EasyMock.isA(Device.class), EasyMock.isA(DeviceDriverStateHolder.class));
      EasyMock.expectLastCall();
   }

   private void expectLostDeviceState(Device device,boolean lost){
      AttributeMap lostAttributes = AttributeMap.newMap();
      lostAttributes.set(DeviceConnectionCapability.KEY_STATUS, lost?DeviceConnectionCapability.STATUS_LOST:DeviceConnectionCapability.STATUS_ONLINE);
      state.getAttributes().addAll(lostAttributes);
      EasyMock.expect(mockDeviceDao.loadDriverState(device)).andReturn(state);
   }

   private CreateDeviceRequest createRequest() {
      CreateDeviceRequest request = createRequestWithNoPlaceId();
      request.setPlaceId(placeId);
      return request;
   }

   private CreateDeviceRequest createRequestWithNoPlaceId() {
      CreateDeviceRequest request = new CreateDeviceRequest();
      request.setAccountId(accountId);
      request.setHubId(hubId);
      request.setProtocolName(protocolName);
      request.setProtocolId(protocolId);
      return request;
   }

   private Device createExpectedDevice() {
      Device device = new Device();
      device.setAccount(accountId);
      device.setPlace(placeId);
      device.setHubId(hubId);
      device.setProtocol(protocolName);
      device.setProtocolid(protocolId.getRepresentation());
      device.setState(Device.STATE_CREATED);
      if(hubId == null) {
         device.setProtocolAddress(Address.protocolAddress(
               device.getProtocol(),
               ProtocolDeviceId.fromRepresentation(device.getProtocolid())).getRepresentation());
      }
      else {
         device.setProtocolAddress(Address.hubProtocolAddress(
               hubId,
               device.getProtocol(),
               ProtocolDeviceId.fromRepresentation(device.getProtocolid())).getRepresentation());
      }
      return device;
   }

   private void expectFindByProtocolNotFound() {
      expectFindByProtocolAndReturn(null);
   }

   private void expectFindByProtocolAndReturn(Device device) {
      Address address = hubId == null ?
            Address.protocolAddress(protocolName, protocolId) :
            Address.hubProtocolAddress(hubId, protocolName, protocolId);
      EasyMock.expect(mockDeviceDao.findByProtocolAddress(address.getRepresentation()))
         .andReturn(device)
         ;
   }

   private void expectFindHub() {
      EasyMock.expect(mockHubDao.findById(hubId)).andReturn(hub).once();
   }

   private Capture<Device> expectSuccessfulCreate() {
      Device expected = createExpectedDevice();
      Capture<Device> ref = expectCreate(expected);
      expectReplaceDriverState();
      expectUpdateConnectionState();
      return ref;
   }

   private Capture<Device> expectCreate(Device device) {
      final Capture<Device> deviceRef = EasyMock.newCapture();
      EasyMock
         .expect(mockDeviceDao.save(EasyMock.capture(deviceRef)))
         .andAnswer(() -> {
            Device response = deviceRef.getValue().copy();
            response.setCreated(new Date());
            response.setModified(new Date()	);
            return response;
         });
      return deviceRef;
   }

   private Capture<Device> expectDeviceUpdate() {
      final Capture<Device> deviceRef = EasyMock.newCapture();

      EasyMock.expect(mockDeviceDao.save(EasyMock.capture(deviceRef)))
         .andAnswer(() -> {
            Device response = deviceRef.getValue().copy();
            response.setModified(new Date());
            return response;
         });
      return deviceRef;
   }

   private Capture<DeviceDriverStateHolder> expectReplaceDriverState() {
      final Capture<DeviceDriverStateHolder> stateRef = EasyMock.newCapture();
      mockDeviceDao.replaceDriverState(EasyMock.isA(Device.class), EasyMock.capture(stateRef));
      EasyMock.expectLastCall().once();
      return stateRef;
   }

   private void assertAdded(String source) throws Exception {
      PlatformMessage message = messages.take();
      assertEquals(Address.broadcastAddress(), message.getDestination());
      assertEquals(Address.fromString(source), message.getSource());
      assertEquals(Capability.EVENT_ADDED, message.getMessageType());
   }

   private void assertConnected(String source) throws Exception {
      PlatformMessage message = messages.take();
      assertEquals(Address.broadcastAddress(), message.getDestination());
      assertEquals(Address.fromString(source), message.getSource());
      assertEquals(Capability.EVENT_VALUE_CHANGE, message.getMessageType());
      assertEquals(DeviceConnectionCapability.STATE_ONLINE, message.getValue().getAttributes().get(DeviceConnectionCapability.ATTR_STATE));
   }

}

