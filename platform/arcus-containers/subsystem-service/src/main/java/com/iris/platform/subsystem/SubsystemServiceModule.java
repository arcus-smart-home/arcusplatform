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
package com.iris.platform.subsystem;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.cassandra.CassandraAuthorizationGrantDAOModule;
import com.iris.core.dao.cassandra.CassandraPersonDAOModule;
import com.iris.core.dao.cassandra.CassandraPersonPlaceAssocDAOModule;
import com.iris.core.dao.cassandra.CassandraPlaceDAOModule;
import com.iris.core.dao.cassandra.CassandraResourceBundleDAOModule;
import com.iris.core.messaging.kafka.KafkaModule;
import com.iris.core.template.TemplateModule;
import com.iris.messages.MessageConstants;
import com.iris.messages.address.Address;
import com.iris.messages.service.SubsystemService;
import com.iris.platform.history.cassandra.CassandraHistoryDAOModule;
import com.iris.platform.manufacture.kitting.dao.ManufactureKittingDaoModule;
import com.iris.platform.model.ModelDaoModule;
import com.iris.platform.rule.RuleDaoModule;
import com.iris.platform.subsystem.incident.AlarmIncidentModule;
import com.iris.platform.subsystem.placemonitor.smarthomealert.SmartHomeAlertModule;
import com.iris.population.PlacePopulationCacheModule;
import com.iris.prodcat.ProductCatalogReloadListener;
import com.netflix.governator.annotations.Modules;

@Modules(include={
      KafkaModule.class,
      SubsystemDaoModule.class,
      CassandraAuthorizationGrantDAOModule.class,
      CassandraHistoryDAOModule.class,
      CassandraPersonDAOModule.class,
      CassandraPersonPlaceAssocDAOModule.class,
      CassandraPlaceDAOModule.class,
      CassandraResourceBundleDAOModule.class,
      RuleDaoModule.class,
      ModelDaoModule.class,
      SubsystemModule.class,
      AlarmIncidentModule.class,
      ManufactureKittingDaoModule.class,
      PlacePopulationCacheModule.class
})
public class SubsystemServiceModule extends AbstractIrisModule {

   @Override
   protected void configure() {

   }
   
   @Provides
   @Singleton
   @Named(ProductCatalogReloadListener.GENERIC_MESSAGE_BUS_ACTOR_ADDRESS)
   public Address provideMessageBusActorAddress() {
      return Address.fromString(MessageConstants.SERVICE + ":" + SubsystemService.NAMESPACE + ":");
   }

}

