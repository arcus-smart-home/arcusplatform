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
package com.iris.bridge.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.netty.BridgeServerEventLoopProvider;
import com.iris.bridge.server.netty.bootstrap.ServerBootstrap;
import com.iris.network.RateLimiter;
import com.iris.network.RateLimiters;

public class ServerRunner implements Runnable {
   private static final Logger logger = LoggerFactory.getLogger(ServerRunner.class);
   
   private final BridgeServerConfig serverConfig;
   private final InetSocketAddress socketBindAddress;
   private final ChannelInitializer<SocketChannel> channelInitializer;
   private final EventLoopGroup bossGroup;
   private final EventLoopGroup workerGroup;
   private Channel channel = null;

   private final Map<ChannelOption<?>, Object> childChannelOptions;
   private final Map<ChannelOption<?>, Object> parentChannelOptions;
   private final BridgeServerEventLoopProvider eventLoopProvider;
   private ServerBootstrap serverBootstrap;
   
   @Inject
   public ServerRunner(
         BridgeServerConfig serverConfig,
         InetSocketAddress socketBindAddress,
         ChannelInitializer<SocketChannel> channelInitializer,
         @Named("bossGroup") EventLoopGroup bossGroup,
         @Named("workerGroup") EventLoopGroup workerGroup,
         @Named("tcpChannelOptions") Map<ChannelOption<?>, Object> childChannelOptions,
         @Named("tcpParentChannelOptions") Map<ChannelOption<?>, Object> parentChannelOptions,
         @Named("bridgeEventLoopProvider") BridgeServerEventLoopProvider eventLoopProvider
   ) {
      this.serverConfig = serverConfig;
      this.socketBindAddress = socketBindAddress;
      this.channelInitializer = channelInitializer;
      this.bossGroup = bossGroup;
      this.workerGroup = workerGroup;
      this.childChannelOptions = childChannelOptions;
      this.parentChannelOptions = parentChannelOptions;
      this.eventLoopProvider = eventLoopProvider;
   }
   
   public boolean isTlsServer() {
      return serverConfig.isTlsServer();
   }

   public boolean isRunning() {
      return channel != null && !channel.closeFuture().isDone();
   }

   /**
    * Starts server and blocks while server is running.
    *
    * @throws InterruptedException
    */
   public void run() {
      try {
         start().get();
      }
      catch (InterruptedException e) {
         Thread.interrupted();
         throw new UncheckedExecutionException(e);
      }
      catch (ExecutionException e) {
         throw new UncheckedExecutionException(e.getCause());
      }
      finally {
         bossGroup.shutdownGracefully();
         workerGroup.shutdownGracefully();
      }
   }

   /**
    * Starts server but doesn't block, the result may be
    * blocked on.
    *
    * @throws InterruptedException
    */
   @SuppressWarnings({ "unchecked", "rawtypes" })
   public Future<Void> start() throws InterruptedException {
      if (serverConfig.getBossAcceptRate() > 0  && serverConfig.getBossAcceptCapacity() > 0) {
         logger.info("rate limiting incoming connections: capacity={}, rate={}", serverConfig.getBossAcceptCapacity(), serverConfig.getBossAcceptRate());
         RateLimiter rateLimit = RateLimiters.tokenBucket(
            serverConfig.getBossAcceptCapacity(), 
            serverConfig.getBossAcceptRate()
         ).build();

         serverBootstrap = new ServerBootstrap(rateLimit);
      } else {
         serverBootstrap = new ServerBootstrap();
      }

      serverBootstrap.group(bossGroup, workerGroup);
      serverBootstrap.channel(eventLoopProvider.getServerSocketChannelClass());

      for (Map.Entry<ChannelOption<?>, Object> e : parentChannelOptions.entrySet()) {
         serverBootstrap.option((ChannelOption) e.getKey(), e.getValue());
      }

      for (Map.Entry<ChannelOption<?>, Object> e : childChannelOptions.entrySet()) {
         serverBootstrap.childOption((ChannelOption) e.getKey(), e.getValue());
      }

      serverBootstrap.childHandler(channelInitializer);

      beforeBind();
      channel = serverBootstrap.bind(serverConfig.getTcpPort()).sync().channel();
      logger.info("Listening at {}{}:{}", isTlsServer() ? "https://" : "http://", serverConfig.getBindAddress(), serverConfig.getTcpPort());
      return channel.closeFuture();
   }

   /**
    * Signals the server to shut down and blocks until shutdown is complete.
    *
    * @throws Exception
    */
   @PreDestroy
   public void shutdown() throws Exception {
      if (channel != null) {
         ChannelFuture closeFuture = channel.close();
         closeFuture.sync();
         channel = null;
         bossGroup.shutdownGracefully();
         workerGroup.shutdownGracefully();
      }
   }

   public int getPortNumber() {
      if(channel == null) {
         return serverConfig.getTcpPort();
      }
      else {
         return ((InetSocketAddress) channel.localAddress()).getPort();
      }
   }

   public InetSocketAddress getSocketBindAddress() {
      return socketBindAddress;
   }

   protected void beforeBind() throws InterruptedException {
      
   }
   
}

