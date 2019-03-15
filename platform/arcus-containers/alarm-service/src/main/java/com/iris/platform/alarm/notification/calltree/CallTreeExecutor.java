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
package com.iris.platform.alarm.notification.calltree;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.type.CallTreeEntry;
import com.iris.platform.alarm.notification.strategy.NotificationConstants;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.LoggingUncaughtExceptionHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class CallTreeExecutor {

   private static final Logger logger = LoggerFactory.getLogger(CallTreeExecutor.class);

   private final PlatformMessageBus bus;
   private final ConcurrentMap<SequentialCallTaskKey, SequentialCallTask> sequentialTasks = new ConcurrentHashMap<>();
   private final HashedWheelTimer sequentialTimer = new HashedWheelTimer(new ThreadFactoryBuilder()
      .setDaemon(true)
      .setNameFormat("calltree-sequential-%d")
      .setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler(logger))
      .build()
   );
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public CallTreeExecutor(PlatformMessageBus bus, PlacePopulationCacheManager populationCacheMgr) {
      this.bus = bus;
      this.populationCacheMgr = populationCacheMgr;
   }

   public void startSequential(CallTreeContext context) {
      Preconditions.checkArgument(context.getSequentialDelaySecs() > 0, "sequential delay must be greater than 0 seconds");
      SequentialCallTaskKey key = new SequentialCallTaskKey(context.getIncidentAddress(), context.getMsgKey());
      SequentialCallTask task = new SequentialCallTask(key, context);
      SequentialCallTask existing = sequentialTasks.putIfAbsent(key, task);
      if(existing != null) {
         logger.debug("{}:  ignoring request to start sequential call tree already in progress", key);
         return;
      }
      sequentialTimer.newTimeout(task, 0, TimeUnit.MILLISECONDS);
   }

   public void stopSequential(Address incidentAddress, String msgKey) {
      SequentialCallTaskKey key = new SequentialCallTaskKey(incidentAddress, msgKey);
      SequentialCallTask task = sequentialTasks.remove(key);
      if(task != null) {
         task.cancel();
      }
   }

   public void notifyParallel(CallTreeContext context) {
      context.getCallTree().stream().filter(CallTreeEntry::getEnabled).forEach(entry -> notifyPerson(context, entry.getPerson()));
   }

   public void notifyOwner(CallTreeContext context) {
      notifyPerson(context, context.getCallTree().get(0).getPerson());
   }

   private void notifyPerson(CallTreeContext context, String address) {
      notifyPerson(context, Address.fromString(address));
   }

   private void notifyPerson(CallTreeContext context, Address personAddress) {
      NotificationCapability.NotifyRequest.Builder builder = NotificationCapability.NotifyRequest.builder()
            .withMsgKey(context.getMsgKey())
            .withPlaceId(context.getPlaceId().toString())
            .withPersonId(personAddress.getId().toString())
            .withPriority(context.getPriority());

      if(!context.getParams().isEmpty()) {
         builder.withMsgParams(context.getParams());
      }

      MessageBody body = builder.build();
      PlatformMessage message =  PlatformMessage.builder()
            .from(NotificationConstants.ALARMSERVICE_ADDRESS)
            .to(NotificationConstants.NOTIFICATIONSERVICE_ADDRESS)
            .withPlaceId(context.getPlaceId())
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(context.getPlaceId()))
            .withActor(NotificationConstants.ALARMSERVICE_ADDRESS)
            .withPayload(body)
            .create();

      bus.send(message);
   }

   private class SequentialCallTaskKey {
      private final Address incidentAddress;
      private final String msgKey;

      SequentialCallTaskKey(Address incidentAddress, String msgKey) {
         this.incidentAddress = incidentAddress;
         this.msgKey = msgKey;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         SequentialCallTaskKey that = (SequentialCallTaskKey) o;

         if (incidentAddress != null ? !incidentAddress.equals(that.incidentAddress) : that.incidentAddress != null)
            return false;
         return msgKey != null ? msgKey.equals(that.msgKey) : that.msgKey == null;

      }

      @Override
      public int hashCode() {
         int result = incidentAddress != null ? incidentAddress.hashCode() : 0;
         result = 31 * result + (msgKey != null ? msgKey.hashCode() : 0);
         return result;
      }

      @Override
      public String toString() {
         return incidentAddress.getRepresentation() + "-" + msgKey;
      }
   }

   private class SequentialCallTask implements TimerTask {

      private final SequentialCallTaskKey key;
      private final CallTreeContext context;
      private volatile boolean cancelled = false;
      private volatile int index = 0;
      private volatile Timeout nextTimeout = null;

      SequentialCallTask(SequentialCallTaskKey key, CallTreeContext context) {
         this.key = key;
         this.context = context;
      }

      @Override
      public void run(Timeout timeout) throws Exception {
         if(cancelled) {
            logger.debug("{}: sequential call tree stopping because it has been cancelled", key);
            return;
         }
         String person = nextPerson();
         if(person != null) {
            notifyPerson(context, person);
            this.nextTimeout = sequentialTimer.newTimeout(this, context.getSequentialDelaySecs(), TimeUnit.SECONDS);
         } else {
            logger.debug("{}: sequential call tree terminated with no additional people to call", key);
            sequentialTasks.remove(key);
         }
      }

      String nextPerson() {
         List<CallTreeEntry> entries = context.getCallTree();
         if(index >= entries.size()) {
            return null;
         }
         do {
            CallTreeEntry entry = entries.get(index++);
            if(entry.getEnabled()) {
               return entry.getPerson();
            }
         } while(index < entries.size());

         return null;
      }

      void cancel() {
         if(cancelled) {
            return;
         }
         cancelled = true;
         if(this.nextTimeout != null && !this.nextTimeout.isCancelled()) {
            this.nextTimeout.cancel();
         }
      }
   }
}

