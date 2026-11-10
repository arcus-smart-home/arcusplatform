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

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.BridgeChildCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.protocol.Protocols;
import com.iris.protocol.RemoveProtocolRequest;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.security.authz.AuthzMetrics;
import com.iris.security.authz.DisabledRiskyOperations;
import com.iris.security.authz.RoleAuthorizer;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * devadv:RemovedDevice and hubadv:GetDeviceInfoResponse name a device by the hub and protocol id
 * in their body, so the handler has to check who sent them. The client bridges refuse a frame
 * with isRequest=false, but the role authorizer on its own doesn't stop an event to the device
 * service, and the handler must not depend on the bridge. Every DAO and the device service are strict mocks: a dropped event that looked a
 * device up, or deleted or created one, fails the test.
 */
@Mocks({
   DeviceDAO.class,
   HubDAO.class,
   IpcdDeviceDao.class,
   PersonDAO.class,
   PersonPlaceAssocDAO.class,
   DeviceService.class
})
@Modules({ InMemoryMessageModule.class })
public class DeviceServiceEventScopeTest extends IrisMockTestCase {
   private static final String HUB_ID = "ABC-1234";
   private static final String OTHER_HUB_ID = "XYZ-5678";

   @Inject private DeviceDAO deviceDao;
   @Inject private HubDAO hubDao;
   @Inject private IpcdDeviceDao ipcdDeviceDao;
   @Inject private PersonDAO personDao;
   @Inject private PersonPlaceAssocDAO personPlaceAssocDao;
   @Inject private DeviceService service;
   @Inject private InMemoryPlatformMessageBus bus;

   private final UUID devicePlace = UUID.randomUUID();
   private final UUID otherPlace = UUID.randomUUID();
   private Device hubDevice;

   @Before
   public void createHubDevice() {
      hubDevice = Fixtures.createDevice();
      hubDevice.setCreated(new Date());
      hubDevice.setModified(new Date());
      hubDevice.setState(Device.STATE_ACTIVE_SUPPORTED);
      hubDevice.setPlace(devicePlace);
      hubDevice.setHubId(HUB_ID);
      hubDevice.setProtocol("ZWAV");
      hubDevice.setProtocolid(ProtocolDeviceId.fromBytes(new byte[] { 5 }).getRepresentation());
      hubDevice.setProtocolAddress(Address.hubProtocolAddress(HUB_ID, "ZWAV", ProtocolDeviceId.fromBytes(new byte[] { 5 })).getRepresentation());
   }

   private DeviceServiceHandler handler() {
      return new DeviceServiceHandler(bus, new DriverServiceConfig(), deviceDao, hubDao, ipcdDeviceDao, personDao, personPlaceAssocDao, service, null);
   }

   private MessageBody removed(String hubId) {
      return DeviceAdvancedCapability.RemovedDeviceEvent.builder()
            .withHubId(hubId)
            .withProtocol(hubDevice.getProtocol())
            .withProtocolId(hubDevice.getProtocolid())
            .withStatus(DeviceAdvancedCapability.RemovedDeviceEvent.STATUS_CLEAN)
            .build();
   }

   private MessageBody deviceInfo(String hubId) {
      return MessageBody.buildMessage(HubAdvancedCapability.GetDeviceInfoResponse.NAME, ImmutableMap.<String, Object>of(
            MessageConstants.ATTR_HUBID, hubId,
            MessageConstants.ATTR_PROTOCOLNAME, hubDevice.getProtocol(),
            MessageConstants.ATTR_DEVICEID, hubDevice.getProtocolid()));
   }

   // what client-bridge builds from a frame with isRequest=false: IrisNettyMessageUtil.convertClientToPlatform
   private PlatformMessage fromClient(MessageBody body, UUID activePlace, UUID person) {
      return PlatformMessage.buildMessage(body, Address.fromString("CLNT:android:session-token"), Address.fromString("SERV:dev:"))
            .withPlaceId(activePlace)
            .withActor(Address.platformService(person, "person"))
            .isRequestMessage(false)
            .withTimestamp(new Date())
            .create();
   }

   // what hub-bridge forwards: the agent's ports send as SERV:<hub>:hub, hub-bridge only accepts
   // that from the hub's own session and stamps the session's place (HubMessageHandler.ensureTimestamp)
   private PlatformMessage fromHub(MessageBody body, String sendingHub, UUID hubPlace) {
      return PlatformMessage.buildMessage(body, Address.hubService(sendingHub, "hub"), Address.platformService(DeviceServiceHandler.NAME))
            .withPlaceId(hubPlace)
            .create();
   }

   private static RoleAuthorizer authorizer() {
      return new RoleAuthorizer(new AuthzMetrics(), new DisabledRiskyOperations());
   }

   private static AuthorizationContext context(UUID person, UUID place) {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setEntityId(person);
      grant.setPlaceId(place);
      grant.setAccountId(UUID.randomUUID());
      grant.addPermissions("*:*:*");
      return new AuthorizationContext(null, null, Arrays.asList(grant));
   }

   // as it comes off the bus
   private static PlatformMessage overTheBus(PlatformMessage message) {
      return JSON.fromJson(JSON.toJson(message), PlatformMessage.class);
   }

   @Test
   public void testClientRemovedDeviceForAnotherPlaceIsDropped() throws Exception {
      UUID person = UUID.randomUUID();
      PlatformMessage message = fromClient(removed(HUB_ID), otherPlace, person);
      // the authorizer on its own lets it through
      assertTrue(authorizer().isAuthorized(context(person, otherPlace), otherPlace.toString(), message));
      replay();

      handler().handleEvent(overTheBus(message));

      verify();
   }

   @Test
   public void testClientRemovedDeviceForItsOwnPlaceIsDropped() throws Exception {
      // a client removes a device with a request to the device, not by reporting it gone
      replay();

      handler().handleEvent(overTheBus(fromClient(removed(HUB_ID), devicePlace, UUID.randomUUID())));

      verify();
   }

   @Test
   public void testClientDeviceInfoForAnotherPlacesHubIsDropped() throws Exception {
      UUID person = UUID.randomUUID();
      PlatformMessage message = fromClient(deviceInfo(OTHER_HUB_ID), otherPlace, person);
      assertTrue(authorizer().isAuthorized(context(person, otherPlace), otherPlace.toString(), message));
      replay();

      handler().handleEvent(overTheBus(message));

      verify();
   }

   @Test
   public void testHubRemovedDeviceForAnotherHubIsDropped() throws Exception {
      replay();

      handler().handleEvent(overTheBus(fromHub(removed(HUB_ID), OTHER_HUB_ID, otherPlace)));

      verify();
   }

   @Test
   public void testHubRemovedDeviceWithoutAHubIsDropped() throws Exception {
      // no hub in the body would name a platform device, such as an IPCD device at any place
      replay();

      handler().handleEvent(overTheBus(fromHub(removed(null), OTHER_HUB_ID, otherPlace)));

      verify();
   }

   @Test
   public void testRemovedDeviceForAnotherPlaceIsDropped() throws Exception {
      // the hub named is the sender, but its session is at another place than the device
      EasyMock.expect(deviceDao.findByProtocolAddress(hubDevice.getProtocolAddress())).andReturn(hubDevice);
      replay();

      handler().handleEvent(overTheBus(fromHub(removed(HUB_ID), HUB_ID, otherPlace)));

      verify();
   }

   @Test
   public void testRemovedDeviceWithoutAPlaceHeaderIsDropped() throws Exception {
      EasyMock.expect(deviceDao.findByProtocolAddress(hubDevice.getProtocolAddress())).andReturn(hubDevice);
      replay();

      handler().handleEvent(overTheBus(fromHub(removed(HUB_ID), HUB_ID, null)));

      verify();
   }

   @Test
   public void testHubRemovesItsOwnDevice() throws Exception {
      EasyMock.expect(deviceDao.findByProtocolAddress(hubDevice.getProtocolAddress())).andReturn(hubDevice);
      EasyMock.expect(service.delete(hubDevice.copy())).andReturn(true);
      replay();

      handler().handleEvent(overTheBus(fromHub(removed(HUB_ID), HUB_ID, devicePlace)));

      verify();
   }

   @Test
   public void testHubReportsItsOwnDeviceLost() throws Exception {
      EasyMock.expect(deviceDao.findByProtocolAddress(hubDevice.getProtocolAddress())).andReturn(hubDevice);
      service.lostDevice(Address.fromString(hubDevice.getProtocolAddress()));
      EasyMock.expectLastCall();
      replay();

      MessageBody spontaneous = DeviceAdvancedCapability.RemovedDeviceEvent.builder()
            .withHubId(HUB_ID)
            .withProtocol(hubDevice.getProtocol())
            .withProtocolId(hubDevice.getProtocolid())
            .withStatus(DeviceAdvancedCapability.RemovedDeviceEvent.STATUS_SPONTANEOUS)
            .build();
      handler().handleEvent(overTheBus(fromHub(spontaneous, HUB_ID, devicePlace)));

      verify();
   }

   @Test
   public void testDriverRemovesItsBridgeChild() throws Exception {
      // a bridge child's driver reports its own removal (ProtocolPlugin.handleRemove)
      Device child = Fixtures.createDevice(DeviceCapability.NAMESPACE, BridgeChildCapability.NAMESPACE);
      child.setCreated(new Date());
      child.setModified(new Date());
      child.setState(Device.STATE_ACTIVE_SUPPORTED);
      child.setPlace(devicePlace);
      EasyMock.expect(deviceDao.findByProtocolAddress(child.getProtocolAddress())).andReturn(child);
      EasyMock.expect(service.delete(child.copy())).andReturn(true);
      replay();

      handler().handleEvent(overTheBus(Protocols.removeBridgeChild(new RemoveProtocolRequest(child))));

      verify();
   }
}
