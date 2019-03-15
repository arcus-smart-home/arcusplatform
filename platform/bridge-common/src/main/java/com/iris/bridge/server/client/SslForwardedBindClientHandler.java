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
package com.iris.bridge.server.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.channel.ChannelHandler.Sharable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.netty.BridgeMdcUtil;


/**
 * Binds the {@link HubClient} to the channel context when initialized.
 */
@Singleton
@Sharable
public class SslForwardedBindClientHandler extends ChannelInboundHandlerAdapter {
   private static final Logger logger = LoggerFactory.getLogger(SslForwardedBindClientHandler.class);
   private ClientFactory registry;

   @Inject
   public SslForwardedBindClientHandler(ClientFactory registry) {
      this.registry = registry;
   }

   @Override
   public boolean isSharable() {
      return true;
   }

   @Override
   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof FullHttpRequest) {
         try {
            String sessionId = extractSessionId((FullHttpRequest) msg);
            if (sessionId != null) {
               Client.bind(ctx.channel(), registry.load(sessionId));
               BridgeMdcUtil.bindHttpContext(registry, ctx.channel(), (FullHttpRequest) msg);
            }
         } catch(Exception ex) {
            logger.warn("Exception while retrieving client session", ex);
            return;
         }
      }

      super.channelRead(ctx, msg);
   }

   private String extractSessionId(FullHttpRequest request) {
      String sessionId = request.headers().get("X-SSL-Client-CN");
      return (sessionId != null) ? sessionId.trim() : null;
   }
}

