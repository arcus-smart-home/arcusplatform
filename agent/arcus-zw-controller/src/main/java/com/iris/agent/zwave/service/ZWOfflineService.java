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
package com.iris.agent.zwave.service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import com.iris.agent.zwave.ZWConfig;
import com.iris.agent.zwave.ZWNetwork;
import com.iris.agent.zwave.ZWServices;
import com.iris.agent.zwave.node.ZWNode;
import com.iris.agent.zwave.util.ZWScheduler;

//TODO: This should be replaced by ZWave Engine functionality

/**
 * This class handles determining when devices are marked as offline.
 * 
 * It uses the following logic on a cycle:
 * 
 * 1. Iterate through all nodes in the offline check queue and increment each
 * node's strike counter.
 * 
 * 2. Clear the offline check queue and iterate through all nodes in the network
 * except for the controller. If the node hasn't communicated with the hub agent
 * for a period of time greater than the node's offline timeout, then mark the device 
 * offline if it is a wakeup device or place it in the offline check queue if it is 
 * not a wakeup device.
 * 
 * 3. Adjust timings based on the size of the queue.
 * 
 * 4. Iterate through the offline check queue and issue a BASIC GET command to
 * each node.
 * 
 * 5. Schedule next cycle.
 * 
 * @author Erik Larson
 */
public class ZWOfflineService {

   // Queue of nodes that need to be messaged to check for online status.
   private final Queue<ZWNode> offlineCheckQueue = new ConcurrentLinkedQueue<>();
   
   // The minimum time that a device must be silent before being checked.
   private long minimumOfflineTimeout = ZWConfig.getMinimumOfflineTimeoutInSecs() * 1000l;

   /**
    * Starts the offline checking process. This should only be called as the hub
    * agent is starting. All nodes will be initialized so that their last call 
    * value is equal to the current time if the device was last known to be online. 
    */
   public void start() {
      ZWScheduler.INSTANCE.startProcess(new ZWOfflineTask(), ZWConfig.getBaseOfflineCheckPeriodInSecs());
      ZWNetwork zwNet = ZWServices.INSTANCE.getNetwork();
      long currentTime = System.currentTimeMillis();
      zwNet.getNodes().forEach(n -> n.setLastCall(n.isOnline() ? currentTime : 0l));
   }
   
   /**
    * Adds a node to the offline check queue.
    * 
    * @param n node to be added
    */
   private void addToOfflineCheckQueue(ZWNode n) {
      if (!offlineCheckQueue.contains(n)) {
         offlineCheckQueue.add(n);
      }
   }

   /**
    * Clears out the offline check queue.
    */
   private void clearOfflineCheckQueue() {
      offlineCheckQueue.clear();
   }
   
   /**
    * Task that performs offline checks. See javadoc for the outer class for
    * a summary of the process.
    * 
    * @author Erik Larson
    */
   private class ZWOfflineTask implements Runnable {

      @Override
      public void run() {
         // Part One, Iterate Through offlineCheckQueue
         offlineCheckQueue.forEach(n -> {
            n.setStrikes(n.getStrikes() + 1);
         });
         
         // Part Two, Iterate Through Nodes
         long currentTime = System.currentTimeMillis();
         ZWNetwork zwNet = ZWServices.INSTANCE.getNetwork();
         clearOfflineCheckQueue();
         zwNet.getNodes().forEach(n -> {
            if (!n.isGateway()) {
               if (n.isOnline()) {
                  long offlineTimeout = Math.max(minimumOfflineTimeout, n.getOfflineTimeout() * 1000l);
                  if ((currentTime - n.getLastCall()) > offlineTimeout) {
                     if (n.isWakeupDevice()) {
                        n.setOnline(false);
                     } else {
                        addToOfflineCheckQueue(n);
                     }
                  }
               } else {                  
                  if (!n.isWakeupDevice()) {
                     addToOfflineCheckQueue(n);
                  }
               }
            }
         });
         
         // Part Three, Use offlineCheckQueue to adapt behavior
         int queueSize = offlineCheckQueue.size();
         long nextCheckDelayInMillis = (ZWConfig.getBaseOfflineCheckPeriodInSecs() * 1000)
               + Math.max(0, (queueSize - ZWConfig.getIncreaseFloor()) * ZWConfig.getMeteringIncreaseInMillis());
         minimumOfflineTimeout = (ZWConfig.getMinimumOfflineTimeoutInSecs() * 1000) 
               + (queueSize - ZWConfig.getIncreaseFloor()) * ZWConfig.getMinimumOfflineTimeoutIncreaseInMillis();
         long messageSendingDelay = ZWConfig.getOfflineCheckPollingDelayInMillis() 
               + (queueSize - ZWConfig.getIncreaseFloor()) * ZWConfig.getOfflinePollingDelayIncreaseInMillis();

         
         // Part Four, Issue Basic Gets
         offlineCheckQueue.forEach((n) -> {
           if (n.getStrikes() > ZWConfig.getNumberOfStrikesBeforeDeviceGoesOffline()) {
              n.setOnline(false);
           }
           ZWServices.INSTANCE.getNetwork().requestBasicGet(n.getNodeId());
           try {
              Thread.sleep(messageSendingDelay);
           } catch (InterruptedException e) {
              // Nothing to do
           }
         });
         
         // Part Five, Schedule next check
         ZWScheduler.INSTANCE.startProcess(new ZWOfflineTask(), nextCheckDelayInMillis, TimeUnit.MILLISECONDS);
         
         // TODO: Send Offline/Online Events when changing state.
      }
   }
}

