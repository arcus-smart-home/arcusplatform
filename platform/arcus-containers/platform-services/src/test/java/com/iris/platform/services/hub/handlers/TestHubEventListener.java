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
package com.iris.platform.services.hub.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceConnectionCapability.LostDeviceRequest;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubAdvancedCapability.GetKnownDevicesRequest;
import com.iris.messages.capability.HubAdvancedCapability.GetKnownDevicesResponse;
import com.iris.messages.capability.HubCapability.HubConnectedEvent;
import com.iris.messages.capability.HubCapability.HubDisconnectedEvent;
import com.iris.messages.capability.HubZwaveCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Hub;
import com.iris.messages.type.Population;
import com.iris.platform.partition.DefaultPartition;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.PlatformPartition;
import com.iris.platform.services.hub.HubRegistry;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Modules({ CapabilityRegistryModule.class, InMemoryMessageModule.class })
@Mocks({ HubDAO.class, HubRegistry.class, Partitioner.class, DeviceDAO.class, PlacePopulationCacheManager.class })
public class TestHubEventListener extends IrisMockTestCase {
   String hubId = "ABC-1234";
   PlatformPartition partition = new DefaultPartition(13);
   Hub hub;

   // unit under test
   @Inject HubEventListener listener;

   @Inject InMemoryPlatformMessageBus platformBus;
   @Inject HubDAO mockHubDao;
   @Inject HubRegistry mockRegistry;
   @Inject Partitioner mockPartitioner;
   @Inject DeviceDAO mockDeviceDao;
   @Inject protected PlacePopulationCacheManager mockPopulationCacheMgr;

   @Provides @Named(HubEventListener.PROP_THREADPOOL)
   public Executor executor() {
      return MoreExecutors.directExecutor();
   }

   @Before
   public void setUp() throws Exception {
      super.setUp();
      reset(); // clear out anything invoked by startup

      hub = Fixtures.createHub();
      hub.setId(hubId);
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }

   @After
   public void tearDown() throws Exception {
      reset(); // allow shutdown to call methods on mocks
      super.tearDown();
   }

   @Test
   public void testHubConnected() throws Exception {
      hub.setLastDeviceAddRemove(UUID.randomUUID());
      // this is the version from the hub
      PlatformMessage connected = PlatformMessage.createBroadcast(createOnConnectedEvent(), Address.hubService(hubId, "hub"));
      expectGetPartition(connected);
      EasyMock.expect(mockHubDao.findById(hubId)).andReturn(hub);
      replay();

      listener.onMessage(connected);
      // the hub-bridge *should* have already sent the ONLINE value change
      assertNoMessages();

      verify();
   }

   @Test
   public void testHubAuthorized() throws Exception {
      // this is the version from the hub bridge
      PlatformMessage connected = broadcast(createOnConnectedEvent());
      expectGetPartition(connected);
      expectOnline(hubId);
      replay();

      listener.onMessage(connected);
      // the hub-bridge *should* have already sent the ONLINE value change
      assertNoMessages();

      verify();
   }

   @Test
   public void testHubDontSync() throws Exception {
      MessageBody body = createOnConnectedEvent();

      Device matchedDevice = createUniqueProtocolIdDevice();
      Device lostDevice = createUniqueProtocolIdDevice();

      PlatformMessage connected = broadcast(body);
      expectGetPartition(connected);
      expectOnline(hubId);
      EasyMock.expect(mockHubDao.findById(hubId)).andReturn(hub);
      EasyMock.expect(mockDeviceDao.findByHubId(hubId)).andReturn(ImmutableList.<Device>of(matchedDevice,lostDevice));
      replay();

      listener.onMessage(connected);
      assertNoMessages();
   }

   @Test
   public void testHubConnectedDeviceSync() throws Exception {
      hub.setLastDeviceAddRemove(UUID.randomUUID());
      MessageBody body = createOnConnectedEvent();
      hub.setLastDeviceAddRemove(UUID.randomUUID());

      Device matchedDevice = createUniqueProtocolIdDevice();
      Device unknownDevice = createUniqueProtocolIdDevice();
      Device lostDevice = createUniqueProtocolIdDevice();

      PlatformMessage connected = PlatformMessage.createBroadcast(body, Address.hubService(hubId, "hub")); // version from the hub
      expectGetPartition(connected);

      EasyMock.expect(mockHubDao.findById(hubId)).andReturn(hub);
      EasyMock.expect(mockDeviceDao.findByHubId(hubId)).andReturn(ImmutableList.<Device>of(matchedDevice,lostDevice));
      replay();
      listener.onMessage(connected);
      PlatformMessage message = assertMessagePutOnBus(GetKnownDevicesRequest.NAME,Address.hubService(hubId, "hub"),ImmutableMap.of());

      //report back some known devices, the matched and unknown devices
      List<String>knownDevices=ImmutableList.of(matchedDevice.getProtocolAddress(),unknownDevice.getProtocolAddress());
      MessageBody getKnownDevicesResponse = GetKnownDevicesResponse.builder().withDevices(knownDevices).build();
      PlatformMessage response = PlatformMessage.buildResponse(message, getKnownDevicesResponse).create();
      listener.onMessage(response);

      //should fire off a single lost device request
      assertMessagePutOnBus(LostDeviceRequest.NAME,Address.fromString(lostDevice.getAddress()),ImmutableMap.of());
      verify();
   }

   @Test
   public void testHubDisconnected() throws Exception {
      PlatformMessage connected = broadcast(HubDisconnectedEvent.instance());
      expectOffline(hubId);
      replay();

      listener.onMessage(connected);
      assertNoMessages();

      verify();
   }

   @Test
   public void testSpuriousHubDisconnected() throws Exception {
      PlatformMessage connected = broadcast(HubDisconnectedEvent.instance());
      expectOffline(hubId);
      replay();

      listener.onMessage(connected);
      assertNoMessages();

      verify();
   }

   @Test
   public void testValueChange() throws Exception {
      PlatformMessage valueChange = PlatformMessage.createBroadcast(
            MessageBody.buildMessage(
                  Capability.EVENT_VALUE_CHANGE,
                  ImmutableMap.of(
                        HubAdvancedCapability.ATTR_AGENTVER, "1.0.97",
                        HubAdvancedCapability.ATTR_OSVER, "2.0.1.022"
                  )
            ),
            Address.hubService(hubId, "hub")
      );
      expectGetHubByIdAndReturn(hub);
      Capture<Hub> saved = expectSaveHub();
      replay();

      listener.onMessage(valueChange);
      Hub hub = saved.getValue();
      assertEquals("1.0.97", hub.getAgentver());
      assertEquals("2.0.1.022", hub.getOsver());
      assertNoMessages();

      verify();
   }

   @Test
   public void testUntrackedValuesChanged() throws Exception {
      PlatformMessage valueChange = PlatformMessage.createBroadcast(
            MessageBody.buildMessage(
                  Capability.EVENT_VALUE_CHANGE,
                  ImmutableMap.of(HubZwaveCapability.ATTR_FIRMWARE, "firmwares")
            ),
            Address.hubService(hubId, "hub")
      );
      expectGetHubByIdAndReturn(hub);
      mockHubDao.updateAttributes(hubId, valueChange.getValue().getAttributes());
      EasyMock.expectLastCall();
      replay();

      listener.onMessage(valueChange);
      assertNoMessages();

      verify();
   }

   private PlatformMessage broadcast(MessageBody message) {
      return
            PlatformMessage
               .broadcast()
               .from(Address.hubService(hubId, "hub"))
               .withActor(Address.platformService("hub-bridge-0", "hub-bridge"))
               .withPayload(message)
               .create();
   }

   private MessageBody createOnConnectedEvent(){
      Map<String,Object>attrs=new HashMap<>(HubConnectedEvent.instance().getAttributes());
      attrs.put(HubAdvancedCapability.ATTR_LASTDEVICEADDREMOVE, hub.getLastDeviceAddRemove());
      MessageBody body = MessageBody.buildMessage(HubConnectedEvent.NAME, attrs);
      return body;
   }

   private Device createUniqueProtocolIdDevice(){
      Device device = Fixtures.createDevice();
      device.setProtocolid((UUID.randomUUID().toString()));
      device.setProtocolAddress(Address.protocolAddress("Ipcd", ProtocolDeviceId.hashDeviceId(device.getProtocolid())).getRepresentation());
      return device;
   }

   private void expectGetPartition(PlatformMessage connected) {
      expectGetPartition(connected, partition);
   }

   private void expectGetPartition(PlatformMessage connect, PlatformPartition partition) {
      EasyMock
         .expect(mockPartitioner.getPartitionForMessage(connect))
         .andReturn(partition);
   }

   private void expectOnline(String hubId) {
      expectOnline(hubId, partition.getId());
   }

   private void expectOnline(String hubId, int partitionId) {
      mockRegistry.online(hubId, partitionId, "hub-bridge-0");
      EasyMock.expectLastCall();
   }

   private void expectOffline(String hubId) {
      mockRegistry.offline(hubId, "hub-bridge-0");
      EasyMock.expectLastCall();
   }

   private Hub expectGetHubByIdAndReturn(Hub hub) {
      Hub value = hub.copy();
      EasyMock
         .expect(mockHubDao.findById(hub.getId()))
         .andReturn(value);
      return value;
   }

   private Capture<Hub> expectSaveHub() {
      Capture<Hub> captured = EasyMock.newCapture();
      EasyMock
         .expect(mockHubDao.save(EasyMock.capture(captured)))
         .andAnswer(() -> captured.getValue().copy());
      return captured;
   }

   private PlatformMessage assertMessagePutOnBus(String messageType,Address destination,Map<String,Object> attributes) throws InterruptedException, TimeoutException {
      PlatformMessage message = platformBus.take();
      assertEquals(messageType, message.getMessageType());
      assertEquals("messageType: " + messageType + " destination does not match",destination.getRepresentation(),message.getDestination().getRepresentation());
      assertEquals("attributes match",attributes,message.getValue().getAttributes());

      return message;
   }

   private void assertNoMessages() {
      assertNull(platformBus.poll());
   }

}

