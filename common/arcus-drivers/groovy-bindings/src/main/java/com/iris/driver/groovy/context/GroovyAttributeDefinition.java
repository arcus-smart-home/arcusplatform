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
package com.iris.driver.groovy.context;

import groovy.lang.Closure;
import groovy.lang.MissingMethodException;
import groovy.lang.MissingPropertyException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.iris.capability.key.NamespacedKey;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.AttributeFlag;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyBuilder;
import com.iris.driver.groovy.GroovyContextObject;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.model.type.AttributeType;
import com.iris.model.type.CollectionType;
import com.iris.model.type.ListType;
import com.iris.model.type.MapType;
import com.iris.model.type.SetType;

/**
 *
 */
@SuppressWarnings("deprecation")
public class GroovyAttributeDefinition extends Closure<Object> {
   private final AttributeDefinition delegate;
   private final GroovyBuilder builder;

   public GroovyAttributeDefinition(AttributeDefinition delegate, EnvironmentBinding binding) {
      super(binding);
      this.delegate = delegate;
      this.builder = binding.getBuilder();
      setResolveStrategy(TO_SELF);
   }

   public AttributeDefinition getAttribute() {
      return delegate;
   }

   public AttributeKey<?> getKey() {
      return delegate.getKey();
   }

   public String getName() {
      return delegate.getName();
   }

   public Type getType() {
      return delegate.getType();
   }

   public boolean isReadable() {
      return delegate.isReadable();
   }

   public boolean isWritable() {
      return delegate.isWritable();
   }

   public boolean isOptional() {
      return delegate.isOptional();
   }

   public Set<AttributeFlag> getFlags() {
      return delegate.getFlags();
   }

   public String getDescription() {
      return delegate.getDescription();
   }

   public String getUnit() {
      return delegate.getUnit();
   }

   public AttributeType getAttributeType() {
      return delegate.getAttributeType();
   }
   
   /* (non-Javadoc)
    * @see groovy.lang.Closure#getProperty(java.lang.String)
    */
   @Override
   public Object getProperty(String property) {
      try {
         return super.getProperty(property);
      }
      catch(MissingPropertyException e) {
         if(isValidInstance(property)) {
            return instance(property);
         }
         else {
            throw e;
         }
      }
   }

   /* (non-Javadoc)
    * @see groovy.lang.GroovyObjectSupport#invokeMethod(java.lang.String, java.lang.Object)
    */
   @Override
   public Object invokeMethod(String name, Object args) {
      try {
         return super.invokeMethod(name, args);
      }
      catch(MissingMethodException e) {
         if(isValidInstance(name)) {
            return instance(name).call(args);
         }
         else {
            throw e;
         }
      }
   }

   public GroovyInstancedAttributeDefinition instance(String instanceId) {
      if(!isValidInstance(instanceId)) {
         throw new IllegalArgumentException("No instance with id [" + instanceId + "] has been defined");
      }
      return new GroovyInstancedAttributeDefinition(delegate, instanceId, (EnvironmentBinding) getOwner());
   }

   public Object get() {
      AttributeKey<?> key = getKey();
      if(GroovyContextObject.isContextSet()) {
         DeviceDriverContext context = GroovyContextObject.getContext();
         return context.getAttributeValue(key);
      }
      else {
         return builder.getAttributes().get(key);
      }
   }
   
   public Object get(String key) {
      if(!isMapType()) {
         throw new MissingPropertyException("May only call get(key) on a Map attribute");
      }
      Object curValue = get();
      if(curValue != null) {
         return ((Map<?, ?>) curValue).get(key);
      }else{
         return null;
      }
   }
   
  
   
   
   public Object put(String key, Object value) {
     if(!isMapType()) {
         throw new IllegalArgumentException("May only call put(key, value) on a Map attribute");
      }
     Map<String, Object> copy = copy((Map<String, Object>) get());
     AttributeType containedType = ((CollectionType) delegate.getAttributeType()).getContainedType();
     Object retValue = containedType.coerce(value);
     if(retValue != null) {
        copy.put(key, retValue);
        set(copy);
     }
     return retValue;
   }
   
   public Object remove(String key) {
     if(isMapType()) {
        Map<String, Object> copy = copy((Map<String, Object>) get());
         Object valueRemoved = copy.remove(key);
         if(valueRemoved != null) {
            set(copy);
         }
         return valueRemoved;
     }else if(isCollectionType()) {
        boolean f = remove((Object)key);
        if(f) {
           return key;
        }else {
           return null;
        }
     }else {
        throw new IllegalArgumentException("May only call remove(key) on a Collection or Map attribute");
     }
      
   }
   
   
   public void set(Object value) {
      if(GroovyContextObject.isContextSet()) {
         DeviceDriverContext context = GroovyContextObject.getContext();
         context.setAttributeValue(getKey().coerceToValue(value));
      }
      else {
         builder.addAttributeValue(getKey(), value);
      }
   }
   
   public boolean add(Object value) {
      if(!isCollectionType()) {
         throw new IllegalArgumentException("May only call add on a set or a list attribute");
      }
      AttributeType containedType = ((CollectionType) delegate.getAttributeType()).getContainedType();
      Collection<Object> copy = copy((Collection<?>) get());
      boolean modified = copy.add(containedType.coerce(value));
      if(modified) {
         set(copy);
      }
      return modified;
   }

   public boolean remove(Object value) {
      if(!isCollectionType()) {
         throw new IllegalArgumentException("May only call add on a set or a list attribute");
      }
      AttributeType containedType = ((CollectionType) delegate.getAttributeType()).getContainedType();
      Collection<Object> copy = copy((Collection<?>) get());
      boolean modified = copy.remove(containedType.coerce(value));
      if(modified) {
         set(copy);
      }
      return modified;
   }

   public boolean addAll(Collection<Object> value) {
      if(!isCollectionType()) {
         throw new IllegalArgumentException("May only call addAll on a set or a list attribute");
      }
      
      Collection<Object> copy = copy((Collection<?>) get());
      boolean modified = copy.addAll((Collection<?>) delegate.getAttributeType().coerce(value));
      if(modified) {
         set(copy);
      }
      return modified;
   }

   public boolean removeAll(Object value) {
      if(!isCollectionType()) {
         throw new IllegalArgumentException("May only call removeAll on a set or a list attribute");
      }
      Collection<Object> copy = copy((Collection<?>) get());
      boolean modified = copy.removeAll((Collection<?>) delegate.getAttributeType().coerce(value));
      if(modified) {
         set(copy);
      }
      return modified;
   }
   
   public boolean contains(Object value) {
   	if (!isCollectionType()) {
   		throw new IllegalArgumentException("May only call contains on a set or a list attribute");
   	}
   	return ((Collection<?>) get()).contains(value);
   }

   public boolean isCollectionType() {
      return delegate.getAttributeType() instanceof SetType || delegate.getAttributeType() instanceof ListType;
   }
   
   public boolean isMapType() {
      return delegate.getAttributeType() instanceof MapType;
   }

   protected Object doCall() {
      return get();
   }

   protected void doCall(Object value) {
      set(extractValue(value));
   }

   @Override
   public boolean isCase(Object o) {
      if(o == null) return false;

      String name;
      if(o instanceof String) {
         name = (String) o;
      }
      else if(o instanceof AttributeKey) {
         name = ((AttributeKey<?>) o).getName();
      }
      else {
         return false;
      }
      
      return delegate.getName().equals(NamespacedKey.parse(name).getNamedRepresentation());
   }

   @Override
   public String toString() {
      return delegate.toString();
   }

   private Collection<Object> copy(Collection<?> oldValue) {
      if(delegate.getAttributeType() instanceof SetType) {
         return oldValue == null ? new HashSet<Object>() : new HashSet<Object>(oldValue);
      }
      else if(delegate.getAttributeType() instanceof ListType) {
         return oldValue == null ? new ArrayList<Object>() : new ArrayList<Object>(oldValue);
      }
      throw new IllegalArgumentException("Invalid request, " + delegate.getName() + " is not a collection");
   }
   
   private Map<String, Object> copy(Map<String, Object> oldValue) {
     if(delegate.getAttributeType() instanceof MapType) {
         return oldValue == null ? new HashMap<String, Object>() : new HashMap<String, Object>(oldValue);
      }
      throw new IllegalArgumentException("Invalid request, " + delegate.getName() + " is not a Map");
   }

   private Object extractValue(Object value) {
      if(value instanceof Object[]) {
         Object [] arguments = (Object[]) value;
         return arguments[0];
      }
      return value;
   }

   private boolean isValidInstance(String instanceId) {
      return ((EnvironmentBinding) getDelegate()).isValidInstance(instanceId, delegate.getKey().getNamespace());
   }
}

