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
package com.iris.alexa.bus;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.alexa.AlexaUtil;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.errors.ErrorEventException;
import com.iris.util.LoggingUncaughtExceptionHandler;
import com.iris.util.ThreadPoolBuilder;

import io.netty.util.HashedWheelTimer;

@Singleton
public class AlexaPlatformService {

   private static final Logger logger = LoggerFactory.getLogger(AlexaPlatformService.class);
   private static final String DISPATCHER_POOL_NAME = "alexa-bridge-dispatcher";
   private static final String TIMEOUT_POOL_NAME = "alexa-bridge-timeouts";

   private final PlatformMessageBus bus;
   private final ThreadPoolExecutor workerPool;
   private final HashedWheelTimer timeoutPool;
   private final int defaultTimeoutSecs;
   private final ConcurrentMap<Address, Pair<Predicate<PlatformMessage>, SettableFuture<PlatformMessage>>> futures = new ConcurrentHashMap<>();

   @Inject
   public AlexaPlatformService(AlexaPlatformServiceConfig config, PlatformMessageBus bus) {
      this.bus = bus;
      workerPool = new ThreadPoolBuilder()
         .withMaxPoolSize(config.getMaxListenerThreads())
         .withKeepAliveMs(config.getListenerThreadKeepAliveMs())
         .withNameFormat(DISPATCHER_POOL_NAME + "-%d")
         .withBlockingBacklog()
         .withMetrics("alexa.bridge")
         .build();
      timeoutPool = new HashedWheelTimer(new ThreadFactoryBuilder()
         .setDaemon(true)
         .setNameFormat(TIMEOUT_POOL_NAME + "-%d")
         .setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler(logger))
         .build());
      defaultTimeoutSecs = config.getDefaultTimeoutSecs();
   }

   @PostConstruct
   public void init() {
      bus.addMessageListener(AddressMatchers.anyOf(Address.broadcastAddress(), AlexaUtil.ADDRESS_BRIDGE), (event) -> queue(event));
   }

   @PreDestroy
   public void stop() {
      shutdown(workerPool, DISPATCHER_POOL_NAME);
   }

   public int getDefaultTimeoutSecs() {
      return defaultTimeoutSecs;
   }

   private void shutdown(ExecutorService executor, String poolName) {
      try {
         executor.shutdownNow();
         executor.awaitTermination(30, TimeUnit.SECONDS);
      } catch(Exception e) {
         logger.warn("Failed clean shutdown {}", poolName, e);
      }
   }

   public ListenableFuture<PlatformMessage> request(PlatformMessage msg, Predicate<PlatformMessage> matcher) {
      return request(msg, matcher, defaultTimeoutSecs);
   }

   public ListenableFuture<PlatformMessage> request(PlatformMessage msg, Predicate<PlatformMessage> matcher, int timeoutSecs) {
      if(timeoutSecs < 0) {
         timeoutSecs = defaultTimeoutSecs;
      }

      final Address addr = msg.getDestination();
      final SettableFuture<PlatformMessage> future = SettableFuture.create();
      future.addListener(() -> { futures.remove(addr); }, workerPool);

      Predicate<PlatformMessage> pred = (pm) -> { return Objects.equals(msg.getCorrelationId(), pm.getCorrelationId()) && msg.isError(); };
      pred = matcher.or(pred);

      Pair<Predicate<PlatformMessage>, SettableFuture<PlatformMessage>> pair = new ImmutablePair<>(matcher, future);
      futures.put(addr, pair);
      bus.send(msg);
      timeoutPool.newTimeout((timer) -> {
         if(!future.isDone()) {
            future.setException(new TimeoutException("future timed out"));
         }
      }, timeoutSecs, TimeUnit.SECONDS);
      return future;
   }

   private void queue(PlatformMessage msg) {
      workerPool.execute(() -> dispatch(msg));
   }

   private void dispatch(PlatformMessage message) {
      try {
         logger.trace("Dispatching {}...", message);
         dispatchExpectedResponse(message);
      } catch(Exception e) {
         logger.warn("Error dispatching message {}", message, e);
      }
   }

   private void dispatchExpectedResponse(PlatformMessage message) {
      Address addr = message.getSource();
      Pair<Predicate<PlatformMessage>, SettableFuture<PlatformMessage>> pair = futures.get(addr);
      if(pair == null) {
         return;
      }

      if(pair.getLeft().test(message)) {
         if(message.isError()) {
            MessageBody body = message.getValue();
            pair.getRight().setException(new ErrorEventException((String) body.getAttributes().get("code"), (String) body.getAttributes().get("message")));
         } else {
            pair.getRight().set(message);
         }
      }
   }
}

