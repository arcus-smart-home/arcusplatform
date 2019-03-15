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
package com.iris.population;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.PopulationService.AddPlacesRequest;
import com.iris.messages.type.Population;

public class PopulationUtils
{   
   public static Population validateAndGetPopulationFromRequest(Population curPopulationContext, String paramName, MessageBody bodyMsg, PopulationDAO populationDao) {
      String populationName = AddPlacesRequest.getPopulation(bodyMsg);
      if(StringUtils.isBlank(populationName) ) {
         if(curPopulationContext == null) {
            throw new ErrorEventException(Errors.missingParam(paramName));
         }else{
            return curPopulationContext;
         }
      }else{
      	Population curPopulation = populationDao.findByName(populationName);
      	if(curPopulation == null) {
      		throw new ErrorEventException(Errors.invalidParam(paramName));
      	}     
         //make sure it matches with the curPopulationContext
         if(curPopulationContext != null && !populationName.equals(curPopulationContext.getName())) {
            throw new ErrorEventException(Errors.invalidParam(paramName));
         }
         return curPopulation;
      }      
   }
   
   public static Set<UUID> validateAndGetPlacesFromRequest(String paramName, MessageBody bodyMsg) {
      List<String> places = AddPlacesRequest.getPlaces(bodyMsg);
      if(CollectionUtils.isEmpty(places)) {
         throw new ErrorEventException(Errors.missingParam(paramName));
      }      
      Set<UUID> placeAddresses = null;
      try{
         placeAddresses = places.stream().map((p) -> (UUID)Address.fromString(p).getId()).collect(Collectors.toSet());
      }catch(Exception e) {
         //If there is any invalid person address
         throw new ErrorEventException(Errors.invalidParam(paramName));
      }
      return placeAddresses;
   }
   
   public static void emitValueChangeForPopulation(String newPopulationValue, UUID placeId, PlatformMessageBus platformBus) {
      Map<String, Object> changes = new HashMap<String, Object>(1);
      changes.put(PlaceCapability.ATTR_POPULATION, newPopulationValue!=null?newPopulationValue:"");
      
      MessageBody body = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, changes);
      PlatformMessage msg = PlatformMessage.buildBroadcast(body, PlatformServiceAddress.platformService(placeId, PlaceCapability.NAMESPACE))
            .withPlaceId(placeId)
            .withPopulation(newPopulationValue!=null?newPopulationValue:Population.NAME_GENERAL)
            .create();
      platformBus.send(msg);
   }
   
   
   public static boolean isPopulationValueChangeEvent(PlatformMessage msg) {
   	return Capability.EVENT_VALUE_CHANGE.equals(msg.getMessageType()) 
   			&& PlaceCapability.NAMESPACE.equals(msg.getSource().getGroup()) 
   			&& msg.getValue().getAttributes().containsKey(PlaceCapability.ATTR_POPULATION);
   }
   
   public static String getUpdatedPopulation(PlatformMessage msg) {
   	return PlaceCapability.getPopulation(msg.getValue(), Population.NAME_GENERAL);
   }
   
   public static String lookupPopulationByPlace(UUID curPlaceId, PlaceDAO placeDao) {
   	String population = Population.NAME_GENERAL;
      if(curPlaceId != null) {
      	String popFromDB = placeDao.getPopulationById(curPlaceId);
      	if(StringUtils.isNotBlank(popFromDB)) {
      		population = popFromDB;
      	}
      }  
      return population;
   }

}

