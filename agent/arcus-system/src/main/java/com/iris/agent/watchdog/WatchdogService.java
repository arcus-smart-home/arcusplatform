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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.util.ThreadUtils;

public final class WatchdogService {
   private static final Logger log = LoggerFactory.getLogger(WatchdogService.class);
   private static final long WATCHDOG_CHECK_INTERVAL = TimeUnit.NANOSECONDS.convert(30, TimeUnit.SECONDS);

   private static final Object START_LOCK = new Object();
   private static final Set<WatchdogCheck> checks = new CopyOnWriteArraySet<>();
   private static @Nullable Thread thread;

   @SuppressWarnings("rawtypes")
   private static @Nullable HubAttributesService.Attribute<Set> lastFailedWatchdogCheck;
   private static @Nullable HubAttributesService.Attribute<Long> lastFailedWatchdogCheckTime;

   private WatchdogService() {
   }

   public static void connectAttributes(@SuppressWarnings("rawtypes") HubAttributesService.Attribute<Set> lastFailedWatchdogCheck, HubAttributesService.Attribute<Long> lastFailedWatchdogCheckTime) {
      WatchdogService.lastFailedWatchdogCheck = lastFailedWatchdogCheck;
      WatchdogService.lastFailedWatchdogCheckTime = lastFailedWatchdogCheckTime;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Watchdog poke API
   /////////////////////////////////////////////////////////////////////////////

   public static WatchdogPoke createWatchdogPoke(String name) {
      WatchdogPokeImpl poke = new WatchdogPokeImpl(name);
      addWatchdogCheck(poke);
      return poke;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Periodic watchdog check API
   /////////////////////////////////////////////////////////////////////////////

   public static void addWatchdogCheck(WatchdogCheck check) {
      checks.add(check);
   }

   public static boolean stopWatchdogCheck() {
      throw new WatchdogCheckShutdown("shutting down");
   }

   /////////////////////////////////////////////////////////////////////////////
   // Lifecycle
   /////////////////////////////////////////////////////////////////////////////

   public static void start() {
      synchronized (START_LOCK) {
         Thread thr = thread;
         if (thr != null) {
            throw new RuntimeException("watchdog service already started");
         }

         IrisHal.startWatchdog();

         thr = new Thread(new WatchdogChecker());
         thr.setName("wtdc");
         thr.setDaemon(true);

         thread = thr;
         thr.start();
         log.warn("watchdog service started");
      }
   }

   public static void shutdown() {
      synchronized (START_LOCK) {
         Thread thr = thread;
         if (thr == null) {
            throw new RuntimeException("watchdog service not started");
         }

         thread = null;
         checks.clear();
         IrisHal.shutdownWatchdog();
         log.warn("watchdog service shutdown");
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation details
   /////////////////////////////////////////////////////////////////////////////

   private static final class WatchdogPokeImpl implements WatchdogPoke, WatchdogCheck {
      private final String name;
      private long poke;
      private long check;

      private WatchdogPokeImpl(String name) {
         this.name = name;
         this.poke = 0;
         this.check = 1;
      }

      @Override
      public void poke() {
         poke = check;
      }

      @Override
      public void shutdown() {
         poke = -1;
      }

      @Override
      public String name() {
         return name;
      }

      @Override
      public boolean check(long nowInNs) throws Exception {
         if (poke < 0) {
            return stopWatchdogCheck();
         } else {
            boolean poked = (poke == check);
            check++;
            return poked;
         }
      }
   }

   private static final class WatchdogChecker implements Runnable {
      @Override
      public void run() {
         Thread thr = thread;
         while (thr == thread) {
            Set<String> failed = runChecks();
            if (failed.isEmpty()) {
               IrisHal.pokeWatchdog();
            } else {
               if (lastFailedWatchdogCheck != null) {
                  lastFailedWatchdogCheck.set(failed);
               }

               if (lastFailedWatchdogCheckTime != null) {
                  lastFailedWatchdogCheckTime.set(System.currentTimeMillis());
               }
            }

            ThreadUtils.sleep(WATCHDOG_CHECK_INTERVAL, TimeUnit.NANOSECONDS);
         }

         log.info("watchdog checker exiting");
      }

      private Set<String> runChecks() {
         long nowInNs = System.nanoTime();

         Set<String> failed = new LinkedHashSet<>();
         for (WatchdogCheck check : checks) {
            try {
               boolean result = check.check(nowInNs);
               if (!result) {
                  log.warn("watchdog check {} failed", check.name());
                  failed.add(check.name());
               }
            } catch (WatchdogCheckShutdown ex) {
               checks.remove(check);
            } catch (Exception ex) {
               log.warn("watchdog check {} failed:", check.name(),ex);
               failed.add(check.name());
            }
         }

         return failed;
      }
   }
}

