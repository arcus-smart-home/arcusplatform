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
package com.iris.common.subsystem.doorsnlocks;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.HubChimeCapability;
import com.iris.messages.capability.KeyPadCapability;
import com.iris.messages.capability.MotorizedDoorCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PetDoorCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.ContactModel;
import com.iris.messages.model.dev.DeviceAdvancedModel;
import com.iris.messages.model.dev.DeviceConnectionModel;
import com.iris.messages.model.dev.DevicePowerModel;
import com.iris.messages.model.dev.DoorLockModel;
import com.iris.messages.model.dev.MotorizedDoorModel;
import com.iris.model.query.expression.ExpressionCompiler;

class DoorsNLocksPredicates {

   private DoorsNLocksPredicates() {
   }

   static final int BATTERY_WARNING_THRESHOLD = 5;
   static final int SIGNAL_WARNING_THRESHOLD = 5;

   static final String QUERY_DOORLOCK_DEVICES =
         "base:caps contains '" + DoorLockCapability.NAMESPACE + "' OR " +
         "base:caps contains '" + MotorizedDoorCapability.NAMESPACE + "' OR " +
         "base:caps contains '" + ContactCapability.NAMESPACE + "' AND cont:usehint == '" + ContactCapability.USEHINT_DOOR + "'";

   static final String QUERY_PEOPLE = "base:caps contains '" + PersonCapability.NAMESPACE + "'";

   static final String QUERY_PETDOOR_DEVICES = "base:caps contains '" + PetDoorCapability.NAMESPACE + "'";
   static final String QUERY_PETDOOR_LOCKED = QUERY_PETDOOR_DEVICES + " AND " + PetDoorCapability.ATTR_LOCKSTATE + " == '"+ PetDoorCapability.LOCKSTATE_UNLOCKED+"'" ;
   static final String QUERY_PETDOOR_AUTO = QUERY_PETDOOR_DEVICES + " AND " + PetDoorCapability.ATTR_LOCKSTATE + " == '"+ PetDoorCapability.LOCKSTATE_AUTO+"'" ;
   static final String QUERY_PETDOOR_OFFLINE = QUERY_PETDOOR_DEVICES + " AND " + DeviceConnectionCapability.ATTR_STATE + " == '"+ DeviceConnectionCapability.STATE_OFFLINE+"'" ;



   static final String QUERY_CHIMING_DEVICES =
         "base:caps contains '" + HubChimeCapability.NAMESPACE + "' OR " +
         "base:caps contains '" + KeyPadCapability.NAMESPACE + "'";

   static final Predicate<Model> IS_DOORLOCK_DEVICE = ExpressionCompiler.compile(QUERY_DOORLOCK_DEVICES);
   static final Predicate<Model> IS_PERSON = ExpressionCompiler.compile(QUERY_PEOPLE);
   static final Predicate<Model> IS_CHIMING_DEVICE = ExpressionCompiler.compile(QUERY_CHIMING_DEVICES);

   static final Predicate<Model> IS_PETDOOR = ExpressionCompiler.compile(QUERY_PETDOOR_DEVICES);
   static final Predicate<Model> IS_PETDOOR_LOCKED = ExpressionCompiler.compile(QUERY_PETDOOR_LOCKED);
   static final Predicate<Model> IS_PETDOOR_AUTO = ExpressionCompiler.compile(QUERY_PETDOOR_AUTO);
   static final Predicate<Model> IS_PETDOOR_OFFLINE = ExpressionCompiler.compile(QUERY_PETDOOR_OFFLINE);

   static final Predicate<Model> IS_CONTACT = new Predicate<Model>() {
      @Override
      public boolean apply(Model arg0) {
         return arg0.getCapabilities().contains(ContactCapability.NAMESPACE);
      }
   };

   static final Predicate<Model> IS_CONTACT_OPEN = new Predicate<Model>() {
      @Override
      public boolean apply(Model arg0) {
         return IS_CONTACT.apply(arg0) && ContactModel.isContactOPENED(arg0);
      }
   };

   static final Predicate<Model> IS_MOTORIZEDDOOR = new Predicate<Model>() {
      @Override
      public boolean apply(Model arg0) {
         return arg0.getCapabilities().contains(MotorizedDoorCapability.NAMESPACE);
      }
   };

   static final Predicate<Model> IS_MOTORIZEDDOOR_OPEN = new Predicate<Model>() {
      @Override
      public boolean apply(Model arg0) {
         return IS_MOTORIZEDDOOR.apply(arg0) && MotorizedDoorModel.isDoorstateOPEN(arg0);
      }
   };

   static final Predicate<Model> IS_DOORLOCK = new Predicate<Model>() {
      @Override
      public boolean apply(Model arg0) {
         return arg0.getCapabilities().contains(DoorLockCapability.NAMESPACE);
      }
   };

   static final Predicate<Model> IS_DOORLOCK_UNLOCKED = new Predicate<Model>() {
      @Override
      public boolean apply(Model arg0) {
         return IS_DOORLOCK.apply(arg0) && DoorLockModel.isLockstateUNLOCKED(arg0);
      }
   };

   static final Predicate<Model> IS_ONLINE = new Predicate<Model>() {
      @Override
      public boolean apply(Model arg0) {
         return DeviceConnectionModel.isStateONLINE(arg0);
      }
   };

   static final Predicate<Model> IS_LOW_BATTERY = new Predicate<Model>() {
      @Override
      public boolean apply(Model arg0) {
         if(!DevicePowerModel.isSourceBATTERY(arg0)) {
            return false;
         }
         return DevicePowerModel.getBattery(arg0, 100) <= BATTERY_WARNING_THRESHOLD;
      }
   };

   static final Predicate<Model> IS_POOR_SIGNAL = new Predicate<Model>() {
      @Override
      public boolean apply(Model arg0) {
         return DeviceConnectionModel.getSignal(arg0, 100) <= SIGNAL_WARNING_THRESHOLD;
      }
   };

   static final Predicate<Model> IS_HUB_CHIME = new Predicate<Model>() {
      @Override
      public boolean apply(Model arg0) {
         return arg0.getCapabilities().contains(HubChimeCapability.NAMESPACE);
      }
   };

   static final Predicate<Model> IS_JAMMED_LOCK = errorCodePredicate("WARN_JAM", DoorLockCapability.NAMESPACE);
   static final Predicate<Model> IS_OBSTRUCTED_DOOR = errorCodePredicate("ERR_OBSTRUCTION", MotorizedDoorCapability.NAMESPACE);

   private static Predicate<Model> errorCodePredicate(final String code, final String reqCap) {
      return new Predicate<Model>() {
         @Override
         public boolean apply(@Nullable Model input) {
            if(input == null) {
               return false;
            }
            if(!input.supports(DeviceAdvancedCapability.NAMESPACE)) {
               return false;
            }
            if(!input.supports(reqCap)) {
               return false;
            }
            Map<String, String> errs = DeviceAdvancedModel.getErrors(input, ImmutableMap.<String, String>of());
            return errs.containsKey(code);
         }
      };
   }

   static Predicate<Model> getOpenPredicateFor(Model m) {
      if(DoorsNLocksPredicates.IS_DOORLOCK.apply(m)) {
         return IS_DOORLOCK_UNLOCKED;
      }
      if(DoorsNLocksPredicates.IS_CONTACT.apply(m)) {
         return IS_CONTACT_OPEN;
      }
      if(DoorsNLocksPredicates.IS_MOTORIZEDDOOR.apply(m)) {
         return IS_MOTORIZEDDOOR_OPEN;
      }
      throw new IllegalStateException("Doors & Locks does not support " + m);
   }
}

