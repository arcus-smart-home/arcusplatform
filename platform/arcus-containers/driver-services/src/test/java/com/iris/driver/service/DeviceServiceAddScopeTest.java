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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.service.DeviceService.CreateDeviceRequest;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Hub;
import com.iris.messages.services.PlatformConstants;
import com.iris.protocol.mock.MockProtocol;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * A device add names its place in the body, or a hub whose place is used instead. When the
 * message has a place header, the device must belong to that place: a device already at the
 * protocol address must be there, a hub the request names must be there, and a new device gets
 * that place's account. The sender matters too: a client may only add mock devices, and a hub
 * only its own. Messages without a place header are left alone. The DAOs are strict mocks: a
 * refused add that saved, deleted or updated anything fails the test.
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

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
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

   private void expectFindByProtocolAddress(Address address, Device device) {
      EasyMock.expect(deviceDao.findByProtocolAddress(address.getRepresentation())).andReturn(device);
   }

   private void expectFindByProtocolAddress(Device device) {
      expectFindByProtocolAddress(protocolAddress, device);
   }

   private void expectHub(UUID placeId, UUID accountId) {
      Hub hub = Fixtures.createHub();
      hub.setId(hubId);
      hub.setPlace(placeId);
      hub.setAccount(accountId);
      EasyMock.expect(hubDao.findById(hubId)).andReturn(hub);
   }

   private void expectRequesterAccount() {
      EasyMock.expect(placeDao.getAccountById(requesterPlace)).andReturn(requesterAccount);
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

   private Device create(CreateDeviceRequest request) {
      replay();
      Device device = uut.create(request, protocolAttributes, requesterPlace.toString());
      assertFalse(uut.isUpgradeInProgress(protocolAddress));
      verify();
      return device;
   }

   // refused

   @Test
   public void testAddAtAnotherPlaceIsRefused() {
      expectFindByProtocolAddress(null);
      expectHub(requesterPlace, requesterAccount);
      assertRefused(request(otherPlace, otherAccount));
   }

   @Test
   public void testAddNamingAnotherPlacesHubIsRefused() {
      expectFindByProtocolAddress(null);
      expectHub(otherPlace, otherAccount);
      // no place in the body, so the hub's place would be used
      assertRefused(request(null, null));
   }

   @Test
   public void testAddNamingAnotherPlacesHubAlongsideTheRequestersPlaceIsRefused() {
      expectFindByProtocolAddress(null);
      expectHub(otherPlace, otherAccount);
      assertRefused(request(requesterPlace, requesterAccount));
   }

   @Test
   public void testAddNamingAnUnknownHubIsRefused() {
      expectFindByProtocolAddress(null);
      EasyMock.expect(hubDao.findById(hubId)).andReturn(null);
      assertRefused(request(requesterPlace, requesterAccount));
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
      expectHub(requesterPlace, requesterAccount);
      // strict mock: deleting the removed device would fail the test
      assertRefused(request(otherPlace, otherAccount));
   }

   // allowed

   @Test
   public void testAddAtTheRequestersPlace() {
      expectFindByProtocolAddress(null);
      expectHub(requesterPlace, requesterAccount);
      expectRequesterAccount();
      expectSuccessfulCreate();

      Device device = create(request(requesterPlace, requesterAccount));
      assertEquals(requesterPlace, device.getPlace());
      assertEquals(requesterAccount, device.getAccount());
   }

   @Test
   public void testAccountComesFromThePlace() {
      expectFindByProtocolAddress(null);
      expectHub(requesterPlace, requesterAccount);
      expectRequesterAccount();
      expectSuccessfulCreate();

      Device device = create(request(requesterPlace, otherAccount));
      assertEquals(requesterAccount, device.getAccount());
   }

   @Test
   public void testHubWithAStaleAccountCanStillPair() {
      // what a hub sends: no place in the body, and the hub record's account is out of date
      expectFindByProtocolAddress(null);
      expectHub(requesterPlace, otherAccount);
      expectRequesterAccount();
      expectSuccessfulCreate();

      Device device = create(request(null, null));
      assertEquals(requesterPlace, device.getPlace());
      assertEquals(requesterAccount, device.getAccount());
   }

   @Test
   public void testReAddAtTheRequestersPlace() {
      Device existing = existingDevice(requesterPlace);
      expectFindByProtocolAddress(existing);
      expectReAdd(existing);

      assertSame(existing, create(request(null, null)));
   }

   @Test
   public void testReAddLeavesAnotherAddsLockAlone() throws Exception {
      Device existing = existingDevice(requesterPlace);
      expectFindByProtocolAddress(existing);
      // while the re-add runs, without the lock, another add for the address takes it
      @SuppressWarnings("unchecked")
      java.util.concurrent.ConcurrentMap<Address, Boolean> adds = (java.util.concurrent.ConcurrentMap<Address, Boolean>) addsInProgress().get(uut);
      EasyMock.expect(placeDao.getPopulationById(EasyMock.anyObject(UUID.class))).andReturn(Fixtures.createPopulation().getName()).anyTimes();
      EasyMock.expect(deviceDao.findById(existing.getId())).andAnswer(() -> {
         adds.put(protocolAddress, Boolean.TRUE);
         return existing;
      });
      AttributeMap status = AttributeMap.newMap();
      status.set(DeviceConnectionCapability.KEY_STATUS, DeviceConnectionCapability.STATUS_ONLINE);
      EasyMock.expect(deviceDao.loadDriverState(existing)).andReturn(new DeviceDriverStateHolder(status));
      deviceDao.updateDriverState(EasyMock.isA(Device.class), EasyMock.isA(DeviceDriverStateHolder.class));
      EasyMock.expectLastCall();

      replay();
      uut.create(request(null, null), protocolAttributes, requesterPlace.toString());
      assertTrue("the re-add must not release a lock it doesn't hold", uut.isUpgradeInProgress(protocolAddress));
      adds.remove(protocolAddress);
      verify();
   }

   private static java.lang.reflect.Field addsInProgress() throws NoSuchFieldException {
      java.lang.reflect.Field field = DeviceService.class.getDeclaredField("addsInProgress");
      field.setAccessible(true);
      return field;
   }

   @Test
   public void testReAddOfADeviceWithoutAPlace() {
      Device existing = existingDevice(null);
      expectFindByProtocolAddress(existing);
      expectReAdd(existing);

      assertSame(existing, create(request(null, null)));
   }

   @Test
   public void testAddReplacesARemovedDeviceUnderTheLock() {
      // a device removed from one place and added at another, as when an IP device moves
      Device removed = existingDevice(otherPlace);
      removed.setState(Device.STATE_TOMBSTONED);
      expectFindByProtocolAddress(removed);
      expectHub(requesterPlace, requesterAccount);
      expectRequesterAccount();
      deviceDao.delete(removed);
      EasyMock.expectLastCall().andAnswer(() -> {
         assertTrue("a removed device must be replaced under the add lock", uut.isUpgradeInProgress(protocolAddress));
         return null;
      });
      expectSuccessfulCreate();

      Device device = create(request(requesterPlace, requesterAccount));
      assertEquals(requesterPlace, device.getPlace());
   }

   @Test
   public void testAddWithoutAPlaceHeaderIsNotChecked() {
      expectFindByProtocolAddress(null);
      expectSuccessfulCreate();
      replay();

      Device device = uut.create(request(otherPlace, otherAccount), protocolAttributes, null);
      assertEquals(otherPlace, device.getPlace());
      assertEquals(otherAccount, device.getAccount());
      verify();
   }

   // through DeviceServiceHandler, which checks the sender and passes the place header on

   private DeviceServiceHandler handler() {
      return new DeviceServiceHandler(bus, new DriverServiceConfig(), deviceDao, hubDao, ipcdDeviceDao, personDao, personPlaceAssocDao, uut, null);
   }

   private PlatformMessage add(String type, Address from, String protocol, String hub, UUID bodyPlace) {
      Map<String, Object> attributes = new HashMap<>();
      attributes.put(MessageConstants.ATTR_PROTOCOLNAME, protocol);
      attributes.put(MessageConstants.ATTR_DEVICEID, protocolId.getRepresentation());
      if(hub != null) {
         attributes.put(MessageConstants.ATTR_HUBID, hub);
      }
      attributes.put(MessageConstants.ATTR_PLACEID, bodyPlace.toString());
      attributes.put(MessageConstants.ATTR_ACCOUNTID, otherAccount.toString());
      return PlatformMessage
            .request(Address.platformService(PlatformConstants.SERVICE_DEVICES))
            .from(from)
            .withPlaceId(requesterPlace)
            .withPayload(MessageBody.buildMessage(type, attributes))
            .create();
   }

   private void assertNotFound(MessageBody response) {
      assertEquals(Errors.CODE_NOT_FOUND, response.getAttributes().get("code"));
      assertNull(bus.poll());
   }

   @Test
   public void testClientCannotAddARealDevice() throws Exception {
      replay();
      assertNotFound(handler().handleRequest(add(MessageConstants.MSG_ADD_DEVICE_REQUEST, Fixtures.createClientAddress(), protocolName, hubId, requesterPlace)));
      verify();
   }

   @Test
   public void testClientMockDeviceForAnotherPlaceIsRefused() throws Exception {
      expectFindByProtocolAddress(Address.protocolAddress(MockProtocol.NAMESPACE, protocolId), null);
      replay();
      assertNotFound(handler().handleRequest(add(MessageConstants.MSG_ADD_DEVICE_REQUEST, Fixtures.createClientAddress(), MockProtocol.NAMESPACE, null, otherPlace)));
      verify();
   }

   @Test
   public void testHubCannotAddAnotherHubsDevice() throws Exception {
      replay();
      assertNotFound(handler().handleRequest(add(MessageConstants.MSG_ADD_DEVICE_REQUEST, Address.hubService("OTH-0001", "hub"), protocolName, hubId, requesterPlace)));
      verify();
   }

   @Test
   public void testDeviceInfoResponseForAnotherPlaceAddsNothing() throws Exception {
      expectFindByProtocolAddress(null);
      expectHub(requesterPlace, requesterAccount);
      replay();

      handler().handleEvent(add(HubAdvancedCapability.GetDeviceInfoResponse.NAME, Address.hubService(hubId, "hub"), protocolName, hubId, otherPlace));
      assertNull(bus.poll());
      verify();
   }

   private void expectReAdd(Device existing) {
      EasyMock.expect(placeDao.getPopulationById(EasyMock.anyObject(UUID.class))).andReturn(Fixtures.createPopulation().getName()).anyTimes();
      EasyMock.expect(deviceDao.findById(existing.getId())).andReturn(existing);
      AttributeMap status = AttributeMap.newMap();
      status.set(DeviceConnectionCapability.KEY_STATUS, DeviceConnectionCapability.STATUS_ONLINE);
      EasyMock.expect(deviceDao.loadDriverState(existing)).andReturn(new DeviceDriverStateHolder(status));
      deviceDao.updateDriverState(EasyMock.isA(Device.class), EasyMock.isA(DeviceDriverStateHolder.class));
      EasyMock.expectLastCall();
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
