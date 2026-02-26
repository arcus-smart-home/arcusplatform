package com.iris.bootstrap.config;

import com.google.common.base.Supplier;

/**
 * A typed configuration property that can be dynamically resolved.
 *
 * Drop-in replacement for com.netflix.governator.configuration.Property.
 */
public abstract class Property<T> implements Supplier<T> {
   @Override
   public abstract T get();
}
