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
package com.iris.core.platform;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.errors.ErrorEventException;
import com.iris.util.LoggingUncaughtExceptionHandler;

import io.netty.util.HashedWheelTimer;

public class PlatformBusClient {

   private static final Logger logger = LoggerFactory.getLogger(PlatformBusClient.class);

   private static final long DEFAULT_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(1);

   private final PlatformMessageBus bus;
   private final Executor executor;
   private final long defaultTimeoutMs;

   private final HashedWheelTimer timeoutTimer = new HashedWheelTimer(new ThreadFactoryBuilder()
         .setDaemon(true)
         .setNameFormat("platform-bus-client-timeout-%d")
         .setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler(logger))
         .build());

   private final ConcurrentHashMap<String, SettableFuture<PlatformMessage>> requests = new ConcurrentHashMap<>();

   public PlatformBusClient(
         @NonNull PlatformMessageBus bus,
         @NonNull Executor executor,
         @Nullable Set<AddressMatcher> matchers
   ) {
      this(bus, executor, DEFAULT_TIMEOUT_MS, matchers);
   }

   public PlatformBusClient(
         @NonNull PlatformMessageBus bus,
         @NonNull Executor executor,
         long defaultTimeoutMs,
         @Nullable Set<AddressMatcher> matchers
   ) {
      Preconditions.checkNotNull(bus);
      Preconditions.checkNotNull(executor);
      this.bus = bus;
      this.executor = executor;
      this.defaultTimeoutMs = defaultTimeoutMs;
      if(matchers != null) {
         bus.addMessageListener(matchers, this::queue);
      }
   }

   @NonNull
   public ListenableFuture<PlatformMessage> request(@NonNull final PlatformMessage msg) {
      Preconditions.checkNotNull(msg);
      Preconditions.checkNotNull(msg.getCorrelationId(), "requests must have a correlation id");

      final SettableFuture<PlatformMessage> future = SettableFuture.create();

      SettableFuture<PlatformMessage> existing = requests.putIfAbsent(msg.getCorrelationId(), future);
      if(existing != null) {
         throw new RuntimeException("request with id " + msg.getCorrelationId() + " already exists");
      }

      future.addListener(() -> requests.remove(msg.getCorrelationId()), executor);
      bus.send(msg);
      timeoutTimer.newTimeout((timer) -> {
         if(!future.isDone()) {
            future.setException(new TimeoutException());
         }
      }, ttl(msg), TimeUnit.MILLISECONDS);

      return future;
   }

   private long ttl(PlatformMessage msg) {
      if(msg.getTimeToLive() > 0) {
         return msg.getTimeToLive();
      }
      return defaultTimeoutMs;
   }

   private void queue(PlatformMessage msg) {
      executor.execute(() -> dispatch(msg));
   }

   private void dispatch(PlatformMessage message) {
      String correlationId = message.getCorrelationId();
      if(StringUtils.isBlank(correlationId)) {
         return;
      }

      SettableFuture<PlatformMessage> future = requests.remove(correlationId);
      if(future == null || future.isDone()) {
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

