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
package com.iris.platform.services;

import com.google.inject.multibindings.Multibinder;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.platform.PlatformService;
import com.iris.platform.address.validation.smartystreets.HttpSmartyStreetsModule;
import com.iris.platform.pairing.PairingDeviceObjectModule;
import com.iris.platform.pairing.PairingDeviceServiceModule;
import com.iris.platform.services.account.AccountServiceModule;
import com.iris.platform.services.hub.HubServiceModule;
import com.iris.platform.services.intraservice.IntraServiceModule;
import com.iris.platform.services.ipcd.IpcdServiceModule;
import com.iris.platform.services.mobiledevice.MobileDeviceServiceModule;
import com.iris.platform.services.person.PersonServiceModule;
import com.iris.platform.services.place.PlaceServiceModule;
import com.iris.platform.services.population.PopulationServiceModule;
import com.iris.platform.services.productcatalog.ProductCatalogServiceModule;
import com.iris.platform.subsystem.SubsystemDaoModule;
import com.iris.population.PlacePopulationCacheModule;
import com.netflix.governator.annotations.Modules;

/**
 * Defines all the services to be loaded.
 */
@Modules(include={
      AccountServiceModule.class,
      HttpSmartyStreetsModule.class,
      SubsystemDaoModule.class,
      HubServiceModule.class,
      IpcdServiceModule.class,
      MobileDeviceServiceModule.class,
      PairingDeviceObjectModule.class,
      PairingDeviceServiceModule.class,
      PersonServiceModule.class,
      PlaceServiceModule.class,
      PopulationServiceModule.class,
      ProductCatalogServiceModule.class,
      IntraServiceModule.class,
      PlacePopulationCacheModule.class
})
public class ServicesModule extends AbstractIrisModule {
   @Override
   protected void configure() {
      Multibinder<PlatformService> serviceBinder = bindSetOf(PlatformService.class);
      serviceBinder.addBinding().to(StatusService.class);
   }

}

