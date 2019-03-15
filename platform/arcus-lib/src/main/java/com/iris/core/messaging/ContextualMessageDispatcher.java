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
package com.iris.core.messaging;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;

/**
 * 
 */
public class ContextualMessageDispatcher<C, T> implements ContextualMessageListener<C, T> {
   private static Logger logger = LoggerFactory.getLogger(ContextualMessageDispatcher.class);
   
   public static <C, T> Builder<C, T> builder() {
      return new Builder<>();
   }
   
   private final List<Entry<C, T>> entries;
   
   private ContextualMessageDispatcher(List<Entry<C, T>> entries) {
      this.entries = entries;
   }

   @Override
   public void onMessage(C context, T message) {
      this.entries.forEach((entry) -> entry.onMessage(context, message));
   }

   public static class Builder<C, T> {
      private ImmutableList.Builder<Entry<C, T>> entries = ImmutableList.builder();
      
      public Builder<C, T> addListener(ContextualMessageListener<? super C, ? super T> listener) {
         return addListener(Predicates.alwaysTrue(), listener);
      }
      
      public Builder<C, T> addListener(Predicate<? super T> predicate, ContextualMessageListener<? super C, ? super T> listener) {
         entries.add(Entry.of(predicate, listener));
         return this;
      }
      
      public ContextualMessageDispatcher<C, T> build() {
         return new ContextualMessageDispatcher<>(entries.build());
      }
   }
   
   private static class Entry<C, T> implements ContextualMessageListener<C, T> {
      private static <C, T> Entry<C, T> of(Predicate<? super T> predicate, ContextualMessageListener<? super C, ? super T> listener) {
         return new Entry<C, T>(predicate, listener);
      }
      
      private final Predicate<? super T> predicate;
      private final ContextualMessageListener<? super C, ? super T> listener;
      
      private Entry(
            Predicate<? super T> predicate,
            ContextualMessageListener<? super C, ? super T> listener
      ) {
         this.predicate = predicate;
         this.listener = listener;
      }

      @Override
      public void onMessage(C context, T message) {
         try {
            if(predicate.apply(message)) {
               listener.onMessage(context, message);
            }
         }
         catch(Exception e) {
            logger.warn("Error notifying listener [{}] of message [{}]", listener, message, e);
         }
      }
      
   }
}

