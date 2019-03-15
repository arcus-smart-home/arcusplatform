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
package com.iris.driver;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.MotorizedDoorCapability;

import java.util.Collection;

/**
 * This class contains attribute values which are of an intermediate state.  For instance, when a lock is unlocked,
 * the unlock value is sent from the client message to the driver for the lockstate attribute.  The driver then sets
 * value of the lockstate to unlocking. Once the device reports a lockstate change, the driver then updates the lockstate
 * value to unlocked.  Thus, unlocking is considered an intermediate value.
 *
 * This is useful for instance if we want the actor context to not be destroyed for an intermediate value and to be associated
 * with the final state change as well.
 */
public class IntermediateAttributeValueRegistry {

   private static final Multimap<String, Object> INTERMEDIATES;

   static {
      INTERMEDIATES = ArrayListMultimap.create();

      //DOOR LOCKS
      INTERMEDIATES.put(DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_UNLOCKING);
      INTERMEDIATES.put(DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_LOCKING);

      //MOTORIZED DOORS
      INTERMEDIATES.put(MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OPENING);
      INTERMEDIATES.put(MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_CLOSING);
   }

   public static boolean isIntermediateValue(String key, Object value) {
      if (INTERMEDIATES.containsKey(key)) {
         Collection<Object> values = INTERMEDIATES.get(key);
         return values.contains(value);
      }
      return false;
   }
}

