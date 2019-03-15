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
package com.iris.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SetMap<K,V> implements Map<K,Set<V>> {

	private final Map<K, Set<V>> delegate;
	
	public SetMap() {
		this(null);
	}
	
	public SetMap(Map<K, Set<V>> delegate) {
		this.delegate = delegate != null ? delegate : new HashMap<K, Set<V>>();
	}
	
   public V putItem(K key, V item) {
   	if (key == null || item == null) {
   		return null;
   	}
   	Set<V> set = delegate.get(key);
   	if (set == null) {
   		set = new HashSet<V>();
   		delegate.put(key, set);
   	}
   	set.add(item);
   	return item;
   }
   
   public boolean containsItem(K key, V item) {
   	if (key == null || item == null) {
   		return false;
   	} 
   	Set<V> set = delegate.get(key);
   	return set != null ? set.contains(item) : false;
   }
   
   public boolean removeItem(K key, V item) {
   	if (key == null || item == null) {
   		return false;
   	}
   	Set<V> set = delegate.get(key);
   	return set != null ? set.remove(item) : false;
   }
	
	@Override
   public int size() {
	   return delegate.size();
   }

	@Override
   public boolean isEmpty() {
	   return delegate.isEmpty();
   }

	@Override
   public boolean containsKey(Object key) {
	   return delegate.containsKey(key);
   }

	@Override
   public boolean containsValue(Object value) {
	   return delegate.containsValue(value);
   }

	@Override
   public Set<V> get(Object key) {
	   return delegate.get(key);
   }

	@Override
   public Set<V> put(K key, Set<V> value) {
	   return delegate.put(key, value);
   }

	@Override
   public Set<V> remove(Object key) {
	   return delegate.remove(key);
   }

	@Override
   public void putAll(Map<? extends K, ? extends Set<V>> m) {
	   delegate.putAll(m);
   }

	@Override
   public void clear() {
	   delegate.clear();
   }

	@Override
   public Set<K> keySet() {
	   return delegate.keySet();
   }

	@Override
   public Collection<Set<V>> values() {
	   return delegate.values();
   }

	@Override
   public Set<java.util.Map.Entry<K, Set<V>>> entrySet() {
	   return delegate.entrySet();
   }

	

}

