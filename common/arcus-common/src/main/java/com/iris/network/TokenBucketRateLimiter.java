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
package com.iris.network;

import java.util.concurrent.TimeUnit;

class TokenBucketRateLimiter implements ConfigurableRateRateLimiter {
   private double capacity;
   private double ratePerNano;
   private double permits;
   private long last;

   TokenBucketRateLimiter(int capacity, double ratePerSecond, int permits) {
      this.capacity = capacity;
      this.permits = permits;
      this.ratePerNano = ratePerSecond / 1000000000.0;
   }

   @Override
   public void acquire(double size, NetworkClock clock) throws InterruptedException {
      while (true) {
         long sleepTimeInNs = waitTimeForAcquire(size, clock);
         if (sleepTimeInNs == 0) {
            return;
         }

         nanoSleep(sleepTimeInNs);
      }
   }

   @Override
   public boolean tryAcquire(double size, NetworkClock clock) {
      return waitTimeForAcquire(size,clock) == 0;
   }

   @Override
   public long tryAcquireOrGetWait(double size, NetworkClock clock) {
      return waitTimeForAcquire(size,clock);
   }

   @Override
   public boolean tryAcquire(double size, long timeout, TimeUnit unit, NetworkClock clock) throws InterruptedException {
      if (timeout <= 0) {
         throw new IllegalArgumentException("timeout must be positive");
      }

      long endSleepTimeInNs = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeout, unit);
      while (true) {
         long sleepTimeInNs = waitTimeForAcquire(size, clock);
         if (sleepTimeInNs == 0) {
            return true;
         }

         long remaining = endSleepTimeInNs - System.nanoTime();
         if (remaining <= 0) {
            return false;
         }

         nanoSleep(Math.min(sleepTimeInNs, remaining));
      }
   }
   
   @Override
   public long getApproximateWaitTimeInNs(double size, NetworkClock clock) {
      return waitTimeForAcquire(size, clock, false);
   }

   private long waitTimeForAcquire(double size, NetworkClock clock) {
      return waitTimeForAcquire(size, clock, true);
   }

   private long waitTimeForAcquire(double size, NetworkClock clock, boolean take) {
      synchronized (this) {
         long now = clock.nanoTime();
         long elapsed = now - last;
         last = now;

         if (elapsed >= 0) {
            permits = Math.min(capacity, permits + (elapsed*ratePerNano));
         }

         if (size <= permits) {
            if (take) {
               permits -= size;
            }

            return 0L;
         }

         return (long)((size - permits) / ratePerNano);
      }
   }

   private static void nanoSleep(long nanos) throws InterruptedException {
      TimeUnit.NANOSECONDS.sleep(nanos);
   }

   public int getCurrentPermits() {
      return (int)permits;
   }

   public void setCurrentPermits(int permits) {
      synchronized (this) {
         this.permits = (permits > capacity) ? capacity : permits;
      }
   }

   public int getCapacity() {
      return (int)capacity;
   }

   public void setCapacity(int capacity) {
      synchronized (this) {
         this.capacity = capacity;
      }
   }

   @Override
   public double getRatePerSecond() {
      return this.ratePerNano * 1000000000.0;
   }

   @Override
   public void setRatePerSecond(double ratePerSecond) {
      synchronized (this) {
         this.ratePerNano = ratePerSecond / 1000000000.0;
      }
   }
}

