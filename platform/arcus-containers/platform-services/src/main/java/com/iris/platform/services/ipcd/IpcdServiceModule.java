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
package com.iris.platform.services.ipcd;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import javax.annotation.PreDestroy;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.cassandra.CassandraDAOModule;
import com.iris.core.messaging.kafka.KafkaModule;
import com.iris.core.protocol.ipcd.IpcdDeviceDaoModule;
import com.iris.util.ThreadPoolBuilder;

public class IpcdServiceModule extends AbstractIrisModule {

   @Inject(optional = true) @Named("ipcd.service.threads.max")
   private int threads = 100;
   @Inject(optional = true) @Named("ipcd.service.threads.keepAliveMs")
   private int keepAliveMs = 10000;

   private ExecutorService serviceExecutor;

   @Inject
   public IpcdServiceModule(
         KafkaModule kafka,
         CassandraDAOModule daoModule,
         IpcdDeviceDaoModule ipcdDeviceModule
   ) {
   }

   @PreDestroy
   public void shutdown() {
      serviceExecutor.shutdown();
   }

   @Override
   protected void configure() {
      serviceExecutor =
            new ThreadPoolBuilder()
               .withBlockingBacklog()
               .withMaxPoolSize(threads)
               .withNameFormat("ipcd-service-%d")
               .withMetrics("service.ipcd")
               .build();

      bind(IpcdService.class);
   }

   @Provides @Singleton @Named(IpcdService.PROP_THREADPOOL)
   public Executor ruleExecutor() {
      return serviceExecutor;
   }
}

