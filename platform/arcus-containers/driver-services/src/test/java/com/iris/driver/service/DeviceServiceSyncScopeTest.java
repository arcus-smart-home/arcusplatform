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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.protocol.ipcd.IpcdDeviceDao;
import com.iris.driver.platform.PlatformDriverService;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Device;
import com.iris.messages.model.Hub;
import com.iris.messages.model.Person;
import com.iris.messages.service.DeviceService.DevicesDegradedEvent;
import com.iris.messages.service.DeviceService.SyncDevicesRequest;
import com.iris.messages.service.DeviceService.SyncDevicesResponse;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.security.authz.AuthzMetrics;
import com.iris.security.authz.DisabledRiskyOperations;
import com.iris.security.authz.RoleAuthorizer;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * A device sync answers with the place's devices, driver reflexes and the hash of every pin at the
 * place, and reconciles the place's devices with the list it was sent, so only the place's own hub
 * may send it. Every DAO is a strict mock: a refused sync that looked anything up fails the test.
 */
@Mocks({
   DeviceDAO.class,
   HubDAO.class,
   IpcdDeviceDao.class,
   PersonDAO.class,
   PersonPlaceAssocDAO.class,
   DriverRegistry.class,
   DriverExecutorRegistry.class,
   PlaceDAO.class,
   PopulationDAO.class
})
@Modules({ InMemoryMessageModule.class })
public class DeviceServiceSyncScopeTest extends IrisMockTestCase {
   private static final String PIN = "1234";

   @Inject private DeviceDAO deviceDao;
   @Inject private HubDAO hubDao;
   @Inject private IpcdDeviceDao ipcdDeviceDao;
   @Inject private PersonDAO personDao;
   @Inject private PersonPlaceAssocDAO personPlaceAssocDao;
   @Inject private DeviceService service;
   @Inject private InMemoryPlatformMessageBus bus;
   // not one of the @Mocks, which would expect the lifecycle calls the injector makes on it
   private final PlatformDriverService driverService = EasyMock.createMock(PlatformDriverService.class);

   private final UUID hubPlace = UUID.randomUUID();
   private final UUID otherPlace = UUID.randomUUID();
   private final Hub hub = hub("TST-0001", hubPlace);
   private final Hub otherHub = hub("TST-0002", otherPlace);
   private final UUID personId = UUID.randomUUID();
   private final List<Device> devices = ImmutableList.of();

   @Override
   protected void replay() {
      super.replay();
      EasyMock.replay(driverService);
   }

   @Override
   protected void verify() {
      super.verify();
      EasyMock.verify(driverService);
   }

   private DeviceServiceHandler handler() {
      return new DeviceServiceHandler(bus, new DriverServiceConfig(), deviceDao, hubDao, ipcdDeviceDao, personDao, personPlaceAssocDao, service, driverService);
   }

   private static Hub hub(String id, UUID place) {
      Hub hub = new Hub();
      hub.setId(id);
      hub.setPlace(place);
      return hub;
   }

   // what the hub agent sends (ReflexController) as hub-bridge passes it on: from the hub's own
   // address, with the hub's place as the header
   private static PlatformMessage fromHub(String type, Hub sender, UUID bodyPlace, UUID placeHeader) throws IOException {
      return sync(type, Address.hubService(sender.getId(), "hub"), null, bodyPlace, placeHeader);
   }

   // what client-bridge passes on: from the session's client address, with the person as the actor
   // and the session's active place as the header
   private PlatformMessage fromClient(String type, UUID place) throws IOException {
      return sync(type, Address.clientAddress("android", "session-1"), Address.platformService(personId, PersonCapability.NAMESPACE), place, place);
   }

   private static PlatformMessage sync(String type, Address source, Address actor, UUID bodyPlace, UUID placeHeader) throws IOException {
      MessageBody body = MessageBody.buildMessage(type, ImmutableMap.<String, Object>of(
            SyncDevicesRequest.ATTR_ACCOUNTID, UUID.randomUUID().toString(),
            SyncDevicesRequest.ATTR_PLACEID, bodyPlace.toString(),
            SyncDevicesRequest.ATTR_DEVICES, compress("[]")));
      PlatformMessage.Builder builder = PlatformMessage
            .request(Address.platformService(DeviceServiceHandler.NAME))
            .from(source)
            .withCorrelationId(UUID.randomUUID().toString())
            .withPayload(body);
      if(actor != null) {
         builder.withActor(actor);
      }
      if(placeHeader != null) {
         builder.withPlaceId(placeHeader);
      }
      return builder.create();
   }

   // the whole trip through the handler, including turning a refusal into an error response
   private PlatformMessage send(PlatformMessage request) throws Exception {
      handler().handleMessage(request);
      return bus.take();
   }

   private void assertRefused(PlatformMessage request) throws Exception {
      try {
         handler().handleRequest(request);
         fail("expected " + request.getMessageType() + " from " + request.getSource() + " to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
   }

   private Person personWithPin() {
      Person person = new Person();
      person.setId(personId);
      person.setPinAtPlace(hubPlace, PIN);
      return person;
   }

   private Pair<List<Map<String, Object>>, List<JsonObject>> reflexes() {
      JsonObject reflex = new JsonObject();
      reflex.addProperty("driver", "Test Driver");
      return Pair.<List<Map<String, Object>>, List<JsonObject>>of(ImmutableList.of(), ImmutableList.of(reflex));
   }

   private static String pinHash(UUID place, String pin) throws Exception {
      return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((place + pin).getBytes(StandardCharsets.UTF_8)));
   }

   private static String compress(String json) throws IOException {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try(GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
         gzip.write(json.getBytes(StandardCharsets.UTF_8));
      }
      return Base64.getEncoder().encodeToString(bytes.toByteArray());
   }

   private static String decompress(String base64) throws IOException {
      try(GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(base64)))) {
         return new String(ByteStreams.toByteArray(gzip), StandardCharsets.UTF_8);
      }
   }

   // a guest at the place, as client-bridge's RoleAuthorizer sees them
   private static AuthorizationContext guestAt(UUID place) {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setPlaceId(place);
      grant.setAccountId(UUID.randomUUID());
      grant.setAccountOwner(false);
      grant.addPermissions("*:*:*");
      return new AuthorizationContext(null, null, ImmutableList.of(grant));
   }

   @Test
   public void testClientSyncForItsOwnPlaceIsRefusedWithoutPins() throws Exception {
      PlatformMessage request = fromClient(SyncDevicesRequest.NAME, hubPlace);
      RoleAuthorizer clientBridge = new RoleAuthorizer(new AuthzMetrics(), new DisabledRiskyOperations());
      assertTrue("client-bridge lets the request through, so the handler has to refuse it",
            clientBridge.isAuthorized(guestAt(hubPlace), hubPlace.toString(), request));

      // everything the place would answer its own hub with, so a sync that got through would carry pins
      AtomicBoolean reconciled = new AtomicBoolean();
      EasyMock.expect(hubDao.findById(hub.getId())).andReturn(hub).anyTimes();
      EasyMock.expect(deviceDao.listDevicesByPlaceId(hubPlace, true)).andReturn(devices).anyTimes();
      EasyMock.expect(driverService.syncDevices(hub.getId(), hubPlace, null, devices, Collections.emptyList()))
            .andAnswer(() -> { reconciled.set(true); return reflexes(); })
            .anyTimes();
      EasyMock.expect(personPlaceAssocDao.findPersonIdsByPlace(hubPlace)).andReturn(ImmutableSet.of(personId)).anyTimes();
      EasyMock.expect(personDao.findById(personId)).andReturn(personWithPin()).anyTimes();
      replay();

      PlatformMessage response = send(request);
      assertEquals(request.getSource(), response.getDestination());
      assertFalse("no pin hashes for a client: " + response.getValue(), response.getValue().getAttributes().containsKey(SyncDevicesResponse.ATTR_PINS));
      assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
      assertEquals(Errors.CODE_NOT_FOUND, response.getValue().getAttributes().get(ErrorEvent.CODE_ATTR));
      assertFalse("a refused sync must not reconcile the place's devices", reconciled.get());
   }

   @Test
   public void testClientSyncIsRefusedBeforeAnythingIsLookedUp() throws Exception {
      replay();
      assertRefused(fromClient(SyncDevicesRequest.NAME, hubPlace));
      assertRefused(fromClient(DeviceServiceHandler.LEGACY_DEVICESYNC_NAMESPACE, hubPlace));
      verify();
   }

   // The hub is read by its own id. The place's hub index is never consulted (HubDAO is a strict
   // mock), since it returns only its first row, which needn't be this hub's.
   private void assertHubGetsItsReflexesAndPins(String type, String responseType) throws Exception {
      EasyMock.expect(hubDao.findById(hub.getId())).andReturn(hub);
      EasyMock.expect(deviceDao.listDevicesByPlaceId(hubPlace, true)).andReturn(devices);
      EasyMock.expect(driverService.syncDevices(hub.getId(), hubPlace, null, devices, Collections.emptyList())).andReturn(reflexes());
      EasyMock.expect(personPlaceAssocDao.findPersonIdsByPlace(hubPlace)).andReturn(ImmutableSet.of(personId));
      EasyMock.expect(personDao.findById(personId)).andReturn(personWithPin());
      replay();

      PlatformMessage request = fromHub(type, hub, hubPlace, hubPlace);
      PlatformMessage response = send(request);

      assertEquals(responseType, response.getMessageType());
      assertEquals(request.getSource(), response.getDestination());
      assertEquals(request.getCorrelationId(), response.getCorrelationId());
      assertEquals(ImmutableMap.of(personId.toString(), pinHash(hubPlace, PIN)), SyncDevicesResponse.getPins(response.getValue()));
      assertEquals("[{\"driver\":\"Test Driver\"}]", decompress(SyncDevicesResponse.getDrivers(response.getValue())));
      assertEquals("[]", decompress(SyncDevicesResponse.getDevices(response.getValue())));
      verify();
   }

   @Test
   public void testHubSyncingItselfGetsItsReflexesAndPins() throws Exception {
      assertHubGetsItsReflexesAndPins(SyncDevicesRequest.NAME, SyncDevicesResponse.NAME);
   }

   @Test
   public void testLegacyHubSyncingItselfGetsItsReflexesAndPins() throws Exception {
      assertHubGetsItsReflexesAndPins(DeviceServiceHandler.LEGACY_DEVICESYNC_NAMESPACE, DeviceServiceHandler.LEGACY_DEVICESYNCRESPONSE_NAMESPACE);
   }

   @Test
   public void testHubSyncForAnotherPlaceIsRefused() throws Exception {
      replay();
      assertRefused(fromHub(SyncDevicesRequest.NAME, otherHub, hubPlace, otherPlace));
      assertRefused(fromHub(DeviceServiceHandler.LEGACY_DEVICESYNC_NAMESPACE, otherHub, hubPlace, otherPlace));
      verify();
   }

   @Test
   public void testHubSyncForAnotherHubIsRefused() throws Exception {
      // hub-bridge stamps the sender's own place, so this can't arrive with another hub's place in
      // the header; without that, or without a header, it still has to come from the place's hub
      EasyMock.expect(hubDao.findById(otherHub.getId())).andReturn(otherHub).times(2);
      replay();
      assertRefused(fromHub(SyncDevicesRequest.NAME, otherHub, hubPlace, hubPlace));
      assertRefused(fromHub(SyncDevicesRequest.NAME, otherHub, hubPlace, null));
      verify();
   }

   @Test
   public void testHubThatIsNoLongerAtThePlaceIsRefused() throws Exception {
      // the hub's own record says it's at another place now, whatever the place's index says
      EasyMock.expect(hubDao.findById(hub.getId())).andReturn(hub);
      replay();
      assertRefused(fromHub(SyncDevicesRequest.NAME, hub, otherPlace, otherPlace));
      verify();
   }

   @Test
   public void testHubWithNoRecordIsRefused() throws Exception {
      EasyMock.expect(hubDao.findById(hub.getId())).andReturn(null);
      replay();
      assertRefused(fromHub(SyncDevicesRequest.NAME, hub, hubPlace, hubPlace));
      verify();
   }

   @Test
   public void testDegradedDevicesForAnotherPlaceAreRefused() throws Exception {
      replay();
      try {
         handler().handleEvent(fromHub(DevicesDegradedEvent.NAME, otherHub, hubPlace, otherPlace));
         fail("expected a degraded devices report for another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
      verify();
   }

   @Test
   public void testDegradedDevicesFromThePlacesHubAreApplied() throws Exception {
      EasyMock.expect(hubDao.findById(hub.getId())).andReturn(hub);
      EasyMock.expect(deviceDao.listDevicesByPlaceId(hubPlace, true)).andReturn(devices);
      driverService.syncDegradedDevices(hub.getId(), hubPlace, devices, Collections.emptyList());
      EasyMock.expectLastCall();
      replay();

      handler().handleEvent(fromHub(DevicesDegradedEvent.NAME, hub, hubPlace, hubPlace));
      verify();
   }

   @Test
   public void testDegradedDevicesFromAClientAreDroppedBeforeAnythingIsLookedUp() throws Exception {
      replay();
      handler().handleEvent(fromClient(DevicesDegradedEvent.NAME, hubPlace));
      verify();
   }

   @Test
   public void testDegradedDevicesFromAnotherPlacesHubAreDropped() throws Exception {
      EasyMock.expect(hubDao.findById(otherHub.getId())).andReturn(otherHub);
      replay();
      handler().handleEvent(fromHub(DevicesDegradedEvent.NAME, otherHub, hubPlace, hubPlace));
      verify();
   }
}
