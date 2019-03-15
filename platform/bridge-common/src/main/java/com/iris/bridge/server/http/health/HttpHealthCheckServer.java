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
package com.iris.bridge.server.http.health;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;

import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpRequestHandler;
import com.iris.bridge.server.http.HttpServerChannelInitializer;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.bridge.server.noauth.NoAuthClientRegistry;
import com.iris.core.StartupListener;
import com.iris.util.ThreadPoolBuilder;

public class HttpHealthCheckServer implements StartupListener {
   private static final Logger logger = LoggerFactory.getLogger(HttpHealthCheckServer.class);
   
   private volatile Channel channel;
   
   private final HttpServerChannelInitializer initializer;
   private final int port;
   
   @Inject 
   public HttpHealthCheckServer(
         HealthCheckServerConfig config,
         BridgeMetrics metrics,
         @Named(HealthCheckServerConfig.NAME_HEALTHCHECK_RESOURCES)
         Set<HttpResource> resources
   ) {
      this.initializer = new HttpServerChannelInitializer();
      this.initializer.setMaxRequestSizeBytes(config.getMaxRequestSizeBytes());
      this.initializer.setHandler(new HttpRequestHandler(resources, metrics));
      this.initializer.setClientFactory(new NoAuthClientRegistry());
      this.port = config.getPort();
   }
      

   @Override
   public void onStarted() {
      while(true) {
         try {
            start();
            return;
         }
         catch (InterruptedException e) {
            logger.info("Interrupted, shutting down health check...");
            return;
         }
         catch (Exception e) {
            logger.warn("Error binding to health check port, will try again in 100ms", e);
            try {
               Thread.sleep(100);
            }
            catch (InterruptedException e1) {
               logger.info("Interrupted, shutting down health check...");
               return;
            }
         }
      }
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
   }

   /**
    * Starts server but doesn't block, the result may be
    * blocked on.
    *
    * @throws InterruptedException
    */
   public Future<Void> start() throws InterruptedException {
      ServerBootstrap serverBootstrap = new ServerBootstrap();
      serverBootstrap.group(
            new NioEventLoopGroup(
                  1, 
                  ThreadPoolBuilder
                     .defaultFactoryBuilder()
                     .setNameFormat(String.format("healthcheck-http-port-%s", port))
                     .build()
            )
      );
      serverBootstrap.channel(NioServerSocketChannel.class);
      serverBootstrap.childHandler(initializer);

      Channel channel = serverBootstrap.bind(port).sync().channel();
      
      logger.info("Status server listening at http://0.0.0.0:{}", port);
      channel.closeFuture().addListener((o) -> {
         serverBootstrap.group().shutdownGracefully();
         serverBootstrap.childGroup().shutdownGracefully();
      });
      this.channel = channel;
      return channel.closeFuture();
   }

   /**
    * Signals the server to shut down and blocks until shutdown is complete.
    *
    * @throws Exception
    */
   @PreDestroy
   public void shutdown() throws Exception {
      Channel channel = this.channel;
      if (channel != null) {
         ChannelFuture closeFuture = channel.close();
         closeFuture.sync();
         this.channel = null;
      }
   }


}

