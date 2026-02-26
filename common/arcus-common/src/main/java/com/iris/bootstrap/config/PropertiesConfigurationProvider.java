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

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * ConfigurationProvider backed by a Properties object.
 *
 * Drop-in replacement for com.netflix.governator.configuration.PropertiesConfigurationProvider.
 */
public class PropertiesConfigurationProvider implements ConfigurationProvider {
   private static final Map<String, String> EMPTY_VARS = Collections.emptyMap();
   private final Properties properties;

   public PropertiesConfigurationProvider(Properties properties) {
      this.properties = properties;
   }

   @Override
   public boolean has(ConfigurationKey key) {
      return properties.containsKey(key.getKey(EMPTY_VARS));
   }

   @Override
   public Supplier<String> getStringSupplier(ConfigurationKey key, String defaultValue) {
      return () -> {
         String val = properties.getProperty(key.getKey(EMPTY_VARS));
         return val != null ? val : defaultValue;
      };
   }

   @Override
   public Supplier<Integer> getIntegerSupplier(ConfigurationKey key, Integer defaultValue) {
      return () -> {
         String val = properties.getProperty(key.getKey(EMPTY_VARS));
         if (val == null || val.isEmpty()) return defaultValue;
         try { return Integer.parseInt(val.trim()); }
         catch (NumberFormatException e) { return defaultValue; }
      };
   }

   @Override
   public Supplier<Long> getLongSupplier(ConfigurationKey key, Long defaultValue) {
      return () -> {
         String val = properties.getProperty(key.getKey(EMPTY_VARS));
         if (val == null || val.isEmpty()) return defaultValue;
         try { return Long.parseLong(val.trim()); }
         catch (NumberFormatException e) { return defaultValue; }
      };
   }

   @Override
   public Supplier<Boolean> getBooleanSupplier(ConfigurationKey key, Boolean defaultValue) {
      return () -> {
         String val = properties.getProperty(key.getKey(EMPTY_VARS));
         if (val == null || val.isEmpty()) return defaultValue;
         return Boolean.parseBoolean(val.trim());
      };
   }

   @Override
   public Supplier<Double> getDoubleSupplier(ConfigurationKey key, Double defaultValue) {
      return () -> {
         String val = properties.getProperty(key.getKey(EMPTY_VARS));
         if (val == null || val.isEmpty()) return defaultValue;
         try { return Double.parseDouble(val.trim()); }
         catch (NumberFormatException e) { return defaultValue; }
      };
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> Supplier<T> getObjectSupplier(ConfigurationKey key, T defaultValue, Class<T> type) {
      return () -> {
         String val = properties.getProperty(key.getKey(EMPTY_VARS));
         if (val == null || val.isEmpty()) return defaultValue;
         try {
            Object result;
            if (type == String.class) result = val;
            else if (type == Integer.class || type == int.class) result = Integer.parseInt(val.trim());
            else if (type == Long.class || type == long.class) result = Long.parseLong(val.trim());
            else if (type == Boolean.class || type == boolean.class) result = Boolean.parseBoolean(val.trim());
            else if (type == Double.class || type == double.class) result = Double.parseDouble(val.trim());
            else if (type == Float.class || type == float.class) result = Float.parseFloat(val.trim());
            else return defaultValue;
            return (T) result;
         } catch (Exception e) {
            return defaultValue;
         }
      };
   }

   @Override
   public Property<String> getStringProperty(ConfigurationKey key, String defaultValue) {
      return new Property<String>() {
         @Override public String get() { return getStringSupplier(key, defaultValue).get(); }
      };
   }

   @Override
   public Property<Integer> getIntegerProperty(ConfigurationKey key, Integer defaultValue) {
      return new Property<Integer>() {
         @Override public Integer get() { return getIntegerSupplier(key, defaultValue).get(); }
      };
   }

   @Override
   public Property<Long> getLongProperty(ConfigurationKey key, Long defaultValue) {
      return new Property<Long>() {
         @Override public Long get() { return getLongSupplier(key, defaultValue).get(); }
      };
   }

   @Override
   public Property<Boolean> getBooleanProperty(ConfigurationKey key, Boolean defaultValue) {
      return new Property<Boolean>() {
         @Override public Boolean get() { return getBooleanSupplier(key, defaultValue).get(); }
      };
   }

   @Override
   public Property<Double> getDoubleProperty(ConfigurationKey key, Double defaultValue) {
      return new Property<Double>() {
         @Override public Double get() { return getDoubleSupplier(key, defaultValue).get(); }
      };
   }
}
