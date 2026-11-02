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

import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.messages.MessagesModule;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.security.apikey.ApiKeyPrincipal;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

/**
 * api-bridge gets the module's default authorizer, so this is what stands between an API key and
 * an operation the instance has switched off. Runs through the real module and configuration.
 */
@Modules({MessagesModule.class, AuthzModule.class})
public class PermissionsAuthorizerRiskyOperationsTest extends IrisTestCase {
   private final UUID placeId = UUID.randomUUID();
   private final UUID keyId = UUID.randomUUID();

   @Inject
   private Authorizer authorizer;

   @Override
   public void setUp() throws Exception {
      System.setProperty(DisabledRiskyOperations.PROP_DISABLED_RISKY_OPERATIONS, "hub.delete,email.change");
      super.setUp();
   }

   @Override
   public void tearDown() throws Exception {
      System.clearProperty(DisabledRiskyOperations.PROP_DISABLED_RISKY_OPERATIONS);
      super.tearDown();
   }

   private boolean allowed(String type, Map<String, Object> attributes, Address destination) {
      AuthorizationContext wildcardKey = AuthzFixtures.createContext(placeId, "*:*:*");
      PlatformMessage message = PlatformMessage
            .buildRequest(type, attributes, Address.clientAddress("api", "1"), destination)
            .withActor(Address.platformService(keyId, ApiKeyPrincipal.ACTOR_NAMESPACE))
            .withPlaceId(placeId)
            .create();
      return authorizer.isAuthorized(wildcardKey, placeId.toString(), message);
   }

   @Test
   public void testThisIsTheAuthorizerApiBridgeGets() {
      assertTrue(authorizer instanceof PermissionsAuthorizer);
   }

   @Test
   public void testWildcardKeyIsRefusedADisabledOperation() {
      assertFalse(allowed(HubCapability.DeleteRequest.NAME, ImmutableMap.of(), Address.fromString("SERV:ABC-1234:hub")));
      assertFalse(allowed(HubCapability.DeleteRequest.NAME, ImmutableMap.of(), Address.fromString("SERV:hub:ABC-1234")));
   }

   @Test
   public void testWildcardKeyIsRefusedAnEmailChange() {
      assertFalse(allowed(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(PersonCapability.ATTR_EMAIL, "new@example.com"), Address.platformService(UUID.randomUUID(), PersonCapability.NAMESPACE)));
   }

   @Test
   public void testOperationsThatAreNotListedStillWork() {
      assertTrue(allowed(AccountCapability.DeleteRequest.NAME, ImmutableMap.of(), Address.platformService(UUID.randomUUID(), AccountCapability.NAMESPACE)));
   }

   @Test
   public void testAlarmAndDevicesStillWork() {
      assertTrue(allowed(AlarmSubsystemCapability.DisarmRequest.NAME, ImmutableMap.of(), Address.platformService(placeId, AlarmSubsystemCapability.NAMESPACE)));
      assertTrue(allowed(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of("swit:state", "ON"), Address.platformDriverAddress(UUID.randomUUID())));
      assertTrue(allowed(PlaceCapability.ListDevicesRequest.NAME, ImmutableMap.of(), Address.platformService(placeId, PlaceCapability.NAMESPACE)));
   }
}
