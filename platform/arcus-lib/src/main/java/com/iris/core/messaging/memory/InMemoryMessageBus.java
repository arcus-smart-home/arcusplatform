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
package com.iris.core.messaging.memory;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.iris.core.messaging.MessageBus;
import com.iris.core.messaging.MessageListener;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.messages.Message;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.platform.partition.PlatformPartition;
import com.iris.util.Subscription;

/**
 *
 */
public class InMemoryMessageBus<T extends Message> implements MessageBus<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryMessageBus.class);

	private final String name;
	private final ExecutorService executor;
	private final BlockingQueue<T> messages = new LinkedBlockingQueue<T>();
   private final BlockingQueue<Future<?>> responses = new LinkedBlockingQueue<>();
	private final ConcurrentLinkedQueue<MessageListener<T>> listeners =
			new ConcurrentLinkedQueue<>();

	private final Serializer<T> serializer;
	private final Deserializer<T> deserializer;

   private volatile long timeoutMs = 5000;

	public InMemoryMessageBus(
		String name,
		Serializer<T> serializer,
		Deserializer<T> deserializer
	) {
		this(
				name,
				serializer,
				deserializer,
				Executors.newSingleThreadExecutor(
						new ThreadFactoryBuilder()
							.setDaemon(true)
							.setNameFormat(name + "-dispatcher")
							.build()
				)
		);
	}

	public InMemoryMessageBus(
			String name,
			Serializer<T> serializer,
			Deserializer<T> deserializer,
			ExecutorService executor
	) {
		this.name = name;
		this.serializer = serializer;
		this.deserializer = deserializer;
		this.executor = executor;
	}

   @PreDestroy
   public void destroy() throws InterruptedException {
      executor.shutdownNow();
      executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
   }

   public long getTimeoutMs() {
      return timeoutMs;
   }

   public void setTimeoutMs(long timeoutMs) {
      this.timeoutMs = timeoutMs;
   }

   public T poll() {
      return getMessageQueue().poll();
   }

   public T take() throws InterruptedException, TimeoutException {
      LOGGER.debug("Waiting for message");
      long timeoutMs = this.timeoutMs;
      T message = getMessageQueue().poll(timeoutMs, TimeUnit.MILLISECONDS);
      if(message != null) {
         return message;
      }
      LOGGER.debug("Message timed out");
      throw new TimeoutException("No message after waiting " + timeoutMs + " ms" );
   }

   public BlockingQueue<T> getMessageQueue() {
      return messages;
   }

   /**
    * Waits until all the messages on the bus
    * have been dispatched.
    */
   public void await() {
      for(Future<?> response: responses) {
         try {
            response.get(timeoutMs, TimeUnit.MILLISECONDS);
         }
         catch(Exception e) {
            e.printStackTrace();
         }
      }
   }

   @Override
	public ListenableFuture<Void> send(T message) {
		// serialize, then deserialize to emulate a real trip through the queue
		final T m = translate(message);
		messages.add(m);
		try {
		   SettableFuture<Void> result = SettableFuture.create();
   		executor.execute(() -> {
   		   try {
   		      listeners.forEach((p) -> p.onMessage(m));
   		      result.set(null);
   		   }
   		   catch(Throwable t) {
   		      LOGGER.warn("Error handling message", t);
   		      result.setException(t);
   		   }
   		});
   		responses.add(result);
   		return Futures.immediateFuture(null);
		} catch(RejectedExecutionException ree) {
		   LOGGER.info("Dropping message {} because the bus has been shut down", message);
		   return Futures.immediateFailedFuture(ree);
		}
	}
   
   @Override
   public ListenableFuture<Void> send(PlatformPartition partition, T message) {
      // partitioning not really supported in memory
      return send(message);
   }

	public Subscription addMessageListener(final MessageListener<T> listener) {
		listeners.add(listener);
	   return () -> listeners.remove(listener);
   }

   @Override
   public Subscription addMessageListener(Set<AddressMatcher> matchers, MessageListener<T> listener) {
      final Predicate<Address> p = Predicates.or(matchers);
      return addMessageListener((m) -> {
         if(p.apply(m.getDestination())) {
            listener.onMessage(m);
         }
         else {
            LOGGER.debug("Ignoring message to [{}], does not match [{}]", m.getDestination(), p);
         }
      });
   }
   
   @Override
   public Subscription addBroadcastMessageListener(Set<AddressMatcher> matchers, MessageListener<T> listener) {
	   final Predicate<Address> p = Predicates.or(matchers);
	      return addMessageListener((m) -> {
	         if( AddressMatchers.BROADCAST_MESSAGE_MATCHER.apply(m.getDestination()) && p.apply(m.getSource())) {
	            listener.onMessage(m);
	         }
	         else {
	            LOGGER.debug("Ignoring message to [{}], does not match [{}]", m.getDestination(), p);
	         }
	      });
   }
   
    
   
   private T translate(T message) {
	   byte [] b = serializer.serialize(message);
	   if(LOGGER.isInfoEnabled()) {
	   	LOGGER.info("{}: {} posted", name, new String(b));
	   }
	   return deserializer.deserialize(b);
   }

}

