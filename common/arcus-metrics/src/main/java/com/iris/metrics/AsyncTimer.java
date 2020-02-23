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
package com.iris.metrics;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Doesn't implement {@link Metric} to make it clear that it's really a lightweight composite of two {@link Timer}s that
 * should be registered directly.
 */
public class AsyncTimer
{
   private final Timer successTimer;
   private final Timer failureTimer;

   public AsyncTimer(Timer successTimer, Timer failureTimer)
   {
      this.successTimer = successTimer;
      this.failureTimer = failureTimer;
   }

   public <V> ListenableFuture<V> time(ListenableFuture<V> operation)
   {
      return start().time(operation);
   }

   private Context start()
   {
      return new Context();
   }

   private class Context
   {
      private final Clock clock;

      private final long startTimeNanos;

      private Context()
      {
         clock = Clock.defaultClock();

         startTimeNanos = clock.getTick();
      }

      private <V> ListenableFuture<V> time(ListenableFuture<V> operation)
      {
         FutureCallback<V> callback = new FutureCallback<V>()
         {
            @Override
            public void onSuccess(V result)
            {
               updateTimer(successTimer);
            }

            @Override
            public void onFailure(Throwable t)
            {
               updateTimer(failureTimer);
            }

            private void updateTimer(Timer timer)
            {
               long duration = clock.getTick() - startTimeNanos;

               timer.update(duration, NANOSECONDS);
            }
         };

         Futures.addCallback(operation, callback, MoreExecutors.directExecutor());

         return operation;
      }
   }
}

