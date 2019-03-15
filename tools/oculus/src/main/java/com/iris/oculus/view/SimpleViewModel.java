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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import com.iris.client.event.Listener;
import com.iris.client.event.ListenerList;
import com.iris.client.event.ListenerRegistration;

/**
 *
 */
public class SimpleViewModel<M> implements ViewModel<M> {
   private List<M> models;
   private ListenerList<ViewModelEvent> listeners = new ListenerList<>();

   public SimpleViewModel() {
      this(null);
   }

   public SimpleViewModel(Collection<M> models) {
      this.models = models != null ? new ArrayList<>(models) : new ArrayList<>();
   }

   List<M> models() {
      return models;
   }

   @Override
   public Iterator<M> iterator() {
      Iterator<M> delegate = models.iterator();
      return new Iterator<M>() {
         int index = -1;

         @Override
         public boolean hasNext() {
            return delegate.hasNext();
         }

         @Override
         public M next() {
            index++;
            return delegate.next();
         }

         @Override
         public void remove() {
            delegate.remove();
            fireEvent(ViewModelEvent.removed(index));
         }
      };
   }

   @Override
   public M get(int index) {
      if (index < 0 || index >= models.size()) return null;
      return models.get(index);
   }

   public void add(M e) {
      models.add(e);
      fireEvent(ViewModelEvent.added(models.size() - 1));
   }

   public void add(int index, M value) {
      models.add(index, value);
      fireEvent(ViewModelEvent.added(index));
   }

   public void update(int index, M value) {
      models.set(index, value);
      fireEvent(ViewModelEvent.updated(index));
   }

   public void remove(int index) {
      models.remove(index);
      fireEvent(ViewModelEvent.removed(index));
   }

   public void addAll(Collection<? extends M> c) {
      int index = models.size();
      models.addAll(c);
      fireEvent(ViewModelEvent.added(index, c.size()));
   }

   public void addAll(int index, Collection<? extends M> c) {
      models.addAll(index, c);
      fireEvent(ViewModelEvent.added(index, c.size()));
   }

   public void removeAll() {
      int size = models.size();
      if (size > 0) {
         models.clear();
         fireEvent(ViewModelEvent.removed(0, size));
      }
   }
   
   public void replaceAll(Collection<? extends M> c) {
      models().clear();
      models().addAll(c);
      fireEvent(ViewModelEvent.changed());
   }

   public int indexOf(M model) {
      return models.indexOf(model);
   }

   @Override
   public M findBy(Predicate<M> predicate) {
      for(M model: models) {
         if(predicate.test(model)) {
            return model;
         }
      }
      return null;
   }

   public int size() {
      return models.size();
   }

   @Override
   public ListenerRegistration addViewListener(Listener<? super ViewModelEvent> listener) {
      return listeners.addListener(listener);
   }

   protected void fireEvent(ViewModelEvent event) {
      listeners.fireEvent(event);
   }
}

