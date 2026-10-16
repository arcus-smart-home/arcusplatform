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
package com.iris.driver.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.messages.capability.DeviceAdvancedCapability;

/**
 * Recovers devices left in a mispaired state by a fallback driver.
 * <p>
 * Only the fallback drivers call {@code mispaired}, which sets the driver state
 * to UNSUPPORTED and adds {@code pairing.failed} to {@code devadv:errors}. Older
 * fallback-to-real driver upgrades did not clear these, so a device can end up
 * running a real driver while still reporting a failed pairing.
 */
public final class MispairedRecovery {
   private static final Logger logger = LoggerFactory.getLogger(MispairedRecovery.class);

   private MispairedRecovery() {
   }

   public static boolean isFallback(DeviceDriver driver) {
      return driver.getDefinition().getName().contains("Fallback");
   }

   /**
    * Marks the driver ACTIVE and removes the pairing errors. The caller is
    * responsible for committing the context.
    */
   public static void clearPairingErrors(DeviceDriverContext context) {
      context.setAttributeValue(DeviceAdvancedCapability.KEY_DRIVERSTATE, DeviceAdvancedCapability.DRIVERSTATE_ACTIVE);
      Map<String, String> errors = context.getAttributeValue(DeviceAdvancedCapability.KEY_ERRORS);
      if(errors != null) {
         errors = new HashMap<>(errors);
         errors.remove(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED);
         errors.remove(DeviceAdvancedCapability.PairingMisconfiguredException.CODE_PAIRING_MISCONFIGURED);
         context.setAttributeValue(DeviceAdvancedCapability.KEY_ERRORS, errors);
      }
   }

   /**
    * True when the device is running a real driver but still carries the
    * UNSUPPORTED state and pairing.failed error from a fallback driver.
    */
   public static boolean hasStalePairingErrors(DriverExecutor executor) {
      if(isFallback(executor.driver())) {
         return false;
      }
      DeviceDriverContext context = executor.context();
      String driverState = context.getAttributeValue(DeviceAdvancedCapability.KEY_DRIVERSTATE);
      if(!DeviceAdvancedCapability.DRIVERSTATE_UNSUPPORTED.equalsIgnoreCase(driverState)) {
         return false;
      }
      Map<String, String> errors = context.getAttributeValue(DeviceAdvancedCapability.KEY_ERRORS);
      return errors != null && errors.containsKey(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED);
   }

   /**
    * Clears stale pairing errors and commits, emitting a value change so the
    * pairing subsystem can move the device to PAIRED.
    *
    * @return true if the device was recovered
    */
   public static boolean recover(DriverExecutor executor) {
      synchronized(executor.context()) {
         if(!hasStalePairingErrors(executor)) {
            return false;
         }
         logger.info("Clearing stale pairing errors for device [{}] running driver [{}]", executor.context().getDriverAddress(), executor.driver().getDriverId());
         clearPairingErrors(executor.context());
         executor.context().commit();
         return true;
      }
   }
}
