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
package com.iris.platform.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;
import com.iris.messages.capability.Capability;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.model.Entity;
import com.iris.messages.model.PersistentModel;
import com.iris.messages.model.SimpleModel;

/**
 * 
 */
public class ModelEntity extends SimpleModel implements Entity<String, ModelEntity>, PersistentModel {
   private Date created;
   private Date modified;
   
   private Map<String, Object> oldAttributes = new HashMap<>();

   /**
    * 
    */
   public ModelEntity() {
      // TODO Auto-generated constructor stub
   }

   /**
    * @param attributes
    */
   public ModelEntity(Map<String, Object> attributes) {
      super(attributes);
   }

   /**
    * @param copy
    */
   public ModelEntity(ModelEntity copy) {
      super(copy);
      this.created = copy.created;
      this.modified = copy.modified;
   }

   @Override
   public boolean isDirty() {
      return !oldAttributes.isEmpty();
   }
   
   @Override
   public Set<String> getDirtyAttributeNames() {
      return oldAttributes.keySet();
   }
   
   @Override
   public Map<String, Object> getDirtyAttributes() {
      Map<String, Object> updated = new HashMap<>((oldAttributes.size()+1)*4/3,0.75f);
      for(String name: oldAttributes.keySet()) {
         Object value = getAttribute(name);
         updated.put(name, value);
      }
      return updated;
   }
   
   @Override
   public Map<String, Object> clearDirtyAttributes() {
      Map<String, Object> attributes = getDirtyAttributes();
      clearDirty();
      return attributes;
   }

   public Map<String, Object> getUpdatedAttributes() {
      return getUpdatedAttributes(false);
   }
   
   public Map<String, Object> getAndClearUpdatedAttributes() {
      return getUpdatedAttributes(true);
   }
   
   private Map<String, Object> getUpdatedAttributes(boolean clear) {
      Map<String, Object> updated = new HashMap<>((oldAttributes.size()+1)*4/3,0.75f);
      for(String name: oldAttributes.keySet()) {
         Object value = getAttribute(name);
         if(value != null) {
            updated.put(name, value);
         }
      }
      if(clear) {
      	clearDirty();
      }
      return updated;
   }
   
   public Set<String> getRemovedAttributeNames() {
      return getRemovedAttributes(false);
   }
   
   public Set<String> getAndClearRemovedAttributeNames() {
      return getRemovedAttributes(true);
   }
   
   private Set<String> getRemovedAttributes(boolean clear) {
      Set<String> removed = new HashSet<>((oldAttributes.size()+1)*4/3,0.75f);
      for(String name: oldAttributes.keySet()) {
         Object value = getAttribute(name);
         if(value == null) {
            removed.add(name);
         }
      }
      if(clear) {
      	clearDirty();
      }
      return removed;
   }
   
   public void clearDirty() {
      this.oldAttributes.clear();
   }
   
   /**
    * Retrieves the list of changes and clears any
    * dirty attributes.
    * @return
    */
	public List<ModelChangedEvent> commit() {
   	List<ModelChangedEvent> changes = new ArrayList<>(oldAttributes.size());
   	if(!isDirty()) {
   		return changes;
   	}
   	
   	for(Map.Entry<String, Object> dirtyAttribute: oldAttributes.entrySet()) {
   		String attributeName = dirtyAttribute.getKey();
   		ModelChangedEvent event = ModelChangedEvent.create(getAddress(), attributeName, super.getAttribute(attributeName), dirtyAttribute.getValue());
   		changes.add(event);
   	}
   	clearDirty();
   	return changes;
   }
   
	public void reset() {
		update(oldAttributes);
		clearDirty();
   }
   
   /* (non-Javadoc)
    * @see com.iris.messages.model.SimpleModel#setAttribute(java.lang.String, java.lang.Object)
    */
   @Override
   public Object setAttribute(String name, Object value) {
      Object old = super.setAttribute(name, value);
   	// don't allow multiple writes to overwrite the original value
   	if(oldAttributes.containsKey(name)) {
   		if(Objects.equal(oldAttributes.get(name), value)) {
   			// re-set to the original, not actually dirty
   			oldAttributes.remove(name);
   		}
   	}
   	else if(!Objects.equal(old, value)) {
   		oldAttributes.put(name, old);
      }
      return old;
   }

   /* (non-Javadoc)
    * @see com.iris.messages.model.SimpleModel#update(java.util.Map)
    */
   @Override
   public void update(Map<String, Object> attributes) {
   	if(attributes == null || attributes.isEmpty()) {
   		return;
   	}
   	for(Map.Entry<String, Object> attribute: attributes.entrySet()) {
   		setAttribute(attribute.getKey(), attribute.getValue());
   	}
   }

   /* (non-Javadoc)
    * @see com.iris.messages.model.Copyable#copy()
    */
   @Override
   public ModelEntity copy() {
      return new ModelEntity(this);
   }

   /* (non-Javadoc)
    * @see com.iris.messages.model.Entity#isPersisted()
    */
   @Override
   public boolean isPersisted() {
      return created != null;
   }

   /* (non-Javadoc)
    * @see com.iris.messages.model.Entity#getCreated()
    */
   @Override
   public Date getCreated() {
      return created;
   }

   /* (non-Javadoc)
    * @see com.iris.messages.model.Entity#getModified()
    */
   @Override
   public Date getModified() {
      return modified;
   }

   /* (non-Javadoc)
    * @see com.iris.messages.model.Entity#setId(java.lang.Object)
    */
   @Override
   public void setId(String id) {
      setAttribute(Capability.ATTR_ID, id);
   }

   /* (non-Javadoc)
    * @see com.iris.messages.model.Entity#setCreated(java.util.Date)
    */
   @Override
   public void setCreated(Date created) {
      this.created = created;
   }

   /* (non-Javadoc)
    * @see com.iris.messages.model.Entity#setModified(java.util.Date)
    */
   @Override
   public void setModified(Date modified) {
      this.modified = modified;
   }

   @Override
   public String toString() {
      return "ModelEntity [address=" + getAddress() + 
            ", type=" + getType() +
            ", created=" + created +
            ", attributes=" + toMap() +
            "]";
   }

   // NOTE no hashCode / equals because equivalency is not affected by created / modified / dirtyAttributeNames
}

