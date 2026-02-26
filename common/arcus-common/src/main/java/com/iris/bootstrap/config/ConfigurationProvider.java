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
