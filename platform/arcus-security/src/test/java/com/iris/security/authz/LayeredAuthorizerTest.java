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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.messages.MessagesModule;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.security.apikey.ApiKeyPrincipal;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

/**
 * api-bridge's authorizer. Runs through the real module, with the setting the bridge ships with
 * and an operation disabled, so the binding and the configuration are covered too.
 */
@Modules({MessagesModule.class, AuthzModule.class})
public class LayeredAuthorizerTest extends IrisTestCase {
   private final UUID keyId = UUID.randomUUID();
   private final UUID personId = UUID.randomUUID();
   private final UUID placeId = UUID.randomUUID();
   private final UUID accountId = UUID.randomUUID();

   @Inject
   private Authorizer authorizer;

   @Override
   public void setUp() throws Exception {
      System.setProperty(AuthzModule.AUTHZ_ALGORITHM_PROP, AuthzModule.AUTHZ_ALGORITHM_LAYERED);
      System.setProperty(DisabledRiskyOperations.PROP_DISABLED_RISKY_OPERATIONS, RiskyOperation.HUB_DELETE.getConfigName());
      super.setUp();
   }

   @Override
   public void tearDown() throws Exception {
      System.clearProperty(AuthzModule.AUTHZ_ALGORITHM_PROP);
      System.clearProperty(DisabledRiskyOperations.PROP_DISABLED_RISKY_OPERATIONS);
      super.tearDown();
   }

   private AuthorizationContext key(String... permissions) {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setPlaceId(placeId);
      grant.setAccountId(accountId);
      grant.addPermissions(permissions);
      return new AuthorizationContext(
            new ApiKeyPrincipal(keyId, "test", placeId, personId, accountId, ImmutableSet.copyOf(permissions)),
            null,
            Collections.singletonList(grant));
   }

   private boolean allowed(AuthorizationContext context, String type, Map<String, Object> attributes, Address destination) {
      PlatformMessage message = PlatformMessage
            .buildRequest(type, attributes, Address.clientAddress("api", "1"), destination)
            .withActor(Address.platformService(keyId, ApiKeyPrincipal.ACTOR_NAMESPACE))
            .withPlaceId(placeId)
            .create();
      return authorizer.isAuthorized(context, placeId.toString(), message);
   }

   @Test
   public void testBindingIsLayered() {
      assertTrue(authorizer instanceof LayeredAuthorizer);
   }

   @Test
   public void testWildcardKeyControlsItsOwnPlace() {
      assertTrue(allowed(key("*:*:*"), PlaceCapability.ListDevicesRequest.NAME, ImmutableMap.of(), Address.platformService(placeId, PlaceCapability.NAMESPACE)));
      assertTrue(allowed(key("*:*:*"), "swit:switch", ImmutableMap.of(), Address.platformDriverAddress(UUID.randomUUID())));
   }

   @Test
   public void testWildcardKeyCannotLeaveItsPlace() {
      assertFalse(allowed(key("*:*:*"), Capability.CMD_GET_ATTRIBUTES, ImmutableMap.of(), Address.platformService(UUID.randomUUID(), PlaceCapability.NAMESPACE)));
      assertFalse(allowed(key("*:*:*"), Capability.CMD_GET_ATTRIBUTES, ImmutableMap.of(), Address.platformService(UUID.randomUUID(), AccountCapability.NAMESPACE)));
   }

   @Test
   public void testWildcardKeyCannotUseOwnerOnlyRequests() {
      assertFalse(allowed(key("*:*:*"), AccountCapability.DeleteRequest.NAME, ImmutableMap.of(), Address.platformService(accountId, AccountCapability.NAMESPACE)));
      assertFalse(allowed(key("*:*:*"), PlaceCapability.DeleteRequest.NAME, ImmutableMap.of(), Address.platformService(placeId, PlaceCapability.NAMESPACE)));
   }

   @Test
   public void testRoleCannotWidenAKeysPermissions() {
      assertTrue(allowed(key("swit:*:*"), Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of("swit:state", "ON"), Address.platformDriverAddress(UUID.randomUUID())));
      assertFalse(allowed(key("swit:*:*"), Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of("dim:brightness", 50), Address.platformDriverAddress(UUID.randomUUID())));
      assertFalse(allowed(key("swit:*:*"), Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(PersonCapability.ATTR_EMAIL, "new@example.com"), Address.platformService(personId, PersonCapability.NAMESPACE)));
   }

   @Test
   public void testDisabledOperationIsRefusedForEveryone() {
      assertFalse(allowed(key("*:*:*"), "hub:Delete", ImmutableMap.of(), Address.fromString("SERV:ABC-1234:hub")));
   }
}
