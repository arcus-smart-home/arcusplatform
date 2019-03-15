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
package com.iris.core.scheduler;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.iris.common.scheduler.BaseScheduler;
import com.iris.common.scheduler.ScheduledTask;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;

public class HashedWheelScheduler extends BaseScheduler {
   private HashedWheelTimer timer;

   public HashedWheelScheduler(HashedWheelTimer timer) {
      this.timer = timer;
   }

   /* (non-Javadoc)
    * @see com.iris.common.scheduler.BaseScheduler#doSchedule(java.lang.Runnable, java.sql.Date, long, java.util.concurrent.TimeUnit)
    */
   @Override
   protected ScheduledTask doSchedule(Runnable task, Date time, long delay, TimeUnit unit) {
      Timeout timo = timer.newTimeout(new TimerTask() {
         @Override
         public void run(Timeout timeout) throws Exception {
            task.run();
         }
      }, delay, unit);
      return new TimeoutScheduledTask(timo);
   }

   private static class TimeoutScheduledTask implements ScheduledTask {
      private final Timeout timeout;

      TimeoutScheduledTask(Timeout timeout) {
         this.timeout = timeout;
      }
      
      @Override
      public boolean isPending() {
         return !(timeout.isExpired() || timeout.isCancelled());
      }

      @Override
      public boolean cancel() {
         return timeout.cancel();
      }
      
   }

}

