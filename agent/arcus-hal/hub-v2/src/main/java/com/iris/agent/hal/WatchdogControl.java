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
package com.iris.agent.hal;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.os.watchdog.WatchdogNative;
import com.iris.agent.util.ThreadUtils;
import com.iris.messages.capability.HubAdvancedCapability;

class WatchdogControl {
   private static final Logger log = LoggerFactory.getLogger(WatchdogControl.class);
   private static final long WATCHDOG_EXPIRES_SECONDS = (System.getenv("IRIS_AGENT_WATCHDOG_TIMEOUT") != null)
      ? Math.max(95,Long.parseLong(System.getenv("IRIS_AGENT_WATCHDOG_TIMEOUT")))
            : 300;
   private static final long WATCHDOG_CHECK_SECONDS = 10;

   private @Nullable static Watchdog hwWd;
   private @Nullable static Watchdog swWd;

   private WatchdogControl() {
   }

   public static final WatchdogControl create() {
      return new WatchdogControl();
   }

   public synchronized void start(long maxWatchdog) {
      // Create hardware watchdog, if supported
      try {
         shutdown(false);
         hwWd = new HardwareWatchdog(maxWatchdog);
      } catch (IOException ex) {
         log.warn("could not open hardware watchdog, not available for this hardware", ex);
         hwWd = null;
      }

      // Setup software watchdog
      swWd = new SoftwareWatchdog();
   }

   public synchronized void shutdown(boolean clean) {
      if (swWd != null) {
         swWd.shutdown(clean);
      }
      swWd = null;

      // Clean up hardware watchdog as well
      if (hwWd != null) {
         hwWd.shutdown(clean);
      }
      hwWd = null;
   }

   public void poke() {
      // Use software watchdog for general watchdog
      if (swWd != null) {
         swWd.poke();
      }
   }

   private interface Watchdog {
      void poke();
      void shutdown(boolean clean);
   }

   private static final class HardwareWatchdog implements Watchdog {
      private final FileOutputStream os;

      HardwareWatchdog(long maxWatchdog) throws IOException {
         this.os = new FileOutputStream("/dev/watchdog0");
         this.os.write('A');
         this.os.flush();

         if (WatchdogNative.isAvailable()) {
            try {
               int dto  = WatchdogNative.getWatchdogTimeout(this.os);
               // If there is a hardware limit on the watchdog period, reset current value
               WatchdogNative.setWatchdogTimeout(this.os, (int)maxWatchdog);
               int nto = WatchdogNative.getWatchdogTimeout(this.os);

               Object info = WatchdogNative.getWatchdogInfo(this.os);
               String id = WatchdogNative.getWatchdogInfoIdentity(info);

               log.warn("hardware watchdog implementation: {}", id);
               log.warn("hardware watchdog timeouts: default={}, updated={}", dto, nto);
               log.warn("hardware watchdog flags: {}", WatchdogNative.getWatchdogInfoFlags(info));
               log.warn("hardware watchdog temp: {}", WatchdogNative.getWatchdogTemp(this.os));
            } catch (Throwable th) {
               log.warn("could not setup watchdog settings, continuing with defaults:", th);
            }
         }
      }

      @Override
      public void poke() {
         try {
            os.write('A');
            os.flush();
         } catch (IOException ex) {
            log.warn("could not poke watchdog, reboot may occur:", ex);
         }
      }

      @Override
      public void shutdown(boolean clean) {
         // NOTE: The V2 hub does not actually shut the HW watchdog off here (which
         //       could be done by writing a 'V' into the watchdog file. We leave
         //       the watchdog so that the hub will reboot if the agent isn't restarted
         //       for any reason.
         poke();
         IOUtils.closeQuietly(os);
      }
   }

   private static final class SoftwareWatchdog implements Watchdog, Runnable {
      private final Thread thr;
      private boolean running;
      private long lastPokeTime;

      SoftwareWatchdog() {
         this.thr = new Thread(this);
         this.thr.setName("wtdg");
         this.thr.setDaemon(true);

         this.running = true;
         this.lastPokeTime = System.nanoTime();
         this.thr.start();
         log.warn("software watchdog timeout: {}", WATCHDOG_EXPIRES_SECONDS);
      }

      @Override
      public void poke() {
         if (running) {
            this.lastPokeTime = System.nanoTime();
         }
      }

      @Override
      public void shutdown(boolean clean) {
         poke();
         running = false;
      }

      @Override
      public void run() {
         try {
            while (running) {
               ThreadUtils.sleep(WATCHDOG_CHECK_SECONDS, TimeUnit.SECONDS);

               long now = System.nanoTime();
               long elapsedSeconds = TimeUnit.SECONDS.convert(now-lastPokeTime, TimeUnit.NANOSECONDS);
               if (elapsedSeconds >= WATCHDOG_EXPIRES_SECONDS) {
                  log.error("SOFTWARE WATCHDOG TIMEOUT EXPIRED, ATTEMPTING TO FORCE REBOOT OF SYSTEM");

                  running = false;
                  HubAttributesService.setLastRestartReason(HubAdvancedCapability.LASTRESTARTREASON_WATCHDOG);
                  IrisHal.rebootAndSelfCheck();
               }

               // Kick hardware watchdog which acts as a deadman
               if (hwWd != null) {
                  hwWd.poke();
               }
            }

            log.warn("software watchdog exiting normally");;
         } catch (Throwable th) {
            log.error("SOFTWARE WATCHDOG EXITED ABNORMALLY:", th);
         }
      }
   }
}

