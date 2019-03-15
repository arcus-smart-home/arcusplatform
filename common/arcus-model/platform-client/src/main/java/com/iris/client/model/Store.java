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
package com.iris.client.model;

import java.util.Collection;
import java.util.Comparator;

import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;

public interface Store<M extends Model> {
   public int size();

   public void add(M model);
   
   public M get(String id);
   
   public void remove(M model);
   
   public void update(Collection<M> model);
   
   public void replace(Collection<M> model);
   
   public void clear();
   
   public Iterable<M> values();
   
   public Iterable<M> values(Comparator<M> sort);
   
   public ListenerRegistration addListener(Listener<? super ModelEvent> listener);
   
   public <E extends ModelEvent> ListenerRegistration addListener(Class<E> type, Listener<? super E> listener); 
}

