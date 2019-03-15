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

import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.iris.bridge.server.ssl.KeyStoreLoader;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class HttpTestServer {
   static final boolean SSL = false;
   static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "1443" : "1080"));

   public static void main(String[] args) throws Exception {
       // Configure SSL.
       final SslContext sslCtx;
       if (SSL) {
          SelfSignedCertificate ssc = new SelfSignedCertificate();
          sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).sslProvider(SslProvider.JDK).build();
       } else {
           sslCtx = null;
       }

       EventLoopGroup bossGroup = new NioEventLoopGroup(1);
       EventLoopGroup workerGroup = new NioEventLoopGroup();
       try {
           ServerBootstrap b = new ServerBootstrap();
           b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new HttpTestServerInitializer(sslCtx));

           Channel ch = b.bind(PORT).sync().channel();

           System.err.println("Open your web browser and navigate to " +
                   (SSL? "https" : "http") + "://127.0.0.1:" + PORT + '/');

           ch.closeFuture().sync();
       } finally {
           bossGroup.shutdownGracefully();
           workerGroup.shutdownGracefully();
       }
   }
}

