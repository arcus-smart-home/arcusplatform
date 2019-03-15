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
/**
 *
 */
package com.iris.bridge.server.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.channel.ChannelHandler.Sharable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;
import com.iris.bridge.server.netty.BridgeMdcUtil;

/**
 * Binds the {@link Client} to the channel context when initialized.
 */
@Singleton
@Sharable
public class ClearClientContextHandler extends ChannelOutboundHandlerAdapter {
   private static final Logger logger = LoggerFactory.getLogger(ClearClientContextHandler.class);

   public ClearClientContextHandler() {
   }

   @Override
   public boolean isSharable() {
      return true;
   }

   @Override
   public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      if(msg != null) {
         if(msg instanceof FullHttpResponse && ((FullHttpResponse) msg).getStatus() == HttpResponseStatus.SWITCHING_PROTOCOLS) {
            // don't clear the context, should stay associated with this channel
         }
         else if(msg instanceof LastHttpContent || msg instanceof CloseWebSocketFrame) {
            logger.trace("Received HTTP response, clearing client context");
            Client.clear(ctx.channel());
            BridgeMdcUtil.clearHttpContext(ctx.channel());
         }
      }
      super.write(ctx, msg, promise);
   }

}

