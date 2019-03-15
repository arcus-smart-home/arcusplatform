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
package com.iris.bridge.server.cluster;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.server.ServerRunner;
import com.iris.bridge.server.config.BridgeServerConfig;
import com.iris.bridge.server.netty.BridgeServerEventLoopProvider;
import com.iris.platform.cluster.ClusterServiceListener;
import com.iris.platform.cluster.ClusterServiceRecord;

@Singleton
public class ClusterAwareServerRunner 
   extends ServerRunner 
   implements ClusterServiceListener
{
   private static final Logger logger = LoggerFactory.getLogger(ClusterAwareServerModule.class);
   
   private final CountDownLatch clusterIdLatch = new CountDownLatch(1);

   @Inject
   public ClusterAwareServerRunner(
         BridgeServerConfig serverConfig,
         InetSocketAddress socketBindAddress,
         ChannelInitializer<SocketChannel> channelInitializer,
         @Named("bossGroup") EventLoopGroup bossGroup,
         @Named("workerGroup") EventLoopGroup workerGroup,
         @Named("tcpChannelOptions") Map<ChannelOption<?>, Object> childChannelOptions,
         @Named("tcpParentChannelOptions") Map<ChannelOption<?>, Object> parentChannelOptions,
         @Named("bridgeEventLoopProvider") BridgeServerEventLoopProvider eventLoopProvider
   ) {
      super(
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
   
   @Override
   protected void beforeBind() throws InterruptedException {
      if(!clusterIdLatch.await(0, TimeUnit.MILLISECONDS)) {
         logger.info("Waiting for cluster id before binding to {}:{}", getSocketBindAddress(), getPortNumber());
         clusterIdLatch.await();
      }
   }

   @Override
   public void onClusterServiceRegistered(ClusterServiceRecord record) {
      logger.debug("Received cluster id {}", record.getMemberId());
      clusterIdLatch.countDown();
   }

   @Override
   public void onClusterServiceDeregistered() {

   }

}

