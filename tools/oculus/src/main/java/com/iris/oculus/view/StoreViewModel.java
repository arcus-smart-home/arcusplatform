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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.collect.Iterators;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.Model;
import com.iris.client.model.ModelAddedEvent;
import com.iris.client.model.ModelChangedEvent;
import com.iris.client.model.ModelDeletedEvent;
import com.iris.client.model.ModelEvent;
import com.iris.client.model.Store;

/**
 * 
 */
public class StoreViewModel<M extends Model> implements ViewModel<M> {
   private SimpleViewModel<M> delegate = new SimpleViewModel<M>();
   private ListenerRegistration binding;
   
   private Comparator<M> comparator;
   
   public StoreViewModel() {
      
   }
   
   public StoreViewModel(Store<M> store) {
      doBind(store);
   }
   
   public void bind(Store<M> models) {
      doBind(models);
   }
   
   public void unbind() {
      doBind(null);
   }
   
   public Iterator<M> iterator() {
      return Iterators.unmodifiableIterator(delegate.iterator());
   }

   public M get(int index) {
      return delegate.get(index);
   }

   public int indexOf(M model) {
      return delegate.indexOf(model);
   }
   
   public M findBy(Predicate<M> predicate) {
      return delegate.findBy(predicate);
   }
   
   public int size() {
      return delegate.size();
   }

   public ListenerRegistration addViewListener(Listener<? super ViewModelEvent> listener) {
      return delegate.addViewListener(listener);
   }

   private void doBind(Store<M> store) {
      if(binding != null) {
         binding.remove();
         delegate.removeAll();
      }
      if(store != null) {
         binding = store.addListener((event) -> onModelEvent(event));
         List<M> models =  new ArrayList<>(store.size() + 1);
         for(M model: store.values()) {
            models.add(model);
         }
         if(comparator != null) {
            Collections.sort(models, comparator);
         }
         delegate.addAll(models);
      }
   }

   private void onModelEvent(ModelEvent event) {
      M model = (M) event.getModel();
      if(event instanceof ModelAddedEvent) {
         int index = delegate.size();
         if(comparator != null) {
            index = Collections.binarySearch(delegate.models(), model, comparator);
         }
         delegate.add(index, model);
      }
      else if(event instanceof ModelDeletedEvent) {
         int index = delegate.indexOf(model);
         if(index >= 0) {
            delegate.remove(index);
         }
      }
      else if(event instanceof ModelChangedEvent) {
         int index = delegate.indexOf(model);
         if(index >= 0) {
            delegate.fireEvent(ViewModelEvent.updated(index));
         }
      }
   }
}

