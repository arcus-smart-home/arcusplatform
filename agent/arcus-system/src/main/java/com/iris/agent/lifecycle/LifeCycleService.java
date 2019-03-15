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
package com.iris.agent.lifecycle;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LifeCycleService {
   private static final Logger log = LoggerFactory.getLogger(LifeCycleService.class);
   private static final Object LOCK = new Object();
   private static final ExecutorService exec = Executors.newFixedThreadPool(1, new LifeCycleThreadFactory());

   private static final CopyOnWriteArraySet<LifeCycleListener> listeners = new CopyOnWriteArraySet<>();
   private static LifeCycle current = LifeCycle.INITIAL;

   public enum Reset {
      FACTORY,
      SOFT
   }

   private LifeCycleService() {
   }

   public static void start() {
      listeners.clear();
   }

   public static void shutdown() {
      listeners.clear();
   }

   public static LifeCycle getState() {
      return current;
   }

   public static void setState(LifeCycle newState) {
       LifeCycle oldState;

      synchronized (LOCK) {
         if (current == newState) {
            return;
         }

         oldState = current;
         current = newState;
      }

      fireStateUpdate(oldState, current);
   }

   public static void setState(LifeCycle fromState, LifeCycle newState) {
       LifeCycle oldState;

      synchronized (LOCK) {
         if (current == newState || current != fromState) {
            return;
         }

         oldState = current;
         current = newState;
      }

      fireStateUpdate(oldState, current);
   }

   public static boolean isShutdown() {
      return current == LifeCycle.SHUTTING_DOWN;
   }

   public static boolean isConnected() {
      return isConnectedState(current);
   }

   public static boolean isAuthorized() {
      return isAuthorizedState(current);
   }

   public static boolean isConnectedState(LifeCycle state) {
      return state == LifeCycle.CONNECTED ||
            state == LifeCycle.AUTHORIZED;
   }

   public static boolean isAuthorizedState(LifeCycle state) {
      return state == LifeCycle.AUTHORIZED;
   }

   public static boolean isNewConnectedState(LifeCycle oldState, LifeCycle newState) {
      return !isConnectedState(oldState) && isConnectedState(newState);
   }

   public static boolean isNewDisconnectedState(LifeCycle oldState, LifeCycle newState) {
      return isConnectedState(oldState) && !isConnectedState(newState);
   }

   public static void addListener(LifeCycleListener listener) {
      listeners.add(listener);
      listener.lifeCycleStateChanged(current,current);
   }

   public static void removeListener(LifeCycleListener listener) {
      listeners.remove(listener);
   }

   public static Future<?> fireHubRegistered(@Nullable final UUID oldAcc, @Nullable final UUID newAcc) {
      return exec.submit(new Runnable() {
         @Override
         public void run() {
            log.info("hub account info updated: {} -> {}", oldAcc, newAcc);
            for (LifeCycleListener listener : listeners) {
               try {
                  listener.hubAccountIdUpdated(oldAcc, newAcc);
               } catch (Throwable th) {
                  log.warn("exception while processing hub registered {}: {}", listener, th.getMessage(), th);
               }
            }
         }
      });
   }

   public static Future<?>fireHubReset(final Reset reset) {
      return exec.submit(new Runnable() {
         @Override
         public void run() {
            log.warn("processing hub reset: {}", reset.toString().toLowerCase());
            for (LifeCycleListener listener : listeners) {
               try {
                  listener.hubReset(reset);
               } catch (Throwable th) {
                  log.warn("exception while processing hub reset on {}: {}", listener, th.getMessage(), th);
               }
            }
         }
      });
   }

   public static Future<?> fireHubDeregistered() {
      return exec.submit(new Runnable() {
         @Override
         public void run() {
            log.warn("processing hub deregistered event");
            for (LifeCycleListener listener : listeners) {
               try {
                  log.debug("attempting hub deregistered: {}", listener);
                  listener.hubDeregistered();
                  log.debug("attempting hub deregistered done: {}", listener);
               } catch (Throwable th) {
                  log.warn("exception while processing hub removed on {}: {}", listener, th.getMessage(), th);
               }
            }
         }
      });
   }

   private static Future<?> fireStateUpdate(final LifeCycle oldState, final LifeCycle newState) {
      return exec.submit(new Runnable() {
         @Override
         public void run() {
            log.info("hub state changed from {} to {}", str(oldState), str(newState));
            for (LifeCycleListener listener : listeners) {
               try {
                  listener.lifeCycleStateChanged(oldState, newState);
               } catch (Throwable th) {
                  log.warn("exception while processing hub state update on {}: {}", listener, th.getMessage(), th);
               }
            }
         }
      });
   }

   private static final class LifeCycleThreadFactory implements ThreadFactory {
      private final AtomicLong next = new AtomicLong();

      @Override
      public Thread newThread(@Nullable Runnable runnable) {
         Thread thr = new Thread(runnable);
         thr.setDaemon(true);
         thr.setName("ilcs" + next.getAndIncrement());

         return thr;
      }

   }

   private static String str(LifeCycle state) {
      String str = state.toString();
      return str.toLowerCase().replace('_', ' ');
   }
}

