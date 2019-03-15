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

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.channel.ChannelHandler.Sharable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.auth.basic.BasicAuthClient;
import com.iris.bridge.server.auth.basic.BasicAuthCredentials;

/**
 * Binds the {@link Client} to the channel context when initialized.
 */
@Singleton
@Sharable
public class BasicAuthClientContextHandler extends ChannelInboundHandlerAdapter {
   private static final Logger logger = LoggerFactory.getLogger(BasicAuthClientContextHandler.class);
   
   private static final byte [] FORBIDDEN    = "Forbidden".getBytes(Charsets.UTF_8);
   private static final byte [] UNAUTHORIZED = "Unauthorized".getBytes(Charsets.UTF_8);
   
   private final ClientFactory registry;

   @Inject
   public BasicAuthClientContextHandler(ClientFactory registry) {
      this.registry = registry;
   }
   
   @Override
   public boolean isSharable() {
      return true;
   }

   @Override
   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      
      if(msg instanceof FullHttpRequest) {
         logger.trace("Received HTTP request, determining context");
         try {
            FullHttpRequest req = (FullHttpRequest)msg;
            String authHeader = req.headers().get(HttpHeaders.Names.AUTHORIZATION);
            BasicAuthCredentials suppliedCreds = BasicAuthCredentials.fromAuthHeaderString(authHeader);
            BasicAuthClient client = (BasicAuthClient)registry.load(suppliedCreds.getUsername());

            if(client.getCredentials().getPassword().equals(suppliedCreds.getPassword())){
               logger.debug("request passed basic authentication for user {}",suppliedCreds.getUsername());
               Client.bind(ctx.channel(), client);
               super.channelRead(ctx, msg);
            }else{
               logger.debug("request failed basic authentication for user {} sending unathorized",suppliedCreds.getUsername());
               ctx.channel().writeAndFlush(createUnauthorizedResponse(true));
            }
         }
         catch(Exception ex) {
            logger.warn("Exception while authenticating with basic auth [{}]", ex.getMessage());
            ctx.channel().writeAndFlush(createUnauthorizedResponse(true));
         }
      }

      
   }
   private DefaultFullHttpResponse createUnauthorizedResponse(boolean includeChallange){
      DefaultFullHttpResponse resp;
      if(includeChallange){
         resp = new DefaultFullHttpResponse(
               HTTP_1_1, 
               HttpResponseStatus.UNAUTHORIZED,
               Unpooled.wrappedBuffer(UNAUTHORIZED)
         );
         resp.headers().add(HttpHeaders.Names.WWW_AUTHENTICATE, "basic realm=\""+"Iris Realm"+"\"" );
      }
      else {
         resp = new DefaultFullHttpResponse(
               HTTP_1_1, 
               HttpResponseStatus.FORBIDDEN,
               Unpooled.wrappedBuffer(FORBIDDEN)
         );
      }
      resp.headers().add(HttpHeaders.Names.CONTENT_TYPE, "text/plain;charset=UTF-8");
      HttpHeaders.setContentLength(resp, resp.content().readableBytes());
      return resp;
   }
}

