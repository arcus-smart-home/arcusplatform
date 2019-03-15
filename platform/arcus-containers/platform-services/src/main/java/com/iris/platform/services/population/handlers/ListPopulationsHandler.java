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
package com.iris.platform.services.population.handlers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.service.PopulationService.ListPopulationsRequest;
import com.iris.messages.service.PopulationService.ListPopulationsResponse;
import com.iris.messages.type.Population;

public class ListPopulationsHandler implements ContextualRequestMessageHandler<Population> {
   private final PopulationDAO populationDao;

   @Inject
   public ListPopulationsHandler(PopulationDAO populationDao) {
      this.populationDao = populationDao;
   }

   @Override
   public String getMessageType() {
      return ListPopulationsRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Population context, PlatformMessage msg) {
      return handleStaticRequest(msg);
   }

   @Override
   public MessageBody handleStaticRequest(PlatformMessage msg) {
      List<Population> populations = populationDao.listPopulations();
      List<Map<String, Object>> result = populations.stream().map((r) -> mapPopulation(r)).collect(Collectors.toList());
      return ListPopulationsResponse.builder()
            .withPopulations(result)
            .build();
   }

   private Map<String, Object> mapPopulation(Population population) {   	
      return population.toMap();
   }
}

