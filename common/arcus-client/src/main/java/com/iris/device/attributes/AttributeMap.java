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
package com.iris.device.attributes;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link AttributeMap}
 */
public class AttributeMap implements Serializable {
   private static final long serialVersionUID = -8718570939132297896L;
   private static final AttributeMap EMPTY_MAP =
         new AttributeMap(Collections.<AttributeKey<?>, Object>emptyMap());

   public static AttributeMap newMap() {
      return new AttributeMap(new HashMap<AttributeKey<?>, Object>());
   }

   public static AttributeMap mapOf(AttributeValue<?>... values) {
      Map<AttributeKey<?>, Object> delegate = new HashMap<AttributeKey<?>, Object>(Math.min(values.length, 1));
      for(AttributeValue<?> value: values) {
         delegate.put(value.getKey(), value.getValue());
      }
      return new AttributeMap(delegate);
   }

   public static AttributeMap copyOf(AttributeMap map) {
      if(map == null || map.isEmpty()) {
         return newMap();
      }

      Map<AttributeKey<?>, Object> delegate = new HashMap<AttributeKey<?>, Object>(map.delegate);
      return new AttributeMap(delegate);
   }

   public static AttributeMap unmodifiableCopy(AttributeMap map) {
      if(map == null || map.isEmpty()) {
         return emptyMap();
      }

      Map<AttributeKey<?>, Object> delegate = new HashMap<AttributeKey<?>, Object>(map.delegate);
      return new AttributeMap(Collections.unmodifiableMap(delegate));
   }

   public static AttributeMap filterKeys(
         AttributeMap attributes,
         // TODO predicate would be nice here but we can't depend on guava or jdk 1.8
         Set<AttributeKey<?>> keys
   ) {
      Map<AttributeKey<?>, Object> delegate = new HashMap<AttributeKey<?>, Object>(keys.size() + 1);
      for(Map.Entry<AttributeKey<?>, Object> entry: attributes.delegate.entrySet()) {
         AttributeKey<?> key = entry.getKey();
         if(keys.contains(key)) {
            delegate.put(key, entry.getValue());
         }
      }
      return new AttributeMap(delegate);
   }

   public static AttributeMap emptyMap() {
      return EMPTY_MAP;
   }

	private final Map<AttributeKey<?>, Object> delegate;

	protected AttributeMap(Map<AttributeKey<?>, Object> delegate) {
	   this.delegate = delegate;
   }

   public <V> V get(AttributeKey<V> key) {
      return (V) delegate.get(key);
   }

   public <V> V set(AttributeKey<V> key, V value) {
      return (V) delegate.put(key, value);
   }

   public <V> boolean setIfNotNull(AttributeKey<V> key, V value) {
      if(value == null) {
         return false;
      }
      delegate.put(key, value);
      return true;
   }

   public <V> V add(AttributeValue<V> attribute) {
      if(attribute == null) {
         return null;
      }
      return set(attribute.getKey(), attribute.getValue());
   }

   public <V> V remove(AttributeKey<V> key) {
      return (V) delegate.remove(key);
   }

   public void addAll(AttributeMap attributes) {
      this.delegate.putAll(attributes.delegate);
   }

   public Set<AttributeKey<?>> keySet() {
      return delegate.keySet();
   }

   public Iterable<AttributeValue<?>> entries() {
      return new Iterable<AttributeValue<?>>() {
         @Override
         public Iterator<AttributeValue<?>> iterator() {
            return new AttributeValueIterator(delegate.entrySet().iterator());
         }
      };
   }

   public int size() {
      return delegate.size();
   }

   public boolean isEmpty() {
      return delegate.isEmpty();
   }

   public boolean containsKey(AttributeKey<?> key) {
      return delegate.containsKey(key);
   }

   public Map<String, Object> toMap() {
      Map<String, Object> map = new HashMap<String, Object>(delegate.size() + 1);
      for(Map.Entry<AttributeKey<?>, Object> entry: delegate.entrySet()) {
         map.put(entry.getKey().getName(), entry.getValue());
      }
      return map;
   }

   @Override
   public String toString() {
      return "AttributeMap [" + delegate + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      AttributeMap other = (AttributeMap) obj;
      if (delegate == null) {
         if (other.delegate != null) return false;
      }
      else if (!delegate.equals(other.delegate)) return false;
      return true;
   }

   private static class AttributeValueIterator implements Iterator<AttributeValue<?>> {
      private final Iterator<Map.Entry<AttributeKey<?>, Object>> delegate;

      AttributeValueIterator(Iterator<Map.Entry<AttributeKey<?>, Object>> delegate) {
         this.delegate = delegate;
      }

      @Override
      public boolean hasNext() {
         return delegate.hasNext();
      }

      @Override
      public AttributeValue<?> next() {
         Map.Entry<AttributeKey<?>, Object> entry = delegate.next();
         return new AttributeValue<Object>((AttributeKey<Object>) entry.getKey(), entry.getValue());
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }
   }

}

