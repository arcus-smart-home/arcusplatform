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
package com.iris.platform.history.cassandra;

import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.cassandra.CassandraModule;
import com.iris.core.dao.cassandra.CassandraResourceBundleDAOModule;
import com.iris.platform.history.HistoryActivityDAO;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogDAO;
import com.netflix.governator.annotations.Modules;

/**
 * 
 */
@Modules(include = { CassandraModule.class, CassandraResourceBundleDAOModule.class, CassandraHistoryModule.class })
public class CassandraHistoryDAOModule extends AbstractIrisModule {

   @Override
   protected void configure() {
      bind(HistoryAppenderDAO.class).to(CassandraHistoryAppenderDao.class);
      bind(HistoryLogDAO.class).to(CassandraHistoryLogDao.class);
      bind(HistoryActivityDAO.class).to(CassandraActivityDao.class);
   }

}

