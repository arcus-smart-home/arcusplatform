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
package com.iris.agent.reflex;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReflexProcessor implements ReflexProcessor {
   private static final Logger log = LoggerFactory.getLogger(ReflexController.class);

   private State state = State.INITIAL;

   /////////////////////////////////////////////////////////////////////////////
   // Lifecycle Reflexes
   /////////////////////////////////////////////////////////////////////////////
   
   protected void start(State curState) {
      synchronized (this) {
         this.state = curState;

         // If we are starting the driver in the initial state then run onAdded
         if (curState == State.INITIAL) {
            onAdded();
         }
      }
   }
   
   @Override
   public synchronized State getCurrentState() {
      return state;
   }
   
   @Override
   public synchronized void setCurrentState(State newState) {
      if (Objects.equals(state,newState)) {
         return;
      }

      try {
         log.info("device {} moving from state {} to state {}", getAddress(), state, newState);
         State oldState = state;
         state = newState;

         switch (newState) {
         case INITIAL:
            // ignore
            break;

         case ADDED:
            onConnected();
            break;

         case CONNECTED:
            if (oldState == State.DISCONNECTED) {
               onConnected();
            }
            break;

         case DISCONNECTED:
            if (oldState == State.CONNECTED) {
               onDisconnected();
            }
            break;

         case REMOVED:
            onRemoved();
            break;

         default:
            log.warn("unknown reflex state: {}", newState);
            break;
         }
      } finally {
         ReflexDao.putReflexCurrentState(getAddress(), state);
      }
   }
   
   protected abstract void onAdded();
   protected abstract void onConnected();
   protected abstract void onDisconnected();
   protected abstract void onRemoved();
}

