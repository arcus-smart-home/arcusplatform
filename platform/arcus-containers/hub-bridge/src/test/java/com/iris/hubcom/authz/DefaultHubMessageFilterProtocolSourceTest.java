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
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.platform.partition.simple.SimplePartitionModule;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

/**
 * driver-services runs the driver of the device a protocol message comes from, so a hub may only
 * send from its own addresses: its devices' PROT:<protocol>-<hub>:<id> and its radio ports'.
 */
@Modules({MessagesModule.class, InMemoryMessageModule.class, SimplePartitionModule.class, HubAuthModule.class })
public class DefaultHubMessageFilterProtocolSourceTest extends IrisTestCase {
   private static final String HUB_ID = "ABC-1234";
   private static final String OTHER_HUB_ID = "XYZ-5678";
   private static final ProtocolDeviceId NODE = ProtocolDeviceId.fromBytes(new byte[] { 0, 0, 0, 5 });

   private HubMessageFilter filter;
   private HubSession session;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      session = new HubSession(null, null, new BridgeMetrics("test"), HubClientToken.fromAddress(Address.hubAddress(HUB_ID)));
      session.setState(State.AUTHORIZED);
      filter = new DefaultHubMessageFilterImpl(
         Predicates.alwaysFalse(),
         Predicates.alwaysFalse(),
         new PlatformBusServiceImpl(ServiceLocator.getInstance(PlatformMessageBus.class), new BridgeMetrics("hub"), Collections.emptySet())
      );
   }

   private static ProtocolMessage from(Address source) {
      return ProtocolMessage
            .buildProtocolMessage(source, Address.broadcastAddress(), ZWaveProtocol.NAMESPACE, new byte[] {})
            .create();
   }

   @Test
   public void testAnotherHubsDeviceIsRefused() {
      assertFalse(filter.acceptFromHub(session, from(Address.hubProtocolAddress(OTHER_HUB_ID, ZWaveProtocol.NAMESPACE, NODE))));
   }

   @Test
   public void testAPlatformDeviceIsRefused() {
      // no hub in the address, as an IPCD device's
      assertFalse(filter.acceptFromHub(session, from(Address.protocolAddress(IpcdProtocol.NAMESPACE, NODE))));
   }

   @Test
   public void testAnotherHubIsRefused() {
      assertFalse(filter.acceptFromHub(session, from(Address.hubAddress(OTHER_HUB_ID))));
   }

   @Test
   public void testItsOwnDevicesAreAccepted() {
      // ZWaveController, ZigbeeController and ReflexController send from the device's address
      assertTrue(filter.acceptFromHub(session, from(Address.hubProtocolAddress(HUB_ID, ZWaveProtocol.NAMESPACE, NODE))));
      assertTrue(filter.acceptFromHub(session, from(Address.hubProtocolAddress(HUB_ID, "ZIGB", NODE))));
   }

   @Test
   public void testItsOwnRadioPortIsAccepted() {
      // BridgePort.getProtocolAddress
      assertTrue(filter.acceptFromHub(session, from(Address.hubProtocolAddress(HUB_ID, ZWaveProtocol.NAMESPACE, ProtocolDeviceId.fromBytes(new byte[] { 0 })))));
   }

   @Test
   public void testOwnDeviceRefusedUnlessAuthorized() {
      session.setState(State.REGISTERED);
      assertFalse(filter.acceptFromHub(session, from(Address.hubProtocolAddress(HUB_ID, ZWaveProtocol.NAMESPACE, NODE))));
   }
}
