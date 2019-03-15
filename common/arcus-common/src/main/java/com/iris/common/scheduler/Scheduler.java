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
package com.iris.common.scheduler;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Supplier;

/**
 * A generic Scheduler interface, currently two implementations available:
 *  1) ExecutorScheduler uses ScheduledExecutorService
 *  2) HashedWheelScheduler uses HashedWheelTimer and is in arcus-lib due to dependency on netty (need optional dependencies)
 */
public interface Scheduler {

   <I> ScheduledTask scheduleDelayed(Function<I, ?> task, Supplier<I> input, long timeout, TimeUnit unit);
   
   <I> ScheduledTask scheduleDelayed(Function<I, ?> task, @Nullable I input, long timeout, TimeUnit unit);
   
   ScheduledTask scheduleDelayed(Runnable task, long timeout, TimeUnit unit);
   
   <I> ScheduledTask scheduleAt(Function<I, ?> task, Supplier<I> input, Date runAt);
   
   <I> ScheduledTask scheduleAt(Function<I, ?> task, @Nullable I input, Date runAt);

   ScheduledTask scheduleAt(Runnable task, Date runAt);
}

