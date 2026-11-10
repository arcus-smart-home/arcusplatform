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

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.platform.hubbridge.HeartbeatMessage;
import com.iris.platform.partition.DefaultPartition;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.services.hub.HubRegistry;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.security.authz.AuthzMetrics;
import com.iris.security.authz.DisabledRiskyOperations;
import com.iris.security.authz.RoleAuthorizer;

/**
 * The platform bus heartbeat listener is a service at SERV:hubheartbeat:, so a client can address
 * it with a frame sent as an event. A heartbeat marks every hub it lists online at the bridge that
 * sent it, so it's only taken from hub-bridge.
 */
public class HubHeartbeatListenerSourceTest {
   private static final String HUB_ID = "ABC-1234";

   private HubRegistry hubs;
   private Partitioner partitioner;
   private HubHeartbeatListener listener;

   @Before
   public void setUp() {
      hubs = EasyMock.createStrictMock(HubRegistry.class);
      partitioner = EasyMock.createNiceMock(Partitioner.class);
      EasyMock.expect(partitioner.getPartitionForMessage(EasyMock.anyObject(PlatformMessage.class))).andReturn(new DefaultPartition(3)).anyTimes();
      EasyMock.replay(partitioner);
      listener = new HubHeartbeatListener(EasyMock.createNiceMock(PlatformMessageBus.class), hubs, partitioner);
   }

   private static MessageBody heartbeat() {
      return HeartbeatMessage.builder().withConnectedHubIds(ImmutableSet.of(HUB_ID)).build();
   }

   @Test
   public void testClientHeartbeatIsDropped() throws Exception {
      UUID person = UUID.randomUUID();
      UUID place = UUID.randomUUID();
      // what client-bridge forwards for a frame with isRequest=false
      PlatformMessage message = PlatformMessage
            .buildMessage(heartbeat(), Address.fromString("CLNT:android:session-token"), Address.platformService("hubheartbeat"))
            .withPlaceId(place)
            .withActor(Address.platformService(person, "person"))
            .create();
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setEntityId(person);
      grant.setPlaceId(place);
      grant.setAccountId(UUID.randomUUID());
      grant.addPermissions("*:*:*");
      // the authorizer on its own lets it through
      assertTrue(new RoleAuthorizer(new AuthzMetrics(), new DisabledRiskyOperations())
            .isAuthorized(new AuthorizationContext(null, null, Arrays.asList(grant)), place.toString(), message));
      EasyMock.replay(hubs);

      listener.handleEvent(message);

      EasyMock.verify(hubs); // strict mock: no hub was marked online
   }

   @Test
   public void testHubBridgeHeartbeatMarksItsHubsOnline() throws Exception {
      // HubSessionRegistry.sendUpdate
      PlatformMessage message = PlatformMessage.createBroadcast(heartbeat(), Address.fromString("CLNT:hub-bridge:0"));
      hubs.online(HUB_ID, 3, "0");
      EasyMock.expectLastCall();
      EasyMock.replay(hubs);

      listener.handleEvent(message);

      EasyMock.verify(hubs);
   }
}
