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
package com.iris.bridge.server.netty;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class PingPong {
   private static final AttributeKey<PingPong> ATTR_PINGPONG = AttributeKey.<PingPong>valueOf(PingPong.class.getName());
   private final Deque<Long> pingQueue = new ConcurrentLinkedDeque<>();
   
   public static PingPong get(Channel ch) {
      PingPong pingPong = ch.attr(ATTR_PINGPONG).get();
      if (pingPong == null) {
         pingPong = new PingPong();
         PingPong current = ch.attr(ATTR_PINGPONG).setIfAbsent(pingPong);
         // If it already had a value, use that one instead.
         if (current != null) {
            pingPong = current;
         }
      }
      return pingPong;
   }
   
   public static void clear(Channel ch) {
      ch.attr(ATTR_PINGPONG).remove();
   }
   
   public void recordPing() {
      pingQueue.add(System.currentTimeMillis());
   }

   public void recordPong() {
      pingQueue.poll();
   }

   public Long getTimeOfOldestPing() {
      return pingQueue.peek();
   }

   public Long getTimeOfLastPing() {
      return pingQueue.peekLast();
   }
}

