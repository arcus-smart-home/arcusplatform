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
import java.util.Map;
import java.util.UUID;

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
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.service.DeviceService.CreateDeviceRequest;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Hub;
import com.iris.messages.model.Place;
import com.iris.messages.services.PlatformConstants;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * A device add names its place (or a hub, whose place is used) in the body. When the message has
 * a place header, the device must belong to that place, under that place's account, and a device
 * already at the protocol address must already be there. Messages without a place header are
 * left alone. The DAOs are strict mocks: a refused add that saved, deleted or updated anything
 * fails the test.
 */
@Mocks({ DeviceDAO.class, HubDAO.class, PersonDAO.class, PersonPlaceAssocDAO.class, PlaceDAO.class, PopulationDAO.class, IpcdDeviceDao.class })
@Modules({ InMemoryMessageModule.class, TestDriverModule.class, AttributeMapTransformModule.class })
public class DeviceServiceAddScopeTest extends IrisMockTestCase {
   @Inject DeviceService uut;
   @Inject DeviceDAO deviceDao;
   @Inject HubDAO hubDao;
   @Inject PlaceDAO placeDao;
   @Inject PersonDAO personDao;
   @Inject PersonPlaceAssocDAO personPlaceAssocDao;
   @Inject IpcdDeviceDao ipcdDeviceDao;
   @Inject InMemoryPlatformMessageBus bus;

   private final String hubId = "LWW-1234";
   private final String protocolName = "TEST";
   private final ProtocolDeviceId protocolId = ProtocolDeviceId.fromBytes(new byte [] { 1, 2, 3, 4, 5 });
   private final Address protocolAddress = Address.hubProtocolAddress(hubId, protocolName, protocolId);
   private final AttributeMap protocolAttributes = AttributeMap.newMap();

   private final UUID requesterPlace = UUID.randomUUID();
   private final UUID requesterAccount = UUID.randomUUID();
   private final UUID otherPlace = UUID.randomUUID();
   private final UUID otherAccount = UUID.randomUUID();

   private Place place;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      place = Fixtures.createPlace();
      place.setId(requesterPlace);
      place.setAccount(requesterAccount);
   }

   private CreateDeviceRequest request(UUID placeId, UUID accountId) {
      CreateDeviceRequest request = new CreateDeviceRequest();
      request.setProtocolName(protocolName);
      request.setProtocolId(protocolId);
      request.setHubId(hubId);
      request.setPlaceId(placeId);
      request.setAccountId(accountId);
      return request;
   }

   private Device existingDevice(UUID placeId) {
      Device device = Fixtures.createDevice();
      device.setId(UUID.randomUUID());
      device.setAddress(Address.platformDriverAddress(device.getId()).getRepresentation());
      device.setPlace(placeId);
      device.setHubId(hubId);
      device.setProtocol(protocolName);
      device.setProtocolid(protocolId.getRepresentation());
      device.setCreated(new Date());
      device.setModified(new Date());
      return device;
   }

   private void expectFindByProtocolAddress(Device device) {
      EasyMock.expect(deviceDao.findByProtocolAddress(protocolAddress.getRepresentation())).andReturn(device);
   }

   private void assertRefused(CreateDeviceRequest request) {
      replay();
      try {
         uut.create(request, protocolAttributes, requesterPlace.toString());
         fail("expected the add to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
      assertFalse("the protocol address must not stay locked", uut.isUpgradeInProgress(protocolAddress));
      assertNull("a refused add must not announce anything", bus.poll());
      verify();
   }

   @Test
   public void testAddAtAnotherPlaceIsRefused() {
      expectFindByProtocolAddress(null);
      assertRefused(request(otherPlace, otherAccount));
   }

   @Test
   public void testAddNamingAnotherPlacesHubIsRefused() {
      Hub hub = Fixtures.createHub();
      hub.setId(hubId);
      hub.setPlace(otherPlace);
      hub.setAccount(otherAccount);
      expectFindByProtocolAddress(null);
      EasyMock.expect(hubDao.findById(hubId)).andReturn(hub);
      // no place in the body, so the hub's place is used
      assertRefused(request(null, null));
   }

   @Test
   public void testAddUnderAnotherAccountIsRefused() {
      expectFindByProtocolAddress(null);
      EasyMock.expect(placeDao.findById(requesterPlace)).andReturn(place);
      assertRefused(request(requesterPlace, otherAccount));
   }

   @Test
   public void testReAddOfADeviceAtAnotherPlaceIsRefused() {
      expectFindByProtocolAddress(existingDevice(otherPlace));
      assertRefused(request(requesterPlace, requesterAccount));
   }

   @Test
   public void testRefusedAddKeepsARemovedDevice() {
      Device removed = existingDevice(otherPlace);
      removed.setState(Device.STATE_TOMBSTONED);
      expectFindByProtocolAddress(removed);
      // strict mock: deleting the removed device would fail the test
      assertRefused(request(otherPlace, otherAccount));
   }

   @Test
   public void testAddAtTheRequestersPlace() {
      expectFindByProtocolAddress(null);
      EasyMock.expect(placeDao.findById(requesterPlace)).andReturn(place);
      expectSuccessfulCreate();
      replay();

      Device device = uut.create(request(requesterPlace, requesterAccount), protocolAttributes, requesterPlace.toString());
      assertEquals(requesterPlace, device.getPlace());
      assertEquals(requesterAccount, device.getAccount());
      verify();
   }

   @Test
   public void testAddReplacesARemovedDeviceFromAnotherPlace() {
      // a device removed from one place and added at another, as when an IP device moves
      Device removed = existingDevice(otherPlace);
      removed.setState(Device.STATE_TOMBSTONED);
      expectFindByProtocolAddress(removed);
      EasyMock.expect(placeDao.findById(requesterPlace)).andReturn(place);
      deviceDao.delete(removed);
      EasyMock.expectLastCall();
      expectSuccessfulCreate();
      replay();

      Device device = uut.create(request(requesterPlace, requesterAccount), protocolAttributes, requesterPlace.toString());
      assertEquals(requesterPlace, device.getPlace());
      verify();
   }

   @Test
   public void testAddWithoutAPlaceHeaderIsNotChecked() {
      expectFindByProtocolAddress(null);
      expectSuccessfulCreate();
      replay();

      Device device = uut.create(request(otherPlace, otherAccount), protocolAttributes, null);
      assertEquals(otherPlace, device.getPlace());
      verify();
   }

   // through DeviceServiceHandler, which has to pass the message's place header on

   private DeviceServiceHandler handler() {
      return new DeviceServiceHandler(bus, new DriverServiceConfig(), deviceDao, hubDao, ipcdDeviceDao, personDao, personPlaceAssocDao, uut, null);
   }

   private PlatformMessage addMessage(String type, UUID bodyPlace) {
      Map<String, Object> attributes = ImmutableMap.<String, Object>of(
            MessageConstants.ATTR_PROTOCOLNAME, protocolName,
            MessageConstants.ATTR_DEVICEID, protocolId.getRepresentation(),
            MessageConstants.ATTR_HUBID, hubId,
            MessageConstants.ATTR_PLACEID, bodyPlace.toString(),
            MessageConstants.ATTR_ACCOUNTID, otherAccount.toString());
      return PlatformMessage
            .request(Address.platformService(PlatformConstants.SERVICE_DEVICES))
            .from(Fixtures.createClientAddress())
            .withPlaceId(requesterPlace)
            .withPayload(MessageBody.buildMessage(type, attributes))
            .create();
   }

   @Test
   public void testAddDeviceRequestForAnotherPlaceIsRefused() throws Exception {
      expectFindByProtocolAddress(null);
      replay();

      MessageBody response = handler().handleRequest(addMessage(MessageConstants.MSG_ADD_DEVICE_REQUEST, otherPlace));
      assertEquals(Errors.CODE_NOT_FOUND, response.getAttributes().get("code"));
      assertNull(bus.poll());
      verify();
   }

   @Test
   public void testDeviceInfoResponseForAnotherPlaceAddsNothing() throws Exception {
      expectFindByProtocolAddress(null);
      replay();

      handler().handleEvent(addMessage(HubAdvancedCapability.GetDeviceInfoResponse.NAME, otherPlace));
      assertNull(bus.poll());
      verify();
   }

   private void expectSuccessfulCreate() {
      EasyMock.expect(placeDao.getPopulationById(EasyMock.anyObject(UUID.class))).andReturn(Fixtures.createPopulation().getName()).anyTimes();
      EasyMock
         .expect(deviceDao.save(EasyMock.isA(Device.class)))
         .andAnswer(() -> {
            Device saved = ((Device) EasyMock.getCurrentArguments()[0]).copy();
            saved.setCreated(new Date());
            saved.setModified(new Date());
            return saved;
         });
      deviceDao.replaceDriverState(EasyMock.isA(Device.class), EasyMock.isA(DeviceDriverStateHolder.class));
      EasyMock.expectLastCall();
      deviceDao.updateDriverState(EasyMock.isA(Device.class), EasyMock.isA(DeviceDriverStateHolder.class));
      EasyMock.expectLastCall();
   }
}
