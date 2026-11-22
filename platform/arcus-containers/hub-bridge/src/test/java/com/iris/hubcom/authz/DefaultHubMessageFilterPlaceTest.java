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
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubZwaveCapability;
import com.iris.platform.partition.simple.SimplePartitionModule;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

/**
 * Nothing answers for a hub that isn't connected, so hub-bridge must not answer a sender that
 * can't see the hub either: an immediate error would tell a client that a guessed hub id is
 * online. Hub ids are short, so this can't rest on the id being hard to guess. Only senders at
 * the hub's place, services and admin addresses ever hear back.
 */
@Modules({MessagesModule.class, InMemoryMessageModule.class, SimplePartitionModule.class, HubAuthModule.class })
public class DefaultHubMessageFilterPlaceTest extends IrisTestCase {
   private static final String HUB_ID = "ABC-1234";

   private final UUID placeId = UUID.randomUUID();
   private final Address client = Address.clientAddress("android", "1");
   private final Address service = Address.platformService("subs");
   private final Address admin = Address.platformService(HubCapability.NAMESPACE);

   private InMemoryPlatformMessageBus bus;
   private HubSession session;
   private HubMessageFilter filter;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      bus = (InMemoryPlatformMessageBus) ServiceLocator.getInstance(PlatformMessageBus.class);
      session = new HubSession(null, null, new BridgeMetrics("test"), HubClientToken.fromAddress(Address.hubAddress(HUB_ID)));
      session.setActivePlace(placeId.toString());
      session.setState(State.AUTHORIZED);
      // only SERV:hub: is admin and only hub:GetLogs is admin-only
      filter = new DefaultHubMessageFilterImpl(
         admin::equals,
         Predicates.equalTo(HubCapability.GetLogsRequest.NAME),
         new PlatformBusServiceImpl(bus, new BridgeMetrics("hub"), Collections.emptySet())
      );
   }

   @Test
   public void testClientAtTheHubsPlaceReachesTheHub() {
      assertTrue(filter.acceptFromPlatform(session, request(Capability.CMD_GET_ATTRIBUTES, client, placeId)));
      assertTrue(filter.acceptFromPlatform(session, request(MessageConstants.MSG_PING_REQUEST, client, placeId)));
      assertNull(bus.poll());
   }

   // each of these used to be answered with an error before the place was compared
   @Test
   public void testClientAtAnotherPlaceGetsNoAnswer() {
      UUID otherPlace = UUID.randomUUID();
      assertNotAnswered(request(Capability.CMD_GET_ATTRIBUTES, client, otherPlace));
      assertNotAnswered(request(HubCapability.GetLogsRequest.NAME, client, otherPlace));
      assertNotAnswered(request(HubZwaveCapability.SubmitS2PinRequest.NAME, client, otherPlace));
      assertNotAnswered(request(HubZwaveCapability.CancelS2PinRequest.NAME, client, otherPlace));
   }

   // client-bridge lets a client send a ping without an active place, and the hub answers a ping
   @Test
   public void testClientWithoutAPlaceHeaderGetsNoAnswer() {
      assertNotAnswered(request(MessageConstants.MSG_PING_REQUEST, client, null));
      assertNotAnswered(request(HubCapability.GetLogsRequest.NAME, client, null));
   }

   @Test
   public void testHubWithoutAPlaceHeaderGetsNoAnswer() {
      assertNotAnswered(request(MessageConstants.MSG_PING_REQUEST, Address.hubService("XYZ-9999", HubCapability.NAMESPACE), null));
   }

   @Test
   public void testHubWithoutAPlaceGivesAClientNoAnswer() {
      session.setActivePlace(null);

      assertNotAnswered(request(HubCapability.GetLogsRequest.NAME, client, placeId));
   }

   @Test
   public void testServiceWithoutAPlaceHeaderReachesTheHub() {
      assertTrue(filter.acceptFromPlatform(session, request(Capability.CMD_GET_ATTRIBUTES, service, null)));
      assertNull(bus.poll());
   }

   @Test
   public void testAdminOnlyRequestFromAServiceWithoutAPlaceHeaderIsRefused() {
      assertRefused(request(HubCapability.GetLogsRequest.NAME, service, null));
   }

   @Test
   public void testClientAtTheHubsPlaceIsToldAnAdminOnlyRequestIsRefused() {
      assertRefused(request(HubCapability.GetLogsRequest.NAME, client, placeId));
   }

   @Test
   public void testAdminReachesTheHubWhateverItsPlaceHeader() {
      assertTrue(filter.acceptFromPlatform(session, request(HubCapability.GetLogsRequest.NAME, admin, null)));
      assertTrue(filter.acceptFromPlatform(session, request(HubCapability.GetLogsRequest.NAME, admin, UUID.randomUUID())));
      assertNull(bus.poll());
   }

   private void assertNotAnswered(PlatformMessage request) {
      assertFalse(request.getMessageType() + " should not reach the hub", filter.acceptFromPlatform(session, request));
      assertNull(request.getMessageType() + " should not be answered", bus.poll());
   }

   private void assertRefused(PlatformMessage request) {
      assertFalse(filter.acceptFromPlatform(session, request));

      PlatformMessage response = bus.poll();
      assertNotNull("the sender should be told", response);
      assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
      assertEquals(request.getSource(), response.getDestination());
      assertNull(bus.poll());
   }

   private PlatformMessage request(String type, Address source, UUID place) {
      PlatformMessage.Builder request =
            PlatformMessage
               .buildRequest(MessageBody.buildMessage(type), source, Address.hubService(HUB_ID, HubCapability.NAMESPACE))
               .withCorrelationId("test");
      if(place != null) {
         request.withPlaceId(place);
      }
      return request.create();
   }
}
