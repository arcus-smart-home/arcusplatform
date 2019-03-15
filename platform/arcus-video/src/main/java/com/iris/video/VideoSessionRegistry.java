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
package com.iris.video;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class VideoSessionRegistry {

   public static final AttributeKey<UUID> REC_KEY = AttributeKey.<UUID>valueOf("recId");

   private final Map<UUID,ChannelHandlerContext> sessions = new ConcurrentHashMap<>();

   public void put(UUID recId, ChannelHandlerContext ctx) {
      if(ctx == null) {
         return;
      }
      Attribute<UUID> recIdAttr = ctx.attr(REC_KEY);
      if(!recIdAttr.compareAndSet(null, recId)) {
         throw new IllegalStateException("attempt to set existing non-null ctx recId [" + recIdAttr.get() + "] to [" + recId + "]");
      }
      sessions.put(recIdAttr.get(), ctx);
   }

   public ChannelHandlerContext remove(ChannelHandlerContext ctx) {
      if(ctx == null) {
         return null;
      }
      Attribute<UUID> recIdAttr = ctx.attr(REC_KEY);
      if(recIdAttr != null && recIdAttr.get() != null) {
         return remove(recIdAttr.get());
      }
      return null;
   }

   public ChannelHandlerContext remove(UUID recId) {
      return sessions.remove(recId);
   }
}

