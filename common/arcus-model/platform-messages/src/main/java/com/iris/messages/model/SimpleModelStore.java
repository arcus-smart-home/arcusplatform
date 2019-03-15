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
package com.iris.messages.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ListenerList;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.event.ModelReportEvent;
import com.iris.util.IrisAttributeLookup;
import com.iris.util.Subscription;

/**
 *
 */
public class SimpleModelStore implements ModelStore {
   private static final Logger logger = LoggerFactory.getLogger(SimpleModelStore.class);

   private Map<String, Model> models;
   private Collection<Model> unmodifiableModels;
   private ListenerList<ModelEvent> listeners;
   private Set<String> types = null;

   public SimpleModelStore() {
      // use linked implementations because we want efficient iteration
      this.models = new LinkedHashMap<String, Model>();
      this.unmodifiableModels = Collections.unmodifiableCollection(models.values());
      this.listeners = new ListenerList<>();
   }

   // TODO push down to ModelStore interface?
   public Set<String> getTrackedTypes() {
      return types;
   }
   
   public void setTrackedTypes(Collection<String> namespaces) {
      if(namespaces == null || namespaces.isEmpty()) {
         this.types = null;
      }
      else {
         this.types = ImmutableSet.copyOf(namespaces);
      }
   }
   
   public void addModel(Collection<Map<String, Object>> attributes) {
      if (attributes != null) {
         for(Map<String, Object> a: attributes) {
            addModel(a);
         }
      }
   }
   
   @Nullable
   public Model addModel(Map<String, Object> attributes) {
      Model model = newModel(attributes);
      return addModel(model);
   }

   protected Model addModel(Model model) {
      Address addr = model.getAddress();
      String key = getKey(addr);
      if(key == null) {
         return null;
      }

      models.put(key, model);
      fireModelAdded(addr);
      return model;
   }
   
   public boolean updateModel(Address address, Map<String, Object> attributes) {
      String key = getKey(address);
      if(key == null) {
         return false;
      }

      Model model = models.get(key);
      if(model == null) {
         return false;
      }

      for(Map.Entry<String, Object> attribute: attributes.entrySet()) {
         String name = attribute.getKey();
         Object newValue = attribute.getValue();
         Object oldValue = model.setAttribute(name, newValue);
         if(!Objects.equal(newValue, oldValue)) {
            fireAttributeValueChanged(model.getAddress(), name, newValue, oldValue);
         }
      }
      return true;
   }

   public boolean removeModel(Address address) {
      Model model = models.remove( getKey(address) );
      if(model == null) {
         // TODO use the context logger
         logger.debug("Received delete for un-tracked model [{}]", address);
         return false;
      }

      fireModelRemoved(model);
      return true;
   }

   @Override
   public Collection<Model> getModels() {
      return unmodifiableModels;
   }

   @Override
   @Nullable
   public Model getModelByAddress(Address address) {
      String key = getKey(address);
      return getByKey(key);
   }

   @Override
   public Iterable<Model> getModelsByType(final String type) {
      return Iterables.filter(models.values(), new Predicate<Model>() {
         @Override
         public boolean apply(Model input) {
            return input != null && input.getType().equals(type);
         }
      });
   }

   @Override
   public Iterable<Model> getModels(final Predicate<? super Model> p) {
      return Iterables.filter(models.values(), p);
   }

   @Override
   @Nullable
   public Object getAttributeValue(Address address, String attributeName) {
      Preconditions.checkNotNull(attributeName, "attributeName may not be null");
      Model model = getModelByAddress(address);
      if(model == null) {
         return null;
      }
      return model.getAttribute(attributeName);
   }

   @Override
   public void update(PlatformMessage message) {
      String type = message.getMessageType();
      if(Capability.EVENT_VALUE_CHANGE.equals(type)) {
         updateEventValueChange(message);
      } else if(Capability.EVENT_REPORT.equals(type)) {
         updateEventAttrReport(message);
      } else if(Capability.EVENT_ADDED.equals(type)) {
         updateEventAdded(message);
      } else if(Capability.EVENT_DELETED.equals(type)) {
         removeModel(message.getSource());
      } else {
         logger.trace("Ignoring non-model message [{}]", message);
      }
   }

   protected void updateEventAdded(PlatformMessage message) {
      if(types != null) {
         String objectType = (String) message.getValue().getAttributes().get(Capability.ATTR_TYPE);
         if(objectType == null) {
            logger.warn("Received add with no type, dropping message [{}]", message);
            return;
         }

         if(!types.contains(objectType)) {
            logger.trace("Dropping add for untracked type [{}]", objectType);
            return;
         }
      }
      
      Address source = message.getSource();
      String key = getKey(source);

      Model model = newModel(message.getValue().getAttributes());
      models.put(key, model);
      fireModelAdded(source);
   }

   protected void updateEventValueChange(PlatformMessage message) {
      Address source = message.getSource();
      String key = getKey(source);

      Model model = models.get(key);
      if(model == null) {
         // TODO use the context logger
         logger.debug("Received value change for un-tracked model [{}]", source);
         return;
      }

      updateEventValueChange(message, source, model);
   }

   protected void updateEventValueChange(PlatformMessage message, Address source, Model model) {
      List<Object[]> changes = updateDifferences(model, message);
      emitModelChanges(source, changes);
   }

   protected void updateEventAttrReport(PlatformMessage message) {
      Address source = message.getSource();
      String key = getKey(source);

      Model model = models.get(key);
      if(model == null) {
         // TODO use the context logger
         logger.debug("Received value change for un-tracked model [{}]", source);
         return;
      }

      updateEventAttrReport(message, source, model);
   }

   protected void updateEventAttrReport(PlatformMessage message, Address source, Model model) {
      List<Object[]> changes = updateDifferences(model, message);
      ModelReportEvent.Builder builder = ModelReportEvent.builder();
      builder.withAddress(source);

      for (Object[] change : changes) {
         builder.addChange((String) change[0], change[2], change[1]);
      }
      fire(builder.build());
      emitModelChanges(source, changes);
   }

   private List<Object[]> updateDifferences(Model model, PlatformMessage message) {
      Set<Map.Entry<String, Object>> updates = message.getValue().getAttributes().entrySet();

      List<Object[]> changes = new ArrayList<>(updates.size());
      for(Map.Entry<String, Object> e : updates) {
         Object value = IrisAttributeLookup.coerce(e.getKey(), e.getValue());
         Object oldValue = model.setAttribute(e.getKey(), value);
         if (!Objects.equal(value, oldValue)) {
            changes.add(new Object[] { e.getKey(), value, oldValue });
         }
      }
      return changes;
   }

   private void emitModelChanges(Address source, List<Object[]> changes) {
      for (Object[] change : changes) {
         fireAttributeValueChanged(source, (String)change[0], change[1], change[2]);
      }
   }

   /* (non-Javadoc)
    * @see com.iris.messages.model.ModelStore#addListener(com.iris.messages.event.Listener)
    */
   @Override
   public Subscription addListener(Listener<ModelEvent> listener) {
      Preconditions.checkNotNull(listener, "listener may not be null");
      return listeners.addListener(listener);
   }

   protected Model newModel(Map<String, Object> attributes) {
   	return new SimpleModel(attributes);
   }
   
   protected void fire(ModelEvent event) {
      listeners.fireEvent(event);
   }

   protected void fireModelAdded(Address source) {
      fire(new ModelAddedEvent(source));
   }

   protected void fireModelRemoved(Model source) {
      fire(ModelRemovedEvent.create(source));
   }

   protected void fireAttributeValueChanged(Address source, String attributeName, Object newValue, Object oldValue) {
      fire(ModelChangedEvent.create(source, attributeName, newValue, oldValue));
   }

   private @Nullable Model getByKey(@Nullable String key) {
      if(key == null) {
         return null;
      }
      return models.get(key);
   }

   private static @Nullable String getKey(@Nullable Address address) {
      if(address == null) {
         return null;
      }
      return address.getRepresentation();
   }

}

