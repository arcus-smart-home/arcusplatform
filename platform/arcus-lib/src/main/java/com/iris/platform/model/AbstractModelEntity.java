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
package com.iris.platform.model;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.iris.capability.definition.AttributeTypes;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Entity;
import com.iris.messages.model.PersistentModel;
import com.iris.util.IrisAttributeLookup;
import com.iris.util.TypeMarker;

/**
 * A base class for building an object that mixes strongly typed
 * attributes and weakly typed attributes while maintaining
 * a PersistentModel and Entity interface.
 * @author tweidlin
 *
 * @param <V>
 */
public abstract class AbstractModelEntity<V> implements Entity<String, V>, PersistentModel {
   private static final List<String> staticKeys = ImmutableList.of(
         Capability.ATTR_ID, 
         Capability.ATTR_ADDRESS, 
         Capability.ATTR_CAPS, 
         Capability.ATTR_IMAGES, 
         Capability.ATTR_INSTANCES, 
         Capability.ATTR_TYPE
   );

   private String id;
   private Map<String, Object> attributes;
   private Date created;
   private Date modified;
   private Map<String, Object> dirtyAttributes = new HashMap<>();
   
   public AbstractModelEntity() {
      this.attributes = new HashMap<>();
   }
   
   /**
    * Creates a new model entity seeded with the given attributes.
    * @param attributes
    */
   public AbstractModelEntity(Map<String, Object> attributes) {
      this.attributes = Maps.newHashMapWithExpectedSize(attributes.size());
      for(Map.Entry<String, Object> entry: attributes.entrySet()) {
         Preconditions.checkArgument(!staticKeys.contains(entry.getKey()), "Can't pass %s as a map constructor value", entry.getKey());
         this.attributes.put(entry.getKey(), IrisAttributeLookup.coerce(entry.getKey(), entry.getValue()));
      }
   }

   /**
    * Copy constructor, this will clear dirty flags.
    * @param copy
    */
   public AbstractModelEntity(AbstractModelEntity<V> copy) {
      this.id = copy.id;
      this.attributes = new HashMap<>(copy.attributes);
      this.created = copy.created == null ? null : new Date(copy.created.getTime());
      this.modified = copy.modified == null ? null : new Date(copy.modified.getTime());
   }

   @Override
   public Map<String, Set<String>> getInstances() {
      return ImmutableMap.of();
   }

   @Override
   public boolean supports(String capabilityNamespace) {
      return getCapabilities().contains(capabilityNamespace);
   }

   @Override
   public boolean hasInstanceOf(String instanceId, String capabilityNamespace) {
      return getInstances().getOrDefault(instanceId, ImmutableSet.of()).contains(capabilityNamespace);
   }

   @Override
   public Object getAttribute(String name) {
      switch(name) {
      case Capability.ATTR_ID:
         return getId();
      case Capability.ATTR_ADDRESS:
         Address address = getAddress();
         return address != null ? address.getRepresentation() : null;
      case Capability.ATTR_CAPS:
         return getCapabilities();
      case Capability.ATTR_IMAGES:
         return ImmutableMap.of();
      case Capability.ATTR_INSTANCES:
         return getInstances();
      // tags are writable, so fall through
      case Capability.ATTR_TYPE:
         return getType();
      default:
         return attributes.get(name);
      }
   }

   @Override
   public <T> Optional<T> getAttribute(TypeMarker<T> type, String name) {
      // FIXME move this to a base class and/or utility
      Object value = getAttribute(name);
      if(value == null) {
         return Optional.<T>absent();
      }
      // TODO cache the AttributeType
      T coerced = (T) AttributeTypes.fromJavaType(type.getType()).coerce(value);
      return Optional.fromNullable(coerced);
   }

   @Nullable
   @Override
   public <T> T getAttribute(TypeMarker<T> type, String name, @Nullable T defaultValue) {
      return getAttribute(type, name).or(defaultValue);
   }

   @Override
   public final Object setAttribute(String name, Object value) {
      Object oldValue = getAttribute(name);
      doSetAttribute(name, value);
      
      // Update the dirty value map with the following considerations
      // 1) If the value is equal to the old value this is a no-op
      // 2) Always put coerced values (the result of a generic getAttribute) in the map to make sure they are properly typed
      // 3) Support null values for cases where the key previously had no mapping or now has no mapping
      // 4) Always save the _original_ value, so if it is put back in after multiple mutations of the key there will be no change
      if(!Objects.equals(value, oldValue)) {
         if(dirtyAttributes.containsKey(name)) {
            if(Objects.equals(value, dirtyAttributes.get(name))) {
               dirtyAttributes.remove(name);
            }
            else {
               // no change, leave the original value in there when the same key has multiple mutations
            }
         }
         else {
            dirtyAttributes.put(name, oldValue);
         }
      }
      return oldValue;
   }
   
   protected void doSetAttribute(String name, Object value) {
      switch(name) {
      case Capability.ATTR_ID:
         setId(AttributeTypes.coerceString(value));
         break;
      case Capability.ATTR_ADDRESS:
         throw new IllegalArgumentException("Can't set base:address");
      case Capability.ATTR_CAPS:
         throw new IllegalArgumentException("Can't set base:caps");
      case Capability.ATTR_IMAGES:
         throw new IllegalArgumentException("Can't set base:images");
      case Capability.ATTR_INSTANCES:
         throw new IllegalArgumentException("Can't set base:instances");
      case Capability.ATTR_TYPE:
         throw new IllegalArgumentException("Can't set base:type");
      default:
         attributes.put(name, IrisAttributeLookup.coerce(name, value));
         break;
      }
   }

   @Override
   public void update(Map<String, Object> attributes) {
      for(Map.Entry<String, Object> attribute: attributes.entrySet()) {
         setAttribute(attribute.getKey(), attribute.getValue());
      }
   }

   /**
    * Gets a read-only view of the attributes that aren't first
    * class properties.
    * @return
    */
   public Map<String, Object> getAttributes() {
      return Collections.unmodifiableMap(attributes);
   }

   @Override
   public Map<String, Object> toMap() {
      Map<String, Object> map = Maps.newHashMapWithExpectedSize(staticKeys.size() + attributes.size());
      // TODO could optimize this by hard coding assignment of static keys
      for(String key: keys()) {
         Object value = getAttribute(key);
         if(value != null) {
            map.put(key, value);
         }
      }
      return map;
   }
   
   protected Set<String> customKeys() {
      return ImmutableSet.of();
   }

   @Override
   public Iterable<String> keys() {
      return () -> Iterators.concat(staticKeys.iterator(), customKeys().iterator(), attributes.keySet().iterator());
   }

   @Override
   public Iterable<Object> values() {
      return () -> Iterators.transform(keys().iterator(), AbstractModelEntity.this::getAttribute);
   }

   @Override
   public boolean isDirty() {
      return !dirtyAttributes.isEmpty();
   }

   @Override
   public Set<String> getDirtyAttributeNames() {
      return Collections.unmodifiableSet(dirtyAttributes.keySet());
   }

   @Override
   public Map<String, Object> getDirtyAttributes() {
      Map<String, Object> dirtyValues = Maps.newHashMapWithExpectedSize(dirtyAttributes.size());
      for(String key: dirtyAttributes.keySet()) {
         dirtyValues.put(key, getAttribute(key));
      }
      return dirtyValues;
   }

   @Override
   public Map<String, Object> clearDirtyAttributes() {
      Map<String, Object> copy = new HashMap<>(dirtyAttributes);
      dirtyAttributes.clear();
      return copy;
   }

   @Override
   public boolean isPersisted() {
      return created != null;
   }

   @Override
   public Address getAddress() {
      String id = getId();
      return id != null ? Address.platformService(getId(), getType()) : null;
   }

   @Override
   public String getId() {
      return id;
   }

   @Override
   public void setId(String id) {
      this.id = id;
   }

   @Override
   public Date getCreated() {
      return created;
   }

   @Override
   public void setCreated(Date created) {
      this.created = created;
   }

   @Override
   public Date getModified() {
      return modified;
   }

   @Override
   public void setModified(Date modified) {
      this.modified = modified;
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      return copy();
   }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((created == null) ? 0 : created.hashCode());
		result = prime * result + ((dirtyAttributes == null) ? 0 : dirtyAttributes.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((modified == null) ? 0 : modified.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractModelEntity other = (AbstractModelEntity) obj;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (created == null) {
			if (other.created != null)
				return false;
		} else if (!created.equals(other.created))
			return false;
		if (dirtyAttributes == null) {
			if (other.dirtyAttributes != null)
				return false;
		} else if (!dirtyAttributes.equals(other.dirtyAttributes))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (modified == null) {
			if (other.modified != null)
				return false;
		} else if (!modified.equals(other.modified))
			return false;
		return true;
	}

}

