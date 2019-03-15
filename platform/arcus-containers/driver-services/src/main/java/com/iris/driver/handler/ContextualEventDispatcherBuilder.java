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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.handler.ContextualEventHandlers;

/**
 *
 */
public class ContextualEventDispatcherBuilder<T> {
   private boolean dispatch = true;
   private Map<PredicateOrTransform<T, ?>, ContextualEventDispatcherBuilder<?>> dispatchers = new LinkedHashMap<>();
   private List<ContextualEventHandler<? super T>> handlers = new ArrayList<>();

   public ContextualEventDispatcherBuilder<T> ifMatches(Predicate<? super T> predicate) {
      return getOrCreateBuilder(key(predicate));
   }

   public <O> ContextualEventDispatcherBuilder<O> transform(Function<? super T, O> transformer) {
      return getOrCreateBuilder(key(transformer));
   }

   public <O extends T> ContextualEventDispatcherBuilder<O> ifInstanceOf(Class<O> type) {
      return
            ifMatches(Predicates.instanceOf(type))
               .transform(cast(type));
   }

   public ContextualEventDispatcherBuilder<T> addHandler(ContextualEventHandler<? super T> handler) {
      handlers.add(handler);
      return this;
   }

   // TODO dispatcher

   public boolean hasHandlers() {
      for(ContextualEventDispatcherBuilder<?> builder: dispatchers.values()) {
         if(builder.hasHandlers()) {
            return true;
         }
      }
      return false;
   }

   public ContextualEventHandler<T> create() {
      ContextualEventHandler<T> handler = create(this);
      if(handler == null) {
         return ContextualEventHandlers.<T>alwaysFalse();
      }
      return handler;
   }

   @SuppressWarnings("rawtypes")
   private <O> ContextualEventDispatcherBuilder<O> getOrCreateBuilder(PredicateOrTransform<T, O> key) {
      ContextualEventDispatcherBuilder builder = dispatchers.get(key);
      if(builder == null) {
         builder = new ContextualEventDispatcherBuilder<O>();
         dispatchers.put(key, builder);
      }
      return builder;
   }

   private static <T> PredicateOrTransform<T, T> key(Predicate<? super T> p) {
      return key(p, null);
   }

   private static <T, O> PredicateOrTransform<T, O> key(Function<? super T, O> t) {
      return key(null, t);
   }

   private static <T, O> PredicateOrTransform<T, O> key(Predicate<? super T> p, Function<? super T, O> t) {
      PredicateOrTransform<T, O> pt = new PredicateOrTransform<>();
      pt.p = p;
      pt.t = t;
      return pt;
   }

   private static <O> Function<Object, O> cast(Class<O> type) {
      return new CastFunction<>(type);
   }

   private static <T> ContextualEventHandler<T> create(ContextualEventDispatcherBuilder<T> builder) {
      List<ContextualEventHandler<? super T>> handlers = new ArrayList<>(builder.dispatchers.size() + 1);
      for(Map.Entry<PredicateOrTransform<T, ?>, ContextualEventDispatcherBuilder<?>> entry: builder.dispatchers.entrySet()) {
         ContextualEventHandler<T> handler = create(entry.getKey(), entry.getValue());
         if(handler != null) {
            handlers.add(handler);
         }
      }
      handlers.addAll(builder.handlers);
      if(handlers.isEmpty()) {
         return null;
      }

      return builder.dispatch ? ContextualEventHandlers.marshalDispatcher(handlers) : ContextualEventHandlers.marshalSender(handlers);
   }

   @SuppressWarnings({ "rawtypes", "unchecked" })
   private static <T, O> ContextualEventHandler<T> create(PredicateOrTransform<T, O> pt, ContextualEventDispatcherBuilder<?> builder) {
      ContextualEventHandler handler = create(builder);
      if(handler == null) {
         return null;
      }

      if(pt.isPredicate()) {
         return new PredicateDispatcher<T>(pt.p, handler);
      }
      else {
         return new TransformerDispatcher<T, O>(pt.t, handler);
      }
   }

   private static class PredicateOrTransform<T, O> {
      private Predicate<? super T> p;
      private Function<? super T, O> t;

      public boolean isPredicate() {
         return p != null;
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((p == null) ? 0 : p.hashCode());
         result = prime * result + ((t == null) ? 0 : t.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) return true;
         if (obj == null) return false;
         if (getClass() != obj.getClass()) return false;
         PredicateOrTransform other = (PredicateOrTransform) obj;
         if (p == null) {
            if (other.p != null) return false;
         }
         else if (!p.equals(other.p)) return false;
         if (t == null) {
            if (other.t != null) return false;
         }
         else if (!t.equals(other.t)) return false;
         return true;
      }

   }

   private static class CastFunction<O> implements Function<Object, O> {
      private final Class<O> type;

      CastFunction(Class<O> type) {
         this.type = type;
      }

      @Override
      public O apply(Object o) {
         return type.cast(o);
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((type == null) ? 0 : type.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) return true;
         if (obj == null) return false;
         if (getClass() != obj.getClass()) return false;
         CastFunction other = (CastFunction) obj;
         if (type == null) {
            if (other.type != null) return false;
         }
         else if (!type.equals(other.type)) return false;
         return true;
      }

   }

   private static class PredicateDispatcher<T> implements ContextualEventHandler<T> {
      private final Predicate<? super T> p;
      private final ContextualEventHandler<? super T> handler;

      PredicateDispatcher(Predicate<? super T> p, ContextualEventHandler<? super T> handler) {
         this.p = p;
         this.handler = handler;
      }

      @Override
      public boolean handleEvent(DeviceDriverContext context, T event) throws Exception {
         if(p.apply(event)) {
            return handler.handleEvent(context, event);
         }
         return false;
      }

   }

   private static class TransformerDispatcher<T, O> implements ContextualEventHandler<T> {
      private final Function<? super T, O> transformer;
      private final ContextualEventHandler<? super O> handler;

      TransformerDispatcher(Function<? super T, O> transformer, ContextualEventHandler<? super O> handler) {
         this.transformer = transformer;
         this.handler = handler;
      }

      @Override
      public boolean handleEvent(DeviceDriverContext context, T event) throws Exception {
         O o = transformer.apply(event);
         return handler.handleEvent(context, o);
      }

   }

}

