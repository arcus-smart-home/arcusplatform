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
/**
 * 
 */
package com.iris.platform.cluster;

import java.time.Clock;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.cassandra.CassandraModule;
import com.iris.platform.cluster.cassandra.CassandraClusterServiceDao;
import com.iris.util.ThreadPoolBuilder;
import com.netflix.governator.annotations.Modules;

/**
 * 
 */
@Modules(include = { CassandraModule.class })
public class ClusterModule extends AbstractIrisModule {

   @Override
   protected void configure() {
      // TODO move Clock to a more generic module
      bind(Clock.class).toInstance(Clock.systemUTC());
      bind(ClusterService.class).asEagerSingleton();
      bind(ClusterServiceDao.class).to(CassandraClusterServiceDao.class);
      OptionalBinder.newOptionalBinder(binder(), new TypeLiteral<Set<ClusterServiceListener>>() {});
   }

   @Provides @Named(ClusterService.NAME_EXECUTOR) @Singleton
   public ScheduledExecutorService clusterServiceHeartbeatExecutor() {
      return ThreadPoolBuilder.newSingleThreadedScheduler("cluster-heartbeat-executor");
   }
}

