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
package com.iris.platform.services.ipcd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Device;
import com.iris.messages.service.IpcdService.DeviceConnectedEvent;
import com.iris.messages.service.IpcdService.DeviceHeartBeatEvent;
import com.iris.messages.service.IpcdService.FindDeviceRequest;
import com.iris.messages.service.IpcdService.FindDeviceResponse;
import com.iris.messages.service.IpcdService.ForceUnregisterRequest;
import com.iris.messages.service.IpcdService.ForceUnregisterResponse;
import com.iris.messages.services.PlatformConstants;
import com.iris.messages.type.IpcdDeviceType;
import com.iris.platform.services.ipcd.registry.IpcdRegistry;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.protocol.ipcd.IpcdDevice;
import com.iris.protocol.ipcd.IpcdDeviceTypeRegistry;
import com.iris.protocol.ipcd.IpcdProtocol;

/**
 * Both requests name a device by serial number, not through an address the authorizer can check.
 */
public class IpcdServiceAuthorizationTest {
   private static final String VENDOR = "A.O. Smith";
   private static final String MODEL = "B2.00";
   private static final String SERIAL = "123456";

   private final UUID devicePlace = UUID.randomUUID();
   private final UUID otherPlace = UUID.randomUUID();

   private IpcdRegistry registry;
   private IpcdDeviceDao ipcdDeviceDao;
   private DeviceDAO deviceDao;
   private IpcdService service;
   private IpcdDevice device;
   private String protocolAddress;

   @SuppressWarnings("unchecked")
   @Before
   public void setUp() {
      registry = EasyMock.createStrictMock(IpcdRegistry.class);
      ipcdDeviceDao = EasyMock.createMock(IpcdDeviceDao.class);
      deviceDao = EasyMock.createMock(DeviceDAO.class);
      service = new IpcdService(
            EasyMock.createNiceMock(PlatformMessageBus.class),
            MoreExecutors.directExecutor(),
            registry,
            ipcdDeviceDao,
            deviceDao,
            EasyMock.createNiceMock(BeanAttributesTransformer.class),
            EasyMock.createNiceMock(PlacePopulationCacheManager.class));

      IpcdDeviceType type = new IpcdDeviceType();
      type.setVendor(VENDOR);
      type.setModel(MODEL);
      protocolAddress = IpcdProtocol.ipcdAddress(IpcdDeviceTypeRegistry.INSTANCE.createDeviceFromType(type, SERIAL)).getRepresentation();

      device = new IpcdDevice();
      device.setProtocolAddress(protocolAddress);
      device.setPlaceId(devicePlace);
      device.setRegistrationState(IpcdDevice.RegistrationState.REGISTERED);
   }

   private PlatformMessage client(MessageBody body, UUID place) {
      return PlatformMessage
            .buildRequest(body, Address.clientAddress("android", "1"), Address.platformService(com.iris.messages.service.IpcdService.NAMESPACE))
            .withPlaceId(place)
            .withCorrelationId("test")
            .create();
   }

   private PlatformMessage internal(MessageBody body) {
      return PlatformMessage
            .buildRequest(body, Address.platformService(PlatformConstants.SERVICE_PLACES), Address.platformService(com.iris.messages.service.IpcdService.NAMESPACE))
            .withCorrelationId("test")
            .create();
   }

   private MessageBody forceUnregister() {
      return ForceUnregisterRequest.builder().withProtocolAddress(protocolAddress).build();
   }

   private MessageBody find() {
      IpcdDeviceType type = new IpcdDeviceType();
      type.setVendor(VENDOR);
      type.setModel(MODEL);
      return FindDeviceRequest.builder().withDeviceType(type.toMap()).withSn(SERIAL).build();
   }

   private void expectDevice() {
      EasyMock.expect(ipcdDeviceDao.findByProtocolAddress(protocolAddress)).andReturn(device).anyTimes();
   }

   private void expectFindDevice() {
      EasyMock.expect(ipcdDeviceDao.findByProtocolAddress(EasyMock.anyObject(Address.class))).andReturn(device).anyTimes();
   }

   private void expectNoPlatformDevice() {
      EasyMock.expect(deviceDao.findByProtocolAddress(protocolAddress)).andReturn((Device) null).anyTimes();
   }

   private void replayAll() {
      EasyMock.replay(registry, ipcdDeviceDao, deviceDao);
   }

   private void assertRefused(PlatformMessage message) throws Exception {
      try {
         service.handleRequest(message);
         fail("expected the request to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
      EasyMock.verify(registry); // strict mock: nothing was unregistered
   }

   // force unregister

   @Test
   public void testClientAtAnotherPlaceCannotForceUnregister() throws Exception {
      expectDevice();
      replayAll();
      assertRefused(client(forceUnregister(), otherPlace));
   }

   @Test
   public void testClientAtTheDevicesPlaceCanForceUnregister() throws Exception {
      expectDevice();
      expectNoPlatformDevice();
      registry.forceUnregister(device);
      EasyMock.expectLastCall();
      replayAll();

      assertEquals(ForceUnregisterResponse.instance(), service.handleRequest(client(forceUnregister(), devicePlace)));
      EasyMock.verify(registry);
   }

   @Test
   public void testOtherServicesCanForceUnregister() throws Exception {
      expectDevice();
      expectNoPlatformDevice();
      registry.forceUnregister(device);
      EasyMock.expectLastCall();
      replayAll();

      assertEquals(ForceUnregisterResponse.instance(), service.handleRequest(internal(forceUnregister())));
      EasyMock.verify(registry);
   }

   // find

   @Test
   public void testClientAtAnotherPlaceCannotFindADevice() throws Exception {
      expectFindDevice();
      replayAll();
      assertRefused(client(find(), otherPlace));
   }

   @Test
   public void testClientAtTheDevicesPlaceCanFindIt() throws Exception {
      expectFindDevice();
      expectNoPlatformDevice();
      replayAll();

      assertEquals(FindDeviceResponse.NAME, service.handleRequest(client(find(), devicePlace)).getMessageType());
   }

   @Test
   public void testAnyClientCanFindADeviceNobodyHasClaimed() throws Exception {
      device.setPlaceId(null);
      expectFindDevice();
      expectNoPlatformDevice();
      replayAll();

      assertEquals(FindDeviceResponse.NAME, service.handleRequest(client(find(), otherPlace)).getMessageType());
   }

   // connected and heartbeat events mark devices online at the bridge they name

   private MessageBody connected() {
      return DeviceConnectedEvent.builder().withProtocolAddress(protocolAddress).build();
   }

   private MessageBody heartbeat() {
      return DeviceHeartBeatEvent.builder().withPartitionId(1).withConnectedDevices(ImmutableSet.of(protocolAddress)).build();
   }

   // a frame with isRequest=false, as client-bridge forwards it
   private PlatformMessage clientEvent(MessageBody body) {
      return PlatformMessage
            .buildMessage(body, Address.clientAddress("android", "1"), Address.platformService(com.iris.messages.service.IpcdService.NAMESPACE))
            .withPlaceId(otherPlace)
            .withActor(Address.platformService(UUID.randomUUID(), "person"))
            .create();
   }

   @Test
   public void testClientConnectedEventIsDropped() throws Exception {
      replayAll();
      service.handleEvent(clientEvent(connected()));
      EasyMock.verify(registry);
   }

   @Test
   public void testIpcdBridgeConnectedEventIsHandled() throws Exception {
      // IpcdSession.onReportOnline
      PlatformMessage message = PlatformMessage
            .buildMessage(connected(), Address.bridgeAddress(IpcdProtocol.NAMESPACE), Address.platformService(com.iris.messages.service.IpcdService.NAMESPACE))
            .withActor(Address.clientAddress("ipcd-bridge", "0"))
            .withPlaceId(devicePlace)
            .create();
      registry.onConnected(message);
      EasyMock.expectLastCall();
      replayAll();

      service.handleEvent(message);
      EasyMock.verify(registry);
   }

   @Test
   public void testHeartbeatOnThePlatformBusIsIgnored() throws Exception {
      // SessionHeartBeater sends heartbeats on the intraservice bus (IpcdHeartbeatListener), so
      // one on the platform bus came from somewhere else, even when it claims to be the bridge
      replayAll();
      service.handleEvent(clientEvent(heartbeat()));
      service.handleEvent(PlatformMessage
            .buildMessage(heartbeat(), Address.clientAddress("ipcd-bridge", "0"), Address.platformService(com.iris.messages.service.IpcdService.NAMESPACE))
            .create());
      EasyMock.verify(registry);
   }

   // an add response completes a registration's pending add

   private MessageBody addResponse() {
      return MessageBody.buildMessage(MessageConstants.MSG_ADD_DEVICE_RESPONSE, ImmutableMap.<String, Object>of(MessageConstants.ATTR_DEVICEADDRESS, "DRIV:dev:" + UUID.randomUUID()));
   }

   @Test
   public void testClientAddResponseIsDropped() throws Exception {
      replayAll();
      service.handleEvent(clientEvent(addResponse()));
      EasyMock.verify(registry);
   }

   @Test
   public void testDeviceServiceAddResponseIsHandled() throws Exception {
      PlatformMessage message = PlatformMessage
            .buildMessage(addResponse(), Address.platformService(PlatformConstants.SERVICE_DEVICES), Address.platformService(com.iris.messages.service.IpcdService.NAMESPACE))
            .withCorrelationId("registration")
            .create();
      registry.onEvent(message);
      EasyMock.expectLastCall();
      replayAll();

      service.handleEvent(message);
      EasyMock.verify(registry);
   }
}
