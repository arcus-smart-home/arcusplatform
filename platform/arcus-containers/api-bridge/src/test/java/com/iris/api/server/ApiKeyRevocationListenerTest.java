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

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.iris.bridge.server.netty.Constants;
import com.iris.bridge.server.session.ClientToken;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.service.ApiKeyService;
import com.iris.netty.server.session.IrisNettyClientClientToken;
import com.iris.security.apikey.ApiKeyPrincipal;
import com.iris.security.authz.AuthorizationContext;

/**
 * A client can address a message of any type to another client, so these check that only the
 * place's own broadcast closes sessions.
 */
public class ApiKeyRevocationListenerTest {
   private final UUID keyId = UUID.randomUUID();
   private final UUID placeId = UUID.randomUUID();
   private final UUID otherPlaceId = UUID.randomUUID();
   private final Address actor = Address.platformService(UUID.randomUUID(), "person");

   private SessionRegistry registry;
   private Session keySession;
   private Session otherKeySession;
   private ApiKeyRevocationListener listener;

   @Before
   public void setUp() {
      keySession = session(keyId, placeId);
      otherKeySession = session(UUID.randomUUID(), placeId);
      registry = EasyMock.createMock(SessionRegistry.class);
      listener = new ApiKeyRevocationListener(registry);
   }

   @Test
   public void testRevokedEventFromThePlaceClosesTheKeysSessions() {
      expectSessions();
      expectClosed(keySession);
      replay();

      listener.onMessage(null, fromPlace(revoked(keyId), placeId));

      verify();
   }

   @Test
   public void testDeletedEventFromThePlaceClosesTheKeysSessions() {
      expectSessions();
      expectClosed(keySession);
      replay();

      listener.onMessage(null, fromPlace(deleted(keyId), placeId));

      verify();
   }

   @Test
   public void testRevokedEventSentToAClientIsIgnored() {
      // what any logged-in client gets by sending apikey:Revoked to a CLNT: address
      Address destination = Address.clientAddress("api", "1234");
      PlatformMessage forged = PlatformMessage.buildMessage(revoked(keyId), Address.clientAddress("android", "5678"), destination)
            .withPlaceId(placeId)
            .withActor(actor)
            .create();
      ClientToken ct = IrisNettyClientClientToken.fromAddress(destination);
      replay();

      listener.onMessage(ct, forged);

      verify();
   }

   @Test
   public void testRevokedEventSentToAClientFromThePlaceAddressIsIgnored() {
      Address destination = Address.clientAddress("api", "1234");
      PlatformMessage directed = PlatformMessage.buildMessage(revoked(keyId), place(placeId), destination)
            .withPlaceId(placeId)
            .create();
      replay();

      listener.onMessage(IrisNettyClientClientToken.fromAddress(destination), directed);

      verify();
   }

   @Test
   public void testRevokedBroadcastFromAClientIsIgnored() {
      PlatformMessage forged = PlatformMessage.buildBroadcast(revoked(keyId), Address.clientAddress("android", "5678"))
            .withPlaceId(placeId)
            .create();
      replay();

      listener.onMessage(null, forged);

      verify();
   }

   @Test
   public void testRevokedBroadcastWhosePlaceIsNotItsSourceIsIgnored() {
      PlatformMessage mismatched = PlatformMessage.buildBroadcast(revoked(keyId), place(otherPlaceId))
            .withPlaceId(placeId)
            .create();
      replay();

      listener.onMessage(null, mismatched);

      verify();
   }

   @Test
   public void testRevokedEventFromAnotherPlaceLeavesThisPlacesSessionOpen() {
      expectSessions();
      replay();

      listener.onMessage(null, fromPlace(revoked(keyId), otherPlaceId));

      verify();
   }

   private PlatformMessage fromPlace(MessageBody body, UUID place) {
      // as RevokeApiKeyHandler and DeleteApiKeyHandler send it
      return PlatformMessage.buildBroadcast(body, place(place))
            .withPlaceId(place)
            .withPopulation("general")
            .withActor(actor)
            .create();
   }

   private static Address place(UUID id) {
      return Address.platformService(id, PlaceCapability.NAMESPACE);
   }

   private static MessageBody revoked(UUID keyId) {
      return ApiKeyService.RevokedEvent.builder().withKeyId(keyId.toString()).withLabel("test").build();
   }

   private static MessageBody deleted(UUID keyId) {
      return ApiKeyService.DeletedEvent.builder().withKeyId(keyId.toString()).withLabel("test").build();
   }

   private static Session session(UUID keyId, UUID placeId) {
      ApiKeyPrincipal principal = new ApiKeyPrincipal(keyId, "test", placeId, UUID.randomUUID(), UUID.randomUUID(), Collections.singleton("*:*:*"));
      Session session = EasyMock.createMock(Session.class);
      EasyMock.expect(session.getAuthorizationContext()).andReturn(new AuthorizationContext(principal, null, Collections.emptyList())).anyTimes();
      return session;
   }

   private void expectSessions() {
      EasyMock.expect(registry.getSessions()).andReturn(Arrays.asList(keySession, otherKeySession));
   }

   private static void expectClosed(Session session) {
      session.disconnect(Constants.SESSION_EXPIRED_STATUS);
      EasyMock.expectLastCall();
      session.destroy();
      EasyMock.expectLastCall();
   }

   private void replay() {
      EasyMock.replay(registry, keySession, otherKeySession);
   }

   private void verify() {
      EasyMock.verify(registry, keySession, otherKeySession);
   }
}
