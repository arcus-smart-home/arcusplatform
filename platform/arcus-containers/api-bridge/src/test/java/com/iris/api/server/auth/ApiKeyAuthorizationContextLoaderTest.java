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
package com.iris.api.server.auth;

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
import com.iris.messages.capability.PlaceCapability;
import com.iris.security.apikey.ApiKeyPrincipal;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.security.authz.Authorizer;
import com.iris.security.authz.AuthzModule;
import com.iris.security.principal.DefaultPrincipal;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

/**
 * The grants an API key session gets. Checked directly, and through api-bridge's authorizer
 * (layered, the setting the bridge ships with) so the grant's effect is covered as well as its fields.
 */
@Modules({MessagesModule.class, AuthzModule.class})
public class ApiKeyAuthorizationContextLoaderTest extends IrisTestCase {
   private final UUID keyId = UUID.randomUUID();
   private final UUID personId = UUID.randomUUID();
   private final UUID placeId = UUID.randomUUID();
   private final UUID accountId = UUID.randomUUID();

   private final ApiKeyAuthorizationContextLoader loader = new ApiKeyAuthorizationContextLoader();

   @Inject
   private Authorizer authorizer;

   @Override
   public void setUp() throws Exception {
      System.setProperty(AuthzModule.AUTHZ_ALGORITHM_PROP, AuthzModule.AUTHZ_ALGORITHM_LAYERED);
      super.setUp();
   }

   @Override
   public void tearDown() throws Exception {
      System.clearProperty(AuthzModule.AUTHZ_ALGORITHM_PROP);
      super.tearDown();
   }

   private ApiKeyPrincipal key(String... permissions) {
      return new ApiKeyPrincipal(keyId, "test", placeId, personId, accountId, ImmutableSet.copyOf(permissions));
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
   public void testNoPrincipalHasNoGrants() {
      assertTrue(loader.loadContext(null).getGrants().isEmpty());
   }

   @Test
   public void testAPersonPrincipalGetsNoGrantsHere() {
      AuthorizationContext context = loader.loadContext(new DefaultPrincipal("someone@example.com", personId));
      assertTrue(context.getGrants().isEmpty());
   }

   @Test
   public void testKeyGetsOneGrantForItsOwnPlace() {
      AuthorizationContext context = loader.loadContext(key("swit:*:*", "place:r,x:" + placeId));

      assertEquals(1, context.getGrants().size());
      AuthorizationGrant grant = context.getGrants().get(0);
      assertEquals(keyId, grant.getEntityId());
      assertEquals(placeId, grant.getPlaceId());
      assertEquals(accountId, grant.getAccountId());
      assertEquals(ImmutableSet.of("swit:*:*", "place:r,x:" + placeId), grant.getPermissions());
   }

   @Test
   public void testKeyIsNeverTheAccountOwner() {
      assertFalse(loader.loadContext(key("*:*:*")).getGrants().get(0).isAccountOwner());
   }

   @Test
   public void testKeyControlsItsOwnPlace() {
      AuthorizationContext context = loader.loadContext(key("*:*:*"));
      assertTrue(allowed(context, PlaceCapability.ListDevicesRequest.NAME, ImmutableMap.of(), Address.platformService(placeId, PlaceCapability.NAMESPACE)));
      assertTrue(allowed(context, Capability.CMD_GET_ATTRIBUTES, ImmutableMap.of(), Address.platformService(accountId, AccountCapability.NAMESPACE)));
   }

   @Test
   public void testKeyCannotReachAnotherPlaceOrAccount() {
      AuthorizationContext context = loader.loadContext(key("*:*:*"));
      assertFalse(allowed(context, PlaceCapability.ListDevicesRequest.NAME, ImmutableMap.of(), Address.platformService(UUID.randomUUID(), PlaceCapability.NAMESPACE)));
      assertFalse(allowed(context, Capability.CMD_GET_ATTRIBUTES, ImmutableMap.of(), Address.platformService(UUID.randomUUID(), AccountCapability.NAMESPACE)));
   }

   @Test
   public void testKeyCannotUseOwnerOnlyRequests() {
      AuthorizationContext context = loader.loadContext(key("*:*:*"));
      assertFalse(allowed(context, AccountCapability.DeleteRequest.NAME, ImmutableMap.of(), Address.platformService(accountId, AccountCapability.NAMESPACE)));
      assertFalse(allowed(context, PlaceCapability.DeleteRequest.NAME, ImmutableMap.of(), Address.platformService(placeId, PlaceCapability.NAMESPACE)));
   }

   @Test
   public void testKeyPermissionsStillApply() {
      AuthorizationContext context = loader.loadContext(key("swit:*:*"));
      assertTrue(allowed(context, Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of("swit:state", "ON"), Address.platformDriverAddress(UUID.randomUUID())));
      assertFalse(allowed(context, Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of("dim:brightness", 50), Address.platformDriverAddress(UUID.randomUUID())));
   }
}
