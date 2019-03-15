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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.iris.messages.model.Copyable;

/**
 * Additions to {@link Collections}.
 */
public class IrisCollections {
   
   public static int size(Collection<?>... collections) {
      if (collections != null && collections.length > 0) {
         int size = 0;
         for (Collection<?> collection : collections) {
            size += collection.size();
         }
         return size;
      }
      return 0;
   }
   
   public static <E> Set<E> addedElements(Set<E> updated, Set<E> orig) {
   	if (updated == null || updated.isEmpty()) {
   		return Collections.emptySet();
   	}
   	else if (orig == null || orig.isEmpty()) {
   		return Collections.unmodifiableSet(updated);
   	}
   	else {
   		return Sets.difference(updated, orig);
   	}
   }
   
   public static <E> Set<E> removedElements(Set<E> updated, Set<E> orig) {
   	return addedElements(orig, updated);
   }
   
   public static <K> Set<K> addedKeys(Map<K, ?> updated, Map<K, ?> orig) {
   	if (updated == null || updated.isEmpty()) {
   		return Collections.emptySet();
   	}
   	else if (orig == null || orig.isEmpty()) {
   		return Collections.unmodifiableSet(updated.keySet());
   	}
   	else {
   		return addedElements(updated.keySet(), orig.keySet());
   	}
   }
   
   public static <K> Set<K> removedKeys(Map<K, ?> updated, Map<K, ?> orig) {
   	return addedKeys(orig, updated);
   }
   
   public static int size(Map<?,?>... maps) {
      if (maps != null && maps.length > 0) {
         int size = 0;
         for (Map<?,?> map : maps) {
            size += map != null ? map.size() : 0;
         }
         return size;
      }
      return 0;
   }
   
   // I'd make this vararg but type erasure.
   public static <K, V> Map<K,V> merge(Map<? extends K, ? extends V> map1, Map<? extends K, ? extends V> map2) {
      if (map1 == null && map2 == null) {
         return null;
      }
      int size = size(map1, map2);
      if (size == 0) {
         return Collections.emptyMap();
      }
      Map<K,V> newMap = new HashMap<>(size);
      if (map1 != null) {
         newMap.putAll(map1);
      }
      if (map2 != null) {
         newMap.putAll(map2);
      }
      return newMap;
   }
   
   @Nullable
   public static <E> E last(@Nullable List<E> list) {
      return list == null || list.isEmpty() ? null : list.get(list.size() - 1);
   }

	/**
	 * Creates a copy of the map, and returns an immutable view of it.
	 * @param map
	 * @return
	 */
	@SuppressWarnings("null")
   public static <K, V> Map<K, V> unmodifiableCopy(Map<? extends K, ? extends V> map) {
		if(map == null || map.isEmpty()) {
			return Collections.emptyMap();
		}
		if(map instanceof LinkedHashMap) {
			// maintain ordering if the delegate had ordering
			return Collections.unmodifiableMap(new LinkedHashMap<>(map));
		}
		else {
			return Collections.unmodifiableMap(new HashMap<>(map));
		}
	}

	@SuppressWarnings("null")
   public static <T> Set<T> unmodifiableCopy(Collection<T> values) {
		if(values == null || values.isEmpty()) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableSet(copyOfSet(values));
   }

	/**
	 * Creates a mutable copy, returning a new empty list if {@code source} is {@code null}.
	 * @param source
	 * @return
	 */
   public static <T> List<T> copyOf(@Nullable List<T> source) {
      if(source == null) {
         return new ArrayList<>();
      }
      return new ArrayList<>(source);
   }
   
   public static <T> Set<T> copyOfSet(@Nullable Collection<T> values) {
      if(values == null) {
         return new HashSet<>();
      }
      if(values instanceof LinkedHashSet || values instanceof List) {
         // maintain ordering if the delegate had ordering
         return new LinkedHashSet<>(values);
      }
      return new HashSet<>(values);
   }

	public static <T> Set<T> setOf(T... values) {
	   Set<T> set = new LinkedHashSet<>(values.length > 0 ? values.length : 1);
	   for(T value: values) {
	   	set.add(value);
	   }
	   return set;
   }

	public static <I, O> List<O> transform(List<I> input, Function<? super I, O> transform) {
		if(input == null) {
			return null;
		}
		
		List<O> output = new ArrayList<O>(input.size());
		for(I i: input) {
			O o = transform.apply(i);
			output.add(o);
		}
		return output;
	}
	
	public static <I, O> Set<O> transform(Set<I> input, Function<? super I, O> transform) {
		if(input == null) {
			return null;
		}
		
		Set<O> output = Sets.newHashSetWithExpectedSize(input.size());
		for(I i: input) {
			O o = transform.apply(i);
			output.add(o);
		}
		return output;
	}
	
	public static <K, I, O> Map<K, O> transform(Map<K, I> input, Function<? super I, O> transform) {
		if(input == null) {
			return null;
		}
		
		Map<K, O> output = Maps.newHashMapWithExpectedSize(input.size());
		for(Map.Entry<K, I> e: input.entrySet()) {
			O o = transform.apply(e.getValue());
			output.put(e.getKey(), o);
		}
		return output;
	}
	
	public static <K, V> MapBuilder<K, V> map() {
		return new MutableMapBuilder<K, V>();
	}

	public static <K, V> MapBuilder<K, V> immutableMap() {
		return new ImmutableMapBuilder<>();
	}

	public static <K, V> MapBuilder<K, V> concurrentMap() {
		return concurrentMap(1);
	}

	public static <K, V> MapBuilder<K, V> concurrentMap(int concurrency) {
		return new ConcurrentMapBuilder<>(concurrency);
	}

	public static <T> boolean addIfNotNull(Collection<T> collection, T element) {
	   if(element == null) {
	      return false;
	   }
	   return collection.add(element);
	}

	public interface MapBuilder<K, V> {

		public MapBuilder<K, V> put(K key, V value);
		
		public MapBuilder<K, V> putAll(Map<K, V> map);

		public MapBuilder<K, V> remove(K key);

		public Map<K, V> create();
	}

	private static abstract class MapBuilderImpl<K, V> implements MapBuilder<K, V> {
		protected final Map<K, V> delegate;

		MapBuilderImpl() {
	      this(new LinkedHashMap<K, V>());
      }

		MapBuilderImpl(Map<K, V> delegate) {
	      this.delegate = delegate;
      }

		@Override
      public MapBuilder<K, V> put(K key, V value) {
			delegate.put(key, value);
	      return this;
      }
		
		@Override
		public MapBuilder<K, V> putAll(Map<K, V> map) {
		   delegate.putAll(map);
		   return this;
		}

		@Override
      public MapBuilder<K, V> remove(K key) {
	      delegate.remove(key);
	      return this;
      }

	}

	private static class MutableMapBuilder<K, V> extends MapBuilderImpl<K, V> {
		MutableMapBuilder() {
	      super(new LinkedHashMap<K, V>());
      }

		@Override
      public Map<K, V> create() {
	      return new LinkedHashMap<K, V>(this.delegate);
      }
	}

	private static class ImmutableMapBuilder<K, V> extends MapBuilderImpl<K, V> {
		@Override
      public Map<K, V> create() {
	      return IrisCollections.unmodifiableCopy(this.delegate);
      }
	}

	private static class ConcurrentMapBuilder<K, V> extends MapBuilderImpl<K, V> {
		private final int concurrency;

		ConcurrentMapBuilder(int concurrency) {
			this.concurrency = concurrency;
		}

		@Override
      public Map<K, V> create() {
	      ConcurrentHashMap<K, V> map = new ConcurrentHashMap<K, V>(Math.max(this.delegate.size(), 16), 0.75f, concurrency);
	      map.putAll(this.delegate);
	      return map;
      }
	}
	
	public static <K, V> Map<K, V> toUnmodifiableMap(V[] elements, Function<V, K> keyFunction) {
      if(elements == null || elements.length == 0) {
         return Collections.emptyMap();
      }
      Map<K, V> map = new HashMap<>(elements.length);
      for(V element: elements) {
         K key = keyFunction.apply(element);
         if(key != null) {
            map.put(key, element);
         }
      }
      return Collections.unmodifiableMap(map);
   }

	public static <K, V> Map<K, V> toUnmodifiableMap(Collection<V> elements, Function<V, K> keyFunction) {
	   if(elements == null || elements.isEmpty()) {
	   	return Collections.emptyMap();
	   }
	   Map<K, V> map = new HashMap<>(elements.size());
	   for(V element: elements) {
	   	K key = keyFunction.apply(element);
	   	if(key != null) {
	   		map.put(key, element);
	   	}
	   }
	   return Collections.unmodifiableMap(map);
   }

	public static <V extends Copyable<V>> V findCopy(Collection<V> values, Predicate<? super V> p) {
	   for (V value : values) {
	      if (p.apply(value)) {
	         return value.copy();
	      }
	   }
	   return null;
	}
	
	public static <V extends Copyable<V>> List<V> deepCopyList(Collection<V> values) {
	   List<V> list = new ArrayList<>();
	   for (V value : values) {
	      list.add(value.copy());
	   }
	   return list;
	}

	public static <V extends Copyable<V>> List<V> collectCopies(Collection<V> values, Predicate<? super V> p) {
	   List<V> list = new ArrayList<>();
	   for (V value : values) {
	      if (p.apply(value)) {
	         list.add(value.copy());
	      }
	   }
	   return list;
	}

   public static boolean isEmpty(Map<?, ?> map) {
      return map == null || map.isEmpty();
   }

}

