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
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.platform.partition.simple.SimplePartitionModule;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({MessagesModule.class, InMemoryMessageModule.class, SimplePartitionModule.class, HubAuthModule.class })
public class TestDefaultHubMessageFilterImpl extends IrisTestCase {

   private static final String HUB_ID = "ABC-1234";

   private String placeId = UUID.randomUUID().toString();
   private HubClientToken clientToken = HubClientToken.fromAddress(Address.hubAddress(HUB_ID));
   private HubMessageFilter authorizer;
   private HubSession clientSession;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      clientSession = new HubSession(null, null, new BridgeMetrics("test"), clientToken);
      authorizer = new DefaultHubMessageFilterImpl(
         Predicates.alwaysFalse(),
         Predicates.alwaysFalse(),
         new PlatformBusServiceImpl(ServiceLocator.getInstance(PlatformMessageBus.class), new BridgeMetrics("hub"), Collections.emptySet())
      );
   }

   @Test
   public void testHubConnectedAlwaysAllowedThrough() {
      PlatformMessage msg = createHubConnected();
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.AUTHORIZED);
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.PENDING_REG_ACK);
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.REGISTERED);
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
   }

   @Test
   public void testErrorEventAlwaysAllowedThrough() {
      PlatformMessage msg = createErrorEvent();
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.AUTHORIZED);
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.PENDING_REG_ACK);
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.REGISTERED);
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
   }

   @Test
   public void testFirmwareUpdateProgressAlwaysAllowedThrough() {
      PlatformMessage msg = createFirmwareUpdateProgress();
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.AUTHORIZED);
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.PENDING_REG_ACK);
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.REGISTERED);
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
   }

   @Test
   public void testFirmwareRefusedAlwaysAllowedThrough() {
      PlatformMessage msg = createFirmwareRefused();
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.AUTHORIZED);
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.PENDING_REG_ACK);
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.REGISTERED);
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
   }

   @Test
   public void testProtocolRefusedUnlessAuthorized() {
      ProtocolMessage msg = createProtocolMessage();
      assertFalse(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.AUTHORIZED);
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.PENDING_REG_ACK);
      assertFalse(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.REGISTERED);
      assertFalse(authorizer.acceptFromHub(clientSession, msg));
   }

   @Test
   public void testArbitraryMessageRejectedUnlessAuthorized() {
      PlatformMessage msg = createPlatformMessage(MessageBody.ping(), Address.platformService("status"));
      assertFalse(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.AUTHORIZED);
      assertTrue(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.PENDING_REG_ACK);
      assertFalse(authorizer.acceptFromHub(clientSession, msg));
      clientSession.setState(State.REGISTERED);
      assertFalse(authorizer.acceptFromHub(clientSession, msg));
   }

   @Test
   public void testAribtraryPlatformMessageLeavesState() {
      State state = clientSession.getState();
      PlatformMessage msg = createPlatformMessage(MessageBody.pong());
      authorizer.acceptFromPlatform(clientSession, msg);
      assertSame(state, clientSession.getState());
   }

   @Test
   public void testProtocolLeavesState() {
      State state = clientSession.getState();
      ProtocolMessage msg = createProtocolMessage();
      authorizer.acceptFromProtocol(clientSession, msg);
      assertSame(state, clientSession.getState());
   }

   @Test
   public void testAcceptOrRejectNoPlaceHeader() {
      PlatformMessage platform = createPlatformMessage(MessageBody.pong());
      platform = PlatformMessage.builder(platform).withPlaceId((UUID) null).create();
      ProtocolMessage protocol = createProtocolMessage();
      protocol = ProtocolMessage.builder(protocol).withPlaceId((UUID) null).create();

      // with no place
      assertTrue(authorizer.acceptFromPlatform(clientSession, platform));
      assertFalse(authorizer.acceptFromProtocol(clientSession, protocol));

      clientSession.setActivePlace(placeId);
      clientSession.setState(State.AUTHORIZED);

      // with a place
      assertTrue(authorizer.acceptFromPlatform(clientSession, platform));
      assertFalse(authorizer.acceptFromProtocol(clientSession, protocol));
   }

   @Test
   public void testAcceptRightPlaceHeader() {
      PlatformMessage platform = createPlatformMessage(MessageBody.pong());
      ProtocolMessage protocol = createProtocolMessage();

      clientSession.setActivePlace(placeId);
      clientSession.setState(State.AUTHORIZED);

      // with a place
      assertTrue(authorizer.acceptFromPlatform(clientSession, platform));
      assertTrue(authorizer.acceptFromProtocol(clientSession, protocol));

   }

   @Test
   public void testRejectPlaceHeaderBeforePlaceSet() {
      PlatformMessage platform = createPlatformMessage(MessageBody.pong());
      ProtocolMessage protocol = createProtocolMessage();

      // before the place has been set
      assertFalse(authorizer.acceptFromPlatform(clientSession, platform));
      assertFalse(authorizer.acceptFromProtocol(clientSession, protocol));
   }

   @Test
   public void testRejectWrongPlaceHeaderBeforePlaceSet() {
      PlatformMessage platform = createPlatformMessage(MessageBody.pong());
      ProtocolMessage protocol = createProtocolMessage();

      clientSession.setActivePlace(UUID.randomUUID().toString());
      clientSession.setState(State.AUTHORIZED);

      // before the place has been set
      assertFalse(authorizer.acceptFromPlatform(clientSession, platform));
      assertFalse(authorizer.acceptFromProtocol(clientSession, protocol));
   }

   private ProtocolMessage createProtocolMessage() {
      return
            ProtocolMessage
               .buildProtocolMessage(
                  Address.hubAddress(HUB_ID),
                  Address.platformDriverAddress(UUID.randomUUID()),
                  ZWaveProtocol.NAMESPACE,
                  new byte[] {}
               )
               .withPlaceId(placeId)
               .create();
   }

   private PlatformMessage createHubConnected() {
      return createPlatformMessage(MessageBody.buildMessage(MessageConstants.MSG_HUB_CONNECTED_EVENT, Collections.<String,Object>emptyMap()));
   }

   private PlatformMessage createErrorEvent() {
      return createPlatformMessage(ErrorEvent.fromCode("code", "message"));
   }

   private PlatformMessage createFirmwareUpdateProgress() {
      return createPlatformMessage(HubAdvancedCapability.FirmwareUpgradeProcessEvent.builder()
            .withStatus("status")
            .withPercentDone(0.0)
            .build());
   }

   private PlatformMessage createFirmwareRefused() {
      return createPlatformMessage(HubAdvancedCapability.FirmwareUpdateResponse.builder()
            .withStatus(HubAdvancedCapability.FirmwareUpdateResponse.STATUS_REFUSED)
            .withMessage("refused because alarm is active")
            .build());
   }

   private PlatformMessage createRegistrationRequest() {
      return createPlatformMessage(MessageBody.buildMessage(MessageConstants.MSG_HUB_REGISTERED_REQUEST, Collections.<String,Object>emptyMap()));
   }

   private PlatformMessage createPlatformMessage(MessageBody body) {
      return createPlatformMessage(body, Address.platformService(HubCapability.NAMESPACE));
   }

   private PlatformMessage createPlatformMessage(MessageBody body, Address from) {
      PlatformMessage msg =
            PlatformMessage
               .buildMessage(body, Address.hubAddress(HUB_ID), from)
               .withPayload(body)
               .withPlaceId(placeId)
               .create()
               ;
      return msg;
   }
}

