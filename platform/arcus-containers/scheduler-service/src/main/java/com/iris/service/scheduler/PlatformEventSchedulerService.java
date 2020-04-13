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
package com.iris.service.scheduler;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import com.iris.messages.services.PlatformConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.google.common.base.Supplier;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.Singleton;
import com.iris.common.scheduler.ScheduledTask;
import com.iris.common.scheduler.Scheduler;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.address.Address;
import com.iris.messages.capability.SchedulerCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Place;
import com.iris.messages.model.serv.SchedulerModel;
import com.iris.messages.PlatformMessage;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.IrisMetricSet;
import com.iris.platform.partition.PartitionChangedEvent;
import com.iris.platform.partition.PartitionListener;
import com.iris.platform.partition.PlatformPartition;
import com.iris.platform.scheduler.model.PartitionOffset;
import com.iris.platform.scheduler.model.ScheduledCommand;
import com.iris.platform.scheduler.ScheduleDao;
import com.iris.platform.scheduler.SchedulerConfig;
import com.iris.util.ThreadPoolBuilder;

@Singleton
public class PlatformEventSchedulerService implements EventSchedulerService, PartitionListener {
   private static final Logger logger = LoggerFactory.getLogger(PlatformEventSchedulerService.class);
   
   public static final String NAME_SCHEDULED_EVENT_LISTENER = "scheduler.event.listener";
   public static final String NAME_SCHEDULED_EVENT_EXECUTOR = "scheduler.event.executor";
   
   private long schedulingHorizonMs = 600000;
   
   private final Listener<ScheduledEvent> listener;
   private final ScheduledExecutorService executor;
   private final Scheduler scheduler;
   private final ScheduleDao scheduleDao;
   private final PlaceDAO placeDao;
   private final PlatformSchedulerRegistry registry;

   private final SchedulerMetrics metrics;
   private final Map<PlatformPartition, PartitionSchedulerJob> partitions =
         new HashMap<>();
   
   // TODO tune this
   private final Map<PartitionOffset, EventSchedulerJob> activeJobs =
         new ConcurrentHashMap<>();

   private final boolean sanityCheckExisting;
   
   @Inject
   public PlatformEventSchedulerService(
         SchedulerConfig config,
         @Named(NAME_SCHEDULED_EVENT_LISTENER)
         Listener<ScheduledEvent> listener,
         Scheduler scheduler,
         ScheduleDao scheduleDao,
         PlaceDAO placeDao,
         PlatformSchedulerRegistry registry
   ) {
      this.listener = listener;
      this.scheduler = scheduler;
      this.scheduleDao = scheduleDao;
      this.placeDao = placeDao;
      this.registry = registry;
      this.sanityCheckExisting = config.getSanityCheckExisting();

      this.schedulingHorizonMs = TimeUnit.MILLISECONDS.convert(config.getSchedulerHorizonSec(), TimeUnit.SECONDS);
      this.metrics = new SchedulerMetrics();
      this.executor = 
            Executors
               .newScheduledThreadPool(
                     config.getSchedulingThreadPoolSize(),
                     ThreadPoolBuilder
                        .defaultFactoryBuilder()
                        .setNameFormat("scheduler-planner-%d")
                        .build()
               );
   }
   
   @PreDestroy
   public void stop() throws InterruptedException {
      logger.info("Stopping schedulers...");
      executor.shutdown();
      executor.awaitTermination(30, TimeUnit.SECONDS);
   }
   
   @Override
   public synchronized void onPartitionsChanged(PartitionChangedEvent event) {
      Set<PlatformPartition> partitions = event.getPartitions();
      Iterator<Map.Entry<PlatformPartition, PartitionSchedulerJob>> it = this.partitions.entrySet().iterator();
      while(it.hasNext()) {
         Map.Entry<PlatformPartition, PartitionSchedulerJob> entry = it.next();
         if(!partitions.contains(entry.getKey())) {
            entry.getValue().cancel();
            it.remove();
         }
      }
      
      Set<PlatformPartition> toAdd = Sets.difference(partitions, this.partitions.keySet());
      Map<PlatformPartition, PartitionOffset> offsets = scheduleDao.getPendingPartitions(toAdd);
      
      for(PartitionOffset offset: offsets.values()) {
         PartitionSchedulerJob job = new PartitionSchedulerJob(offset);
         Future<?> future = executor.scheduleAtFixedRate(
               () -> job.schedule(),
               0,
               scheduleDao.getTimeBucketDurationMs(),
               TimeUnit.MILLISECONDS
         );
         job.setSchedulerFuture(future);
         this.partitions.put(offset.getPartition(), job);
      }

      if (sanityCheckExisting) {
         logger.info("Checking assigned schedulers for past due events.");
         // Check all schedulers
         for (PlatformPartition p : partitions) {
            placeDao.streamByPartitionId(p.getId())
                    .forEach((place) -> checkByPlace(place));
         }
         logger.info("Finished checking assigned schedulers for past due events.");
      }
   }

   private void checkByPlace(Place place) {
      logger.debug("checking place [{}]", place.getId());
      registry.loadByPlace(place.getId(), true).forEach((executor) -> checkScheduler(executor, place.getPopulation()));
   }

   private void checkScheduler(SchedulerCapabilityDispatcher executor, String population) {
      if (SchedulerModel.getNextFireTime(executor.getScheduler()) == null) {
         logger.debug("Skipping disabled scheduler [{}]", executor.getScheduler().getId());
         return;
      }

      if (SchedulerModel.getNextFireTime(executor.getScheduler()).getTime() < System.currentTimeMillis()) {
         metrics.onCommandRescheduled();
         logger.warn("Found scheduler in need of update: [{}]", executor.getScheduler().getId());
         executor.onPlatformMessage(PlatformMessage
                 .builder()
                 .from(Address.fromString(SchedulerModel.getTarget(executor.getScheduler())))
                 .to(Address.platformService(executor.getScheduler().getId(), SchedulerCapability.NAMESPACE))
                 .withPlaceId(SchedulerModel.getPlaceId(executor.getScheduler()))
                 .withPayload(SchedulerCapability.RecalculateScheduleRequest.NAME)
                 .withPopulation(population)
                 .create());
      }
   }

   /**
    * Schedule the scheduler at a given time, both in Dao and memory.
    *
    * @param placeId
    * @param address
    * @param time
    */
   @Override
   public void fireEventAt(UUID placeId, Address address, Date time) {
      ScheduledCommand command = scheduleDao.schedule(placeId, address, time);
      schedule(command);
   }

   @Override
   public void rescheduleEventAt(UUID placeId, Address address, Date oldTime, Date newTime) {
      ScheduledCommand command = new ScheduledCommand();
      command.setPlaceId(placeId);
      command.setSchedulerAddress(address);
      command.setScheduledTime(oldTime);
      
      ScheduledCommand updated = scheduleDao.reschedule(command, newTime);
      unscheduleFromMemory(placeId, address, oldTime);
      schedule(updated);
   }

   @Override
   public void cancelEvent(UUID place, Address address, Date time) {
      if(time == null) {
         return;
      }
      
      unscheduleFromDao(place, address, time);
      unscheduleFromMemory(place, address, time);
   }
   
   private void unscheduleFromDao(UUID place, Address address, Date time) {
      scheduleDao.unschedule(place, address, time);
   }
   
   private boolean unscheduleFromMemory(UUID placeId, Address address, Date time) {
      PartitionOffset offset = scheduleDao.getPartitionOffsetFor(placeId, time);
      EventSchedulerJob partition = activeJobs.get(offset);
      if(partition == null) {
         return false;
      }
      
      return partition.cancel(address);
   }
   
   void schedule(ScheduledCommand command) {
      EventSchedulerJob job = activeJobs.get(command.getOffset());
      if(job != null) {
         job.schedule(command);
         return;
      }
      
      if(isPastDue(command) && !command.isExpired()) {
         scheduler.scheduleDelayed(() -> dispatch(command), 0, TimeUnit.MILLISECONDS);
      }
//      else if(command.getScheduledTime().before(when))
   }
   
   boolean isInSchedulerHorizon(Date date) {
      return isInSchedulerHorizon(date, System.currentTimeMillis());
   }
   
   boolean isInSchedulerHorizon(Date date, long nowMs) {
      return date.getTime() < (nowMs + schedulingHorizonMs);
   }
   
   boolean isPastDue(ScheduledCommand command) {
      return isPastDue(command.getScheduledTime(), System.currentTimeMillis());
   }

   boolean isPastDue(Date date, long nowMs) {
      return date.getTime() < nowMs;
   }

   void dispatch(ScheduledCommand command) {
      try {
         ScheduledEvent event = new ScheduledEvent(command.getSchedulerAddress(), command.getScheduledTime().getTime());
         logger.trace("Invoking scheduled event [{}]", event);
         listener.onEvent(event);
      }
      catch(Exception e) {
         logger.warn("Error dispatching scheduled event to scheduler", e);
      }
   }
   
   class PartitionSchedulerJob {
      private PlatformPartition partition;
      private PartitionOffset next;
      private EventSchedulerJob currentJob;
      private Future<?> schedulerFuture;
      
      PartitionSchedulerJob(PartitionOffset next) {
         this.partition = next.getPartition();
         this.next = next;
      }
      
      public void setSchedulerFuture(Future<?> schedulerFuture) {
         this.schedulerFuture = schedulerFuture;
      }
      
      public void schedule() {
         try {
            while(isInSchedulerHorizon(next.getOffset())) {
               try(Timer.Context timer = metrics.startSchedulingPartition(partition.getId())) {
                  scheduleNext();
               }
            }
            while(checkDone()) { }
         }
         catch(Throwable t) {
            logger.warn("Error scheduling for partition [{}]: {}", partition, t.getMessage(), t);
            metrics.onPartitionError();
         }
      }
      
      public void scheduleNext() throws Exception {
         EventSchedulerJob offsetJob = new EventSchedulerJob(next);
         if(currentJob == null) {
            currentJob = offsetJob;
         }
         
         activeJobs.put(next, offsetJob);
         offsetJob.schedule();
         next = next.getNextPartitionOffset();
         metrics.onPartitionScheduled(partition.getId());
      }
      
      public boolean checkDone() {
         if(currentJob == null) {
            return true;
         }
         if(!currentJob.isDone()) {
            return false;
         }
         
         if(activeJobs.remove(currentJob.getOffset(), currentJob)) {
            // FIXME set currentJob to null here to prevent it from never completing
            logger.info("Partition commands completed: [{}]", currentJob.getOffset());
            metrics.onPartitionCompleted(partition.getId());
         }
         else {
            logger.warn("Attempting to mark partition [{}] completed again, likely due to db error");
         }
         PartitionOffset activeOffset = scheduleDao.completeOffset(currentJob.getOffset());
         currentJob = activeJobs.get(activeOffset);
         return true;
      }
      
      public void cancel() {
         Future<?> future = schedulerFuture;
         if(future != null) {
            future.cancel(false);
         }
         
         Iterator<Map.Entry<PartitionOffset, EventSchedulerJob>> it = activeJobs.entrySet().iterator();
         while(it.hasNext()) {
            Map.Entry<PartitionOffset, EventSchedulerJob> entry = it.next();
            if(entry.getKey().getPartition().equals(partition)) {
               entry.getValue().cancel();
               it.remove();
            }
         }
         
      }
   }
   
   class EventSchedulerJob {
      private final SettableFuture<Boolean> result = SettableFuture.create();
      private final ConcurrentMap<Address, Future<ScheduledTask>> pendingRequests = 
            new ConcurrentHashMap<>();
      
      PartitionOffset offset;
      
      EventSchedulerJob(PartitionOffset offset) {
         this.offset = offset;
      }
      
      public PartitionOffset getOffset() {
         return offset;
      }
      
      public void schedule() {
         logger.info("Scheduling partition offset [{}]", offset);
         try {
            scheduleDao
               .streamByPartitionOffset(offset)
               .forEach((command) -> schedule(command));
         }
         catch(RuntimeException e) {
            logger.warn("Unable to schedule partition offset [{}] -- will retry in [{}] seconds", offset, scheduleDao.getTimeBucketDurationMs() / 1000.0f, e);
            throw e;
         }
      }
      
      public void schedule(ScheduledCommand command) {
         logger.trace("Scheduling command [{}]", command);
         final SettableFuture<ScheduledTask> ref = SettableFuture.create();
         pendingRequests.put(command.getSchedulerAddress(), ref);
         try {
            ScheduledTask task = scheduler.scheduleAt(() -> dispatch(command, ref), command.getScheduledTime());
            ref.set(task);
            metrics.onCommandScheduled();
         }
         finally {
            // should never happen, but...
            // if anything goes wrong, clear it out
            if(!ref.isDone()) {
               pendingRequests.remove(command.getSchedulerAddress(), ref);
               ref.cancel(true);
            }
         }
      }
      
      public void dispatch(ScheduledCommand command, Future<ScheduledTask> ref) {
         if(!pendingRequests.remove(command.getSchedulerAddress(), ref)) {
            logger.debug("Dropping replaced scheduled command: [{}]", command);
            return;
         }
         try {
            // TODO also check cancelled
            if(command.isExpired()) {
               logger.debug("Dropping expired scheduled command: [{}]", command);
               // TODO need to reschedule the associated thing...
               metrics.onCommandExpired();
               return;
            }
         
            PlatformEventSchedulerService.this.dispatch(command);
            metrics.onCommandFired();
         }
         catch(Exception e) {
            logger.warn("Error dispatching scheduled event to scheduler", e);
            metrics.onCommandError();
         }
      }
      
      public boolean cancel(Address address) {
         Future<ScheduledTask> taskRef = pendingRequests.remove(address);
         if(taskRef == null) {
            return false;
         }
         try {
            return taskRef.get().cancel();
         }
         catch(Exception e) {
            return false;
         }
      }
      
      public void cancel() {
         for(Future<ScheduledTask> taskRef: pendingRequests.values()) {
            try {
               taskRef.get().cancel();
            }
            catch(Exception e) {
               logger.warn("Unable to cancel task [{}]: {}", taskRef, e.getMessage(), e);
            }
         }
      }
      
      public void addCompletionListener(Runnable task) {
         result.addListener(task, MoreExecutors.directExecutor());
      }
      
      public boolean isDone() {
         if(System.currentTimeMillis() < offset.getNextOffset().getTime()) {
            return false;
         }
         
         // TODO add in a max delay
         
         return pendingRequests.isEmpty();
      }
      
   }

   private class SchedulerMetrics {
      private final Counter partitionScheduled;
      private final Counter partitionCompleted;
      private final Counter partitionErrors;
      private final Timer partitionSchedulingTime;
      private final Counter commandScheduled;
      private final Counter commandFired;
      private final Counter commandExpired;
      private final Counter commandError;
      private final Counter commandRescheduled;
      
      private SchedulerMetrics() {
         IrisMetricSet metrics = IrisMetrics.metrics("scheduler");
         this.partitionScheduled = metrics.counter("partition.scheduled");
         this.partitionCompleted = metrics.counter("partition.completed");
         this.partitionErrors = metrics.counter("partition.errors");
         this.partitionSchedulingTime = metrics.timer("partition.schedule.time");
         this.commandScheduled = metrics.counter("command.scheduled");
         this.commandFired = metrics.counter("command.sent");
         this.commandExpired = metrics.counter("command.expired");
         this.commandError = metrics.counter("command.error");
         this.commandRescheduled = metrics.counter("command.rescheduled");
         
         metrics.gauge("partition.count",   (Supplier<Integer>) () -> partitions.size());
         metrics.gauge("partition.pending", (Supplier<Integer>) () -> activeJobs.size());
      }
      
      public Timer.Context startSchedulingPartition(int partitionId) {
         return this.partitionSchedulingTime.time();
      }
      
      public void onPartitionScheduled(int partitionId) {
         this.partitionScheduled.inc();
      }
      
      public void onPartitionCompleted(int partitionId) {
         this.partitionCompleted.inc();
      }
      
      public void onPartitionError() {
         this.partitionErrors.inc();
      }
      
      public void onCommandScheduled() {
         this.commandScheduled.inc();
      }
      
      public void onCommandFired() {
         this.commandFired.inc();
      }
      
      public void onCommandExpired() {
         this.commandExpired.inc();
      }
      
      public void onCommandError() {
         this.commandError.inc();
      }

      public void onCommandRescheduled() {
         this.commandRescheduled.inc();
      }
   }
}

