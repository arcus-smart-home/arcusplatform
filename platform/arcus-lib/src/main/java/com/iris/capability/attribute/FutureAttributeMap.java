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
package com.iris.capability.attribute;

import java.util.Set;
import java.util.stream.Stream;

import com.google.common.util.concurrent.ListenableFuture;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;

/**
 * A map-like structure with strongly typed keys. Each
 * key may have a different type associated with it.
 */
public interface FutureAttributeMap extends ListenableFuture<AttributeMap> {

	/**
	 * Retrieves the value associated with {@code key}
	 * or {@code null} if no such key exists. Key
	 * may not be null.
	 * @param key
	 * @return
	 * 	The current value, or {@code null} if there is none.
	 */
	public <V> ListenableFuture<V> get(AttributeKey<V> key);
	
	/**
	 * A mutable view of the keys associated with this object.
	 * @return
	 */
	public Set<AttributeKey<?>> keySet();
	
	/**
	 * A stream of the keys in the map.
	 * @return
	 */
	public default Stream<AttributeKey<?>> keys() { return keySet().stream(); }
	
	/**
	 * A stream of the entries in the map.
	 * @return
	 */
	public Stream<FutureAttributeValue<?>> entries();
	
	/**
	 * A stream of the values in the map.
	 * @return
	 */
	public default Stream<? extends ListenableFuture<?>> values() {
		return entries();
	}
	
	/**
	 * Returns true if {@code key} is mapped to a value.
	 * @param key
	 * @return
	 */
	public default boolean contains(AttributeKey<?> key) { return keySet().contains(key); }

	public default int size() { return keySet().size(); }
	
}

