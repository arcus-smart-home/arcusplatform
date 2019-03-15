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

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.ScheduledEvent;
import com.iris.common.scheduler.ScheduledTask;
import com.iris.common.scheduler.Scheduler;

/**
 * 
 */
@Singleton
public class PlaceExecutorEventLoop {

   PlaceExecutorRegistry registry;
   Scheduler scheduler;
   
   /**
    * 
    */
   @Inject
   public PlaceExecutorEventLoop(
         PlaceExecutorRegistry registry,
         Scheduler scheduler
   ) {
      this.registry = registry;
      this.scheduler = scheduler;
   }

   /**
    * Fires the event, may run in this thread, may not.  This
    * will return {@code false} if the executor can't be loaded.
    * @param placeId
    * @param event
    * @return
    */
   public boolean submit(UUID placeId, RuleEvent event) {
      Optional<PlaceEnvironmentExecutor> executorRef = registry.getExecutor(placeId);
      if(!executorRef.isPresent()) {
         return false;
      }
      executorRef.get().fire(event);
      return true;
   }
   
   /**
    * Submits the rule event on another thread.
    * @param placeId
    * @param event
    * @return
    */
   public ScheduledTask defer(UUID placeId, RuleEvent event) {
      return scheduler.scheduleDelayed(() -> submit(placeId, event), 0, TimeUnit.MILLISECONDS);
   }

   public ScheduledTask scheduleDelayed(UUID placeId, ScheduledEvent event, long timeout, TimeUnit unit) {
      return scheduler.scheduleDelayed(() -> submit(placeId, event), timeout, unit);
   }

   /**
    * @param task
    * @param input
    * @param runAt
    * @return
    * @see com.iris.common.scheduler.Scheduler#scheduleAt(com.google.common.base.Function, java.lang.Object, java.util.Date)
    */
   public <I> ScheduledTask scheduleAt(UUID placeId, ScheduledEvent event, Date runAt) {
      return scheduler.scheduleAt(() -> submit(placeId, event), runAt);
   }
   
}

