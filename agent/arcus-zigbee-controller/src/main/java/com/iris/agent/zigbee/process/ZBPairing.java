/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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

package com.iris.agent.zigbee.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.zigbee.ZBServices;
import com.iris.agent.zigbee.ember.ZigbeeDriver;
import com.iris.agent.zigbee.events.ZBEvent;
import com.iris.agent.zigbee.events.ZBEventDispatcher;
import com.iris.agent.zigbee.events.ZBEventListener;
import com.iris.agent.zigbee.util.ZBScheduler;

public class ZBPairing implements ZBEventListener {
   private static final Logger logger = LoggerFactory.getLogger(ZBPairing.class);

   public static final ZBPairing INSTANCE = new ZBPairing();

   private boolean isPairing = false;
   private boolean isRemoving = false;

   private ZBPairing() {}

   public synchronized void startPairing(int pairingTimeoutInSecs) {
      if (isPairing || isRemoving) {
         logger.warn("Attempting to start pairing while already in pairing or removal mode.");
         return;
      }

      ZigbeeDriver driver = ZBServices.INSTANCE.getDriver();
      if (driver == null) {
         logger.error("Cannot start pairing: driver not initialized");
         return;
      }

      // ZigBee spec limits permit join to 254 seconds
      int permitDuration = Math.min(pairingTimeoutInSecs, 254);
      driver.permitJoin(permitDuration);
      isPairing = true;
      ZBScheduler.INSTANCE.startProcess(this::stopPairing, pairingTimeoutInSecs);
      logger.info("ZigBee pairing started for {} seconds (permit join {}s)", pairingTimeoutInSecs, permitDuration);
   }

   public synchronized void stopPairing() {
      if (isPairing) {
         ZigbeeDriver driver = ZBServices.INSTANCE.getDriver();
         if (driver != null) {
            driver.denyJoin();
         }
         isPairing = false;
         logger.info("ZigBee pairing stopped");
      }
   }

   public synchronized void startRemoval(int removalTimeoutInSecs) {
      if (isRemoving || isPairing) {
         logger.warn("Attempting to start removal while already in pairing or removal mode.");
         return;
      }

      isRemoving = true;
      ZBScheduler.INSTANCE.startProcess(this::stopRemoval, removalTimeoutInSecs);
      logger.info("ZigBee removal mode started for {} seconds", removalTimeoutInSecs);
   }

   public synchronized void stopRemoval() {
      if (isRemoving) {
         isRemoving = false;
         logger.info("ZigBee removal mode stopped");
      }
   }

   public void removeDevice(long ieeeAddr) {
      ZigbeeDriver driver = ZBServices.INSTANCE.getDriver();
      if (driver != null) {
         driver.leave(ieeeAddr);
      }
   }

   @Override
   public void onZBEvent(ZBEvent event) {
      // ZBPairing can listen for events if needed in the future
   }
}
