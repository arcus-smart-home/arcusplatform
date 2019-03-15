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
package com.iris.platform.services.population;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.platform.ContextualEventMessageHandler;
import com.iris.core.platform.ContextualPlatformMessageDispatcher;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.platform.PlatformService;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.services.PlatformConstants;
import com.iris.messages.type.Population;

public class PopulationService extends ContextualPlatformMessageDispatcher<Population> implements PlatformService {
   public static final String PROP_THREADPOOL = "platform.service.population.threadpool";

   private static final Address address = Address.platformService(PlatformConstants.SERVICE_POPULATION);
   private final PopulationDAO populationDao;
   
   @Inject
   public PopulationService(
         PlatformMessageBus platformBus,
         @Named(PROP_THREADPOOL) Executor executor,
         PopulationDAO populationDao,
         Set<ContextualRequestMessageHandler<Population>> handlers
   ) {
      super(platformBus, executor, handlers, Collections.<ContextualEventMessageHandler<Population>>emptySet());
      this.populationDao = populationDao;
   }

   @Override
   public Address getAddress() {
      return address;
   }

   @Override
   public void handleMessage(PlatformMessage message) {
      super.handleMessage(message);
   }

   @Override
   protected Population loadContext(Object contextId, Integer qualifier) {
      if (!(contextId instanceof String)) {
         throw new IllegalArgumentException("The context ID must be a String");
      }
      Population population = populationDao.findByName((String)contextId);
      return population != null ? population : populationDao.getDefaultPopulation();
   }
}

