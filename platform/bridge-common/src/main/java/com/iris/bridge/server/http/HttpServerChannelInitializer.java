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
package com.iris.bridge.server.http;

import com.google.common.base.Preconditions;
import com.iris.bridge.server.CookieConfig;
import com.iris.bridge.server.client.BindClientContextHandler;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpServerChannelInitializer extends ChannelInitializer<Channel> {
   private int maxRequestSizeBytes = -1;
   private boolean chunkedWrites = true;
   private ChannelInboundHandler handler;
   private CookieConfig cookieConfig;
   private ClientFactory clientFactory;
   private RequestAuthorizer requestAuthorizer = new AlwaysAllow();
   
   /**
    * @return the maxRequestSizeBytes
    */
   public int getMaxRequestSizeBytes() {
      return maxRequestSizeBytes;
   }

   /**
    * @param maxRequestSizeBytes the maxRequestSizeBytes to set
    */
   public void setMaxRequestSizeBytes(int maxRequestSizeBytes) {
      this.maxRequestSizeBytes = maxRequestSizeBytes;
   }

   /**
    * @return the chunkedWrites
    */
   public boolean isChunkedWrites() {
      return chunkedWrites;
   }

   /**
    * @param chunkedWrites the chunkedWrites to set
    */
   public void setChunkedWrites(boolean chunkedWrites) {
      this.chunkedWrites = chunkedWrites;
   }

   /**
    * @return the handler
    */
   public ChannelInboundHandler getHandler() {
      return handler;
   }

   /**
    * @param handler the handler to set
    */
   public void setHandler(ChannelInboundHandler handler) {
      this.handler = handler;
   }

   /**
    * @return the factory
    */
   public ClientFactory getClientFactory() {
      return clientFactory;
   }

   /**
    * @param factory the factory to set
    */
   public void setClientFactory(ClientFactory factory) {
      this.clientFactory = factory;
   }

   /**
    * @return the authorizer
    */
   public RequestAuthorizer getRequestAuthorizer() {
      return requestAuthorizer;
   }

   /**
    * @param authorizer the authorizer to set
    */
   public void setRequestAuthorizer(RequestAuthorizer authorizer) {
      this.requestAuthorizer = authorizer;
   }

   public CookieConfig getCookieConfig() {
      return cookieConfig;
   }

   public void setCookieConfig(CookieConfig cookieConfig) {
      this.cookieConfig = cookieConfig;
   }

   @Override
   protected void initChannel(Channel ch) throws Exception {
      Preconditions.checkNotNull(handler, "Must specify a channel handler");
      
      ChannelPipeline pipeline = ch.pipeline();
      pipeline.addLast(new HttpServerCodec());
      if(maxRequestSizeBytes > 0) {
         pipeline.addLast(new HttpObjectAggregator(maxRequestSizeBytes));
      }
      if(chunkedWrites) {
         pipeline.addLast(new ChunkedWriteHandler());
      }
      if(clientFactory != null) {
         pipeline.addLast(new BindClientContextHandler(cookieConfig, clientFactory, requestAuthorizer));
      }
      pipeline.addLast(handler);
   }

}

