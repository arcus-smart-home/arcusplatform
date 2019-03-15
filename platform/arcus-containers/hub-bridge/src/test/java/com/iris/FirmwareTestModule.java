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
package com.iris;

import java.net.URISyntaxException;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.firmware.FirmwareManager;
import com.iris.firmware.FirmwareUpdateResolver;
import com.iris.firmware.HubMinimumFirmwareVersionResolver;
import com.iris.firmware.MinimumFirmwareVersionResolver;
import com.iris.firmware.XMLFirmwareResolver;
import com.iris.messages.model.Hub;
import com.iris.population.DaoHubPopulationResolver;
import com.iris.population.HubPopulationResolver;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

public class FirmwareTestModule extends AbstractIrisModule {

   @Override
   protected void configure() {
      bind(HubPopulationResolver.class).to(DaoHubPopulationResolver.class);
      bind(FirmwareUpdateResolver.class).to(XMLFirmwareResolver.class);
   }

   @Provides @Singleton
   public MinimumFirmwareVersionResolver<Hub> hubMinimumFirmwareVersionResolver(PlaceDAO placeDao, PopulationDAO populationDao) {
      return new HubMinimumFirmwareVersionResolver(placeDao, populationDao);
   }

   @Provides @Singleton
   public FirmwareManager provideFirmwareManager() throws IllegalArgumentException, URISyntaxException {
      Resource firmwareUpdates = Resources.getResource("classpath:///test-firmware.xml");
      return new FirmwareManager(firmwareUpdates);
   }
}

