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
/**
 * 
 */
package com.iris.platform.history.service;

import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.platform.history.HistoryAppenderConfig;
import com.iris.util.MdcContext.MdcContextReference;
import com.iris.util.ThreadPoolBuilder;

/**
 * 
 */
@Singleton
public class HistoryEventListener {
   private static Logger logger = LoggerFactory.getLogger(HistoryEventListener.class);
   
   private final PlatformMessageBus platformBus;
   private final HistoryAppenderService dispatcher;
   private final Executor workerPool;

   /**
    * 
    */
   @Inject
   public HistoryEventListener(
         PlatformMessageBus platformBus,
         HistoryAppenderService dispatcher,
         HistoryAppenderConfig config
   ) {
      this.platformBus = platformBus;
      this.dispatcher = dispatcher;
      logger.info("Started history service with threads: {} thread-keepalive: {} ms", config.getMaxThreads(), config.getThreadKeepAliveMs());
      this.workerPool = 
            new ThreadPoolBuilder()
               .withMaxPoolSize(config.getMaxThreads())
               .withKeepAliveMs(config.getThreadKeepAliveMs())
               .withNameFormat("history-appender-dispatcher-%d")
               .withBlockingBacklog()
               .withMetrics("history.appender")
               .build()
               ;
   }
   
   @PostConstruct
   public void init() {
      platformBus.addMessageListener(
            AddressMatchers.anyOf(Address.broadcastAddress()),
            (event) -> queue(event)
      );
   }
   
   protected void queue(PlatformMessage message) {
      workerPool.execute(() -> dispatch(message));
   }

   protected void dispatch(PlatformMessage message) {
      try(MdcContextReference ctx = PlatformMessage.captureAndInitializeContext(message)) {
         logger.trace("Dispatching {}...", message);
         dispatcher.dispatch(message);
      }
      catch(Exception e) {
         logger.warn("Error dispatching message {}", message, e);
      }
   }
}

