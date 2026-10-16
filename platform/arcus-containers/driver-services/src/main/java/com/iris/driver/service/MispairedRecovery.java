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
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;

/**
 * Clears the mispaired state a fallback driver leaves on a device.
 * <p>
 * Only the fallback drivers set the UNSUPPORTED driver state, and only they call
 * {@code mispaired}, which adds {@code pairing.failed} to {@code devadv:errors}.
 * On any other driver both are stale. Older fallback-to-real driver upgrades did
 * not clear them, so a device can end up running a real driver while still
 * reporting a failed pairing.
 * <p>
 * The pairing device follows its device only through value changes. Older code
 * ignored the change that cleared a failed pairing, so a pairing device can still
 * show a failed pairing for a device that is now fine, and nothing will change on
 * the device to correct it. Loading a device that has had errors announces its
 * pairing state again, so that pairing device can catch up.
 */
public final class MispairedRecovery {
   private static final Logger logger = LoggerFactory.getLogger(MispairedRecovery.class);

   private MispairedRecovery() {
   }

   public static boolean isFallback(DeviceDriver driver) {
      return driver.getDriverId().getName().contains("Fallback");
   }

   /**
    * True when the device is running a non-fallback driver but still has the
    * fallback's UNSUPPORTED driver state or {@code pairing.failed} error.
    */
   public static boolean hasStalePairingState(DriverExecutor executor) {
      DeviceDriverContext context = executor.context();
      if(!hasPairingFailed(context) && !isUnsupported(context)) {
         return false;
      }
      return !isFallback(executor.driver());
   }

   /**
    * Removes a stale {@code pairing.failed} and changes a stale UNSUPPORTED driver
    * state to ACTIVE. Any other driver state, such as PROVISIONING or a lost
    * state, is left alone. The caller is responsible for committing.
    *
    * @return true if anything changed
    */
   public static boolean clearStalePairingState(DriverExecutor executor) {
      if(!hasStalePairingState(executor)) {
         return false;
      }
      DeviceDriverContext context = executor.context();
      logger.info("Clearing stale pairing state for device [{}] running driver [{}]", context.getDriverAddress(), executor.driver().getDriverId());
      if(hasPairingFailed(context)) {
         Map<String, String> errors = new HashMap<>(context.getAttributeValue(DeviceAdvancedCapability.KEY_ERRORS));
         errors.remove(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED);
         context.setAttributeValue(DeviceAdvancedCapability.KEY_ERRORS, errors);
      }
      if(isUnsupported(context)) {
         context.setAttributeValue(DeviceAdvancedCapability.KEY_DRIVERSTATE, DeviceAdvancedCapability.DRIVERSTATE_ACTIVE);
      }
      return true;
   }

   /**
    * Clears stale pairing state on a newly loaded executor and commits, which
    * announces the new pairing state. A device with nothing to clear has its
    * pairing state announced as it is, if it has had errors. Only call this before
    * the executor starts, while nothing else can be using its context. A failure is
    * logged, not thrown, so the load carries on; cleared values stay in the context
    * and are saved by its next commit.
    *
    * @return true if the device was recovered
    */
   public static boolean recoverBeforeStart(DriverExecutor executor) {
      try {
         if(!clearStalePairingState(executor)) {
            announcePairingState(executor.context());
            return false;
         }
         executor.context().commit();
         return true;
      }
      catch(Exception e) {
         logger.warn("Unable to recover pairing state for device [{}]", executor.context().getDriverAddress(), e);
         return false;
      }
   }

   // Only a device that has had errors can have a pairing device left showing a
   // failed or misconfigured pairing, so don't announce the others.
   private static void announcePairingState(DeviceDriverContext context) {
      if(context.getAttributeValue(DeviceAdvancedCapability.KEY_ERRORS) == null) {
         return;
      }
      context.broadcast(MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, PlatformDeviceDriverContext.getPairingStateAttributes(context)));
   }

   private static boolean hasPairingFailed(DeviceDriverContext context) {
      Map<String, String> errors = context.getAttributeValue(DeviceAdvancedCapability.KEY_ERRORS);
      return errors != null && errors.containsKey(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED);
   }

   private static boolean isUnsupported(DeviceDriverContext context) {
      String driverState = context.getAttributeValue(DeviceAdvancedCapability.KEY_DRIVERSTATE);
      return DeviceAdvancedCapability.DRIVERSTATE_UNSUPPORTED.equalsIgnoreCase(driverState);
   }
}
