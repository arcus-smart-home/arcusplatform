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
package com.iris.ipcd.bridge.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpTestServerInitializer extends ChannelInitializer<SocketChannel> {
   private final SslContext sslContext;

   public HttpTestServerInitializer(SslContext sslContext) {
       this.sslContext = sslContext;
   }

   @Override
   public void initChannel(SocketChannel ch) {
       ChannelPipeline pipeline = ch.pipeline();
       if (sslContext != null) {
           pipeline.addLast(sslContext.newHandler(ch.alloc()));
       }
       pipeline.addLast(new HttpServerCodec());
       pipeline.addLast(new HttpObjectAggregator(65536));
       pipeline.addLast(new ChunkedWriteHandler());
       pipeline.addLast(new HttpTestServerHandler());
   }
}

