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
package com.iris.hubcom.authz;

import java.util.Collections;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Predicates;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.core.messaging.MessagesModule;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.hubcom.bus.PlatformBusServiceImpl;
import com.iris.hubcom.server.session.HubAuthModule;
import com.iris.hubcom.server.session.HubClientToken;
import com.iris.hubcom.server.session.HubSession;
import com.iris.hubcom.server.session.HubSession.State;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubZwaveCapability;
import com.iris.messages.capability.PairingSubsystemCapability;
import com.iris.platform.partition.simple.SimplePartitionModule;
import com.iris.security.authz.AuthzUtil;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({MessagesModule.class, InMemoryMessageModule.class, SimplePartitionModule.class, HubAuthModule.class })
public class DefaultHubMessageFilterS2PinTest extends IrisTestCase {
   private static final String HUB_ID = "ABC-1234";
   private static final String REQUEST_ID = "aaaaaaaa-0000-4000-8000-000000000001";

   private final UUID placeId = UUID.randomUUID();
   private final UUID otherPlaceId = UUID.randomUUID();
   private final Address hub = Address.hubAddress(HUB_ID);
   private final Address pairing = Address.platformService(placeId, PairingSubsystemCapability.NAMESPACE);
   private final Address otherPairing = Address.platformService(otherPlaceId, PairingSubsystemCapability.NAMESPACE);
   private final Address client = Address.clientAddress("test", "session");

   private InMemoryPlatformMessageBus bus;
   private HubSession session;
   private HubMessageFilter filter;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      bus = (InMemoryPlatformMessageBus) ServiceLocator.getInstance(PlatformMessageBus.class);
      session = new HubSession(null, null, new BridgeMetrics("test"), HubClientToken.fromAddress(hub));
      session.setActivePlace(placeId.toString());
      session.setState(State.AUTHORIZED);
      filter = newFilter(false);
   }

   private HubMessageFilter newFilter(boolean sourcesAreAdmin) {
      return new DefaultHubMessageFilterImpl(
         sourcesAreAdmin ? Predicates.alwaysTrue() : Predicates.alwaysFalse(),
         Predicates.alwaysFalse(),
         new PlatformBusServiceImpl(bus, new BridgeMetrics("hub"), Collections.emptySet())
      );
   }

   @Test
   public void testPinFromThePairingSubsystemReachesTheHub() {
      assertTrue(filter.acceptFromPlatform(session, toHub(submit(), pairing, placeId)));
      assertTrue(filter.acceptFromPlatform(session, toHub(cancel(), pairing, placeId)));
   }

   @Test
   public void testPinDirectlyFromAClientIsRefused() throws Exception {
      assertFalse(filter.acceptFromPlatform(session, toHub(submit(), client, placeId)));

      PlatformMessage response = bus.take();
      assertEquals(client, response.getDestination());
      assertEquals(AuthzUtil.UNAUTHORIZED_CODE, ((ErrorEvent) response.getValue()).getCode());
   }

   @Test
   public void testCancelDirectlyFromAClientIsRefused() {
      assertFalse(filter.acceptFromPlatform(session, toHub(cancel(), client, placeId)));
   }

   @Test
   public void testPinFromAnotherPlacesSubsystemIsRefused() {
      assertFalse(filter.acceptFromPlatform(session, toHub(submit(), otherPairing, placeId)));
      assertFalse(filter.acceptFromPlatform(session, toHub(submit(), otherPairing, otherPlaceId)));
   }

   @Test
   public void testPinWithAnotherPlaceHeaderIsRefused() {
      assertFalse(filter.acceptFromPlatform(session, toHub(submit(), pairing, otherPlaceId)));
   }

   @Test
   public void testPinWithoutAPlaceHeaderIsRefused() {
      assertFalse(filter.acceptFromPlatform(session, toHub(submit(), client, null)));
      assertFalse(filter.acceptFromPlatform(session, toHub(submit(), pairing, null)));
   }

   @Test
   public void testPinFromAnAdminAddressIsRefused() {
      assertFalse(newFilter(true).acceptFromPlatform(session, toHub(submit(), client, placeId)));
   }

   @Test
   public void testPinForAHubWithoutAPlaceIsRefused() {
      session.setActivePlace(null);

      assertFalse(filter.acceptFromPlatform(session, toHub(submit(), pairing, placeId)));
   }

   @Test
   public void testOtherZWaveRequestsAreUntouched() {
      MessageBody heal = HubZwaveCapability.HealRequest.builder().build();

      assertTrue(filter.acceptFromPlatform(session, toHub(heal, client, placeId)));
   }

   @Test
   public void testDskToThePairingSubsystemIsForwarded() {
      assertTrue(filter.acceptFromHub(session, fromHub(required(), pairing)));
   }

   @Test
   public void testDskOnABroadcastIsDropped() {
      PlatformMessage broadcast = PlatformMessage.buildBroadcast(required(), Address.hubService(HUB_ID, "zwave")).create();

      assertFalse(filter.acceptFromHub(session, broadcast));
   }

   @Test
   public void testDskToAnotherPlaceIsDropped() {
      assertFalse(filter.acceptFromHub(session, fromHub(required(), otherPairing)));
   }

   @Test
   public void testDskToAnotherServiceIsDropped() {
      assertFalse(filter.acceptFromHub(session, fromHub(required(), Address.platformService(placeId, "subcare"))));
   }

   private MessageBody submit() {
      return HubZwaveCapability.SubmitS2PinRequest.builder().withRequestId(REQUEST_ID).withNode(12).withPin("12345").build();
   }

   private MessageBody cancel() {
      return HubZwaveCapability.CancelS2PinRequest.builder().withRequestId(REQUEST_ID).withNode(12).build();
   }

   private MessageBody required() {
      return HubZwaveCapability.S2PinRequiredEvent
            .builder()
            .withRequestId(REQUEST_ID)
            .withNode(12)
            .withDsk("00000-11111-22222-33333-44444-55555-11111-22222")
            .withTimeoutMs(200000L)
            .build();
   }

   private PlatformMessage toHub(MessageBody body, Address source, UUID place) {
      PlatformMessage.Builder message = PlatformMessage.buildRequest(body, source, hub).withCorrelationId("correlation");
      if(place != null) {
         message.withPlaceId(place);
      }
      return message.create();
   }

   private PlatformMessage fromHub(MessageBody body, Address destination) {
      return PlatformMessage.buildMessage(body, Address.hubService(HUB_ID, "zwave"), destination).create();
   }
}
