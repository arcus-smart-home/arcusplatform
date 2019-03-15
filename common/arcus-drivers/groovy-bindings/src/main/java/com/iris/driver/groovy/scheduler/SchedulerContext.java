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
package com.iris.driver.groovy.scheduler; 

import com.iris.messages.address.Address;
import groovy.lang.GroovyObjectSupport;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.event.ScheduledDriverEvent;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.driver.service.executor.DriverExecutors;

public class SchedulerContext extends GroovyObjectSupport {
	private static final Logger logger = LoggerFactory.getLogger(SchedulerContext.class);

   /**
    * Adds a {@link ScheduledDriverEvent} to be fired immediately
    * after the current execution completes. Data on the event
    * will be {@code null}
    * @param name
    *    The name for the event
    */
   public void defer(String name) {
      doSchedule(name, null, 0);
   }

   /**
    * Adds a {@link ScheduledDriverEvent} to be fired immediately
    * after the current execution completes.
    * @param name
    *    The name for the event
    * @param data
    *    The data for the event
    */
   public void defer(String name, Object data) {
      doSchedule(name, data, 0);
   }

   public void scheduleIn(String name, long delayMs) {
      doSchedule(name, null, delayMs);
   }

   public void scheduleIn(String name, Object data, long delayMs) {
      doSchedule(name, data, delayMs);
   }
   
   public void scheduleRepeating(String name, Map<String, Object> data, long delayMs, int maxRetry) {
	   doScheduleRepeating(DriverExecutors.get(), name, data, delayMs, maxRetry, 0);
   } 

   public void scheduleRepeating(String name, long delayMs, int maxRetry) {
	   scheduleRepeating(name, null, delayMs, maxRetry);
   } 
   
   

   public void cancel(String name) {
      Preconditions.checkArgument(!StringUtils.isEmpty(name), "event name may not be null or empty");
      DriverExecutors.get().cancel(name);
      logger.debug("event {} is cancelled.", name);
   }

   
   protected void doScheduleRepeating(final DriverExecutor driverExecutor, final String name, final Object data, final long delayMs, final int maxRetries, final int currentRetry) {
	   logger.debug("doScheduleRepeating for event {}, currentRetry: {}, maxRetries: {}.", name, currentRetry, maxRetries);
	   if(currentRetry > maxRetries) {
	       // bail
	       return;
	   }
	   final ListenableFuture<Void> curScheduledEvent = doSchedule(driverExecutor, name, data, delayMs);
	   curScheduledEvent.addListener(new Runnable() {
               @Override
               public void run() {
            	   if(!curScheduledEvent.isCancelled()) {
            		   doScheduleRepeating(driverExecutor,name, data, delayMs, maxRetries, currentRetry + 1);
            	   }
               }
            },
            MoreExecutors.directExecutor()
       );
   }

   protected ListenableFuture<Void> doSchedule(final String name, @Nullable Object data, long delayMs) {
	   return doSchedule(DriverExecutors.get(), name, data, delayMs);
   }
   
   protected ListenableFuture<Void> doSchedule(DriverExecutor executor, final String name, @Nullable Object data, long delayMs) {
       Preconditions.checkArgument(!StringUtils.isEmpty(name), "event name may not be null or empty");

       Address actor = executor.context().getActor();
       Date runAt = new Date(System.currentTimeMillis() + delayMs);

       ScheduledDriverEvent event = DriverEvent.createScheduledEvent(name, data, actor, runAt);
       final ListenableFuture<Void> ref = executor.defer(name, event, runAt);

       return ref;
   }
   
}

