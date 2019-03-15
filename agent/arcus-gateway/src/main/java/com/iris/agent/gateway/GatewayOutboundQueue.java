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
package com.iris.agent.gateway;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.HubWiFiCapability;
import com.iris.protocol.ProtocolMessage;

class GatewayOutboundQueue {
   private static final Logger log = LoggerFactory.getLogger(GatewayOutboundQueue.class);
   private static final int MAX_QUEUE_SIZE = 256;
   private static final long RETENTION_TIME = TimeUnit.NANOSECONDS.convert(1, TimeUnit.HOURS);

   private final BlockingQueue<Entry> queue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

   boolean queueIfNeeded(PlatformMessage message) {
      switch (message.getMessageType()) {
      case MessageConstants.MSG_ADD_DEVICE_REQUEST:
      case MessageConstants.MSG_REMOVE_DEVICE_REQUEST:
      case Capability.EVENT_VALUE_CHANGE:
      case HubWiFiCapability.WiFiConnectResultEvent.NAME:
      case DeviceAdvancedCapability.RemovedDeviceEvent.NAME:
         return queue(message);

      default:
         // not important enough to keep
         return false;
      }
   }

   boolean queueIfNeeded(ProtocolMessage message) {
      // no messages are important enough to queue
      return false;
   }

   boolean queueIfNeeded(Object message) {
      // no messages are important enough to queue
      return false;
   }

   @Nullable
   Object take(long time) {
      while (true) {
         Entry next = queue.poll();
         if (next == null) {
            return null;
         }

         if (next.expires >= time) {
            return next.message;
         } else {
            log.info("queued message expired, dropping: {}", next.message);
         }
      }
   }

   private boolean queue(Object message) {
      long currentTime = System.nanoTime();
      Entry entry = new Entry(message, currentTime + RETENTION_TIME);

      // Remove expired entries from the queue, the entries should
      // be in chronological order, so start at the front of the
      // queue and proceed forward until we find a non-expired
      // message in the queue.
      while (true) {
         Entry next = queue.peek();
         if (next == null) {
            break;
         }

         if (next.expires > currentTime) {
            queue.poll();
         } else {
            break;
         }
      }

      if (queue.offer(entry)) {
         log.info("queued message for later delivery: {}", message);
         return true;
      }

      log.info("could not queue message for later delivery, dropping: {}", message);
      return false;
   }

   private static final class Entry {
      private final long expires;
      private final Object message;

      public Entry(Object message, long expires) {
         this.message = message;
         this.expires = expires;
      }
   }
}

