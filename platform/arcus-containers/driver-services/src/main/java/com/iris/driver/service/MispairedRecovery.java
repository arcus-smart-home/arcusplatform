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
 * Clears the mispaired state a fallback driver leaves on a device.
 * <p>
 * Only the fallback drivers call {@code mispaired}, which sets the driver state
 * to UNSUPPORTED and adds {@code pairing.failed} to {@code devadv:errors}. On any
 * other driver that error is stale. Older fallback-to-real driver upgrades did
 * not clear it, so a device can end up running a real driver while still
 * reporting a failed pairing.
 */
public final class MispairedRecovery {
   private static final Logger logger = LoggerFactory.getLogger(MispairedRecovery.class);

   private MispairedRecovery() {
   }

   public static boolean isFallback(DeviceDriver driver) {
      return driver.getDefinition().getName().contains("Fallback");
   }

   /**
    * True when the device is running a non-fallback driver but still carries
    * the {@code pairing.failed} error from a fallback driver.
    */
   public static boolean hasStalePairingError(DriverExecutor executor) {
      if(isFallback(executor.driver())) {
         return false;
      }
      Map<String, String> errors = executor.context().getAttributeValue(DeviceAdvancedCapability.KEY_ERRORS);
      return errors != null && errors.containsKey(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED);
   }

   /**
    * Removes a stale {@code pairing.failed} and, if the fallback's UNSUPPORTED
    * driver state is still set, marks the driver ACTIVE. Any other driver state,
    * such as PROVISIONING set by the new driver or a lost state, is left alone.
    * The caller must hold the context lock and commit.
    *
    * @return true if anything changed
    */
   public static boolean clearStalePairingError(DriverExecutor executor) {
      if(!hasStalePairingError(executor)) {
         return false;
      }
      DeviceDriverContext context = executor.context();
      logger.info("Clearing stale pairing error for device [{}] running driver [{}]", context.getDriverAddress(), executor.driver().getDriverId());
      Map<String, String> errors = new HashMap<>(context.getAttributeValue(DeviceAdvancedCapability.KEY_ERRORS));
      errors.remove(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED);
      context.setAttributeValue(DeviceAdvancedCapability.KEY_ERRORS, errors);
      String driverState = context.getAttributeValue(DeviceAdvancedCapability.KEY_DRIVERSTATE);
      if(DeviceAdvancedCapability.DRIVERSTATE_UNSUPPORTED.equalsIgnoreCase(driverState)) {
         context.setAttributeValue(DeviceAdvancedCapability.KEY_DRIVERSTATE, DeviceAdvancedCapability.DRIVERSTATE_ACTIVE);
      }
      return true;
   }

   /**
    * Clears a stale pairing error and commits, for callers outside the driver's
    * own event handling. It checks before taking the context lock because hub
    * sync calls this for every device, and the executor holds that lock while
    * the driver handles an event. Failures are logged, not thrown, so callers
    * carry on with the rest of their work.
    *
    * @return true if the device was recovered
    */
   public static boolean recover(DriverExecutor executor) {
      try {
         if(!hasStalePairingError(executor)) {
            return false;
         }
         synchronized(executor.context()) {
            if(!clearStalePairingError(executor)) {
               return false;
            }
            executor.context().commit();
            return true;
         }
      }
      catch(Exception e) {
         logger.warn("Unable to clear stale pairing error for device [{}]", executor.context().getDriverAddress(), e);
         return false;
      }
   }
}
