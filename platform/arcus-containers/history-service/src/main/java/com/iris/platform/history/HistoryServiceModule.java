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
package com.iris.platform.history;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.cassandra.CassandraAlarmIncidentDAOModule;
import com.iris.core.dao.cassandra.CassandraAuthorizationGrantDAOModule;
import com.iris.core.dao.cassandra.CassandraDeviceDAOModule;
import com.iris.core.dao.cassandra.CassandraHubDAOModule;
import com.iris.core.dao.cassandra.CassandraPersonDAOModule;
import com.iris.core.dao.cassandra.CassandraPlaceDAOModule;
import com.iris.core.messaging.kafka.KafkaModule;
import com.iris.platform.history.appender.HistoryAppenderModule;
import com.iris.platform.history.cassandra.CassandraHistoryDAOModule;
import com.iris.platform.history.service.HistoryEventListener;
import com.iris.platform.rule.RuleDaoModule;
import com.netflix.governator.annotations.Modules;

/**
 * 
 */
@Modules(include={ 
      KafkaModule.class, 
      HistoryAppenderModule.class, 
      CassandraHistoryDAOModule.class,
      CassandraPlaceDAOModule.class,
      CassandraDeviceDAOModule.class,
      CassandraAlarmIncidentDAOModule.class,
      CassandraHubDAOModule.class,
      CassandraPersonDAOModule.class,
      CassandraAuthorizationGrantDAOModule.class,
      RuleDaoModule.class
})
public class HistoryServiceModule extends AbstractIrisModule {
   
   @Inject(optional = true)
   @Named(value = "history.appenders.path")
   private String historyAppendersPath = "conf/history.xml";

   @Override
   protected void configure() {
      // make it go
      bind(HistoryEventListener.class).asEagerSingleton();
   }

}

