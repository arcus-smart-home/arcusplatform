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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.iris.client.ClientEvent;
import com.iris.client.ClientMessage;
import com.iris.client.IrisClientFactory;
import com.iris.client.Types;
import com.iris.client.capability.Capability;
import com.iris.client.capability.Capability.AddedEvent;
import com.iris.client.capability.Capability.ReportEvent;
import com.iris.client.capability.Capability.DeletedEvent;
import com.iris.client.capability.Capability.ValueChangeEvent;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerList;

public class ModelCache implements Listener<ClientMessage> {
   private static final Logger logger = LoggerFactory.getLogger(ModelCache.class);
   
   private final ModelFactory factory;
   private final Map<String, Model> models;
   private final ListenerList<ModelEvent> listeners;
   
   public ModelCache(ModelFactory factory) {
      this.factory = factory;
      this.models = new ConcurrentHashMap<String, Model>();
      this.listeners = new ListenerList<ModelEvent>();
   }

   public void clearCache() {
   	Iterator<Model> it = models.values().iterator();
   	while (it.hasNext()) {
   		removeModel(it.next().getAddress());
   	}
   }
   
   public Model get(String id) {
      return models.get(id);
   }

   @Override
   public void onEvent(ClientMessage message) {
      ClientEvent event = message.getEvent();
      if(event instanceof AddedEvent) {
         addModel(message.getSource(), event.getAttributes());
      }
      else if(event instanceof DeletedEvent) {
         removeModel(event.getSourceAddress());
      }
      else if(event instanceof ValueChangeEvent) {
         updateModel(event.getSourceAddress(), event.getAttributes());
      }
      else if(event instanceof Capability.GetAttributesValuesResponseEvent) {
         updateModel(event.getSourceAddress(), event.getAttributes());
      }
      else if(event instanceof ReportEvent) {
         updateModelWithReport(event.getSourceAddress(), event.getAttributes());
      }
   }

   protected void addModel(String source, Map<String, Object> attributes) {
      Model m = models.get(source);
      if(m != null) {
         logger.debug("Received add for existing model, updating {} with attributes {}", m, attributes);
         m.updateAttributes(attributes);
         fireChanged(m, attributes);
      }
      else {
         logger.debug("Creating model {} with attributes {}", source, attributes);
         m = factory.create(attributes, getModelType(attributes), getCapabilityTypes(attributes));
         try {
         	IrisClientFactory.getStore(Types.getModel(m.getType()));
         	// By getting the store, we're adding listeners to it to receive the added event below.
         } catch (Exception e) {
         	logger.debug("Exception trying to init store: [{}]", Types.getModel(m.getType()), e);
         }
         models.put(source, m);
         fireAdded(m);
      }
      
   }

   protected void updateModel(String source, Map<String, Object> attributes) {
      Model m = models.get(source);
      if(m != null) {
         logger.debug("Updating model {} with attributes {}", m, attributes);
         m.updateAttributes(attributes);
         fireChanged(m, attributes);
      }
      else {
         logger.debug("Ignoring update for untracked model {}", m);
      }
   }

   protected void updateModelWithReport(String source, Map<String, Object> attributes) {
      Model m = models.get(source);
      if (m == null) {
         logger.debug("Ignoring update for untracked model {}", m);
         return;
      }

      Map<String,Object> updates = new HashMap<>();
      for (Map.Entry<String,Object> entry : attributes.entrySet()) {
         Object old = m.get(entry.getKey());
         Object cur = entry.getValue();
         boolean eq = (old == null && cur == null) || (cur != null && cur.equals(old));
         if (!eq) {
            updates.put(entry.getKey(), entry.getValue());
         }
      }

      if (!updates.isEmpty()) {
         logger.debug("Updating model {} with attributes {}", m, updates);
         m.updateAttributes(updates);
         fireChanged(m, updates);
      }
   }

   protected void removeModel(String source) {
      Model m = models.remove(source);
      if(m != null) {
         logger.debug("Model {} deleted", m);
         m.onDeleted();
         fireRemoved(m);
      }
      else {
         logger.debug("Ignoring delete for untracked model {}", source);
      }
   }

   protected void fireAdded(Model model) {
      listeners.fireEvent(new ModelAddedEvent(model));
   }
   
   protected void fireChanged(Model model, Map<String, Object> attributes) {
      listeners.fireEvent(new ModelChangedEvent(model, attributes));
   }
   
   protected void fireRemoved(Model model) {
      listeners.fireEvent(new ModelDeletedEvent(model));
   }
   
   private Class<? extends Model> getModelType(Map<String, Object> attributes) {
      String type = (String) attributes.get(Capability.ATTR_TYPE);
      if(type == null) {
         return Model.class;
      }
      return Types.getModel(type);
   }

   @SuppressWarnings("unchecked")
	private List<Class<? extends Capability>> getCapabilityTypes(Map<String, Object> attributes) {
      Collection<String> caps = (Collection<String>) attributes.get(Capability.ATTR_CAPS);
      if (caps != null && !caps.isEmpty()) {
	      List<Class<? extends Capability>> capabilityTypes = new ArrayList<Class<? extends Capability>>(caps.size() + 1);
	      for(String cap: caps) {
	         capabilityTypes.add(Types.getCapability(cap));
	      }
	      return capabilityTypes;
      } else {
      	return Collections.<Class<? extends Capability>>singletonList(Capability.class);
      }
   }

   public void addModelListener(Listener<? super ModelEvent> listener) {
      listeners.addListener(listener);
   }
   
   public Model addOrUpdate(Map<String, Object> model) {
      if(model == null) {
         logger.debug("Ignoring update for empty model");
         return null;
      }
      String source = (String) model.get(Capability.ATTR_ADDRESS);
      if(source == null) {
         logger.debug("Ignoring update with no source address");
         return null;
      }
      addModel(source, model);
      return get(source);
   }

   public List<Model> addOrUpdate(Collection<Map<String, Object>> models) {
      if(models == null) {
         logger.debug("Ignoring update for empty models");
         return new ArrayList<Model>(1);
      }
      List<Model> result = new ArrayList<Model>(models.size() + 1);
      for(Map<String, Object> model: models) {
         Model m = addOrUpdate(model);
         if(model != null) {
            result.add(m);
         }
      }
      return result;
   }

   /**
    * Adds or updates all the models in this list, removing all other models of the same
    * type from the cache.
    * @param models
    * @return
    */
   public List<Model> retainAll(String type, Collection<Map<String, Object>> models) {
      if(models == null) {
         models = ImmutableSet.of();
      }
      Set<String> addresses = new HashSet<>(models.size() + 1);
      List<Model> result = new ArrayList<Model>(models.size() + 1);
      for(Map<String, Object> model: models) {
         Model m = addOrUpdate(model);
         if(m != null) {
            addresses.add(m.getAddress());
            result.add(m);
         }
      }
      Iterator<Model> it = this.models.values().iterator();
      while(it.hasNext()) {
         Model m = it.next();
         if(type.equals(m.getType()) && !addresses.contains(m.getAddress())) {
            it.remove();
            fireRemoved(m);
         }
      }
      return result;
   }

}

