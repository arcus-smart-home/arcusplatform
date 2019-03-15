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
package com.iris.platform.scheduler;

import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.cassandra.CassandraModule;
import com.iris.platform.scheduler.cassandra.CassandraScheduleDao;
import com.iris.platform.scheduler.cassandra.CassandraSchedulerModelDao;
import com.netflix.governator.annotations.Modules;

/**
 * 
 */
@Modules(include=CassandraModule.class)
public class SchedulerDaoModule extends AbstractIrisModule {

   /* (non-Javadoc)
    * @see com.google.inject.AbstractModule#configure()
    */
   @Override
   protected void configure() {
      bind(SchedulerModelDao.class).to(CassandraSchedulerModelDao.class);
      bind(ScheduleDao.class).to(CassandraScheduleDao.class);
   }

}

