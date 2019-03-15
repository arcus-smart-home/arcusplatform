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
import java.util.Set;
import java.util.UUID;

import com.google.inject.Inject;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.Place;
import com.iris.messages.service.PopulationService;
import com.iris.messages.service.PopulationService.AddPlacesRequest;
import com.iris.messages.type.Population;
import com.iris.population.PopulationUtils;

public class AddPlacesToPopulationHandler implements ContextualRequestMessageHandler<Population> {
   private final PopulationDAO populationDao;
   private final PlaceDAO placeDao;
   private final PlatformMessageBus platformBus;

   @Inject
   public AddPlacesToPopulationHandler(PopulationDAO populationDao, PlaceDAO placeDao, PlatformMessageBus platformBus) {
      this.populationDao = populationDao;
      this.placeDao = placeDao;
      this.platformBus = platformBus;
   }

   @Override
   public String getMessageType() {
      return AddPlacesRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Population context, PlatformMessage msg) {
      return doHandleRequest(context, msg);
   }

   @Override
   public MessageBody handleStaticRequest(PlatformMessage msg) {      
      return doHandleRequest(null, msg);
   }
   
   private MessageBody doHandleRequest(Population curPopulationContext, PlatformMessage msg) {
      MessageBody bodyMsg = msg.getValue();
      Population curPopulation = PopulationUtils.validateAndGetPopulationFromRequest(curPopulationContext, AddPlacesRequest.ATTR_POPULATION, bodyMsg, populationDao);      
      Set<UUID> placeIDs = PopulationUtils.validateAndGetPlacesFromRequest(AddPlacesRequest.ATTR_PLACES, bodyMsg);
      List<Place> places = placeDao.findByPlaceIDIn(placeIDs);
      if(places != null && !places.isEmpty()) {
         for(Place curPlace : places) {
            curPlace.setPopulation(curPopulation.getName());
            placeDao.save(curPlace);
            PopulationUtils.emitValueChangeForPopulation(curPopulation.getName(), curPlace.getId(), platformBus);
         }
      }
      
      return PopulationService.AddPlacesResponse.instance();
   }
   
}

