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

import org.eclipse.jdt.annotation.Nullable;

import com.iris.agent.watchdog.WatchdogPoke;
import com.iris.agent.watchdog.WatchdogService;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

class GatewayPokeHandler extends ChannelDuplexHandler {
   private final WatchdogPoke watchdog;

   public GatewayPokeHandler() {
      this.watchdog = WatchdogService.createWatchdogPoke("gateway event loop");
   }

   @Override
   public void channelActive(@Nullable ChannelHandlerContext ctx) throws Exception {
      watchdog.poke();
      super.channelActive(ctx);
   }

   @Override
   public void channelRead(@Nullable ChannelHandlerContext ctx, @Nullable Object msg) throws Exception {
      watchdog.poke();
      super.channelRead(ctx, msg);
   }

   @Override
   public void write(@Nullable ChannelHandlerContext ctx, @Nullable Object msg, @Nullable ChannelPromise promise) throws Exception {
      watchdog.poke();
      super.write(ctx, msg, promise);
   }

   @Override
   public void channelRegistered(@Nullable ChannelHandlerContext ctx) throws Exception {
      watchdog.poke();
      super.channelRegistered(ctx);
   }

   @Override
   public void channelUnregistered(@Nullable ChannelHandlerContext ctx) throws Exception {
      watchdog.poke();
      super.channelUnregistered(ctx);
   }

   @Override
   public void handlerAdded(@Nullable ChannelHandlerContext ctx) throws Exception {
      watchdog.poke();
      super.handlerAdded(ctx);
   }

   @Override
   public void handlerRemoved(@Nullable ChannelHandlerContext ctx) throws Exception {
      watchdog.poke();
      super.handlerRemoved(ctx);
   }

   @Override
   public boolean isSharable() {
      return true;
   }
}

