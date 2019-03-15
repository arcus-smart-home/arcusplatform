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
package com.iris.platform.rule.environment;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.iris.common.rule.event.MessageReceivedEvent;
import com.iris.common.rule.event.RuleEvent;
import com.iris.core.messaging.SingleThreadDispatcher;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.Message;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.Errors;
import com.iris.platform.rule.RuleEnvironment;
import com.iris.util.MdcContext;
import com.iris.util.MdcContext.MdcContextReference;

/**
 * Maintains a single thread of execution for a set of PlaceEventHandlers.
 */
public class DefaultPlaceExecutor implements PlaceEnvironmentExecutor {
   private static final Logger logger = LoggerFactory.getLogger(DefaultPlaceExecutor.class);

   private static final Object EVENT_START = new Object() { @Override
   public String toString() { return "StartEvent"; } };
   private static final Object EVENT_STOP = new Object()  { @Override
   public String toString() { return "StopEvent"; } };
   
   private final PlatformMessageBus platformBus;
   private final SingleThreadDispatcher<Object> dispatcher;
   private final RuleModelStore models;
   private final ExecutorService executor;

   private volatile Map<Address, PlaceEventHandler> handlers;
   private volatile boolean running = false;

   public DefaultPlaceExecutor(
         PlatformMessageBus platformBus,
         RuleEnvironment environment,
         RuleModelStore models,
         ExecutorService executor,
         int maxQueueDepth,
         String ruleCascadeMode
   ) {
      this.platformBus = platformBus;
      this.dispatcher = new SingleThreadDispatcher<>((event) -> onEvent(event), maxQueueDepth);
      this.executor = executor;
      this.models = models;
      this.models.addListener((RuleEvent event) -> dispatch(event));
   }
   
   // implementation detail: this needs to allow late-binding because there
   //  is a circular dependency between the RuleExecutor and the RuleContext in
   //  order to enable scheduling
   public void setHandlers(List<PlaceEventHandler> handlers) {
      if(handlers.isEmpty()) {
         this.handlers = new LinkedHashMap<Address, PlaceEventHandler>();
      }
      else {
         Map<Address, PlaceEventHandler> temp = new LinkedHashMap<>(2 * handlers.size());
         for(PlaceEventHandler handler: handlers) {
            temp.put(handler.getAddress(), handler);
         }
         this.handlers = temp;
      }
   }
   
   public RuleModelStore getModelStore() {
      return models;
   }

   public boolean isRunning() {
      return running;
   }

   @Override
   public void start() {
      executor.submit(new DispatchTask(dispatcher, EVENT_START));
   }
   
   @Override
   public void stop() {
      executor.submit(new DispatchTask(dispatcher, EVENT_STOP));
   }

   @Override
   public ListenableFuture<Void> submit(Runnable task) {
      return submit((Callable<Void>) () -> { task.run(); return null; });
   }

   @Override
   public <V> ListenableFuture<V> submit(Callable<V> task) {
      SettableFuture<V> result = SettableFuture.create();
      Runnable delegate = () -> {
         try {
            V value = task.call();
            result.set(value);
         }
         catch(Exception e) {
            result.setException(e);
         }
      };
      executor.submit(new DispatchTask(dispatcher, delegate));
      return result;
   }

   @Override
   public void handleRequest(PlatformMessage message) {
      RuleEvent event = MessageReceivedEvent.create(message);
      fire(event);
   }

   @Override
   public void onMessageReceived(PlatformMessage message) {
      RuleEvent event = MessageReceivedEvent.create(message);
      fire(event);
   }

   @Override
   public void fire(RuleEvent event) {
      executor.submit(new DispatchTask(dispatcher, event));
   }
   
   @Override
   public PlaceEnvironmentStatistics getStatistics() {
      int rules = 0;
      int scenes = 0;
      int activeRules = 0;
      int activeScenes = 0;
      for(PlaceEventHandler handler: handlers.values()) {
         if(handler instanceof RuleHandler){
            rules++;
            if(handler.isAvailable()) {
               activeRules++;
            }
         }
         else if(handler instanceof SceneHandler) {
            scenes++;
            if(handler.isAvailable()) {
               activeScenes++;
            }
         }
      }
      PlaceEnvironmentStatistics stats = new PlaceEnvironmentStatistics();
      stats.setRules(rules);
      stats.setActiveRules(activeRules);
      stats.setScenes(scenes);
      stats.setActiveScenes(activeScenes);
      return stats;
   }

   protected void startRules() {
      running = true;
      for(PlaceEventHandler handler: handlers.values()) {
         try {
            handler.start();
         }
         catch(Exception e) {
            handler.getContext().logger().warn("Unable to activate rule", e);
         }
      }
   }

   protected void stopRules() {
      for(PlaceEventHandler handler: handlers.values()) {
         try {
            handler.stop();
         }
         catch(Exception e) {
            handler.getContext().logger().warn("Unable to deactivate rule", e);
         }
      }
      running = false;
   }
   
   protected void execute(Object event) throws InterruptedException, ExecutionException {
      executor.submit(new DispatchTask(dispatcher, event)).get();
   }

   protected void onEvent(Object event) {
      if(event instanceof RuleEvent) {
         dispatch((RuleEvent) event);
      }
      else if(event == EVENT_START) {
         startRules();
      }
      else if(event == EVENT_STOP) {
         stopRules();
      }
      else if(event instanceof Runnable) {
         ((Runnable) event).run();
      }
      else {
         logger.warn("Unrecognized event [{}]", event);
      }
   }

   protected void dispatch(RuleEvent event) {
      // TODO filter out messages to the Rule and handle enable/disable/etc here

      try(MdcContextReference mdcContext = MdcContext.captureMdcContext()) {
         if(event instanceof MessageReceivedEvent) {
            PlatformMessage message = ((MessageReceivedEvent) event).getMessage();
            // this will be cleared by the surrounding capture
            Message.captureAndInitializeContext(message);
            
            if(message.isRequest()) {
               platformBus.invokeAndSendResponse(message, () -> dispatch(message));
               return;
            }
   
            // note this will execute model change events here due to the listener
            // which is the behavior we want -- order should be:
            //  1) MessageReceivedEvent
            //  2) Update models
            //  3) Any change events
            models.update(message);
         }
   
         Iterator<PlaceEventHandler> it = handlers.values().iterator();
         while(it.hasNext()) {
            PlaceEventHandler handler = it.next();
            try {
               MDC.put(MdcContext.MDC_TARGET, handler.getAddress().getRepresentation());
               handler.onEvent(event);
            } catch(Exception e) {
               handler.getContext().logger().warn("Error dispatching [{}]", event, e);
            }
            if(handler.isDeleted()) {
               it.remove();
            }
         }
      }
   }
   
   private MessageBody dispatch(PlatformMessage message) {
      Address destination = message.getDestination();
      Map<Address, PlaceEventHandler> handlers = this.handlers; // de-reference volatile
      PlaceEventHandler handler = handlers.get(destination);
      if(handler == null) {
         return Errors.notFound(destination);
      }
      try {
         return handler.handleRequest(message);
      }
      finally {
         if(handler.isDeleted()) {
            handlers.remove(destination);
         }
      }
   }

   private class DispatchTask implements Runnable {
      private final SingleThreadDispatcher<Object> dispatcher;
      private final Object event;

      DispatchTask(SingleThreadDispatcher<Object> dispatcher, Object event) {
         this.dispatcher = dispatcher;
         this.event = event;
      }

      @Override
      public void run() {
         dispatcher.dispatchOrQueue(event);
      }

   }
   
}

