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
package com.iris.netty.bus;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionRegistry;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.service.SessionService;
import com.iris.netty.server.message.IrisNettyMessageUtil;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.security.authz.AuthzMetrics;
import com.iris.security.authz.DisabledRiskyOperations;
import com.iris.security.authz.RoleAuthorizer;
import com.iris.security.principal.DefaultPrincipal;

/**
 * A person has grants at two places and is removed from one of them. Each connection caches the
 * person's grants, so the removal has to drop the grant on every connection the person has open,
 * not only on one whose active place is the removed place.
 */
public class IrisNettyPlatformBusListenerTest {
   // requests that RoleAuthorizer allows by the destination place alone
   private static final List<String> PLACE_REQUESTS = ImmutableList.of(
         Capability.CMD_GET_ATTRIBUTES,
         Capability.CMD_SET_ATTRIBUTES,
         PlaceCapability.ListPersonsWithAccessRequest.NAME,
         PlaceCapability.AddPersonRequest.NAME,
         PlaceCapability.SendInvitationRequest.NAME);

   private final UUID personId = UUID.randomUUID();
   private final UUID placeA = UUID.randomUUID();
   private final UUID placeB = UUID.randomUUID();

   private final List<Connection> connections = new ArrayList<>();
   private RoleAuthorizer authorizer;
   private IrisNettyPlatformBusListener listener;

   @Before
   public void setUp() {
      authorizer = new RoleAuthorizer(EasyMock.createNiceMock(AuthzMetrics.class), new DisabledRiskyOperations());
      SessionRegistry registry = EasyMock.createMock(SessionRegistry.class);
      expect(registry.getSessions()).andAnswer(() -> connections.stream().map((c) -> c.session).collect(Collectors.toList())).anyTimes();
      EasyMock.replay(registry);
      listener = new IrisNettyPlatformBusListener(authorizer, new IrisNettyMessageUtil(), registry);
   }

   @Test
   public void testRemovalFromAnotherPlaceDropsItsGrant() {
      Connection phone = connect(personId, placeA, placeA, placeB);
      for(String type : PLACE_REQUESTS) {
         assertTrue("before removal " + type, allowed(phone, type, placeB));
      }

      remove(personId, placeB);

      for(String type : PLACE_REQUESTS) {
         assertFalse("after removal " + type, allowed(phone, type, placeB));
         assertTrue("other place " + type, allowed(phone, type, placeA));
      }
      assertEquals(placeA.toString(), phone.activePlace);
      assertEquals(ImmutableList.of(), phone.sent);
   }

   @Test
   public void testRemovalFromTheActivePlaceClearsIt() {
      Connection phone = connect(personId, placeB, placeA, placeB);

      remove(personId, placeB);

      assertNull(phone.activePlace);
      assertEquals(ImmutableList.of(placeA), phone.grantedPlaces());
      assertEquals(1, phone.sent.size());
      assertTrue(phone.sent.get(0), phone.sent.get(0).contains(SessionService.ActivePlaceClearedEvent.NAME));
   }

   @Test
   public void testRemovalDropsTheGrantOnEveryConnectionOfThePerson() {
      Connection phone = connect(personId, placeA, placeA, placeB);
      Connection tablet = connect(personId, placeB, placeA, placeB);

      remove(personId, placeB);

      assertEquals(ImmutableList.of(placeA), phone.grantedPlaces());
      assertEquals(placeA.toString(), phone.activePlace);
      assertEquals(ImmutableList.of(placeA), tablet.grantedPlaces());
      assertNull(tablet.activePlace);
   }

   @Test
   public void testABrokenSessionDoesNotStopTheRemoval() {
      connections.add(new Connection(null));
      connections.add(new Connection(new IllegalStateException("Client has been disconnected")));
      Connection phone = connect(personId, placeA, placeA, placeB);

      remove(personId, placeB);

      assertEquals(ImmutableList.of(placeA), phone.grantedPlaces());
   }

   @Test
   public void testRemovalOfSomeoneElseLeavesTheConnectionAlone() {
      Connection other = connect(UUID.randomUUID(), placeA, placeA, placeB);

      remove(personId, placeB);

      assertEquals(0, other.reloads);
      assertEquals(placeA.toString(), other.activePlace);
      assertEquals(ImmutableList.of(), other.sent);
   }

   /**
    * Removes the grant from the store the way PersonDeleter does, then delivers the event it sends.
    */
   private void remove(UUID person, UUID place) {
      for(Connection c : connections) {
         if(person.equals(c.personId)) {
            c.grantsInStore.remove(place);
         }
      }
      MessageBody body = PersonCapability.AuthorizationRemovedEvent.builder().withPlaceId(place.toString()).build();
      PlatformMessage event = PlatformMessage.buildBroadcast(body, Address.platformService(person, PersonCapability.NAMESPACE))
            .withPlaceId(place)
            .create();
      listener.onMessage(null, event);
   }

   private boolean allowed(Connection c, String type, UUID destination) {
      PlatformMessage request = PlatformMessage
            .buildRequest(type, ImmutableMap.of(), Address.clientAddress("android", "1"), Address.platformService(destination, PlaceCapability.NAMESPACE))
            .withActor(Address.platformService(c.personId, PersonCapability.NAMESPACE))
            .withPlaceId(c.activePlace)
            .create();
      return authorizer.isAuthorized(c.session.getAuthorizationContext(), c.activePlace, request);
   }

   private Connection connect(UUID person, UUID activePlace, UUID... grantedPlaces) {
      Connection c = new Connection(person, activePlace, grantedPlaces);
      connections.add(c);
      return c;
   }

   /**
    * One open WebSocket. Like ShiroClient, it loads the grants once and keeps them until asked to reload.
    */
   private static class Connection {
      final UUID personId;
      final List<UUID> grantsInStore;
      final List<String> sent = new ArrayList<>();
      final Session session;
      String activePlace;
      AuthorizationContext context;
      int reloads = 0;

      Connection(UUID personId, UUID activePlace, UUID... grantedPlaces) {
         this.personId = personId;
         this.activePlace = activePlace.toString();
         this.grantsInStore = new ArrayList<>(Arrays.asList(grantedPlaces));
         this.context = load();

         session = EasyMock.createMock(Session.class);
         expect(session.getAuthorizationContext()).andAnswer(() -> context).anyTimes();
         expect(session.getAuthorizationContext(true)).andAnswer(() -> {
            reloads++;
            context = load();
            return context;
         }).anyTimes();
         expect(session.getActivePlace()).andAnswer(() -> this.activePlace).anyTimes();
         session.setActivePlace(EasyMock.<String>anyObject());
         expectLastCall().andAnswer(() -> {
            this.activePlace = (String) EasyMock.getCurrentArguments()[0];
            return null;
         }).anyTimes();
         session.sendMessage(EasyMock.anyString());
         expectLastCall().andAnswer(() -> {
            sent.add((String) EasyMock.getCurrentArguments()[0]);
            return null;
         }).anyTimes();
         expect(session.getClientToken()).andReturn(null).anyTimes();
         EasyMock.replay(session);
      }

      /**
       * A session the listener can't use: a principal-less context (what SubscriberAuthorizationContextLoader
       * returns after a logout) when failure is null, otherwise one whose context lookup throws failure.
       */
      Connection(RuntimeException failure) {
         this.personId = null;
         this.grantsInStore = new ArrayList<>();
         this.context = new AuthorizationContext(null, null, ImmutableList.of());

         session = EasyMock.createMock(Session.class);
         if(failure == null) {
            expect(session.getAuthorizationContext()).andReturn(context).anyTimes();
         }
         else {
            expect(session.getAuthorizationContext()).andThrow(failure).anyTimes();
         }
         expect(session.getActivePlace()).andReturn(null).anyTimes();
         expect(session.getClientToken()).andReturn(null).anyTimes();
         EasyMock.replay(session);
      }

      List<UUID> grantedPlaces() {
         return context.getGrants().stream().map(AuthorizationGrant::getPlaceId).collect(Collectors.toList());
      }

      private AuthorizationContext load() {
         List<AuthorizationGrant> grants = new ArrayList<>();
         for(UUID place : grantsInStore) {
            AuthorizationGrant grant = new AuthorizationGrant();
            grant.setEntityId(personId);
            grant.setPlaceId(place);
            grant.setAccountId(UUID.randomUUID());
            grant.setAccountOwner(false);
            // what an accepted invitation grants
            grant.addPermissions("*:*:*");
            grants.add(grant);
         }
         return new AuthorizationContext(new DefaultPrincipal("person@example.com", personId), null, grants);
      }
   }
}
