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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;

import java.net.InetSocketAddress;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.netty.BridgeServerEventLoopProvider;
import com.iris.core.IrisAbstractApplication;

public class BridgeServer extends IrisAbstractApplication {
   private static final Logger logger = LoggerFactory.getLogger(BridgeServer.class);

   private final ServerRunner runner;

   public BridgeServer(
         BridgeServerConfig serverConfig,
         InetSocketAddress socketBindAddress,
         ChannelInitializer<SocketChannel> channelInitializer,
         EventLoopGroup bossGroup,
         EventLoopGroup workerGroup,
         Map<ChannelOption<?>, Object> childChannelOptions,
         Map<ChannelOption<?>, Object> parentChannelOptions,
         BridgeServerEventLoopProvider eventLoopProvider
   ) {
      this.runner = new ServerRunner(
            serverConfig,
            socketBindAddress,
            channelInitializer,
            bossGroup,
            workerGroup,
            childChannelOptions,
            parentChannelOptions,
            eventLoopProvider
      );
   }

   @Inject
   public BridgeServer(ServerRunner runner) {
      this.runner = runner;
   }

   /* (non-Javadoc)
    * @see com.iris.core.IrisAbstractApplication#start()
    */
   @Override
   protected void start() throws Exception {
      logger.info("Starting {} at {}:{}", getApplicationName(), getSocketBindAddress().getAddress(), getPortNumber());
      startServer();
   }

   /**
    * Starts server and blocks while server is running.
    */
   public void startServer() {
      logger.info("Starting {}...", getClass().getSimpleName());
      runner.run();
   }

   /**
    * Starts server but doesn't block.
    *
    * @throws InterruptedException
    */
   public void executeServer() throws InterruptedException {
      logger.info("Starting {}...", getClass().getSimpleName());
      runner.start();
   }

   /**
    * Signals the server to shut down and blocks until shutdown is complete.
    *
    * @throws Exception
    */
   public void shutdownServer() throws Exception {
      runner.shutdown();
   }

   public int getPortNumber() {
      return runner.getPortNumber();
   }

   public InetSocketAddress getSocketBindAddress() {
      return runner.getSocketBindAddress();
   }

   public static void main(String [] args) throws Exception {
      exec(BridgeServer.class, ImmutableSet.of(), args);
   }
}

