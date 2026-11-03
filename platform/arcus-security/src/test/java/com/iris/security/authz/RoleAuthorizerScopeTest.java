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
package com.iris.security.authz;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.MobileDeviceCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.service.AccountService;
import com.iris.messages.service.PopulationService;
import com.iris.security.apikey.ApiKeyPrincipal;

/**
 * The actor owns one account, is a guest at a place in a second, and has nothing to do with a third.
 */
public class RoleAuthorizerScopeTest {
   private final UUID personId = UUID.randomUUID();
   private final UUID ownPlace = UUID.randomUUID();
   private final UUID ownAccount = UUID.randomUUID();
   private final UUID guestPlace = UUID.randomUUID();
   private final UUID guestAccount = UUID.randomUUID();
   private final UUID strangerPlace = UUID.randomUUID();
   private final UUID strangerAccount = UUID.randomUUID();
   private final UUID strangerPerson = UUID.randomUUID();

   private final Address actor = Address.platformService(personId, PersonCapability.NAMESPACE);
   private final AuthorizationContext context = new AuthorizationContext(null, null, Arrays.asList(
         grant(ownPlace, ownAccount, true),
         grant(guestPlace, guestAccount, false)));

   private RoleAuthorizer authorizer;

   @Before
   public void setUp() {
      authorizer = new RoleAuthorizer(EasyMock.createNiceMock(AuthzMetrics.class), new DisabledRiskyOperations());
   }

   private static AuthorizationGrant grant(UUID place, UUID account, boolean owner) {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setPlaceId(place);
      grant.setAccountId(account);
      grant.setAccountOwner(owner);
      grant.addPermissions("*:*:*");
      return grant;
   }

   private static Address account(UUID id) {
      return Address.platformService(id, AccountCapability.NAMESPACE);
   }

   private static Address person(UUID id) {
      return Address.platformService(id, PersonCapability.NAMESPACE);
   }

   private PlatformMessage request(Address from, String type, Map<String, Object> attributes, Address destination) {
      return PlatformMessage
            .buildRequest(type, attributes, Address.clientAddress("android", "1"), destination)
            .withActor(from)
            .create();
   }

   private boolean allowed(String type, Address destination) {
      return allowed(type, ImmutableMap.of(), destination);
   }

   private boolean allowed(String type, Map<String, Object> attributes, Address destination) {
      return authorizer.isAuthorized(context, ownPlace.toString(), request(actor, type, attributes, destination));
   }

   private boolean allowedWithoutActivePlace(String type, Address destination) {
      return authorizer.isAuthorized(context, null, request(actor, type, ImmutableMap.of(), destination));
   }

   // account destinations, owner only

   @Test
   public void testOwnerCanDeleteTheirOwnAccount() {
      assertTrue(allowed(AccountCapability.DeleteRequest.NAME, account(ownAccount)));
   }

   @Test
   public void testOwnerCannotDeleteAnotherAccount() {
      assertFalse(allowed(AccountCapability.DeleteRequest.NAME, account(strangerAccount)));
   }

   @Test
   public void testOwnerCannotDeleteAnAccountTheyAreAGuestOf() {
      assertFalse(allowed(AccountCapability.DeleteRequest.NAME, account(guestAccount)));
   }

   @Test
   public void testGuestCannotDeleteTheAccount() {
      assertFalse(authorizer.isAuthorized(context, guestPlace.toString(), request(actor, AccountCapability.DeleteRequest.NAME, ImmutableMap.of(), account(guestAccount))));
   }

   @Test
   public void testEveryOwnerOnlyAccountRequestIsLimitedToTheOwnAccount() {
      for(String type : new String[] {
            AccountCapability.AddPlaceRequest.NAME,
            AccountCapability.SignupTransitionRequest.NAME,
            AccountCapability.SkipPremiumTrialRequest.NAME,
            AccountCapability.ListDevicesRequest.NAME,
            AccountCapability.ListHubsRequest.NAME,
      }) {
         assertTrue(type, allowed(type, account(ownAccount)));
         assertFalse(type, allowed(type, account(strangerAccount)));
         assertFalse(type, allowed(type, account(guestAccount)));
      }
   }

   // account destinations, any member

   @Test
   public void testMemberCanReadAnAccountTheyHaveAGrantOn() {
      assertTrue(allowed(Capability.CMD_GET_ATTRIBUTES, account(ownAccount)));
      assertTrue(allowed(Capability.CMD_GET_ATTRIBUTES, account(guestAccount)));
   }

   @Test
   public void testStrangerCannotReadOrChangeAnAccount() {
      assertFalse(allowed(Capability.CMD_GET_ATTRIBUTES, account(strangerAccount)));
      assertFalse(allowed(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of("account:owner", "x"), account(strangerAccount)));
      assertFalse(allowed(AccountCapability.ListPlacesRequest.NAME, account(strangerAccount)));
      assertFalse(allowed(AccountCapability.ActivateRequest.NAME, account(strangerAccount)));
   }

   @Test
   public void testAccountRequestWithoutAnActivePlaceIsRefused() {
      assertFalse(allowedWithoutActivePlace(Capability.CMD_GET_ATTRIBUTES, account(ownAccount)));
   }

   // account creation

   @Test
   public void testAccountCreationOverTheSocketIsRefused() {
      assertFalse(allowed(MessageConstants.MSG_CREATE_ACCOUNT, Address.platformService(AccountService.NAMESPACE)));
   }

   // mobile devices

   @Test
   public void testPersonCanReachTheirOwnMobileDevice() {
      Address own = Address.platformService(personId, MobileDeviceCapability.NAMESPACE, 1);
      assertTrue(allowed(Capability.CMD_GET_ATTRIBUTES, own));
      assertTrue(allowed(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of("mobiledevice:name", "x"), own));
   }

   @Test
   public void testPersonCannotReachAnotherPersonsMobileDevice() {
      Address other = Address.platformService(strangerPerson, MobileDeviceCapability.NAMESPACE, 1);
      assertFalse(allowed(Capability.CMD_GET_ATTRIBUTES, other));
      assertFalse(allowed(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of("mobiledevice:name", "x"), other));
   }

   // invitations

   @Test
   public void testPersonCanHandleTheirOwnInvitationsWithoutAPlace() {
      assertTrue(allowedWithoutActivePlace(PersonCapability.PendingInvitationsRequest.NAME, actor));
      assertTrue(allowedWithoutActivePlace(PersonCapability.AcceptInvitationRequest.NAME, actor));
      assertTrue(allowedWithoutActivePlace(PersonCapability.RejectInvitationRequest.NAME, actor));
   }

   @Test
   public void testPersonCannotHandleSomeoneElsesInvitations() {
      assertFalse(allowed(PersonCapability.PendingInvitationsRequest.NAME, person(strangerPerson)));
      assertFalse(allowed(PersonCapability.AcceptInvitationRequest.NAME, person(strangerPerson)));
      assertFalse(allowed(PersonCapability.RejectInvitationRequest.NAME, person(strangerPerson)));
   }

   // removing someone from a place

   private boolean removeFrom(UUID place) {
      Map<String, Object> body = place == null ? ImmutableMap.of() : ImmutableMap.of(PersonCapability.RemoveFromPlaceRequest.ATTR_PLACEID, place.toString());
      return allowed(PersonCapability.RemoveFromPlaceRequest.NAME, body, person(strangerPerson));
   }

   @Test
   public void testCanRemoveSomeoneFromAPlaceWithAccess() {
      assertTrue(removeFrom(ownPlace));
      assertTrue(removeFrom(guestPlace));
   }

   @Test
   public void testBlankPlaceMeansTheActivePlace() {
      assertTrue(removeFrom(null));
   }

   @Test
   public void testCannotRemoveSomeoneFromAPlaceWithoutAccess() {
      assertFalse(removeFrom(strangerPlace));
   }

   @Test(expected = ErrorEventException.class)
   public void testMalformedPlaceIsRefused() {
      allowed(PersonCapability.RemoveFromPlaceRequest.NAME, ImmutableMap.of(PersonCapability.RemoveFromPlaceRequest.ATTR_PLACEID, "not-a-uuid"), person(strangerPerson));
   }

   // populations

   @Test
   public void testClientsCannotMovePlacesBetweenPopulations() {
      Address population = Address.platformService(PopulationService.NAMESPACE);
      assertFalse(allowed(PopulationService.AddPlacesRequest.NAME, population));
      assertFalse(allowed(PopulationService.RemovePlacesRequest.NAME, population));
   }

   // API keys

   private final UUID keyId = UUID.randomUUID();
   private final Address keyActor = Address.platformService(keyId, ApiKeyPrincipal.ACTOR_NAMESPACE);
   private final AuthorizationContext keyContext = new AuthorizationContext(
         new ApiKeyPrincipal(keyId, "test", ownPlace, personId, ownAccount, ImmutableSet.of("*:*:*")),
         null,
         Arrays.asList(grant(ownPlace, ownAccount, false)));

   private boolean keyAllowed(String type, Address destination) {
      return authorizer.isAuthorized(keyContext, ownPlace.toString(), request(keyActor, type, ImmutableMap.of(), destination));
   }

   @Test
   public void testKeyCanListPlacesOfItsCreator() {
      assertTrue(keyAllowed(PersonCapability.ListAvailablePlacesRequest.NAME, actor));
   }

   @Test
   public void testKeyCannotListPlacesOfAnyoneElse() {
      assertFalse(keyAllowed(PersonCapability.ListAvailablePlacesRequest.NAME, person(strangerPerson)));
   }

   @Test
   public void testKeyGetsNoOtherSelfMethodsOfItsCreator() {
      assertFalse(keyAllowed(PersonCapability.DeleteLoginRequest.NAME, actor));
      assertFalse(keyAllowed(PersonCapability.SetSecurityAnswersRequest.NAME, actor));
      assertFalse(keyAllowed(PersonCapability.PromoteToAccountRequest.NAME, actor));
   }

   @Test
   public void testKeyCannotReachItsCreatorsMobileDevices() {
      assertFalse(keyAllowed(Capability.CMD_GET_ATTRIBUTES, Address.platformService(personId, MobileDeviceCapability.NAMESPACE, 1)));
   }

   @Test
   public void testKeyIsNeverAnOwner() {
      assertFalse(keyAllowed(AccountCapability.DeleteRequest.NAME, account(ownAccount)));
      assertFalse(keyAllowed(PlaceCapability.DeleteRequest.NAME, Address.platformService(ownPlace, PlaceCapability.NAMESPACE)));
   }

   @Test
   public void testKeyCanReadItsOwnAccountAndPlaceOnly() {
      assertTrue(keyAllowed(Capability.CMD_GET_ATTRIBUTES, account(ownAccount)));
      assertFalse(keyAllowed(Capability.CMD_GET_ATTRIBUTES, account(strangerAccount)));
      assertTrue(keyAllowed(PlaceCapability.ListDevicesRequest.NAME, Address.platformService(ownPlace, PlaceCapability.NAMESPACE)));
      assertFalse(keyAllowed(PlaceCapability.ListDevicesRequest.NAME, Address.platformService(strangerPlace, PlaceCapability.NAMESPACE)));
   }

   @Test
   public void testKeyCanStillControlTheAlarmAndDevices() {
      assertTrue(keyAllowed(AlarmSubsystemCapability.DisarmRequest.NAME, Address.platformService(ownPlace, AlarmSubsystemCapability.NAMESPACE)));
      assertTrue(keyAllowed("swit:switch", Address.platformDriverAddress(UUID.randomUUID())));
   }
}
