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

import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.MoreExecutors;
import com.iris.core.messaging.MessageListener;
import com.iris.messages.Message;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.errors.Errors;
import com.iris.util.IrisCollections;
import com.iris.util.Subscription;
import com.iris.util.ThreadPoolBuilder;
import com.netflix.governator.annotations.WarmUp;

public abstract class AbstractMessageListener<T extends Message> implements MessageListener<T> {
   private static final Logger logger = LoggerFactory.getLogger(AbstractMessageListener.class);
   
   private final RequestResponseMessageBus<T> bus;
   private final Queue<Subscription> subscriptions = new ConcurrentLinkedQueue<>();
   private final Executor executor;
   
   protected AbstractMessageListener(RequestResponseMessageBus<T> bus) {
      this(bus, MoreExecutors.directExecutor());
   }
   
   protected AbstractMessageListener(RequestResponseMessageBus<T> bus, String name, int maxThreads, long threadTimeoutMs) {
      this.bus = bus;
      this.executor =
            new ThreadPoolBuilder()
               .withBlockingBacklog()
               .withMaxPoolSize(maxThreads)
               .withKeepAliveMs(threadTimeoutMs)
               .withNameFormat(name + "-%d")
               .withMetrics(name.replace('-', '.'))
               .build()
               ;
   }
   
   protected AbstractMessageListener(RequestResponseMessageBus<T> bus, Executor executor) {
      this.bus = bus;
      this.executor = executor;
   }

   public RequestResponseMessageBus<T> getMessageBus() {
      return bus;
   }

   @PostConstruct
   public final void init() {
      onInit();
   }

   @WarmUp
   public final void start() {
      logger.info("Starting message bus listener {}", this);
      onStart();
   }

   @PreDestroy
   public final void stop() {
      logger.info("Stopping message bus listener {}", this);
      unlisten();
      onStop();
   }

   protected abstract boolean isError(T message);
   protected abstract boolean isRequest(T message);
   protected abstract MessageBody getMessageBody(T message);

   protected void onInit() { }
   
   protected void onStart() { }
   
   protected void onStop() { }

   protected void addBroadcastMessageListeners(AddressMatcher... matchers) {
	   Set<AddressMatcher> matcherSet = IrisCollections.setOf(matchers);
	   logger.debug("Listening broadcast messages for source addresses [{}] from {}", matchers, this);
	   Subscription s = getMessageBus().addBroadcastMessageListener(matcherSet, this);
	   subscriptions.add(s);
   }
   
   protected void addListeners(Address... addresses) {
      addListeners(AddressMatchers.anyOf(addresses));
   }
   
   protected void addListeners(AddressMatcher... matchers) {
      addListeners(IrisCollections.setOf(matchers));
   }
   
   protected void addListeners(Set<AddressMatcher> matchers) {
      logger.debug("Listening for {} from {}", matchers, this);
      Subscription s = getMessageBus().addMessageListener(matchers, this);
      subscriptions.add(s);
   }

   protected void unlisten() {
      Iterator<Subscription> it = subscriptions.iterator();
      while(it.hasNext()) {
         it.next().remove();
         it.remove();
      }
   }
   
   protected Executor executor() {
      return executor;
   }

   @Override
   public void onMessage(T message) {
      executor().execute(() -> handleMessage(message));
   }
   
   protected void handleMessage(T message) {
      try {
         if(isError(message)) {
            handleErrorEvent(message);
         } else if(isRequest(message)) {
            handleRequestAndSendResponse(message);
         } else {
            handleEvent(message);
         }
      }
      catch(Exception e) {
         logger.warn("Uncaught message handler exception", e);
      }
   }

   protected void handleRequestAndSendResponse(T message) {
      getMessageBus().invokeAndSendResponse(message, () -> handleRequest(message));
   }

   /**
    * Handle the message and create a response.  By default, just call traditional handleRequest().
    * However, if the subclass needs to send multiple messages back to the client based on a single
    * request (say, a service that starts based on a message, and sends data at intervals until
    * another message is sent to tell it to stop), then the subclass needs the original message to
    * construct the multiple responses.
    * @param message the original message
    * @return the result of handling the request
    */
   protected MessageBody handleRequest(T message) throws Exception {
      return handleRequest(getMessageBody(message));
   }

   /**
    * Handle the message and create a response.  If the message is a type that can't be handled
    * then return an unsupported message type error.
    * @param body body of the message
    * @return the result of handling the request
    */
   protected MessageBody handleRequest(MessageBody body) throws Exception {
      logger.trace("[{}] returning an error for an unexpected request: {}", this, body);
      return Errors.unsupportedMessageType(body.getMessageType());
   }

   /**
    * Hook method to handle unsolicited, non-error events.  The default implementation just logs
    * the incoming message
    * @param message message to handle
    */
   protected void handleEvent(T message) throws Exception {
      logger.trace("[{}] ignoring unsolicited message: {}", this, message);
   }

   /**
    * Hook method to handle unsolicited, error events.  The default implementation just logs
    * the incoming message
    *
    * @param message unsolicited message
    */
   protected void handleErrorEvent(T message) throws Exception {
      logger.trace("[{}] ignoring an unexpected error: {}", this, message);
   }
   
   protected void sendMessage(T message) {
      getMessageBus().send(message);
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return getClass().getSimpleName() + " [subscribed to: " + subscriptions + "]";
   }
}

