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

import java.util.Collections;

import org.eclipse.jdt.annotation.Nullable;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.google.common.base.Preconditions;
import com.google.inject.Module;
import com.iris.agent.test.AbstractSystemTestCase;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class GatewayTestCase extends AbstractSystemTestCase {
   @BeforeClass
   public static void gatewayRouterTest() throws Exception {
      startIrisSystem(Collections.<Class<? extends Module>>emptyList());
   }

   @AfterClass
   public static void shutdownGatewayTest() throws Exception {
      shutdownIrisSystem();
   }

   @Before
   public void startGateway() throws Exception {
   }

   protected void shutdownGateway() throws Exception {
   }

   protected static final class TestServerInitializer extends ChannelInitializer<SocketChannel> {
      public TestServerInitializer(boolean ssl) {
      }

      @Override
      public void initChannel(@Nullable SocketChannel ch) throws Exception {
         Preconditions.checkNotNull(ch);
      }
   }

   protected static final class TestServer {
      private final boolean ssl;

      public TestServer(boolean ssl) {
         this.ssl = ssl;
      }

      public void start() {
         EventLoopGroup bossGroup = new NioEventLoopGroup(1);
         EventLoopGroup workerGroup = new NioEventLoopGroup();

         try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
               .channel(NioServerSocketChannel.class)
               .handler(new LoggingHandler(LogLevel.INFO))
               .childHandler(new TestServerInitializer(ssl));

            Channel ch = bootstrap.bind(19423).sync().channel();
            ch.closeFuture().sync();
         } catch (InterruptedException ex) {
         } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
         }
      }
   }
}

