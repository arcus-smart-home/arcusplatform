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
package com.iris.agent.fourg;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.exec.ExecService;

public final class FourgService {
   private static final Logger log = LoggerFactory.getLogger(FourgService.class);
   private static final Object LOCK = new Object();

   private static final CopyOnWriteArraySet<FourgListener> listeners = new CopyOnWriteArraySet<>();
   private static State current = State.UNAVAILABLE;

   public enum State {
      UNAVAILABLE,
      CONNECTING,
      CONNECTED,
      AUTHORIZED,
   }

   private FourgService() {
   }

   public static void start() {
      listeners.clear();
   }

   public static void shutdown() {
      listeners.clear();
   }

   public static State getState() {
      return current;
   }

   public static void setState(State newState) {
      State oldState;

      synchronized (LOCK) {
         if (current == newState) {
            return;
         }

         oldState = current;
         current = newState;
      }

      fireStateUpdate(oldState, current);
   }

   public static boolean isAvailable() {
      return isAvailableState(current);
   }

   public static boolean isAvailableState(State state) {
      return state != State.UNAVAILABLE;
   }

   public static boolean isConnected() {
      return isConnectedState(current);
   }

   public static boolean isConnectedState(State state) {
      return state == State.CONNECTED ||
            state == State.AUTHORIZED;
   }

   public static boolean isAuthorized() {
      return isAuthorizedState(current);
   }

   public static boolean isAuthorizedState(State state) {
      return state == State.AUTHORIZED;
   }

   public static boolean isNewConnectedState(State oldState, State newState) {
      return !isConnectedState(oldState) && isConnectedState(newState);
   }

   public static boolean isNewDisconnectedState(State oldState, State newState) {
      return isConnectedState(oldState) && !isConnectedState(newState);
   }

   public static void addListener(FourgListener listener) {
      listeners.add(listener);
      listener.fourgStateChanged(current,current);
   }

   public static void removeListener(FourgListener listener) {
      listeners.remove(listener);
   }

   private static Future<?> fireStateUpdate(final State oldState, final State newState) {
      return ExecService.io().submit(new Runnable() {
         @Override
         public void run() {
            log.info("4g state changed from {} to {}", str(oldState), str(newState));
            for (FourgListener listener : listeners) {
               try {
                  listener.fourgStateChanged(oldState, newState);
               } catch (Throwable th) {
                  log.warn("exception while processing hub state update on {}: {}", listener, th.getMessage(), th);
               }
            }
         }
      });
   }

   private static String str(State state) {
      return String.valueOf(state).toLowerCase();
   }
}

