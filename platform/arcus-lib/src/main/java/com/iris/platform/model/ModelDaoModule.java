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
package com.iris.platform.model;

import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.cassandra.CassandraAccountDAOModule;
import com.iris.core.dao.cassandra.CassandraDeviceDAOModule;
import com.iris.core.dao.cassandra.CassandraHubDAOModule;
import com.iris.core.dao.cassandra.CassandraPersonDAOModule;
import com.iris.core.dao.cassandra.CassandraPersonPlaceAssocDAOModule;
import com.iris.core.dao.cassandra.CassandraPlaceDAOModule;
import com.iris.platform.model.wrapper.DelegatingModelDao;
import com.iris.platform.pairing.PairingDeviceDaoModule;
import com.iris.platform.rule.RuleDaoModule;
import com.iris.platform.scheduler.SchedulerDaoModule;
import com.iris.platform.subsystem.SubsystemDaoModule;
import com.netflix.governator.annotations.Modules;

@Modules(include={
      CassandraAccountDAOModule.class,
      CassandraDeviceDAOModule.class,
      CassandraHubDAOModule.class,
      CassandraPersonDAOModule.class,
      CassandraPersonPlaceAssocDAOModule.class,
      CassandraPlaceDAOModule.class,
      PairingDeviceDaoModule.class,
      RuleDaoModule.class,
      SubsystemDaoModule.class,
      SchedulerDaoModule.class
})
public class ModelDaoModule extends AbstractIrisModule {

   @Override
   protected void configure() {
      bind(ModelDao.class).to(DelegatingModelDao.class);
   }

}

