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
package com.iris.hubcom.server;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.iris.core.dao.cassandra.CassandraHubDAOModule;
import com.iris.core.dao.cassandra.CassandraPlaceDAOModule;
import com.iris.core.dao.file.PopulationDAOModule;
import com.iris.firmware.HubMinimumFirmwareVersionResolver;
import com.iris.firmware.MinimumFirmwareVersionResolver;
import com.iris.firmware.hub.HubFirmwareModule;
import com.iris.messages.model.Hub;
import com.iris.population.DaoHubPopulationResolver;
import com.iris.population.HubPopulationResolver;

public class PopulationAwareFirmwareModule extends HubFirmwareModule {

   @Inject
   public PopulationAwareFirmwareModule(CassandraHubDAOModule hubDao, CassandraPlaceDAOModule placeDao, PopulationDAOModule populationDao) {
   }

   @Override
   protected void configure() {
      super.configure();
      bind(HubPopulationResolver.class).to(DaoHubPopulationResolver.class);
      bind(new TypeLiteral<MinimumFirmwareVersionResolver<Hub>>(){}).to(new TypeLiteral<HubMinimumFirmwareVersionResolver>(){});
   }

}

