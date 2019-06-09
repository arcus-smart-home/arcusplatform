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
package com.iris.hubcom.server.message;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.server.session.Session;
import com.iris.messages.PlatformMessage;
import com.iris.util.ThreadPoolBuilder;

@Singleton
public class DirectMessageExecutor {

   private static final Logger logger = LoggerFactory.getLogger(DirectMessageExecutor.class);

   @Inject(optional = true)
   @Named("direct.message.pool.size")
   private int poolSize = 20;

   @Inject(optional = true)
   @Named("direct.message.pool.queue.size")
   private int poolQueueSize = 250;

   private final RefuseConnectionException refuseConnection;

   private ExecutorService pool;
   private final Map<String, DirectMessageHandler> handlers = new HashMap<>();

   @Inject
   public DirectMessageExecutor(Set<DirectMessageHandler> handlers) {
      if (handlers != null) {
         handlers.forEach((h) -> this.handlers.put(h.supportsMessageType(), h));
      }

      this.refuseConnection = new RefuseConnectionException();
   }

   public boolean handle(final Session session, final PlatformMessage msg) {
      if (handlers.containsKey(msg.getMessageType())) {
         try {
            pool.execute(() -> {
               DirectMessageHandler handler = handlers.get(msg.getMessageType());
               handler.handle(session, msg);
            });
            return true;
         } catch (RejectedExecutionException ex) {
            throw refuseConnection;
         }
      }
      return false;
   }

   @PostConstruct
   public void init() {
      pool = new ThreadPoolBuilder()
            .withNameFormat("hub-directmsg-%d")
            .withMetrics("hub.direct.message")
            .withMaxPoolSize(poolSize)
            .withCorePoolSize(poolSize)
            .withPrestartCoreThreads(true)
            .withDaemon(true)
            .withMaxBacklog(poolQueueSize)
            .build();
   }

   @PreDestroy
   public void shutdown() {
      try {
         pool.shutdownNow();
         pool.awaitTermination(30, TimeUnit.SECONDS);
      } catch (Exception e) {
         logger.warn("Failed clean shutdown", e);
      }
   }

   private static class RefuseConnectionException extends RuntimeException {
      private RefuseConnectionException() {
         super("Refusing socket connection", null, true, false);
      }
   }
}

