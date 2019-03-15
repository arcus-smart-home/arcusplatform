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
package com.iris.platform.services.ipcd.registry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.ErrorEventException;
import com.iris.platform.services.ipcd.IpcdService;
import com.iris.util.LoggingUncaughtExceptionHandler;

import io.netty.util.HashedWheelTimer;

@Singleton
public class PlatformBusClient {

   private static final Logger logger = LoggerFactory.getLogger(PlatformBusClient.class);
   private static final String TIMEOUT_POOL_NAME = "ipcd-service-timeouts";

   private final Executor executor;
   private final PlatformMessageBus bus;
   private final HashedWheelTimer timeoutPool;
   private final ConcurrentMap<String, SettableFuture<PlatformMessage>> futures = new ConcurrentHashMap<>();

   @Inject
   public PlatformBusClient(@Named(IpcdService.PROP_THREADPOOL) Executor executor, PlatformMessageBus bus) {
      this.executor = executor;
      this.bus = bus;
      timeoutPool = new HashedWheelTimer(new ThreadFactoryBuilder()
         .setDaemon(true)
         .setNameFormat(TIMEOUT_POOL_NAME + "-%d")
         .setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler(logger))
         .build());
   }

   public void onEvent(PlatformMessage msg) {
      handleEvent(msg);
   }

   public void onErrorEvent(PlatformMessage msg) {
      handleEvent(msg);
   }

   private void handleEvent(PlatformMessage msg) {
      dispatchExpectedResponse(msg);
   }

   public void sendEvent(PlatformMessage msg) {
      bus.send(msg);
   }

   public ListenableFuture<PlatformMessage> request(PlatformMessage msg, int timeoutSecs) {
      Preconditions.checkNotNull(msg.getCorrelationId(), "correlationId is required");

      final SettableFuture<PlatformMessage> future = SettableFuture.create();
      future.addListener(() -> { futures.remove(msg.getCorrelationId()); }, executor);
      futures.put(msg.getCorrelationId(), future);
      bus.send(msg);
      timeoutPool.newTimeout((timer) -> {
         if(!future.isDone()) {
            future.setException(new TimeoutException("future timed out"));
         }
      }, timeoutSecs, TimeUnit.SECONDS);
      return future;
   }

   private void dispatchExpectedResponse(PlatformMessage message) {
      String correlationId = message.getCorrelationId();
      if(StringUtils.isBlank(correlationId)) {
         return;
      }

      SettableFuture<PlatformMessage> future = futures.get(message.getCorrelationId());
      if(future == null) {
         return;
      }

      if(message.isError()) {
         MessageBody body = message.getValue();
         future.setException(new ErrorEventException((String) body.getAttributes().get("code"), (String) body.getAttributes().get("message")));
      } else {
         future.set(message);
      }
   }
}

