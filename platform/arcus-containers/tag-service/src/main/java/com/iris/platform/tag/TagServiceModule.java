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
package com.iris.platform.tag;

import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static com.iris.platform.tag.TagServiceListener.NAME_EXECUTOR_POOL;
import static com.iris.platform.tag.TagServiceListener.PROP_HANDLERS;

import java.util.concurrent.ExecutorService;

import javax.annotation.PreDestroy;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.EmptyResourceBundle;
import com.iris.core.dao.ResourceBundleDAO;
import com.iris.core.messaging.kafka.KafkaModule;
import com.iris.population.PlacePopulationCacheModule;
import com.iris.util.ThreadPoolBuilder;
import com.netflix.governator.annotations.Modules;

@Modules(include = {
   KafkaModule.class,
   PlacePopulationCacheModule.class
})
public class TagServiceModule extends AbstractIrisModule
{
   @Inject
   private TagServiceConfig config;

   private ExecutorService executor;

   @Override
   protected void configure()
   {
      Multibinder<Object> handlers = newSetBinder(binder(), Object.class, Names.named(PROP_HANDLERS));
      handlers.addBinding().to(TaggedEventHandler.class);

      bind(TagServiceListener.class).asEagerSingleton();

      // tag-service doesn't use any DAO functionality, so this binding is just to avoid Guice startup exceptions
      bind(ResourceBundleDAO.class).to(EmptyResourceBundle.class);

      executor = new ThreadPoolBuilder()
         .withBlockingBacklog()
         .withMaxPoolSize(config.getThreads())
         .withKeepAliveMs(config.getKeepAliveMs())
         .withNameFormat("tag-%d")
         .withMetrics("tag")
         .build();
   }

   @Provides @Singleton @Named(NAME_EXECUTOR_POOL)
   public ExecutorService provideTagThreadPool()
   {
      return executor;
   }

   @PreDestroy
   public void stop()
   {
      executor.shutdown();
   }
}

