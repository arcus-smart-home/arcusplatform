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
