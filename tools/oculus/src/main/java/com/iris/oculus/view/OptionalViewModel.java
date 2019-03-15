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
package com.iris.oculus.view;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;
import com.iris.oculus.view.ViewModelEvent.ViewModelAddedEvent;
import com.iris.oculus.view.ViewModelEvent.ViewModelChangedEvent;
import com.iris.oculus.view.ViewModelEvent.ViewModelRemovedEvent;
import com.iris.oculus.view.ViewModelEvent.ViewModelUpdatedEvent;

/**
 * 
 */
public class OptionalViewModel<M> implements ViewModel<Optional<M>> {
   private static final List<Optional<?>> singleton = ImmutableList.of(Optional.empty());
   
   private ViewModel<M> delegate;
   
   /**
    * 
    */
   public OptionalViewModel(ViewModel<M> delegate) {
      this.delegate = delegate;
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   private List<Optional<M>> singleton() {
      return (List) singleton;
   }

   @Override
   public Iterator<Optional<M>> iterator() {
      return Iterators.concat(singleton().iterator(), Iterators.transform(delegate.iterator(), Optional::of));
   }

   @Override
   public Optional<M> get(int index) {
      return index == 0 ? Optional.empty() : Optional.of(delegate.get(index - 1));
   }

   @Override
   public int indexOf(Optional<M> model) {
      if(model.isPresent()) {
         int index = delegate.indexOf(model.get());
         return index < 0 ? index : index + 1;
      }
      else {
         return 0;
      }
   }

   @Override
   public int size() {
      return delegate.size() + 1;
   }

   @Override
   public Optional<M> findBy(Predicate<Optional<M>> predicate) {
      for(Optional<M> model: this) {
         if(predicate.test(model)) {
            return model;
         }
      }
      return null;
   }

   @Override
   public ListenerRegistration addViewListener(Listener<? super ViewModelEvent> listener) {
      return delegate.addViewListener(new Listener<ViewModelEvent>() {
         @Override
         public void onEvent(ViewModelEvent event) {
            listener.onEvent(translate(event));
         }

         private ViewModelEvent translate(ViewModelEvent event) {
            if(event == null) {
               return null;
            }
            if(event instanceof ViewModelAddedEvent) {
               return ViewModelEvent.added(event.getStart() + 1, event.getEnd() - event.getStart());
            }
            if(event instanceof ViewModelChangedEvent) {
               return ViewModelEvent.changed();
            }
            if(event instanceof ViewModelRemovedEvent) {
               return ViewModelEvent.removed(event.getStart() + 1, event.getEnd() - event.getStart());
            }
            if(event instanceof ViewModelUpdatedEvent) {
               return ViewModelEvent.updated(event.getStart() + 1, event.getEnd() - event.getStart());
            }
            throw new IllegalArgumentException("Unsupported event type " + event.getClass());
         }

         @Override
         public int hashCode() {
            return listener.hashCode();
         }

         @Override
         public boolean equals(Object obj) {
            return listener.equals(obj);
         }

         @Override
         public String toString() {
            return "OptionalListener " + listener;
         }
      });
   }

}

