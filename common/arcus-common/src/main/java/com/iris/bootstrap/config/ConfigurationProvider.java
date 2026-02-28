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
package com.iris.bootstrap.config;

import com.google.common.base.Supplier;

/**
 * Provides typed access to configuration properties.
 *
 * Drop-in replacement for com.netflix.governator.configuration.ConfigurationProvider.
 */
public interface ConfigurationProvider {

   boolean has(ConfigurationKey key);

   Supplier<String> getStringSupplier(ConfigurationKey key, String defaultValue);

   Supplier<Integer> getIntegerSupplier(ConfigurationKey key, Integer defaultValue);

   Supplier<Long> getLongSupplier(ConfigurationKey key, Long defaultValue);

   Supplier<Boolean> getBooleanSupplier(ConfigurationKey key, Boolean defaultValue);

   Supplier<Double> getDoubleSupplier(ConfigurationKey key, Double defaultValue);

   <T> Supplier<T> getObjectSupplier(ConfigurationKey key, T defaultValue, Class<T> type);

   Property<String> getStringProperty(ConfigurationKey key, String defaultValue);

   Property<Integer> getIntegerProperty(ConfigurationKey key, Integer defaultValue);

   Property<Long> getLongProperty(ConfigurationKey key, Long defaultValue);

   Property<Boolean> getBooleanProperty(ConfigurationKey key, Boolean defaultValue);

   Property<Double> getDoubleProperty(ConfigurationKey key, Double defaultValue);
}
