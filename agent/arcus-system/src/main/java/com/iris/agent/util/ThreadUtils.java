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
package com.iris.agent.util;

import java.util.concurrent.TimeUnit;

public final class ThreadUtils {
   public static void sleep(long time, TimeUnit unit) {
      nanosleep(TimeUnit.NANOSECONDS.convert(time, unit));
   }

   private static void nanosleep(long delay) {
      long end = System.nanoTime() + delay;

      while (true) {
         long time = System.nanoTime();
         long remn = end - time;
         if (remn <= 0) {
            return;
         }

         try {
            Thread.sleep(remn/1000000L, (int)(remn%100000L));
         } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
         }
      }
   }
}

