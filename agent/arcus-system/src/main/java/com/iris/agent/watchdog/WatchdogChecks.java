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
package com.iris.agent.watchdog;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

public final class WatchdogChecks {
   private WatchdogChecks() {
   }

   public static void addExecutorWatchdog(String name, Executor exec) {
      if (exec instanceof ThreadPoolExecutor) {
         WatchdogService.addWatchdogCheck(new ThreadPoolExecutorWatchdog(name,(ThreadPoolExecutor)exec));
      } else {
         throw new RuntimeException("cannot monitor executor of type: " + exec.getClass());
      }
   }

   private static final class ThreadPoolExecutorWatchdog extends AbstractWatchdogCheck {
      private final ThreadPoolExecutor exec;
      private final BlockingQueue<?> queue;

      ThreadPoolExecutorWatchdog(String name, ThreadPoolExecutor exec) {
         super(name);
         this.exec = exec;
         this.queue = exec.getQueue();
      }

      @Override
      public boolean check(long nowInNs) throws Exception {
         if (exec.isShutdown()) {
            return WatchdogService.stopWatchdogCheck();
         }

         return (exec.getPoolSize() < exec.getMaximumPoolSize()) ||
                (queue.remainingCapacity() > 0);
      }
   }
}

