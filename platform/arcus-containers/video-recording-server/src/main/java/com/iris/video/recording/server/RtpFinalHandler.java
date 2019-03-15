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
package com.iris.video.recording.server;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.video.VideoSessionRegistry;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public final class RtpFinalHandler extends  ChannelInboundHandlerAdapter {
   private static final Logger log = LoggerFactory.getLogger(RtpFinalHandler.class);

   private final VideoSessionRegistry registry;

   public RtpFinalHandler(VideoSessionRegistry registry) {
      this.registry = registry;
   }

   @Override
   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      log.trace("read reached end of chain, releasing: {}", msg);
      super.channelRead(ctx, msg);
   }

   @Override
   public void exceptionCaught(@Nullable ChannelHandlerContext ctx, @Nullable Throwable cause) throws Exception {
      if (cause != null) {
         log.warn("video channel closing abnormally: {}", cause.getMessage(), cause);
      }

      registry.remove(ctx);
      if (ctx != null) {
         ctx.close();
      }
   }

   @Override
   public void userEventTriggered(@Nullable ChannelHandlerContext ctx, @Nullable Object evt) throws Exception {
      if (evt instanceof IdleStateEvent) {
         IdleStateEvent e = (IdleStateEvent)evt;
         if (e.state() == IdleState.READER_IDLE) {
            log.warn("connection idle for too long, terminating");
            registry.remove(ctx);
            if (ctx != null) {
               ctx.close();
            }
         }
      }

      super.userEventTriggered(ctx, evt);
   }
}

