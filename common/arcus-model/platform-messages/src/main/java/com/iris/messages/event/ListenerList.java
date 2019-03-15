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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.MoreExecutors;
import com.iris.util.Subscription;

/**
 *
 */
public class ListenerList<T> {
   private static final Logger logger = LoggerFactory.getLogger(ListenerList.class);
   
   private final Set<Listener<? super T>> listeners = new LinkedHashSet<>();
   private final Executor executor;
   
   public ListenerList() {
      this(null);
   }
   
   public ListenerList(Executor executor) {
      if(executor == null) {
         this.executor = MoreExecutors.directExecutor();
      }
      else {
         this.executor = executor;
      }
   }
   
   @SuppressWarnings("unchecked")
   public <V extends T> Subscription addListener(final Class<V> type, final Listener<? super V> listener) {
      return addListener(new Listener<T>() {
         @Override
         public void onEvent(T event) {
            if (event != null && type.isAssignableFrom(event.getClass())) {
               listener.onEvent((V) event);
            }
         }
      });
   }
   
   public Subscription addListener(Listener<? super T> listener) {
      synchronized(listeners) {
         listeners.add(listener);
      }
      return new ListenerRegistrationImpl(listener);
   }
   
   public boolean hasListeners() {
      synchronized(listeners) {
         return !listeners.isEmpty();
      }
   }
   
   public void fireEvent(T event) {
	   NotifierTask<T> task;
	   synchronized(this.listeners) {
		   if(this.listeners.isEmpty()) {
			   return;
		   }
	      // copy out for dispatch, this allows new event listeners to be added/removed while
	      // we're notifying about the event
		   task = new NotifierTask<T>(event, new ArrayList<Listener<? super T>>(this.listeners)); 
	   }
	   executor.execute(task);
   }
   
   private class ListenerRegistrationImpl implements Subscription {
      private Listener<? super T> listener;
      
      private ListenerRegistrationImpl(Listener<? super T> listener) {
         this.listener = listener;
      }
      
      public void remove() {
         synchronized(listeners) {
            listeners.remove(listener);
         }
      }
   }
   
   private static class NotifierTask<E> implements Runnable {
      private final E event;
      private final List<Listener<? super E>> listeners;
      
      NotifierTask(E event, List<Listener<? super E>> listeners) {
         this.event = event;
         this.listeners = listeners;
      }
      
      public void run() {
         for(Listener<? super E> l: listeners) {
            try {
               l.onEvent(event);
            }
            catch(Exception e) {
               logger.warn("Error dispatching event [{}] to [{}]", event, l, e);
            }
         }
      }
   }

}

