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
package com.iris.bootstrap.guice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.iris.annotation.Version;

public class Injectors {
	private Injectors() {}

	public static <T> T findInstanceOf(Injector injector, Class<T> type) {
		for(Binding<?> binding: collectBindings(injector)) {
			if(type.isAssignableFrom(binding.getKey().getTypeLiteral().getRawType())) {
				return (T) binding.getProvider().get();
			}
		}
		throw new IllegalStateException("No implementation of [" + type + "] found!");
	}

	public static <T> List<T> listInstancesOf(Injector injector, Class<T> type) {
		final List<T> instances = new ArrayList<>();
		visitBindings(injector, type, new BindingVisitor<T>() {
		   @Override
		   public void visit(Binding<T> binding) {
		      instances.add(binding.getProvider().get());
		   }
		});
		return instances;
	}

	public static <T> Map<String, T> mapInstancesOf(Injector injector, Class<T> type) {
		return mapInstancesOf(injector, type, new Function<T,String>() {
		   @Override
		   public String apply(T value) {
		      return Injectors.getServiceName(value);
		   }
		});
	}

	public static <K, T> Map<K, T> mapInstancesOf(Injector injector, Class<T> type, final Function<T, K> keyFunction) {
		Preconditions.checkNotNull(injector, "injector");
		Preconditions.checkNotNull(type, "type");
		Preconditions.checkNotNull(keyFunction, "keyFunction");

		final Map<K, T> results = new LinkedHashMap<K, T>();
		visitBindings(injector, type, new BindingVisitor<T>() {
		   @Override
		   public void visit(Binding<T> binding) {
			   T value = binding.getProvider().get();
			   K key = keyFunction.apply(value);
			   results.put(key, value);
         }
		});
		return results;
	}

	// TODO this isn't necessarilly specific to Guice, move somewhere else?
	public static String getServiceName(Object value) {
		Preconditions.checkNotNull(value, "value");
		Class<?> type = value.getClass();
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

	// TODO this isn't necessarilly specific to Guice, move somewhere else?
	public static com.iris.model.Version getServiceVersion(Object service) {
		com.iris.model.Version version = getServiceVersion(service, null);
	   if(version == null) {
	   	throw new IllegalArgumentException(service.getClass() + " is not versioned!");
	   }
	   return version;
   }

	public static com.iris.model.Version getServiceVersion(Object service, com.iris.model.Version dflt) {
		Preconditions.checkNotNull(service, "service");
	   Version v = service.getClass().getAnnotation(Version.class);
	   if(v == null) {
	   	return dflt;
	   }

	   return new com.iris.model.Version(v.value(), v.minor(), v.qualifier());
   }

	private static <T> void visitBindings(
			final Injector injector,
			final Class<T> instanceOf,
			final BindingVisitor<T> visitor
	) {
	   for (Binding<?> binding : collectBindings(injector)) {
			Key<?> key = binding.getKey();
			if(instanceOf.isAssignableFrom(key.getTypeLiteral().getRawType())) {
				visitor.visit((Binding<T>) binding);
			}
	   }
	}

	private static List<Binding<?>> collectBindings(Injector injector) {
	   List<Injector> injectors = new ArrayList<Injector>();
	   Injector i = injector;
	   while(i != null) {
	   	injectors.add(0, i);
	   	i = i != i.getParent() ? i.getParent() : null;
	   }

	   List<Binding<?>> bindings = new ArrayList<>();
	   for(Injector i2: injectors) {
	   	bindings.addAll(i2.getBindings().values());
	   }
	   return bindings;
   }

	static interface BindingVisitor<T> {
		public void visit(Binding<T> binding);
	}

}

