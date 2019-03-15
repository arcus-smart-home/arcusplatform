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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.common.scheduler.ExecutorScheduler;
import com.iris.common.scheduler.Scheduler;
import com.iris.common.sunrise.ReedellCalculatorWrapper;
import com.iris.common.sunrise.SunriseSunsetCalc;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ScheduledEvent;
import com.iris.platform.partition.PartitionListener;
import com.iris.platform.scheduler.SchedulerConfig;
import com.iris.util.ThreadPoolBuilder;

public class SchedulerModule extends AbstractIrisModule {

   public SchedulerModule() {
      // TODO Auto-generated constructor stub
   }

   @Override
   protected void configure() {
      bind(SchedulerCapabilityService.class).asEagerSingleton();
      bind(SchedulerRegistry.class).to(PlatformSchedulerRegistry.class);
      
      bind(
            Key.get(new TypeLiteral<Listener<ScheduledEvent>>() {}, 
            Names.named(PlatformEventSchedulerService.NAME_SCHEDULED_EVENT_LISTENER))
      ).to(SchedulerCapabilityService.class);
      bind(SunriseSunsetCalc.class).to(ReedellCalculatorWrapper.class).asEagerSingleton();
      bind(EventSchedulerService.class)
         .to(PlatformEventSchedulerService.class);
      bindSetOf(PartitionListener.class).addBinding().to(PlatformEventSchedulerService.class);
   }

   @Provides
   public Scheduler scheduler(SchedulerConfig config) {
      ScheduledExecutorService schedulerPool =
            Executors.newScheduledThreadPool(
                  config.getDispatchThreadPoolSize(),
                  ThreadPoolBuilder
                     .defaultFactoryBuilder()
                     .setNameFormat("scheduler-dispatcher-%d")
                     .build()
            );
      return new ExecutorScheduler(schedulerPool);
   }
   
}

