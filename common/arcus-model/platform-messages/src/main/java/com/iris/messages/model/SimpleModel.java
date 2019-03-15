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
package com.iris.messages.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.AttributeTypes;
import com.iris.capability.key.NamespacedKey;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.util.IrisAttributeLookup;
import com.iris.util.IrisInterner;
import com.iris.util.IrisInterners;
import com.iris.util.TypeMarker;

public class SimpleModel implements Model {
   private static final Logger log = LoggerFactory.getLogger(SimpleModel.class);
   private static final TypeMarker<Map<String, Set<String>>> TYPE_INSTANCES =
         new TypeMarker<Map<String,Set<String>>>() {};

   private static final IrisInterner<String> STRING_INTERN = IrisInterners.strings();
   private static final IrisInterner<Set<String>> CAPS_INTERN = IrisInterners.interner("capabilities", -1, new Function<Set<String>,Set<String>>() {
      @Override
      public Set<String> apply(Set<String> caps) {
         ImmutableSet.Builder<String> bld = ImmutableSet.builder();
         for (String cap : caps) {
            bld.add(STRING_INTERN.intern(cap));
         }

         return bld.build();
      }
   });

   private String id;
   private Address address;
   private String type;
   private Set<String> capabilities;
   private Map<String, Object> attributes;
   
   private final Iterable<String> keys = new Iterable<String>() {
      private final List<String> extraKeys = 
            Arrays.asList(Capability.ATTR_ID, Capability.ATTR_ADDRESS, Capability.ATTR_TYPE, Capability.ATTR_TAGS);
      @Override
      public Iterator<String> iterator() {
         return Iterators.concat(extraKeys.iterator(), attributes.keySet().iterator());
      }
   };
   private final Iterable<Object> values = new Iterable<Object>() {
      @Override
      public Iterator<Object> iterator() {
         return Iterators.concat(Arrays.asList(id, address, type, capabilities).iterator(), attributes.values().iterator());
      }
   };
   
   public SimpleModel() {
      this.attributes = new HashMap<String, Object>();
   }
   
   public SimpleModel(Map<String, Object> attributes) {
      if(attributes == null || attributes.isEmpty()) {
         this.attributes = new HashMap<String, Object>();
      }
      else {
         this.attributes = new HashMap<String, Object>((attributes.size()+1)*4/3,0.75f);
         updateInternal(attributes);
      }
   }
   
   public SimpleModel(@Nullable Model copy) {
      this(copy != null ? copy.toMap() : Collections.<String, Object>emptyMap());
   }
   
   @Override
   public String getId() {
      return id;
   }

   @Override
   public Address getAddress() {
      return address;
   }

   @Override
   public String getType() {
      return type;
   }

   @Override
   public Set<String> getCapabilities() {
      return capabilities;
   }

   @Override
   public Map<String, Set<String>> getInstances() {
      return getAttribute(TYPE_INSTANCES, Capability.ATTR_INSTANCES, ImmutableMap.<String, Set<String>>of());
   }

   @Override
   public boolean supports(String capabilityNamespace) {
      Preconditions.checkNotNull(capabilityNamespace, "capabilityNamespace may not be null");
      // everything supports base, right?
      if(Capability.NAMESPACE.equals(capabilityNamespace)) {
         return true;
      }
      if(StringUtils.equals(type, capabilityNamespace)) {
         return true;
      }
      if(this.capabilities != null && this.capabilities.contains(capabilityNamespace)) {
         return true;
      }
      return false;
   }

   /* (non-Javadoc)
    * @see com.iris.messages.model.Model#hasInstanceOf(java.lang.String, java.lang.String)
    */
   @Override
   public boolean hasInstanceOf(String instanceId, String capabilityNamespace) {
      Preconditions.checkNotNull(instanceId, "instanceId may not be null");
      Preconditions.checkNotNull(capabilityNamespace, "capabilityNamespace may not be null");

      Set<String> caps = getInstances().get(instanceId);
      return caps != null && caps.contains(capabilityNamespace);
   }

   @Override
   public Object getAttribute(String name) {
      Preconditions.checkNotNull(name, "name may not be null");
      switch(name) {
      case Capability.ATTR_ID:
         return id;
      case Capability.ATTR_ADDRESS:
         return address.getRepresentation();
      case Capability.ATTR_TYPE:
         return type;
      case Capability.ATTR_CAPS:
         return capabilities;
      default:
         return attributes.get(name);
      }
   }

   @Override
   public <T> Optional<T> getAttribute(TypeMarker<T> type, String name) {
      Object value = getAttribute(name);
      if(value == null) {
         return Optional.<T>absent();
      }
      // TODO cache the AttributeType
      T coerced = (T) AttributeTypes.fromJavaType(type.getType()).coerce(value);
      return Optional.fromNullable(coerced);
   }
   
   @Override
   public <T> T getAttribute(TypeMarker<T> type, String name, T defaultValue) {
      return getAttribute(type, name).or(defaultValue);
   }
   
   @Override
   public Object setAttribute(String name, Object value) {
      return updateInternal(name, value);
   }

   @Override
   public void update(Map<String, Object> attributes) {
      updateInternal(attributes);
   }

   @Override
   public Map<String, Object> toMap() {
      Map<String, Object> map = new HashMap<String, Object>((this.attributes.size() + 5)*4/3,0.75f);
      map.putAll(this.attributes);
      map.put(Capability.ATTR_ID, this.id);
      if(this.address != null) {
         map.put(Capability.ATTR_ADDRESS, this.address.getRepresentation());
      }
      map.put(Capability.ATTR_TYPE, this.type);
      map.put(Capability.ATTR_CAPS, this.capabilities);
      return map;
   }

   @Override
   public Iterable<String> keys() {
      return keys;
   }

   @Override
   public Iterable<Object> values() {
      return values;
   }

   // private since this is indirectly referenced from the constructor
   private Object coerce(String name, Object value) {
      return IrisAttributeLookup.coerce(name, value);
   }

   // private since this is referenced from the constructor
   private Object updateInternal(String key, Object value) {
      String name = STRING_INTERN.intern(key);

      Preconditions.checkNotNull(name, "name may not be null");
      Object old;
      switch(name) {
      case Capability.ATTR_ID:
         old = id;
         id = String.valueOf(value);
         break;
      case Capability.ATTR_ADDRESS:
         old = address;
         if (value instanceof Address) {
            address = (Address)value;
         } else {
            address = Address.fromString(String.valueOf(value));
         }
         break;
      case Capability.ATTR_TYPE:
         old = type;
         type = STRING_INTERN.intern((String)value);
         break;
      case Capability.ATTR_CAPS:
         old = capabilities;
         capabilities = CAPS_INTERN.intern((Set<String>) Capability.TYPE_CAPS.coerce(value));
         break;
      default:
         if(value == null) {
            old = attributes.remove(name);
         }
         else {
            old = attributes.put(name, coerce(name, value));
         }
      }
      return old;
   }

   private void updateInternal(Map<String, Object> attributes) {
      if(attributes == null || attributes.isEmpty()) {
         return;
      }
      Iterator<Map.Entry<String, Object>> it = attributes.entrySet().iterator();
      while(it.hasNext()) {
         Map.Entry<String, Object> attribute = it.next();
         updateInternal(attribute.getKey(), attribute.getValue());
         if(attribute.getValue() == null) {
            it.remove();
         }
      }
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "SimpleModel [address=" + address + ", attributes="
            + attributes + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + toMap().hashCode();
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      if(!(obj instanceof Model)) return false;
      Model other = (Model) obj;
      return this.toMap().equals(other.toMap());
   }
}

