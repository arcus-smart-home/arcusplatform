package com.iris.bootstrap.guice;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import javax.inject.Singleton;

/**
 * Guice module that enables lifecycle management via @PostConstruct,
 * @PreDestroy, and @WarmUp annotations.
 *
 * Install this module in your injector to get Governator-like lifecycle
 * behavior with plain Guice.
 */
public class LifecycleModule extends AbstractModule {

   @Override
   protected void configure() {
      IrisLifecycleManager manager = new IrisLifecycleManager();
      bind(IrisLifecycleManager.class).toInstance(manager);

      bindListener(Matchers.any(), new TypeListener() {
         @Override
         public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            encounter.register((InjectionListener<I>) injectee -> manager.register(injectee));
         }
      });
   }
}
