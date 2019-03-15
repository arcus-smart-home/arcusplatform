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
package com.iris.bootstrap.guice;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.ScopedBindingBuilder;

/**
 * Extensions for binding special type resolvers.
 */
// TODO is there a way to auto-detect these injection points and bind them on demand?
public class Binders {

	/**
	 * Adds a Binder for Map<String, T> to anything in the context
	 * that keyed to a type that implements T.  Note that this has to be a literal class
	 * because runtime reflection can't handle generics.
	 * @param binder
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
   public static <T> ScopedBindingBuilder bindMapToInstancesOf(Binder binder, final Class<T> type) {
		return bindMapToInstancesOf(
				binder,
				(TypeLiteral<Map<String, T>>) TypeLiteral.get(new ParameterizedType() {
   				final Type [] args = new Type[] { String.class, type };
					@Override
					public Type getRawType() {
						return Map.class;
					}

					@Override
					public Type getOwnerType() {
						return null;
					}

					@Override
					public Type[] getActualTypeArguments() {
						return args;
					}
				})
			);
	}

   public static <T> ScopedBindingBuilder bindMapToInstancesOf(Binder binder, TypeLiteral<Map<String, T>> type) {
   	return bindMapToInstancesOf(binder, type, new Function<T,String>() {
   	   @Override
   	   public String apply(T service) {
   	      return Injectors.getServiceName(service);
   	   }
   	});
	}

   public static <K, V> ScopedBindingBuilder bindMapToInstancesOf(Binder binder, TypeLiteral<Map<K, V>> type, Function<V, K> keyFunction) {
   	Class<V> containedType = extractContainedMapType(type);
		return binder
					.bind(type)
					.toProvider(new MapProvider<K, V>(containedType, keyFunction));
	}

   public static <T> ScopedBindingBuilder bindListToInstancesOf(Binder binder, Class<T> type) {
		return bindListToInstancesOf(binder, type, null);
	}

	@SuppressWarnings("unchecked")
   public static <T> ScopedBindingBuilder bindListToInstancesOf(Binder binder, final Class<T> type, Comparator<T> comparator) {
		return bindListToInstancesOf(
				binder,
				(TypeLiteral<List<T>>) TypeLiteral.get(new ParameterizedType() {
   				final Type [] args = new Type[] { type };
					@Override
					public Type getRawType() {
						return List.class;
					}

					@Override
					public Type getOwnerType() {
						return null;
					}

					@Override
					public Type[] getActualTypeArguments() {
						return args;
					}
				}),
				comparator
			);
	}

   public static <T> ScopedBindingBuilder bindListToInstancesOf(Binder binder, TypeLiteral<List<T>> type) {
   	return bindListToInstancesOf(binder, type, null);
   }

   public static <T> ScopedBindingBuilder bindListToInstancesOf(Binder binder, TypeLiteral<List<T>> type, Comparator<T> comparator) {
   	Class<T> containedType = extractContainedListType(type);
		return binder
					.bind(type)
					.toProvider(new ListProvider<T>(containedType, comparator));
	}

	public static String getServiceName(Object service) {
		Preconditions.checkNotNull(service, "service");
		Class<?> type = service.getClass();
		{
   		javax.inject.Named name = type.getAnnotation(javax.inject.Named.class);
   		if(name != null) {
   			return name.value();
   		}
		}
		{
   		com.google.inject.name.Named name = type.getAnnotation(com.google.inject.name.Named.class);
   		if(name != null) {
   			return name.value();
   		}
		}
		return type.getSimpleName();
	}

	@SuppressWarnings("unchecked")
   private static <T> Class<T> extractContainedListType(TypeLiteral<List<T>> type) {
		return (Class<T>) extractContainedType(type, 0);
	}

	@SuppressWarnings("unchecked")
   private static <K, T> Class<T> extractContainedMapType(TypeLiteral<Map<K, T>> type) {
		return (Class<T>) extractContainedType(type, 1);
	}

	private static Class<?> extractContainedType(TypeLiteral<?> type, int index) {
		ParameterizedType containerType = (ParameterizedType) type.getType();
		Type containedType = containerType.getActualTypeArguments()[index];
		if(containedType instanceof Class) {
			return (Class<?>) containedType;
		}
		if(containedType instanceof ParameterizedType) {
			ParameterizedType pContainedType = (ParameterizedType) containedType;
			for(Type subtype: pContainedType.getActualTypeArguments()) {
				checkWildcard(subtype, pContainedType);
			}
			return (Class<?>) pContainedType.getRawType();
		}
		throw new ProvisionException("Unable to determine value type of map, contained type: [" + containedType + "].  This must either be a non-generic object or a generic with all ? for the types.");
	}

	private static void checkWildcard(Type subtype, ParameterizedType parentType) {
		if(subtype instanceof WildcardType) {
			// TODO check bounds
			return;
		}
		throw new ProvisionException("Invalid plugin binding [" + parentType + "].  Types may only be non-generic or generics with a wildcard (?) specification.");
   }

	private static class MapProvider<K, T> implements Provider<Map<K, T>> {
		private final Class<T> containedType;
		private final Function<T, K> keyFunction;
		@Inject private Injector injector;

		MapProvider(Class<T> containedType, Function<T, K> keyFunction) {
			this.containedType = containedType;
			this.keyFunction = keyFunction;
		}

		@Override
      public Map<K, T> get() {
			Preconditions.checkNotNull(injector, "The injector was not properly exposed to this instance");
			return Injectors.mapInstancesOf(injector, containedType, keyFunction);
		}

	}

	private static class ListProvider<T> implements Provider<List<T>> {
		private final Class<T> containedType;
		private final Comparator<T> comparator;
		@Inject private Injector injector;

		ListProvider(Class<T> containedType, Comparator<T> comparator) {
			this.containedType = containedType;
			this.comparator = comparator;
		}

		@Override
		public List<T> get() {
			Preconditions.checkNotNull(injector, "The injector was not properly exposed to this instance");
			List<T> services = Injectors.listInstancesOf(injector, containedType);
			if(comparator != null) {
				Collections.sort(services, comparator);
			}
         return services;
		}

	}

}

