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
package com.iris.agent;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.boot.BootUtils;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.logging.IrisAgentLogging;
import com.iris.agent.watchdog.WatchdogCheck;
import com.iris.agent.watchdog.WatchdogService;
import com.iris.bootstrap.BootstrapException;

public final class IrisAgent implements Callable<Integer> {
   private static final Logger log = LoggerFactory.getLogger(IrisAgent.class);
   private long startTime;

   private IrisAgent() {
      this.startTime = System.nanoTime();
   }

   private void initialize(String[] args) throws Exception {
      log.info("iris agent bootstrapping...");

      Collection<String> configs = new LinkedList<String>();
      for (int i = 1; i < args.length; ++i) {
         String confDir = args[i];
         log.info("iris agent loading configuration files from: {}", confDir);

         File confPath = new File(confDir);
         try {
            String[] children = confPath.list();
            for (String child : children) {
               if (child.endsWith(".conf")) {
                  configs.add(confDir + "/" + child);
               }
            }
         } catch (Throwable th) {
            log.debug("failure adding config files during initialization: {}", th.getMessage(), th);
         }
      }

      BootUtils.initialize(new File(args[0]), configs);
      log.info("iris agent bootstrap complete");
   }

   @Override
   public Integer call() throws Exception {
      log.info("iris agent services started.");

      long elapsed = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
      log.info("iris agent startup took {}ms", elapsed);

      // Wait here until notified to shutdown by the
      // system. This prevents the agent from exiting
      // while processes are still running.
      IrisHal.waitForShutdown();
      log.info("iris agent shut down");
      return 0;
   }

   public static void main(String[] args) {
      try {
         IrisAgentLogging.setupInitialLogging();

         StartupWatchdog watchdog = new StartupWatchdog();
         WatchdogService.start();
         WatchdogService.addWatchdogCheck(watchdog);

         IrisAgent main = new IrisAgent();
         main.initialize(args);
         watchdog.done();

         main.call();
      } catch (BootstrapException ex) {
         log.warn("iris agent bootstrap failed: {}", ex, ex);
      } catch (Throwable th) {
         log.warn("iris agent exited abnormally: {}", th, th);
      } finally {
         WatchdogService.shutdown();
      }

      IrisHal.restart();
   }

   private static final class StartupWatchdog implements WatchdogCheck {
      private static final long endWatchdogTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(5L, TimeUnit.MINUTES);
      private static final AtomicBoolean done = new AtomicBoolean(false);

      void done() {
         done.set(true);
      }

      @Override
      public String name() {
         return "startup";
      }

      @Override
      public boolean check(long nowInNs) {
         if (done.get()) {
            return WatchdogService.stopWatchdogCheck();
         }

         return nowInNs < endWatchdogTime;
      }
   }
}

