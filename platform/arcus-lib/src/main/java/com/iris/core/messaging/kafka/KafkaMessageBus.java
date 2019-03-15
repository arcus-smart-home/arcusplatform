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
package com.iris.core.messaging.kafka;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.iris.core.messaging.MessageBus;
import com.iris.core.messaging.MessageListener;
import com.iris.core.messaging.kafka.KafkaMessageSender.MessageTooBigException;
import com.iris.io.Deserializer;
import com.iris.io.Serializer;
import com.iris.messages.Message;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatcher;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.errors.Errors;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.PlatformPartition;
import com.iris.util.Subscription;
import com.iris.util.Subscriptions;

public abstract class KafkaMessageBus<M extends Message, C extends AbstractKafkaConfig> implements MessageBus<M> {
   private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessageBus.class);
   private static final String ERROR_BIGASS_MESSAGE = "message.oversized";

   private final KafkaMetrics metrics;
   private final KafkaMessageSender sender;
   private final KafkaDispatcher dispatcher;
   private final C config;
   private final Serializer<M> serializer;
   private final Deserializer<M> deserializer;
   private final Partitioner partitioner;
   private final LoadingCache<String, TopicFilter<PlatformPartition, M>> cache;

   KafkaMessageBus(
         String name,
         KafkaMessageSender sender,
         KafkaDispatcher dispatcher,
         C config,
         Partitioner partitioner,
         Serializer<M> serializer,
         Deserializer<M> deserializer
   ) {
      this.metrics = new KafkaMetrics(name, 32, config);
      this.sender = sender;
      this.dispatcher = dispatcher;
      this.config = config;
      this.partitioner = partitioner;
      this.serializer = serializer;
      this.deserializer = deserializer;
      
      this.cache =
            CacheBuilder
               .newBuilder()
               .build(new CacheLoader<String, TopicFilter<PlatformPartition, M>>() {

                  @Override
                  public TopicFilter<PlatformPartition, M> load(String key) throws Exception {
                     return registerTopicFilter(key);
                  }
                  
               });
   }

   @Override
   public ListenableFuture<Void> send(M message) {
      PlatformPartition partition = partitioner.getPartitionForMessage(message);
      return send(partition, message);
   }
   
   // TODO is there a case where a null partition is legal?
   @SuppressWarnings("unchecked") // there is an instanceof check in the if expression that guarantees M is a PlatformMessage
	@Override
   public ListenableFuture<Void> send(@Nullable PlatformPartition partition, M message) {
      if(partition == null) {
         LOGGER.warn("Sending message without a partition, this message may not be processed by clustered nodes");
      }
      try(Timer.Context time = metrics.startSending()) {
         metrics.sent(partition, message);
         String topic = getTopic(message.getDestination());
         if (isLogged()) {
            LOGGER.debug("Sending message [{}]", message);
         }

         try {
            sender.submit(topic, partition, message, serializer);
         }
         catch(MessageTooBigException e) {
            if(message instanceof PlatformMessage) {
               M error = null;
               if(message.isRequest()) {
                  // this was a request, send a response that the message didn't make it
                  error = 
                     (M) PlatformMessage
                        .respondTo((PlatformMessage) message)
                        .withPayload(Errors.fromCode(ERROR_BIGASS_MESSAGE, "Request was too large to transmit"))
                        .create();
               }
               else if(!message.getDestination().isBroadcast()) {
                  // this was a response, let the requestor know the message was lost
                  error = 
                     (M) PlatformMessage
                        .respondTo((PlatformMessage) message)
                        .withPayload(Errors.fromCode(ERROR_BIGASS_MESSAGE, "Response was too large to transmit"))
                        .create();
               }
               if(error != null) {
                  sender.submit(topic, partition, error, serializer);
               }
            }
            throw e;
         }
         return Futures.immediateFuture(null);
      }
      catch(Exception e) {
         LOGGER.warn("Error sending message [{}] to message bus", message, e);
         return Futures.immediateFailedFuture(e);
      }
   }

   @Override
   public Subscription addMessageListener(
         final Set<AddressMatcher> matchers, 
         final MessageListener<M> listener
   ) {
      Set<String> topics = getTopics(matchers);
      MessageListener<M> l = new MessageListener<M>() {
         @Override
         public void onMessage(M message) {
        	 dispatchMessage(message, message.getDestination(), matchers, listener);             
         }
      };      
      return createSubscriptionsFromTopics(topics, l);
   }
   
   @Override
   public Subscription addBroadcastMessageListener(
		   final Set<AddressMatcher> matchers, 
		   final MessageListener<M> listener
   ) {
	   Set<String> topics = getTopics(matchers);
	   MessageListener<M> l = new MessageListener<M>() {
         @Override
         public void onMessage(M message) {
        	 Address destAddress = message.getDestination();
        	 if(AddressMatchers.BROADCAST_MESSAGE_MATCHER.apply(destAddress)) {
        		 dispatchMessage(message, message.getSource(), matchers, listener); 
        	 }        	       
         }
	   };
	   return createSubscriptionsFromTopics(topics, l);	      
   }
   
   private void dispatchMessage(final M message,
		   	final Address targetAddress,
			final Set<AddressMatcher> matchers, 
			final MessageListener<M> listener
   ) {
	   for(AddressMatcher m: matchers) {           
           if(m.apply(targetAddress)) {
              LOGGER.trace("Dispatching message [{}] to [{}]", message, listener);
              Timer.Context c = metrics.startProcessing(message);
              try {
                 listener.onMessage(message);
              }
              finally {
                 c.stop();
              }
              return;
           }
        }
        LOGGER.trace("Ignoring message [{}], does not match [{}]", message, matchers);
		
   }
   
   private Subscription createSubscriptionsFromTopics(Set<String> topics, MessageListener<M> l) {
	   List<Subscription> subscriptions = new ArrayList<>(topics.size());
	      for(String topic: topics) {
	         Subscription subscription = cache.getUnchecked(topic).addMessageListener(l);
	         subscriptions.add(subscription);
	      }
	      return Subscriptions.marshal(subscriptions);
   }

   protected C getConfig() {
      return config;
   }
   
   protected TopicFilter<PlatformPartition, M> registerTopicFilter(String topic) {
      MessageTopicFilter<M> filter = new MessageTopicFilter<>(metrics, getConfig(topic), deserializer);
      partitioner.addPartitionListener(filter);
      dispatcher.addListener(topic, filter);
      return filter;
   }

   protected Set<String> getTopics(Set<AddressMatcher> matchers) {
      if(matchers == null || matchers.isEmpty()) {
         throw new IllegalArgumentException("Must specify at least one matcher");
      }
      Set<String> topics = new HashSet<>(matchers.size());
      for(AddressMatcher matcher: matchers) {
         topics.add(getTopic(matcher));
      }
      return topics;
   }
   
   protected TopicConfig getConfig(String topic) {
      TopicConfig config = new TopicConfig();
      config.setName(topic);
      config.setDefaultTimeoutMs(this.config.getDefaultTimeoutMs());
      config.setTotalPartitionCount(this.partitioner.getPartitionCount());
      return config;
   }

   protected abstract boolean isLogged();

   protected abstract String getTopic(Address address);

   protected abstract String getTopic(AddressMatcher matcher) throws IllegalArgumentException;
}

