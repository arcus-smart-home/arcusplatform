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

public final class NetworkClocks {
   private NetworkClocks() {
   }

   public static NetworkClock system() {
      return SystemNetworkClock.INSTANCE;
   }

   public static NetworkClock scaled(NetworkClock ref, double scale) {
      return new ScaledNetworkClock(ref, scale);
   }

   public static NetworkClock constantRate(long rate, TimeUnit unit) {
      return new ConstantRateNetworkClock(unit.toNanos(rate));
   }

   public static com.iris.network.ControlledNetworkClock controlled() {
      return new ControlledNetworkClock();
   }

   private static enum SystemNetworkClock implements NetworkClock {
      INSTANCE;

      @Override
      public long nanoTime() {
         return System.nanoTime();
      }
   }

   private static class ConstantRateNetworkClock implements NetworkClock {
      private final long rate;
      private long current;

      public ConstantRateNetworkClock(long rate) {
         this.rate = rate;
         this.current = 0;
      }

      @Override
      public long nanoTime() {
         current += rate;
         return current;
      }
   }

   private static class ControlledNetworkClock implements com.iris.network.ControlledNetworkClock {
      private long current;

      public ControlledNetworkClock() {
         this.current = 0;
      }

      @Override
      public long nanoTime() {
         return current;
      }

      @Override
      public void tick(long time, TimeUnit unit) {
         current += unit.toNanos(time);
      }
   }

   private static abstract class DerivedNetworkClock implements NetworkClock {
      private final NetworkClock reference;

      public DerivedNetworkClock(NetworkClock reference) {
         this.reference = reference;
      }

      @Override
      public long nanoTime() {
         return reference.nanoTime();
      }
   }

   private static class ScaledNetworkClock extends DerivedNetworkClock {
      private final double scale;

      public ScaledNetworkClock(NetworkClock reference, double scale) {
         super(reference);
         this.scale = scale;
      }

      @Override
      public long nanoTime() {
         return (long)(scale * super.nanoTime());
      }
   }
}

