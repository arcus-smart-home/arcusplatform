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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.channel.ChannelHandler.Sharable;

import java.nio.channels.ClosedChannelException;

import javax.net.ssl.SSLSession;
import javax.security.auth.x500.X500Principal;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Binds the {@link HubClient} to the channel context when initialized.
 */
@Singleton
@Sharable
public class SslBindClientHandler extends ChannelInboundHandlerAdapter {
   private static final Logger logger = LoggerFactory.getLogger(SslBindClientHandler.class);

   private ClientFactory registry;

   @Inject
   public SslBindClientHandler(ClientFactory registry) {
      this.registry = registry;
   }

   @Override
   public boolean isSharable() {
      return true;
   }

   /* (non-Javadoc)
    * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.channel.ChannelHandlerContext)
    */
   @Override
   public void channelActive(ChannelHandlerContext ctx) throws Exception {
      super.channelActive(ctx);
      SslHandler handler = ctx.channel().pipeline().get(SslHandler.class);
      handler.handshakeFuture().addListener((result) -> this.onSslHandshakeComplete(result, handler));
   }

   private void onSslHandshakeComplete(Future<? super Channel> result, SslHandler handler) {
      try {
         if(!result.isSuccess()) {
            if (logger.isDebugEnabled()) {
               Throwable cause = result.cause();
               if (!(cause instanceof ClosedChannelException)) {
                  logger.debug("SSL handshake failed: {}", (cause == null) ? "unknown" : cause.getMessage(), cause);
               }
            }
            return;
         }

         String clientName = extractClientName(handler);
         if(clientName != null) {
            Channel channel = (Channel) result.get();
            Client.bind(channel, registry.load(clientName));
         }
      }
      catch(Exception e) {
         logger.debug("Unable to determine client auth", e);
      }
   }

   private @Nullable String extractClientName(SslHandler handler) {
      try {
         SSLSession sslSession = handler.engine().getSession();
         return ((X500Principal) sslSession.getPeerPrincipal()).getName();
      }
      catch(Exception e) {
         logger.debug("Unable to get client principal, mutual auth will fail", e);
         return null;
      }
   }

}

