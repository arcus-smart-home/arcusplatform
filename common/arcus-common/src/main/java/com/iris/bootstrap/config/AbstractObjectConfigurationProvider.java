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
 * Base class for ConfigurationProvider implementations that back each
 * supplier method with a Property object.
 *
 * Drop-in replacement for com.netflix.governator.configuration.AbstractObjectConfigurationProvider.
 */
public abstract class AbstractObjectConfigurationProvider implements ConfigurationProvider {

   @Override
   public boolean has(ConfigurationKey key) {
      return true;
   }

   @Override
   public Supplier<String> getStringSupplier(ConfigurationKey key, String defaultValue) {
      return getStringProperty(key, defaultValue);
   }

   @Override
   public Supplier<Integer> getIntegerSupplier(ConfigurationKey key, Integer defaultValue) {
      return getIntegerProperty(key, defaultValue);
   }

   @Override
   public Supplier<Long> getLongSupplier(ConfigurationKey key, Long defaultValue) {
      return getLongProperty(key, defaultValue);
   }

   @Override
   public Supplier<Boolean> getBooleanSupplier(ConfigurationKey key, Boolean defaultValue) {
      return getBooleanProperty(key, defaultValue);
   }

   @Override
   public Supplier<Double> getDoubleSupplier(ConfigurationKey key, Double defaultValue) {
      return getDoubleProperty(key, defaultValue);
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> Supplier<T> getObjectSupplier(ConfigurationKey key, T defaultValue, Class<T> type) {
      if (type == String.class) return (Supplier<T>)(Supplier<?>)getStringSupplier(key, (String) defaultValue);
      if (type == Integer.class || type == int.class) return (Supplier<T>)(Supplier<?>)getIntegerSupplier(key, (Integer) defaultValue);
      if (type == Long.class || type == long.class) return (Supplier<T>)(Supplier<?>)getLongSupplier(key, (Long) defaultValue);
      if (type == Boolean.class || type == boolean.class) return (Supplier<T>)(Supplier<?>)getBooleanSupplier(key, (Boolean) defaultValue);
      if (type == Double.class || type == double.class) return (Supplier<T>)(Supplier<?>)getDoubleSupplier(key, (Double) defaultValue);
      return () -> defaultValue;
   }
}
