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

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.core.messaging.MessagesModule;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.hubcom.bus.PlatformBusServiceImpl;
import com.iris.hubcom.server.HubServerModule;
import com.iris.hubcom.server.session.HubAuthModule;
import com.iris.hubcom.server.session.HubClientToken;
import com.iris.hubcom.server.session.HubSession;
import com.iris.hubcom.server.session.HubSession.State;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubDebugCapability;
import com.iris.messages.capability.HubMetricsCapability;
import com.iris.platform.partition.simple.SimplePartitionModule;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

/**
 * Hub logs, configuration, debug dumps (the whole agent database among them), 4G and metrics
 * requests are for the platform only, even from a client at the hub's own place. Uses the
 * shipped hub-bridge.properties through the real providers, so the configured list is what's
 * tested.
 */
@Modules({MessagesModule.class, InMemoryMessageModule.class, SimplePartitionModule.class, HubAuthModule.class })
public class HubAdminOnlyMessagesTest extends IrisTestCase {
   private static final String HUB_ID = "ABC-1234";

   private static final List<String> ADMIN_ONLY = ImmutableList.of(
         HubCapability.GetLogsRequest.NAME,
         HubCapability.StreamLogsRequest.NAME,
         HubCapability.SetLogLevelRequest.NAME,
         HubCapability.ResetLogLevelsRequest.NAME,
         HubCapability.GetConfigRequest.NAME,
         HubCapability.SetConfigRequest.NAME,
         HubDebugCapability.GetFilesRequest.NAME,
         HubDebugCapability.GetAgentDbRequest.NAME,
         HubDebugCapability.GetSyslogRequest.NAME,
         HubDebugCapability.GetBootlogRequest.NAME,
         HubDebugCapability.GetProcessesRequest.NAME,
         HubDebugCapability.GetLoadRequest.NAME,
         // the 4G capability is gone, but a hub still running the old 4G controller answers these
         "hub4g:GetInfo",
         "hub4g:ResetStatistics",
         "hub4g:GetStatistics",
         HubMetricsCapability.StartMetricsJobRequest.NAME,
         HubMetricsCapability.EndMetricsJobsRequest.NAME,
         HubMetricsCapability.GetMetricsJobInfoRequest.NAME,
         HubMetricsCapability.ListMetricsRequest.NAME,
         HubMetricsCapability.GetStoredMetricsRequest.NAME
   );

   private final String place = UUID.randomUUID().toString();
   private final Address client = Address.clientAddress("android", "1");

   private InMemoryPlatformMessageBus bus;
   private HubMessageFilter filter;
   private HubSession session;

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      Properties shipped = new Properties();
      try(InputStream is = getClass().getClassLoader().getResourceAsStream("hub-bridge.properties")) {
         assertNotNull("hub-bridge.properties should be on the test classpath", is);
         shipped.load(is);
      }
      HubServerModule module = new HubServerModule(new BridgeServerConfig(), null, null, null);

      bus = (InMemoryPlatformMessageBus) ServiceLocator.getInstance(PlatformMessageBus.class);
      filter = new DefaultHubMessageFilterImpl(
            module.getHubAdminAddresses(shipped.getProperty("hub.bridge.admin.addresses")),
            module.getHubAdminOnlyMessages(shipped.getProperty("hub.bridge.admin.only.messages")),
            new PlatformBusServiceImpl(bus, new BridgeMetrics("hub"), Collections.emptySet()));

      session = new HubSession(null, null, new BridgeMetrics("test"), HubClientToken.fromAddress(Address.hubAddress(HUB_ID)));
      session.setActivePlace(place);
      session.setState(State.AUTHORIZED);
   }

   private PlatformMessage request(String type, Address from) {
      return PlatformMessage
            .buildRequest(MessageBody.buildMessage(type), from, Address.hubService(HUB_ID, "hub"))
            .withCorrelationId("test")
            .withPlaceId(place)
            .create();
   }

   @Test
   public void testClientAtTheHubsPlaceCannotSendAdminOnlyRequests() {
      for(String type: ADMIN_ONLY) {
         assertFalse(type + " from a client should be refused", filter.acceptFromPlatform(session, request(type, client)));

         PlatformMessage response = bus.poll();
         assertNotNull(type + " should be answered with an error", response);
         assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
         assertEquals(client, response.getDestination());
         assertNull(bus.poll());
      }
   }

   @Test
   public void testThePlatformCanSendAdminOnlyRequests() {
      for(String type: ADMIN_ONLY) {
         assertTrue(type + " from the hub service should be accepted", filter.acceptFromPlatform(session, request(type, Address.platformService(HubCapability.NAMESPACE))));
      }
      assertNull(bus.poll());
   }

   @Test
   public void testClientAtTheHubsPlaceCanSendOtherRequests() {
      assertTrue(filter.acceptFromPlatform(session, request(Capability.CMD_GET_ATTRIBUTES, client)));
      assertTrue(filter.acceptFromPlatform(session, request(HubCapability.PairingRequestRequest.NAME, client)));
      assertNull(bus.poll());
   }
}
