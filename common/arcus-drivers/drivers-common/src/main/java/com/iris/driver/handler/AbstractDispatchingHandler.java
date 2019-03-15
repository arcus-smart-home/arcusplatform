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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.util.IrisCollections;

/**
 * A base class for building event handlers that dispatch
 * based on some attribute of the event.
 */
public abstract class AbstractDispatchingHandler<T> {
   private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDispatchingHandler.class);
   protected static final String WILDCARD = "";

   private Map<String, ContextualEventHandler<? super T>> handlers;

   protected AbstractDispatchingHandler(Map<String, ContextualEventHandler<? super T>> handlers) {
      this.handlers = IrisCollections.unmodifiableCopy(handlers);
   }

   protected boolean deliver(String key, DeviceDriverContext context, T event) throws Exception {
      ContextualEventHandler<? super T> delegate = handlers.get(key);
      if(delegate != null && delegate.handleEvent(context, event)) {
         return true;
      }
      return false;
   }

   // debugging helpers

   public boolean hasAnyHandlers() {
      return !handlers.isEmpty();
   }

   public boolean hasWildcardHandler() {
      return handlers.get(WILDCARD) != null;
   }

   protected abstract static class Builder<T, A extends AbstractDispatchingHandler<T>> {
      private Map<String, List<ContextualEventHandler<? super T>>> handlers =
            new HashMap<String, List<ContextualEventHandler<? super T>>>();

      protected void doAddHandler(String key, ContextualEventHandler<? super T> handler) {
         key = key == null ? WILDCARD : key;
         List<ContextualEventHandler<? super T>> h = handlers.get(key);
         if(h == null) {
            h = new ArrayList<ContextualEventHandler<? super T>>();
            handlers.put(key, h);
         }
         h.add(handler);
      }

      protected abstract A create(Map<String, ContextualEventHandler<? super T>> handlers);

      /**
       * Marshal all the handlers for a given key, default is to
       * use {@link ContextualEventHandlers#marshalDispatcher(Iterable)}.
       * @param handlers
       * @return
       */
      protected ContextualEventHandler<T> marshal(Iterable<ContextualEventHandler<? super T>> handlers) {
         return ContextualEventHandlers.marshalDispatcher(handlers);
      }

      public boolean hasAnyHandlers() {
         return !handlers.isEmpty();
      }

      public boolean hasWildcardHandler() {
         return handlers.get(WILDCARD) != null;
      }

      public A build() {
         Map<String, ContextualEventHandler<? super T>> rval = new HashMap<String, ContextualEventHandler<? super T>>(handlers.size());
         for(Map.Entry<String, List<ContextualEventHandler<? super T>>> e: handlers.entrySet()) {
            List<ContextualEventHandler<? super T>> value = e.getValue();
            if(value == null || value.isEmpty()) {
               // this is weird
               LOGGER.warn("Ran into empty handler for key [{}]", e.getKey());
            }
            if(value.size() == 1) {
               rval.put(e.getKey(), value.get(0));
            }
            else {
               rval.put(e.getKey(), marshal(value));
            }
         }
         return create(rval);
      }
   }


}

