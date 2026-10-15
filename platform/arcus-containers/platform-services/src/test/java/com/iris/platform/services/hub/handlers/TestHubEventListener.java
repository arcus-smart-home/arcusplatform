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

import java.util.Date;
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
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceConnectionCapability.LostDeviceRequest;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubAdvancedCapability.GetKnownDevicesRequest;
import com.iris.messages.capability.HubAdvancedCapability.GetKnownDevicesResponse;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubCapability.HubConnectedEvent;
import com.iris.messages.capability.HubConnectionCapability;
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
      // the ONLINE ValueChange already landed, stamped just before the authorization;
      // lastchange moves up to the authorization so a late HANDSHAKE stays stale
      String stored = expectStoredConnectionState(HubConnectionCapability.STATE_ONLINE, connected.getTimestamp().getTime() - 5);
      EasyMock.expect(mockHubDao.updateConnectionState(hubId, HubConnectionCapability.STATE_ONLINE, connected.getTimestamp(), stored)).andReturn(true);
      replay();

      listener.onMessage(connected);
      // the hub-bridge *should* have already sent the ONLINE value change, so nothing is broadcast
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
      // already ONLINE as of this authorization
      expectStoredConnectionState(HubConnectionCapability.STATE_ONLINE, connected.getTimestamp().getTime());
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

   @Test
   public void testLateHandshakeDoesNotOverwriteOnline() throws Exception {
      // ONLINE was stored first; the earlier HANDSHAKE was processed second
      expectStoredConnectionState(HubConnectionCapability.STATE_ONLINE, 2000L);
      expectGetHubByIdAndReturn(hub);
      replay();

      listener.onMessage(connectionValueChange(HubConnectionCapability.STATE_HANDSHAKE, 1000L));
      assertNoMessages();

      verify();
   }

   @Test
   public void testHandshakeInSameMillisecondDoesNotOverwriteOnline() throws Exception {
      expectStoredConnectionState(HubConnectionCapability.STATE_ONLINE, 2000L);
      expectGetHubByIdAndReturn(hub);
      replay();

      listener.onMessage(connectionValueChange(HubConnectionCapability.STATE_HANDSHAKE, 2000L));
      assertNoMessages();

      verify();
   }

   @Test
   public void testHandshakeFromReconnectIsSaved() throws Exception {
      // the hub dropped and reconnected: the new HANDSHAKE is newer than the old ONLINE
      String stored = expectStoredConnectionState(HubConnectionCapability.STATE_ONLINE, 1000L);
      expectGetHubByIdAndReturn(hub);
      expectConnectionStateWrite(HubConnectionCapability.STATE_HANDSHAKE, 2000L, stored, true);
      replay();

      listener.onMessage(connectionValueChange(HubConnectionCapability.STATE_HANDSHAKE, 2000L));
      assertNoMessages();

      verify();
   }

   @Test
   public void testNewerConnectionStateIsSaved() throws Exception {
      String stored = expectStoredConnectionState(HubConnectionCapability.STATE_HANDSHAKE, 1000L);
      expectGetHubByIdAndReturn(hub);
      expectConnectionStateWrite(HubConnectionCapability.STATE_ONLINE, 2000L, stored, true);
      replay();

      listener.onMessage(connectionValueChange(HubConnectionCapability.STATE_ONLINE, 2000L));
      assertNoMessages();

      verify();
   }

   @Test
   public void testFirstConnectionStateIsSaved() throws Exception {
      EasyMock.expect(mockHubDao.findConnectionState(hubId)).andReturn(ImmutableMap.of());
      expectGetHubByIdAndReturn(hub);
      expectConnectionStateWrite(HubConnectionCapability.STATE_HANDSHAKE, 1000L, null, true);
      replay();

      listener.onMessage(connectionValueChange(HubConnectionCapability.STATE_HANDSHAKE, 1000L));
      assertNoMessages();

      verify();
   }

   @Test
   public void testConcurrentWriteIsRechecked() throws Exception {
      // ONLINE lands between this HANDSHAKE's read and its write; the re-check drops the HANDSHAKE
      String offline = expectStoredConnectionState(HubConnectionCapability.STATE_OFFLINE, 500L);
      expectGetHubByIdAndReturn(hub);
      expectConnectionStateWrite(HubConnectionCapability.STATE_HANDSHAKE, 1000L, offline, false);
      expectStoredConnectionState(HubConnectionCapability.STATE_ONLINE, 2000L);
      replay();

      listener.onMessage(connectionValueChange(HubConnectionCapability.STATE_HANDSHAKE, 1000L));
      assertNoMessages();

      verify();
   }

   @Test
   public void testOtherAttributesInStaleMessageAreStillSaved() throws Exception {
      expectStoredConnectionState(HubConnectionCapability.STATE_ONLINE, 2000L);
      expectGetHubByIdAndReturn(hub);
      Map<String, Object> attributes = new HashMap<>();
      attributes.put(HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_HANDSHAKE);
      attributes.put(HubConnectionCapability.ATTR_LASTCHANGE, new Date(1000L));
      attributes.put(HubZwaveCapability.ATTR_FIRMWARE, "firmwares");
      mockHubDao.updateAttributes(hubId, ImmutableMap.of(HubZwaveCapability.ATTR_FIRMWARE, "firmwares"));
      EasyMock.expectLastCall();
      replay();

      listener.onMessage(PlatformMessage.createBroadcast(MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, attributes), Address.hubService(hubId, "hub")));
      assertNoMessages();

      verify();
   }

   @Test
   public void testStaleHubStateIsDroppedWithConnectionState() throws Exception {
      // a late echo of NORMAL/ONLINE after the hub was marked DOWN/OFFLINE
      expectStoredConnectionState(HubConnectionCapability.STATE_OFFLINE, 2000L);
      hub.setState(HubCapability.STATE_DOWN);
      expectGetHubByIdAndReturn(hub);
      replay();

      listener.onMessage(PlatformMessage.createBroadcast(
            MessageBody.buildMessage(
                  Capability.EVENT_VALUE_CHANGE,
                  ImmutableMap.of(
                        HubCapability.ATTR_STATE, HubCapability.STATE_NORMAL,
                        HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_ONLINE,
                        HubConnectionCapability.ATTR_LASTCHANGE, new Date(1000L)
                  )
            ),
            Address.hubService(hubId, "hub")
      ));
      // nothing saved: no hub:state NORMAL left behind next to OFFLINE
      assertNoMessages();

      verify();
   }

   @Test
   public void testAuthorizedHubLeftAtHandshakeIsSetOnline() throws Exception {
      // the ONLINE ValueChange was lost; HubConnectedEvent from the bridge confirms authorization
      PlatformMessage connected = authorizedEvent();
      Date authorizedAt = connected.getTimestamp();
      expectGetPartition(connected);
      expectOnline(hubId);
      String stored = expectStoredConnectionState(HubConnectionCapability.STATE_HANDSHAKE, authorizedAt.getTime() - 1000);
      EasyMock.expect(mockHubDao.updateConnectionState(hubId, HubConnectionCapability.STATE_ONLINE, authorizedAt, stored)).andReturn(true);
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(hub.getPlace().toString())).andReturn(Population.NAME_GENERAL);
      replay();

      listener.onMessage(connected);

      PlatformMessage valueChange = platformBus.take();
      assertEquals(Capability.EVENT_VALUE_CHANGE, valueChange.getMessageType());
      assertEquals(Address.hubService(hubId, "hub"), valueChange.getSource());
      assertEquals(hub.getPlace().toString(), valueChange.getPlaceId());
      assertEquals(HubConnectionCapability.STATE_ONLINE, valueChange.getValue().getAttributes().get(HubConnectionCapability.ATTR_STATE));
      assertEquals(authorizedAt, HubConnectionCapability.TYPE_LASTCHANGE.coerce(valueChange.getValue().getAttributes().get(HubConnectionCapability.ATTR_LASTCHANGE)));
      assertNoMessages();

      verify();
   }

   @Test
   public void testHandshakeFromLaterReconnectIsKeptOnAuthorization() throws Exception {
      // the hub reconnected after this authorization and is negotiating again
      PlatformMessage connected = authorizedEvent();
      expectGetPartition(connected);
      expectOnline(hubId);
      expectStoredConnectionState(HubConnectionCapability.STATE_HANDSHAKE, connected.getTimestamp().getTime() + 1000);
      replay();

      listener.onMessage(connected);
      assertNoMessages();

      verify();
   }

   @Test
   public void testAuthorizationRechecksWhenStateChangesConcurrently() throws Exception {
      // the real ONLINE lands between the read and the write
      PlatformMessage connected = authorizedEvent();
      Date authorizedAt = connected.getTimestamp();
      expectGetPartition(connected);
      expectOnline(hubId);
      String stored = expectStoredConnectionState(HubConnectionCapability.STATE_HANDSHAKE, authorizedAt.getTime() - 1000);
      EasyMock.expect(mockHubDao.updateConnectionState(hubId, HubConnectionCapability.STATE_ONLINE, authorizedAt, stored)).andReturn(false);
      String online = expectStoredConnectionState(HubConnectionCapability.STATE_ONLINE, authorizedAt.getTime() - 5);
      EasyMock.expect(mockHubDao.updateConnectionState(hubId, HubConnectionCapability.STATE_ONLINE, authorizedAt, online)).andReturn(true);
      replay();

      listener.onMessage(connected);
      // it was ONLINE already, so nothing to broadcast
      assertNoMessages();

      verify();
   }

   @Test
   public void testAuthorizationErrorDoesNotFailConnect() throws Exception {
      PlatformMessage connected = authorizedEvent();
      expectGetPartition(connected);
      expectOnline(hubId);
      EasyMock.expect(mockHubDao.findConnectionState(hubId)).andThrow(new RuntimeException("cassandra unavailable"));
      replay();

      listener.onMessage(connected);
      assertNoMessages();

      verify();
   }

   @Test
   public void testIdenticalConnectionStateIsNotRewritten() throws Exception {
      // the echo of HubRegistry's own NORMAL/ONLINE write: no conditional write, and
      // hub:state still goes through the normal save as before
      expectStoredConnectionState(HubConnectionCapability.STATE_ONLINE, 2000L);
      expectGetHubByIdAndReturn(hub);
      Capture<Hub> saved = expectSaveHub();
      replay();

      listener.onMessage(PlatformMessage.createBroadcast(
            MessageBody.buildMessage(
                  Capability.EVENT_VALUE_CHANGE,
                  ImmutableMap.of(
                        HubCapability.ATTR_STATE, HubCapability.STATE_NORMAL,
                        HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_ONLINE,
                        HubConnectionCapability.ATTR_LASTCHANGE, new Date(2000L)
                  )
            ),
            Address.hubService(hubId, "hub")
      ));
      assertEquals(HubCapability.STATE_NORMAL, saved.getValue().getState());
      assertNoMessages();

      verify();
   }

   @Test
   public void testStaleOfflineEchoIsDropped() throws Exception {
      // HubRegistry's DOWN/OFFLINE echo arrives after the hub reconnected and went ONLINE
      expectStoredConnectionState(HubConnectionCapability.STATE_ONLINE, 2000L);
      hub.setState(HubCapability.STATE_NORMAL);
      expectGetHubByIdAndReturn(hub);
      replay();

      listener.onMessage(PlatformMessage.createBroadcast(
            MessageBody.buildMessage(
                  Capability.EVENT_VALUE_CHANGE,
                  ImmutableMap.of(
                        HubCapability.ATTR_STATE, HubCapability.STATE_DOWN,
                        HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_OFFLINE,
                        HubConnectionCapability.ATTR_LASTCHANGE, new Date(1000L)
                  )
            ),
            Address.hubService(hubId, "hub")
      ));
      // nothing saved: hub:state isn't set back to DOWN
      assertNoMessages();

      verify();
   }

   @Test
   public void testRetriesExhaustedStillSavesOtherAttributes() throws Exception {
      expectGetHubByIdAndReturn(hub);
      for(int i = 0; i < 3; i++) {
         String stored = expectStoredConnectionState(HubConnectionCapability.STATE_OFFLINE, 500L + i);
         expectConnectionStateWrite(HubConnectionCapability.STATE_ONLINE, 2000L, stored, false);
      }
      mockHubDao.updateAttributes(hubId, ImmutableMap.of(HubZwaveCapability.ATTR_FIRMWARE, "firmwares"));
      EasyMock.expectLastCall();
      replay();

      listener.onMessage(connectionValueChangeWithFirmware(HubConnectionCapability.STATE_ONLINE, 2000L));
      assertNoMessages();

      verify();
   }

   @Test
   public void testCassandraErrorStillSavesOtherAttributes() throws Exception {
      expectGetHubByIdAndReturn(hub);
      EasyMock.expect(mockHubDao.findConnectionState(hubId)).andThrow(new RuntimeException("cassandra unavailable"));
      mockHubDao.updateAttributes(hubId, ImmutableMap.of(HubZwaveCapability.ATTR_FIRMWARE, "firmwares"));
      EasyMock.expectLastCall();
      replay();

      listener.onMessage(connectionValueChangeWithFirmware(HubConnectionCapability.STATE_ONLINE, 2000L));
      assertNoMessages();

      verify();
   }

   @Test
   public void testNumericLastChangeIsOrdered() throws Exception {
      // after Kafka, lastchange arrives as epoch millis rather than a Date
      expectStoredConnectionState(HubConnectionCapability.STATE_ONLINE, 2000L);
      expectGetHubByIdAndReturn(hub);
      replay();

      listener.onMessage(PlatformMessage.createBroadcast(
            MessageBody.buildMessage(
                  Capability.EVENT_VALUE_CHANGE,
                  ImmutableMap.of(
                        HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_HANDSHAKE,
                        HubConnectionCapability.ATTR_LASTCHANGE, 1000L
                  )
            ),
            Address.hubService(hubId, "hub")
      ));
      assertNoMessages();

      verify();
   }

   @Test
   public void testNumericLastChangeIsWritten() throws Exception {
      String stored = expectStoredConnectionState(HubConnectionCapability.STATE_HANDSHAKE, 1000L);
      expectGetHubByIdAndReturn(hub);
      EasyMock.expect(mockHubDao.updateConnectionState(hubId, HubConnectionCapability.STATE_ONLINE, 2000L, stored)).andReturn(true);
      replay();

      listener.onMessage(PlatformMessage.createBroadcast(
            MessageBody.buildMessage(
                  Capability.EVENT_VALUE_CHANGE,
                  ImmutableMap.of(
                        HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_ONLINE,
                        HubConnectionCapability.ATTR_LASTCHANGE, 2000L
                  )
            ),
            Address.hubService(hubId, "hub")
      ));
      assertNoMessages();

      verify();
   }

   @Test
   public void testConnectionStateWithoutLastChangeIsPlainUpdate() throws Exception {
      expectGetHubByIdAndReturn(hub);
      mockHubDao.updateAttributes(hubId, ImmutableMap.of(HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_ONLINE));
      EasyMock.expectLastCall();
      replay();

      listener.onMessage(PlatformMessage.createBroadcast(
            MessageBody.buildMessage(
                  Capability.EVENT_VALUE_CHANGE,
                  ImmutableMap.of(HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_ONLINE)
            ),
            Address.hubService(hubId, "hub")
      ));
      assertNoMessages();

      verify();
   }

   private PlatformMessage connectionValueChangeWithFirmware(String state, long lastChange) {
      return PlatformMessage.createBroadcast(
            MessageBody.buildMessage(
                  Capability.EVENT_VALUE_CHANGE,
                  ImmutableMap.of(
                        HubConnectionCapability.ATTR_STATE, state,
                        HubConnectionCapability.ATTR_LASTCHANGE, new Date(lastChange),
                        HubZwaveCapability.ATTR_FIRMWARE, "firmwares"
                  )
            ),
            Address.hubService(hubId, "hub")
      );
   }

   @Test
   public void testIsStaleConnectionState() {
      Date earlier = new Date(1000L);
      Date later = new Date(2000L);
      String handshake = HubConnectionCapability.STATE_HANDSHAKE;
      String online = HubConnectionCapability.STATE_ONLINE;
      String offline = HubConnectionCapability.STATE_OFFLINE;

      assertTrue(HubEventListener.isStaleConnectionState(online, later, handshake, earlier));
      assertTrue(HubEventListener.isStaleConnectionState(offline, later, online, earlier));
      assertTrue(HubEventListener.isStaleConnectionState(online, later, handshake, later));
      assertFalse(HubEventListener.isStaleConnectionState(handshake, earlier, online, later));
      assertFalse(HubEventListener.isStaleConnectionState(handshake, later, online, later));
      assertFalse(HubEventListener.isStaleConnectionState(online, later, offline, later));
      // nothing to compare against
      assertFalse(HubEventListener.isStaleConnectionState(null, null, handshake, earlier));
      assertFalse(HubEventListener.isStaleConnectionState(online, later, handshake, null));
   }

   private PlatformMessage connectionValueChange(String state, long lastChange) {
      return PlatformMessage.createBroadcast(
            MessageBody.buildMessage(
                  Capability.EVENT_VALUE_CHANGE,
                  ImmutableMap.of(
                        HubConnectionCapability.ATTR_STATE, state,
                        HubConnectionCapability.ATTR_LASTCHANGE, new Date(lastChange)
                  )
            ),
            Address.hubService(hubId, "hub")
      );
   }

   /** @return the stored lastchange JSON, as passed to the conditional write */
   private String expectStoredConnectionState(String state, long lastChange) {
      String stored = JSON.toJson(new Date(lastChange));
      EasyMock
         .expect(mockHubDao.findConnectionState(hubId))
         .andReturn(ImmutableMap.of(
               HubConnectionCapability.ATTR_STATE, JSON.toJson(state),
               HubConnectionCapability.ATTR_LASTCHANGE, stored
         ));
      return stored;
   }

   private void expectConnectionStateWrite(String state, long lastChange, String expectedLastChange, boolean applied) {
      EasyMock
         .expect(mockHubDao.updateConnectionState(hubId, state, new Date(lastChange), expectedLastChange))
         .andReturn(applied);
   }

   private PlatformMessage authorizedEvent() {
      return
            PlatformMessage
               .broadcast()
               .from(Address.hubService(hubId, "hub"))
               .withActor(Address.platformService("hub-bridge-0", "hub-bridge"))
               .withPlaceId(hub.getPlace())
               .withPayload(createOnConnectedEvent())
               .create();
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
      mockRegistry.online(hubId, partition.getId(), "hub-bridge-0");
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

