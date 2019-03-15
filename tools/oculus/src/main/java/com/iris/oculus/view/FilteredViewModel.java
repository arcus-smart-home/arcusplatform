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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.iris.client.event.Listener;
import com.iris.client.event.ListenerList;
import com.iris.client.event.ListenerRegistration;

/**
 * 
 */
public class FilteredViewModel<M> implements ViewModel<M> {
   private ViewModel<M> delegate;
   private Predicate<? super M> filter;
   private Comparator<? super M> comparator;
   private List<M> view;
   private ListenerList<ViewModelEvent> listeners = new ListenerList<>();
   
   /**
    * 
    */
   public FilteredViewModel(ViewModel<M> delegate) {
      this.delegate = delegate;
      this.delegate.addViewListener(new ViewListener());
      this.rebuildView();
   }

   @Override
   public Iterator<M> iterator() {
      return view.iterator();
   }

   @Override
   public M get(int index) {
      return view.get(index);
   }

   @Override
   public int indexOf(M model) {
      return view.indexOf(model);
   }

   @Override
   public int size() {
      return view.size();
   }

   @Override
   public M findBy(Predicate<M> predicate) {
      for(M model: this) {
         if(predicate.test(model)) {
            return model;
         }
      }
      return null;
   }

   @Override
   public ListenerRegistration addViewListener(Listener<? super ViewModelEvent> listener) {
      return listeners.addListener(listener);
   }

   public void filterBy(Predicate<? super M> predicate) {
      this.filter = predicate;
      rebuildView();
   }
   
   public void sortBy(Comparator<? super M> comparator) {
      this.comparator = comparator;
      rebuildView();
   }
   
   protected void rebuildView() {
      Stream<M> stream = StreamSupport.stream(delegate.spliterator(), false);
      if(filter != null) {
         stream = stream.filter(filter);
      }
      if(comparator != null) {
         stream = stream.sorted(comparator);
      }
      this.view = stream.collect(Collectors.toList());
      this.listeners.fireEvent(ViewModelEvent.changed());
   }

   private class ViewListener implements Listener<ViewModelEvent> {

      @Override
      public void onEvent(ViewModelEvent event) {
         // TODO make this smarter
         rebuildView();
      }
      
   }

}

