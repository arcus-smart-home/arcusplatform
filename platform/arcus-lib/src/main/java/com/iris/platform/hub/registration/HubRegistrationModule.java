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
package com.iris.platform.hub.registration;

import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.HubRegistrationDAO;
import com.iris.core.dao.cassandra.CassandraModule;
import com.iris.core.dao.cassandra.CassandraResourceBundleDAOModule;
import com.iris.core.dao.cassandra.HubRegistrationDAOImpl;
import com.iris.platform.manufacture.kitting.dao.ManufactureKittingDao;
import com.iris.platform.manufacture.kitting.dao.cassandra.ManufactureKittingDaoImpl;
import com.netflix.governator.annotations.Modules;

@Modules(include = { CassandraModule.class
})
public class HubRegistrationModule extends AbstractIrisModule {

   @Override
   protected void configure() {
	  bind(ManufactureKittingDao.class).to(ManufactureKittingDaoImpl.class);
      bind(HubRegistrationDAO.class).to(HubRegistrationDAOImpl.class);
      bind(HubRegistrationConfig.class);
      bind(HubRegistrationRegistry.class);
      bind(HubRegistrationMessageHandlerAdaptor.class);
   }

}

