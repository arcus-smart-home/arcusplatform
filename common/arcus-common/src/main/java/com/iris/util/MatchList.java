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
import java.util.Collections;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;

/**
 * Walks a list of predicates to find the first best match.
 */
public class MatchList<T, V> {

	public static <T, V> MatchList.Builder<T, V> builder() {
		return new Builder<T, V>();
	}

	public static <T, V> MatchList.Builder<T, V> builder(Predicate<T> predicate, Supplier<V> value) {
		return new Builder<T, V>();
	}

	public static <T, V> MatchList.Builder<T, V> builder(Predicate<T> predicate, V value) {
		return new Builder<T, V>();
	}

	private final List<Entry<T, V>> entries;
	private final V fallback;

	private MatchList(List<Entry<T, V>> entries, V fallback) {
		this.entries = Collections.unmodifiableList(entries);
		this.fallback = fallback;
	}

	public V match(T input) {
		for(Entry<T, V> e: entries) {
			if(e.predicate.apply(input)) {
				return e.supplier.get();
			}
		}
		return fallback;
	}

	private static class Entry<T, V> {
		private final Predicate<T> predicate;
		private final Supplier<V> supplier;

		Entry(Predicate<T> predicate, Supplier<V> supplier) {
			this.predicate = predicate;
			this.supplier = supplier;
		}

      /* (non-Javadoc)
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString() {
         return "MatchList$Entry [predicate=" + predicate + ", supplier=" + supplier
               + "]";
      }

	}

	public static class Builder<T, V> {
		private ArrayList<Entry<T, V>> entries = new ArrayList<>();
		private V fallback = null;

		public Builder<T,V> addSupplier(Predicate<T> predicate, Supplier<V> supplier) {
			entries.add(new Entry<T, V>(predicate, supplier));
			return this;
		}

		public Builder<T,V> addValue(Predicate<T> predicate, final V value) {
			return addSupplier(predicate, new Supplier<V>() {
			   @Override
			   public V get() {
			      return value;
			   }
			});
		}

		public Builder<T, V> withFallbackValue(V fallback) {
			this.fallback = fallback;
			return this;
		}

		@SuppressWarnings("unchecked")
      public MatchList<T, V> create() {
			return new MatchList<T, V>((List<Entry<T, V>>) entries.clone(), fallback);
		}

	}
}

