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
package com.iris.driver.handler;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.driver.DeviceDriverContext;

/**
 *
 */
public class ContextualEventHandlers {
   private static final Logger LOGGER = LoggerFactory.getLogger(ContextualEventHandlers.class);

   protected ContextualEventHandlers() {

   }

   @SafeVarargs
   public static <T> ContextualEventHandler<T> marshalDispatcher(final ContextualEventHandler<? super T>... handlers) {
      return marshalDispatcher(Arrays.asList(handlers));
   }

   public static <T> ContextualEventHandler<T> marshalDispatcher(final Iterable<? extends ContextualEventHandler<? super T>> handlers) {
      return new ContextualEventHandler<T>() {
         @Override
         public boolean handleEvent(DeviceDriverContext context, T event) throws Exception {
            return dispatch(handlers, context, event);
         }

         @Override
         public String toString() {
            return "ContextualEventDispatcher [dispatchers=" + handlers + "]";
         }
      };
   }

   public static <T> ContextualEventHandler<T> marshalSender(final Iterable<? extends ContextualEventHandler<? super T>> handlers) {
      return new ContextualEventHandler<T>() {
         @Override
         public boolean handleEvent(DeviceDriverContext context, T event) throws Exception {
            return send(handlers, context, event);
         }


         @Override
         public String toString() {
            return "ContextualEventSender [listeners=" + handlers + "]";
         }
      };
   }

   /**
    * Send the event to the first handler in the chain.  If that handler
    * returns {@code false} then the next handler is tried.  If no
    * handler returns {@code true} then the return value will be
    * false.  If any handler throws an exception that will be thrown
    * from this method.
    * @param handlers
    * @param context
    * @param event
    * @return
    * @throws Exception
    */
   public static <T> boolean dispatch(Iterable<? extends ContextualEventHandler<? super T>> handlers, DeviceDriverContext context, T event) throws Exception {
      for(ContextualEventHandler<? super T> handler: handlers) {
         if(handler == null) {
            continue;
         }
         if(handler.handleEvent(context, event)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Sends the event to each handler in the list.  Any exceptions will be logged, the
    * event will always be sent to every handler regardless of return value.
    * @param handlers
    * @param context
    * @param event
    */
   // TODO add an UncaughtExceptionHandler and log in the driver's context
   public static <T> boolean send(Iterable<? extends ContextualEventHandler<? super T>> handlers, DeviceDriverContext context, T event) {
      boolean anyHandlers = false;
      for(ContextualEventHandler<? super T> handler: handlers) {
         if(handler == null) {
            continue;
         }
         try {
            handler.handleEvent(context, event);
            anyHandlers = true;
         }
         catch(Exception e) {
            LOGGER.warn("Unhandled error while dispatching event [{}] to [{}]", event, handler, e);
         }
      }
      return anyHandlers;
   }

   @SuppressWarnings("unchecked")
   public static <T> ContextualEventHandler<T> alwaysFalse() {
      return (ContextualEventHandler<T>) AlwaysFalseHandler.INSTANCE;
   }

   private enum AlwaysFalseHandler implements ContextualEventHandler<Object> {
      INSTANCE;

      @Override
      public boolean handleEvent(DeviceDriverContext context, Object event) throws Exception {
         return false;
      }

   }

}

