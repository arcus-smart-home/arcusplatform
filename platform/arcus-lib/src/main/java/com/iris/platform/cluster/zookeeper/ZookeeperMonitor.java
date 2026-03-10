/*
 * Copyright 2020 Arcus Project
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
package com.iris.platform.cluster.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ZookeeperMonitor implements Watcher, StatCallback {
   private static final Logger logger = LoggerFactory.getLogger(ZookeeperMonitor.class);

   private volatile Runnable onSessionExpired;
   private volatile Runnable onReconnected;
   private volatile CountDownLatch connectedLatch = new CountDownLatch(1);
   private volatile boolean connected;

   public ZookeeperMonitor() {
   }

   public void setOnSessionExpired(Runnable onSessionExpired) {
      this.onSessionExpired = onSessionExpired;
   }

   public void setOnReconnected(Runnable onReconnected) {
      this.onReconnected = onReconnected;
   }

   public boolean isConnected() {
      return connected;
   }

   public boolean awaitConnection(long timeout, TimeUnit unit) throws InterruptedException {
      return connectedLatch.await(timeout, unit);
   }

   public void resetConnectionLatch() {
      connectedLatch = new CountDownLatch(1);
      connected = false;
   }

   @Override
   public void process(WatchedEvent event) {
      if (event.getType() == Event.EventType.None) {
         switch (event.getState()) {
            case SyncConnected:
               logger.info("Connected to zookeeper");
               boolean wasDisconnected = !connected;
               connected = true;
               connectedLatch.countDown();
               if (wasDisconnected) {
                  Runnable handler = onReconnected;
                  if (handler != null) {
                     handler.run();
                  }
               }
               break;
            case Disconnected:
               logger.warn("Disconnected from zookeeper, waiting for reconnect");
               connected = false;
               break;
            case Expired:
               logger.error("Zookeeper session expired, all ephemeral nodes lost");
               connected = false;
               Runnable handler = onSessionExpired;
               if (handler != null) {
                  handler.run();
               } else {
                  logger.error("No session expiry handler registered, forcing shutdown");
                  System.exit(-1);
               }
               break;
            default:
               logger.warn("Unhandled zookeeper state: {}", event.getState());
               break;
         }
      }
   }

   @Override
   public void processResult(int rc, String path, Object ctx, Stat stat) {
   }
}
