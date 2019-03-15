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
package com.iris.platform.health;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.core.StartupListener;
import com.iris.util.ThreadPoolBuilder;

public class TcpHealthCheck implements StartupListener {
   private static final Logger logger = LoggerFactory.getLogger(TcpHealthCheck.class);
   private static final byte[] RESPONSE = new byte [] { 'O', 'N', 'L', 'I', 'N', 'E', '\r', '\n' };
   
   private final Executor executor;
   private final int healthCheckPort;
   
   @Inject 
   public TcpHealthCheck(@Named("healthcheck.tcp.port") int healthCheckPort) {
      this.healthCheckPort = healthCheckPort;
      this.executor =
            Executors
               .newSingleThreadExecutor(
                     ThreadPoolBuilder
                        .defaultFactoryBuilder()
                        .setNameFormat(String.format("healthcheck-port-%s", healthCheckPort))
                        .build()
               );
   }

   @Override
   public void onStarted() {
      executor.execute(() -> listen());
   }
   
   public void listen() {
      while(true) {
         try {
            doListen();
            logger.info("Interupted, shutting down health check...");
            return;
         }
         catch(Exception e) {
            logger.warn("Error establishing health check port, will try again in 100ms", e);
            try {
               Thread.sleep(100);
            }
            catch (InterruptedException e1) {
               logger.info("Interupted, shutting down health check...");
               return;
            }
         }
      }
   }
   
   protected boolean doListen() throws IOException {
      ServerSocket server = new ServerSocket(healthCheckPort);
      server.setSoTimeout(1000);
      server.setReuseAddress(true);
      logger.info("Health check listening at tcp://0.0.0.0:{}", healthCheckPort);
      try {
         while(!Thread.interrupted()) {
            try {
               Socket client = server.accept();
               client.getOutputStream().write(RESPONSE);
               client.close();
            }
            catch(SocketTimeoutException e) {
               // ignore
            }
            catch(Exception e) {
               logger.debug("Error responding to health check", e);
            }
         }
         return false;
      }
      finally {
         IOUtils.closeQuietly(server);
      }
   }
   
}

