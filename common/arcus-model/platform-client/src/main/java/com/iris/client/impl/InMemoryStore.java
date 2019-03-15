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
package com.iris.client.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.client.event.Listener;
import com.iris.client.event.ListenerList;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.Model;
import com.iris.client.model.ModelAddedEvent;
import com.iris.client.model.ModelChangedEvent;
import com.iris.client.model.ModelDeletedEvent;
import com.iris.client.model.ModelEvent;
import com.iris.client.model.Store;

public class InMemoryStore<M extends Model> implements Store<M> {
   private static final Logger logger = LoggerFactory.getLogger(InMemoryStore.class);
   
   private final ListenerList<ModelEvent> listenerList = new ListenerList<>();
   private final Map<String, M> models = new ConcurrentHashMap<>();
   // create unmodifiable version of the variable.
   
   public InMemoryStore() {
   }
   
   public int size() {
      return models.size();
   }

	@Override
	public void add(M m) {
	   String id = m.getId();
	   if(id == null) {
	      logger.warn("Can't add model without an id");
	      return;
	   }
	   
	   // always update the existing version if its in here,
	   // this is the one that everyone added listeners to
	   // if the ModelCache is working right these should be the
	   // same instance anyway, and this should be a no-op
		M model = models.get(id);
		
		if (model != null) {
			logger.debug("Updating model {}", m.getId());

			Map<String, Object> attributes = m.toMap();
			model.updateAttributes(attributes);
			// we really have no idea what changed...
			fireEvent(new ModelChangedEvent(model, attributes));
		} else {
			logger.debug("Added model {}", m.getId());

			models.put(m.getId(), m);
			fireEvent(new ModelAddedEvent(m));
		}
	}

   @Override
   public M get(String s) {
       return(models.get(s));
   }

   @Override
   public void remove(M m) {
      if(m == null || m.getId() == null) {
         logger.debug("Can't remove null entry");
         return;
      }
      Model old = models.remove(m.getId());
      if(old != null) {
         fireEvent(new ModelDeletedEvent(old));
      }
   }

	/**
	 * Updates, or adds if the item in the {@code Collection<M> ms} was not previously
	 * in the store, and triggers a ModelChanged (or Added) event.
	 */
	@Override
	public void update(Collection<M> models) {
      for (M m : models) {
         // TODO rename add to update?
         add(m);
     }
	}

   @Override
   public void replace(Collection<M> models) {
   	clear();
      update(models);
   }

   @Override
   public void clear() {
      Iterator<M> it = models.values().iterator();
      while(it.hasNext()) {
         remove(it.next());
      }
   }

   @Override
   public Iterable<M> values() {
   	 // keep unmodifiable copy of the map to return with.
   	 // so clients can't remove
      return(models.values());
   }

   @Override
   public Iterable<M> values(Comparator<M> mComparator) {
      List<M> sortedValues = new ArrayList<>(models.values());
      Collections.sort(sortedValues, mComparator);

      return sortedValues;
   }

   @Override
   public ListenerRegistration addListener(Listener<? super ModelEvent> listener) {
      return listenerList.addListener(listener);
   }

   @Override
   public <E extends ModelEvent> ListenerRegistration addListener(Class<E> eClass, Listener<? super E> listener) {
      return listenerList.addListener(eClass, listener);
   }

   protected void fireEvent(ModelEvent event) {
      listenerList.fireEvent(event);
   }
}

