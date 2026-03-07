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

package com.iris.agent.zigbee.service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.zigbee.ZBNetwork;
import com.iris.agent.zigbee.ZBServices;
import com.iris.agent.zigbee.node.ZBNode;
import com.iris.agent.zigbee.util.ZBConfig;
import com.iris.agent.zigbee.util.ZBScheduler;

public class ZBOfflineService {
   private static final Logger logger = LoggerFactory.getLogger(ZBOfflineService.class);

   private final Queue<ZBNode> offlineCheckQueue = new ConcurrentLinkedQueue<>();
   private long minimumOfflineTimeout = ZBConfig.getMinimumOfflineTimeoutInSecs() * 1000L;

   public void start() {
      ZBScheduler.INSTANCE.startProcess(new ZBOfflineTask(), ZBConfig.getBaseOfflineCheckPeriodInSecs());
      ZBNetwork zbNet = ZBServices.INSTANCE.getNetwork();
      long currentTime = System.currentTimeMillis();
      zbNet.getNodes().forEach(n -> n.setLastCall(n.isOnline() ? currentTime : 0L));
   }

   private void addToOfflineCheckQueue(ZBNode n) {
      if (!offlineCheckQueue.contains(n)) {
         offlineCheckQueue.add(n);
      }
   }

   private class ZBOfflineTask implements Runnable {
      @Override
      public void run() {
         // Part 1: Increment strikes for queued nodes
         offlineCheckQueue.forEach(n -> n.setStrikes(n.getStrikes() + 1));

         // Part 2: Scan all nodes for offline candidates
         long currentTime = System.currentTimeMillis();
         ZBNetwork zbNet = ZBServices.INSTANCE.getNetwork();
         offlineCheckQueue.clear();

         zbNet.getNodes().forEach(n -> {
            if (n.isOnline()) {
               long offlineTimeout = Math.max(minimumOfflineTimeout, n.getOfflineTimeout() * 1000L);
               if ((currentTime - n.getLastCall()) > offlineTimeout) {
                  if (n.isSleepyDevice()) {
                     n.setOnline(false);
                  } else {
                     addToOfflineCheckQueue(n);
                  }
               }
            } else {
               if (!n.isSleepyDevice()) {
                  addToOfflineCheckQueue(n);
               }
            }
         });

         // Part 3: Adapt timing based on queue size
         int queueSize = offlineCheckQueue.size();
         long nextCheckDelayInMillis = (ZBConfig.getBaseOfflineCheckPeriodInSecs() * 1000L)
               + Math.max(0, (queueSize - ZBConfig.getIncreaseFloor()) * ZBConfig.getMeteringIncreaseInMillis());
         minimumOfflineTimeout = (ZBConfig.getMinimumOfflineTimeoutInSecs() * 1000L)
               + (long)(queueSize - ZBConfig.getIncreaseFloor()) * ZBConfig.getMinimumOfflineTimeoutIncreaseInMillis();

         // Part 4: Check strikes and mark offline
         offlineCheckQueue.forEach(n -> {
            if (n.getStrikes() > ZBConfig.getNumberOfStrikesBeforeDeviceGoesOffline()) {
               n.setOnline(false);
            }
         });

         // Part 5: Schedule next check
         ZBScheduler.INSTANCE.startProcess(new ZBOfflineTask(), nextCheckDelayInMillis, TimeUnit.MILLISECONDS);
      }
   }
}
