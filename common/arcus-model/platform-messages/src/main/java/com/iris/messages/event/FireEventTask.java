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
/**
 * 
 */
package com.iris.messages.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends an event to a listener, usually used
 * to run on a different thread or at a future
 * point in time.
 */
public class FireEventTask<E> implements Runnable {
   private static final Logger logger = LoggerFactory.getLogger(FireEventTask.class);
   
   public static <E> FireEventTask<E> create(E event, Listener<? super E> listener) {
      return new FireEventTask<E>(event, listener);
   }
   
   private final E event;
   private final Listener<? super E> listener;
   
   protected FireEventTask(E event, Listener<? super E> listener) {
      this.event = event;
      this.listener = listener;
   }

   @Override
   public void run() {
      try {
         listener.onEvent(event);
      }
      catch(Exception e) {
         logger.warn("Unable to notify listener [{}] of event [{}]", listener, event);
      }
   }
}

