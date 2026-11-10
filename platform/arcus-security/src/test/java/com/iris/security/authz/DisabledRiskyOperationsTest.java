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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubBackupCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubZigbeeCapability;
import com.iris.messages.capability.HubZwaveCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;

public class DisabledRiskyOperationsTest {
   private final UUID personId = UUID.randomUUID();
   private final UUID placeId = UUID.randomUUID();
   private final UUID accountId = UUID.randomUUID();
   private final Address actor = Address.platformService(personId, PersonCapability.NAMESPACE);
   private final Address self = actor;
   private final Address place = Address.platformService(placeId, PlaceCapability.NAMESPACE);
   private final Address account = Address.platformService(accountId, AccountCapability.NAMESPACE);
   private final Address hub = Address.fromString("SERV:ABC-1234:hub");

   private static DisabledRiskyOperations disabled(String config) {
      DisabledRiskyOperations operations = new DisabledRiskyOperations();
      operations.setDisabled(config);
      return operations;
   }

   private PlatformMessage message(String type, Address destination) {
      return message(type, ImmutableMap.of(), destination);
   }

   private PlatformMessage message(String type, Map<String, Object> attributes, Address destination) {
      return PlatformMessage
            .buildRequest(type, attributes, Address.clientAddress("android", "1"), destination)
            .withActor(actor)
            .create();
   }

   private PlatformMessage emailChange() {
      return message(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(PersonCapability.ATTR_EMAIL, "new@example.com"), self);
   }

   private PlatformMessage phoneChange() {
      return message(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(PersonCapability.ATTR_MOBILENUMBER, "5555550100"), self);
   }

   private void assertDisabledBy(String name, PlatformMessage... messages) {
      for(PlatformMessage message : messages) {
         assertTrue(name + " " + message.getMessageType(), disabled(name).isDisabled(message));
         assertTrue("all " + message.getMessageType(), disabled(DisabledRiskyOperations.ALL).isDisabled(message));
         assertFalse("default " + message.getMessageType(), new DisabledRiskyOperations().isDisabled(message));
      }
   }

   // each operation

   @Test
   public void testEmailChange() {
      assertDisabledBy("email.change", emailChange());
   }

   @Test
   public void testEmailChangeAlongsideOtherAttributes() {
      assertDisabledBy("email.change", message(Capability.CMD_SET_ATTRIBUTES,
            ImmutableMap.of(PersonCapability.ATTR_FIRSTNAME, "Jane", PersonCapability.ATTR_EMAIL, "new@example.com"), self));
   }

   @Test
   public void testEmailGuardLeavesOtherAttributesAlone() {
      assertFalse(disabled("email.change").isDisabled(message(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(PersonCapability.ATTR_FIRSTNAME, "Jane"), self)));
   }

   @Test
   public void testPhoneChange() {
      assertDisabledBy("phone.change", phoneChange());
   }

   @Test
   public void testPhoneChangeAlongsideOtherAttributes() {
      assertDisabledBy("phone.change", message(Capability.CMD_SET_ATTRIBUTES,
            ImmutableMap.of(PersonCapability.ATTR_FIRSTNAME, "Jane", PersonCapability.ATTR_MOBILENUMBER, "5555550100"), self));
   }

   @Test
   public void testPhoneGuardLeavesOtherAttributesAlone() {
      assertFalse(disabled("phone.change").isDisabled(message(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(PersonCapability.ATTR_FIRSTNAME, "Jane"), self)));
   }

   @Test
   public void testEmailAndPhoneAreSwitchedSeparately() {
      assertFalse(disabled("phone.change").isDisabled(emailChange()));
      assertFalse(disabled("email.change").isDisabled(phoneChange()));
   }

   @Test
   public void testAuthorizerRefusesAPersonChangingTheirOwnPhone() {
      AuthzMetrics metrics = EasyMock.createNiceMock(AuthzMetrics.class);
      assertFalse(authorizer(disabled("phone.change"), metrics).isAuthorized(owner(), placeId.toString(), phoneChange()));
   }

   @Test
   public void testAccountDelete() {
      assertDisabledBy("account.delete", message(AccountCapability.DeleteRequest.NAME, account));
   }

   @Test
   public void testPlaceDelete() {
      assertDisabledBy("place.delete", message(PlaceCapability.DeleteRequest.NAME, place));
   }

   @Test
   public void testHubDeleteOnBothAddressForms() {
      assertDisabledBy("hub.delete",
            message(HubCapability.DeleteRequest.NAME, hub),
            message(HubCapability.DeleteRequest.NAME, Address.fromString("SERV:hub:ABC-1234")));
   }

   @Test
   public void testHubFactoryReset() {
      assertDisabledBy("hub.factoryreset",
            message(HubAdvancedCapability.FactoryResetRequest.NAME, hub),
            message(HubZigbeeCapability.FactoryResetRequest.NAME, hub),
            message(HubZwaveCapability.FactoryResetRequest.NAME, hub),
            message(HubZigbeeCapability.ResetRequest.NAME, hub),
            message(HubZwaveCapability.ResetRequest.NAME, hub),
            message(HubBackupCapability.RestoreRequest.NAME, hub));
   }

   @Test
   public void testHubBackup() {
      assertDisabledBy("hub.backup", message(HubBackupCapability.BackupRequest.NAME, hub));
   }

   @Test
   public void testPersonDelete() {
      assertDisabledBy("person.delete",
            message(PersonCapability.DeleteRequest.NAME, self),
            message(PersonCapability.RemoveFromPlaceRequest.NAME, self));
   }

   @Test
   public void testLoginDelete() {
      assertDisabledBy("login.delete", message(PersonCapability.DeleteLoginRequest.NAME, self));
   }

   @Test
   public void testIsRiskyWhateverIsDisabled() {
      assertTrue(RiskyOperation.isRisky(emailChange()));
      assertTrue(RiskyOperation.isRisky(message(PlaceCapability.DeleteRequest.NAME, place)));
      assertTrue(RiskyOperation.isRisky(message(HubZwaveCapability.FactoryResetRequest.NAME, hub)));
      assertFalse(RiskyOperation.isRisky(message("swit:switch", Address.platformDriverAddress(UUID.randomUUID()))));
   }

   // what must keep working

   @Test
   public void testAllLeavesAlarmAndDeviceControlAlone() {
      DisabledRiskyOperations all = disabled(DisabledRiskyOperations.ALL);
      Address device = Address.platformDriverAddress(UUID.randomUUID());
      Address alarm = Address.platformService(placeId, AlarmSubsystemCapability.NAMESPACE);
      Address security = Address.platformService(placeId, SecuritySubsystemCapability.NAMESPACE);

      assertFalse(all.isDisabled(message(AlarmSubsystemCapability.DisarmRequest.NAME, alarm)));
      assertFalse(all.isDisabled(message(AlarmSubsystemCapability.ArmRequest.NAME, alarm)));
      assertFalse(all.isDisabled(message(SecuritySubsystemCapability.DisarmRequest.NAME, security)));
      assertFalse(all.isDisabled(message("swit:switch", device)));
      assertFalse(all.isDisabled(message(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of("swit:state", "ON"), device)));
      assertFalse(all.isDisabled(message(DeviceCapability.RemoveRequest.NAME, device)));
   }

   // configuration

   @Test
   public void testNothingIsDisabledByDefault() {
      assertFalse(new DisabledRiskyOperations().isDisabled(emailChange()));
   }

   @Test
   public void testBlankDisablesNothing() {
      assertFalse(disabled("").isDisabled(emailChange()));
      assertFalse(disabled(" , ").isDisabled(emailChange()));
   }

   @Test
   public void testSeveralNamesWithSpacesAndMixedCase() {
      DisabledRiskyOperations operations = disabled(" Email.Change , hub.delete,");
      assertTrue(operations.isDisabled(emailChange()));
      assertTrue(operations.isDisabled(message(HubCapability.DeleteRequest.NAME, hub)));
      assertFalse(operations.isDisabled(message(AccountCapability.DeleteRequest.NAME, account)));
   }

   @Test
   public void testAllIsCaseInsensitive() {
      assertTrue(disabled("ALL").isDisabled(emailChange()));
   }

   @Test
   public void testMistypedNameStopsStartupAndNamesTheChoices() {
      try {
         disabled("email.change,hub.dlete");
         fail("a mistyped name must not silently leave the operation on");
      }
      catch(IllegalArgumentException e) {
         assertTrue(e.getMessage(), e.getMessage().contains("hub.dlete"));
         assertTrue(e.getMessage(), e.getMessage().contains("hub.delete"));
      }
   }

   @Test
   public void testEveryOperationCanBeNamed() {
      for(RiskyOperation operation : RiskyOperation.values()) {
         assertEquals(operation, RiskyOperation.fromConfigName(operation.getConfigName()));
      }
   }

   // in the authorizer

   private RoleAuthorizer authorizer(DisabledRiskyOperations operations, AuthzMetrics metrics) {
      return new RoleAuthorizer(metrics, operations);
   }

   private AuthorizationContext owner() {
      AuthorizationGrant grant = new AuthorizationGrant();
      grant.setPlaceId(placeId);
      grant.setAccountId(accountId);
      grant.setAccountOwner(true);
      grant.addPermissions("*:*:*");
      return new AuthorizationContext(null, null, java.util.Collections.singletonList(grant));
   }

   @Test
   public void testAuthorizerRefusesEvenTheOwnerAndCountsIt() {
      AuthzMetrics metrics = EasyMock.createStrictMock(AuthzMetrics.class);
      metrics.onUnauthorizedDisabled();
      EasyMock.replay(metrics);

      assertFalse(authorizer(disabled("account.delete"), metrics)
            .isAuthorized(owner(), placeId.toString(), message(AccountCapability.DeleteRequest.NAME, account)));
      EasyMock.verify(metrics);
   }

   @Test
   public void testAuthorizerAllowsTheOwnerWhenNotDisabled() {
      AuthzMetrics metrics = EasyMock.createNiceMock(AuthzMetrics.class);
      assertTrue(authorizer(new DisabledRiskyOperations(), metrics)
            .isAuthorized(owner(), placeId.toString(), message(AccountCapability.DeleteRequest.NAME, account)));
   }

   @Test
   public void testAuthorizerRefusesSelfMethodsBeforeTheSelfCheck() {
      AuthzMetrics metrics = EasyMock.createNiceMock(AuthzMetrics.class);
      assertFalse(authorizer(disabled("login.delete"), metrics)
            .isAuthorized(owner(), placeId.toString(), message(PersonCapability.DeleteLoginRequest.NAME, self)));
   }

   @Test
   public void testAuthorizerRefusesAPersonChangingTheirOwnEmail() {
      AuthzMetrics metrics = EasyMock.createNiceMock(AuthzMetrics.class);
      assertFalse(authorizer(disabled("email.change"), metrics).isAuthorized(owner(), placeId.toString(), emailChange()));
   }
}
