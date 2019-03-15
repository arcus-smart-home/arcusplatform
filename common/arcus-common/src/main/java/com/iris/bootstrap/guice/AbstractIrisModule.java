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

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.netflix.governator.configuration.ConfigurationKey;
import com.netflix.governator.configuration.ConfigurationProvider;

/**
 *
 */
public abstract class AbstractIrisModule extends AbstractModule {

	protected <K, V> MapBinder<K, V> bindMapOf(Class<K> keyType, Class<V> valueType) {
		return MapBinder.newMapBinder(binder(), keyType, valueType);
	}

	protected <K, V> MapBinder<K, V> bindMapOf(TypeLiteral<K> keyType, TypeLiteral<V> valueType) {
		return MapBinder.newMapBinder(binder(), keyType, valueType);
	}

	protected <T> Multibinder<T> bindSetOf(Class<T> type) {
		return Multibinder.newSetBinder(binder(), type);
	}

	protected <T> Multibinder<T> bindSetOf(TypeLiteral<T> type) {
		return Multibinder.newSetBinder(binder(), type);
	}

	protected <T> ScopedBindingBuilder bindMapToInstancesOf(Class<T> containedType) {
		return Binders.bindMapToInstancesOf(this.binder(), containedType);
	}

	protected <T> ScopedBindingBuilder bindMapToInstancesOf(TypeLiteral<Map<String, T>> type) {
		return Binders.bindMapToInstancesOf(this.binder(), type);
   }

	protected <K, V> ScopedBindingBuilder bindMapToInstancesOf(TypeLiteral<Map<K, V>> type, Function<V, K> keyFunction) {
		return Binders.bindMapToInstancesOf(binder(), type, keyFunction);
   }

	protected <T> ScopedBindingBuilder bindListToInstancesOf(Class<T> containedType) {
		return Binders.bindListToInstancesOf(this.binder(), containedType);
	}

	protected <T> ScopedBindingBuilder bindListToInstancesOf(Class<T> containedType, Comparator<T> comparator) {
		return Binders.bindListToInstancesOf(this.binder(), containedType, comparator);
	}

	protected <T> ScopedBindingBuilder bindListToInstancesOf(TypeLiteral<List<T>> type) {
		return Binders.bindListToInstancesOf(this.binder(), type);
   }

	protected <T> ScopedBindingBuilder bindListToInstancesOf(TypeLiteral<List<T>> type, Comparator<T> comparator) {
		return Binders.bindListToInstancesOf(this.binder(), type, comparator);
   }

   protected <T> T getConfig(ConfigurationProvider config, ConfigurationKey key, Class<T> type, T dflt) {
   	if(config.has(key)) {
   		return config.getObjectSupplier(key, dflt, type).get();
   	}
   	else {
   		return dflt;
   	}
	}

}

