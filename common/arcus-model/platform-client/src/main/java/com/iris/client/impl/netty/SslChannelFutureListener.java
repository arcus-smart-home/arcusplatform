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
package com.iris.client.impl.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * 
 */
public abstract class SslChannelFutureListener implements ChannelFutureListener {

   @Override
   public void operationComplete(ChannelFuture future) throws Exception {
      if(!future.isSuccess()) {
         onConnectError(future.cause());
         return;
      }
      
      final Channel channel = future.channel();
      SslHandler handler = channel.pipeline().get(SslHandler.class);
      if(handler == null) {
         onConnected(channel);
      }
      else {
         handler.handshakeFuture().addListener(new GenericFutureListener<Future<Channel>>() {
            @Override
            public void operationComplete(Future<Channel> future) throws Exception {
               if(!future.isSuccess()) {
                  onConnectError(future.cause());
               }
               else {
                  onConnected(channel);
               }
            }
         });
      }
      
   }

   protected abstract void onConnected(Channel channel);
   
   protected abstract void onConnectError(Throwable cause);
}

