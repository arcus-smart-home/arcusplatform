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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubBackupCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubZigbeeCapability;
import com.iris.messages.capability.HubZwaveCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;

/**
 * Operations an instance can switch off for clients with {@value DisabledRiskyOperations#PROP_DISABLED_RISKY_OPERATIONS}.
 *
 * This is a fixed list rather than a list of message types so a mistyped name stops the bridge
 * from starting instead of quietly leaving the operation on, and so alarm and device control
 * can't be switched off by accident.
 */
public enum RiskyOperation {
   EMAIL_CHANGE("email.change") {
      @Override
      public boolean matches(PlatformMessage message) {
         return setsAttribute(message, PersonCapability.ATTR_EMAIL);
      }
   },
   PHONE_CHANGE("phone.change") {
      @Override
      public boolean matches(PlatformMessage message) {
         return setsAttribute(message, PersonCapability.ATTR_MOBILENUMBER);
      }
   },
   ACCOUNT_DELETE("account.delete", AccountCapability.DeleteRequest.NAME),
   PLACE_DELETE("place.delete", PlaceCapability.DeleteRequest.NAME),
   HUB_DELETE("hub.delete", HubCapability.DeleteRequest.NAME),
   HUB_FACTORY_RESET("hub.factoryreset",
         HubAdvancedCapability.FactoryResetRequest.NAME,
         HubZigbeeCapability.FactoryResetRequest.NAME,
         HubZwaveCapability.FactoryResetRequest.NAME,
         // resetting a radio or restoring a backup rewinds or tears down the networks too
         HubZigbeeCapability.ResetRequest.NAME,
         HubZwaveCapability.ResetRequest.NAME,
         HubBackupCapability.RestoreRequest.NAME),
   // the backup holds the radio network keys and weakly hashed copies of people's PINs
   HUB_BACKUP("hub.backup", HubBackupCapability.BackupRequest.NAME),
   PERSON_DELETE("person.delete", PersonCapability.DeleteRequest.NAME, PersonCapability.RemoveFromPlaceRequest.NAME),
   LOGIN_DELETE("login.delete", PersonCapability.DeleteLoginRequest.NAME);

   private final String configName;
   private final Set<String> messageTypes;

   RiskyOperation(String configName, String... messageTypes) {
      this.configName = configName;
      this.messageTypes = ImmutableSet.copyOf(messageTypes);
   }

   public String getConfigName() {
      return configName;
   }

   public boolean matches(PlatformMessage message) {
      return messageTypes.contains(message.getMessageType());
   }

   private static boolean setsAttribute(PlatformMessage message, String attribute) {
      return Capability.CMD_SET_ATTRIBUTES.equals(message.getMessageType())
            && message.getValue().getAttributes().containsKey(attribute);
   }

   // values() clones the array, so keep one copy for the per-message checks
   private static final RiskyOperation[] VALUES = values();

   public static boolean isRisky(PlatformMessage message) {
      for(RiskyOperation operation : VALUES) {
         if(operation.matches(message)) {
            return true;
         }
      }
      return false;
   }

   public static RiskyOperation fromConfigName(String name) {
      for(RiskyOperation operation : values()) {
         if(operation.configName.equalsIgnoreCase(name)) {
            return operation;
         }
      }
      throw new IllegalArgumentException("Unknown operation [" + name + "] in " + DisabledRiskyOperations.PROP_DISABLED_RISKY_OPERATIONS
            + ", expected " + DisabledRiskyOperations.ALL + " or one of "
            + Arrays.stream(values()).map(RiskyOperation::getConfigName).collect(Collectors.joining(", ")));
   }
}
