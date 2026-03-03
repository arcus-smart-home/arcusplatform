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
package com.iris.api.server;

import static org.easymock.EasyMock.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.netty.server.message.IrisNettyMessageUtil;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.Authorizer;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({SessionRegistry.class, Session.class, Authorizer.class, IrisNettyMessageUtil.class, AuthorizationContext.class, Client.class})
public class TestApiPlatformBusListener extends IrisMockTestCase {

   private static final String ACTIVE_PLACE = UUID.randomUUID().toString();
   private static final String OTHER_PLACE = UUID.randomUUID().toString();
   private static final ClientToken CT = () -> "test-token";
   private static final Address SOURCE = Address.platformService("account");
   private static final Address DESTINATION = Address.clientAddress("api", "test-token");

   @Inject private SessionRegistry mockSessionRegistry;
   @Inject private Session mockSession;
   @Inject private Authorizer mockAuthorizer;
   @Inject private IrisNettyMessageUtil mockMessageUtil;
   @Inject private AuthorizationContext mockAuthContext;
   @Inject private Client mockClient;

   private ApiPlatformBusListener listener;
   private Capture<PlatformMessage> capturedMsg;

   @Provides @Singleton @Named("message.prefix")
   public String messagePrefix() { return "CLNT:api:"; }

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      listener = new ApiPlatformBusListener(mockAuthorizer, mockMessageUtil, mockSessionRegistry);
      capturedMsg = Capture.newInstance(CaptureType.LAST);
   }

   /**
    * Stub session methods that BridgeMdcUtil.captureAndInitializeContext calls.
    */
   private void expectSessionStubs() {
      expect(mockSession.getClient()).andReturn(mockClient).anyTimes();
      expect(mockClient.getPrincipalName()).andReturn("test-principal").anyTimes();
      expect(mockSession.getClientType()).andReturn(null).anyTimes();
      expect(mockSession.getClientVersion()).andReturn(null).anyTimes();
   }

   /**
    * Expect the calls for a ListPlaces-type message through our override + super.onMessage:
    *   1. Our override: sessionRegistry.getSession(ct) → session, session.getActivePlace()
    *   2. super.onMessage: sessionRegistry.getSession(ct) → session, filter(), send
    */
   private void expectInterceptAndSend() {
      // Shared session stubs (called by both our override and super)
      expect(mockSession.getActivePlace()).andReturn(ACTIVE_PLACE).anyTimes();
      expect(mockSession.getAuthorizationContext()).andReturn(mockAuthContext).anyTimes();
      expectSessionStubs();

      // Our override's lookup
      expect(mockSessionRegistry.getSession(CT)).andReturn(mockSession);
      // super.onMessage's lookup
      expect(mockSessionRegistry.getSession(CT)).andReturn(mockSession);

      // super's filter → messageUtil → send chain
      expect(mockAuthorizer.filter(eq(mockAuthContext), eq(ACTIVE_PLACE), capture(capturedMsg)))
            .andAnswer(() -> capturedMsg.getValue());
      expect(mockMessageUtil.convertPlatformToClient(anyObject(PlatformMessage.class)))
            .andReturn(ClientMessage.builder().withPayload(MessageBody.emptyMessage()).create());
      mockSession.sendMessage(anyString());
      expectLastCall();
   }

   private PlatformMessage buildListPlacesResponse(String messageType, List<Map<String, Object>> places) {
      MessageBody body = MessageBody.buildMessage(messageType, Collections.singletonMap("places", places));
      return PlatformMessage.buildMessage(body, SOURCE, DESTINATION)
            .withPlaceId(ACTIVE_PLACE)
            .create();
   }

   // --- account:ListPlacesResponse tests (uses base:id) ---

   @Test
   public void testAccountListPlacesFilteredToActivePlace() {
      Map<String, Object> matchingPlace = ImmutableMap.of("base:id", ACTIVE_PLACE, "place:name", "My Place");
      Map<String, Object> otherPlace = ImmutableMap.of("base:id", OTHER_PLACE, "place:name", "Other Place");

      PlatformMessage msg = buildListPlacesResponse(
            AccountCapability.ListPlacesResponse.NAME, Arrays.asList(matchingPlace, otherPlace));

      expectInterceptAndSend();
      replay();

      listener.onMessage(CT, msg);

      PlatformMessage filtered = capturedMsg.getValue();
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> places = (List<Map<String, Object>>)
            filtered.getValue().getAttributes().get("places");
      assertEquals(1, places.size());
      assertEquals(ACTIVE_PLACE, places.get(0).get("base:id"));

      verify();
   }

   @Test
   public void testAccountListPlacesAlreadyScoped() {
      Map<String, Object> matchingPlace = ImmutableMap.of("base:id", ACTIVE_PLACE, "place:name", "My Place");

      PlatformMessage msg = buildListPlacesResponse(
            AccountCapability.ListPlacesResponse.NAME, Collections.singletonList(matchingPlace));

      expectInterceptAndSend();
      replay();

      listener.onMessage(CT, msg);

      PlatformMessage filtered = capturedMsg.getValue();
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> places = (List<Map<String, Object>>)
            filtered.getValue().getAttributes().get("places");
      assertEquals(1, places.size());
      assertEquals(ACTIVE_PLACE, places.get(0).get("base:id"));

      verify();
   }

   // --- person:ListAvailablePlacesResponse tests (uses placeId) ---

   @Test
   public void testPersonListAvailablePlacesFilteredByPlaceId() {
      Map<String, Object> matchingPlace = new HashMap<>();
      matchingPlace.put("placeId", ACTIVE_PLACE);
      matchingPlace.put("name", "My Place");
      matchingPlace.put("role", "OWNER");

      Map<String, Object> otherPlace = new HashMap<>();
      otherPlace.put("placeId", OTHER_PLACE);
      otherPlace.put("name", "Other Place");
      otherPlace.put("role", "FULL_ACCESS");

      PlatformMessage msg = buildListPlacesResponse(
            PersonCapability.ListAvailablePlacesResponse.NAME, Arrays.asList(matchingPlace, otherPlace));

      expectInterceptAndSend();
      replay();

      listener.onMessage(CT, msg);

      PlatformMessage filtered = capturedMsg.getValue();
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> places = (List<Map<String, Object>>)
            filtered.getValue().getAttributes().get("places");
      assertEquals(1, places.size());
      assertEquals(ACTIVE_PLACE, places.get(0).get("placeId"));

      verify();
   }

   // --- Edge cases ---

   @Test
   public void testEmptyPlacesListPassesThrough() {
      PlatformMessage msg = buildListPlacesResponse(
            AccountCapability.ListPlacesResponse.NAME, Collections.emptyList());

      expectInterceptAndSend();
      replay();

      listener.onMessage(CT, msg);

      // Empty list passes through unmodified
      PlatformMessage filtered = capturedMsg.getValue();
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> places = (List<Map<String, Object>>)
            filtered.getValue().getAttributes().get("places");
      assertTrue(places.isEmpty());

      verify();
   }

   @Test
   public void testNullSessionSkipsFiltering() {
      Map<String, Object> otherPlace = ImmutableMap.of("base:id", OTHER_PLACE);
      PlatformMessage msg = buildListPlacesResponse(
            AccountCapability.ListPlacesResponse.NAME, Collections.singletonList(otherPlace));

      // Our override looks up session → null, so skips filtering
      expect(mockSessionRegistry.getSession(CT)).andReturn(null);
      // super.onMessage also gets null session → no send
      expect(mockSessionRegistry.getSession(CT)).andReturn(null);
      replay();

      listener.onMessage(CT, msg);

      verify();
   }

   @Test
   public void testNonListPlacesMessageNotIntercepted() {
      MessageBody body = MessageBody.buildMessage(
            Capability.EVENT_VALUE_CHANGE,
            ImmutableMap.of("place:name", "Updated"));
      PlatformMessage msg = PlatformMessage.buildMessage(body, SOURCE, DESTINATION)
            .withPlaceId(ACTIVE_PLACE)
            .create();

      // Not a ListPlaces response → goes straight to super.onMessage, no intercept lookup
      expect(mockSessionRegistry.getSession(CT)).andReturn(mockSession);
      expect(mockSession.getActivePlace()).andReturn(ACTIVE_PLACE).anyTimes();
      expect(mockSession.getAuthorizationContext()).andReturn(mockAuthContext).anyTimes();
      expect(mockAuthorizer.filter(eq(mockAuthContext), eq(ACTIVE_PLACE), anyObject(PlatformMessage.class)))
            .andAnswer(() -> (PlatformMessage) getCurrentArguments()[2]);
      expect(mockMessageUtil.convertPlatformToClient(anyObject(PlatformMessage.class)))
            .andReturn(ClientMessage.builder().withPayload(MessageBody.emptyMessage()).create());
      expectSessionStubs();
      mockSession.sendMessage(anyString());
      expectLastCall();
      replay();

      listener.onMessage(CT, msg);

      verify();
   }

   @Test
   public void testBroadcastMessageNotIntercepted() {
      Map<String, Object> otherPlace = ImmutableMap.of("base:id", OTHER_PLACE);
      PlatformMessage msg = buildListPlacesResponse(
            AccountCapability.ListPlacesResponse.NAME, Collections.singletonList(otherPlace));

      // ct == null → broadcast path, our override skips, parent iterates sessions
      expect(mockSessionRegistry.getSessions()).andReturn(Collections.emptyList());
      replay();

      listener.onMessage(null, msg);

      verify();
   }
}
