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
