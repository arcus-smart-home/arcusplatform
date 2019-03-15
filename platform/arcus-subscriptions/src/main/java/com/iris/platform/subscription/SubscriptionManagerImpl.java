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
package com.iris.platform.subscription;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.model.Account;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SubscriptionManagerImpl implements SubscriptionManager {
   private static final Logger logger = LoggerFactory.getLogger(SubscriptionManagerImpl.class);
   private final PlaceDAO placeDao;
   
   @Inject
   public SubscriptionManagerImpl(PlaceDAO placeDao) {
      this.placeDao = placeDao;
   }

   @Override
   public IrisSubscription extractSubscription(Collection<Place> places) {
      if (places == null) {
         return null;
      }
      final Map<ServiceLevel, Integer> subscriptionNumbers = new HashMap<>();
      final Map<ServiceLevel, Map<String, Integer>> addons = new HashMap<>();
      
      for (Place place : places) {
         ServiceLevel serviceLevel = place.getServiceLevel();
         if (serviceLevel != null) {
            incServiceLevel(serviceLevel, subscriptionNumbers);
            Set<String> addonKeys = place.getServiceAddons();
            if (addonKeys != null && addonKeys.size() > 0) {
               for (String addonKey : addonKeys) {
                  incAddon(serviceLevel, addonKey, addons);
               }
            }
         }
         else {
            logger.error("Place {} has an null service level.", place.getId());
         }
      }
      
      Preconditions.checkState(count(subscriptionNumbers.values()) == places.size(), "Invalid number of subscriptions for places: " + places);
      
      IrisSubscription.Builder subscriptionBuilder = IrisSubscription.builder();
      subscriptionNumbers.keySet().stream().forEach(
            (sl) -> subscriptionBuilder.setNumberOfSubscriptions(sl, subscriptionNumbers.get(sl)));
      addons.keySet().stream().forEach(
            (sl) -> subscriptionBuilder.setAddons(sl, addons.get(sl)));
      return subscriptionBuilder.create();
   }

   @Override
   public List<Place> collectPlacesForAccount(Account account, UUID... excludedPlaces) {
      Preconditions.checkArgument(account != null, "Null account passed into collectPlacesForAccount");
      Set<UUID> placeIds = new HashSet<>(account.getPlaceIDs());
      for (UUID excludedPlaceId : excludedPlaces) {
         placeIds.remove(excludedPlaceId);
      }
      return placeDao.findByPlaceIDIn(placeIds);
   }

   private static void incServiceLevel(ServiceLevel serviceLevel, Map<ServiceLevel, Integer> serviceMap) {
      Integer number = serviceMap.get(serviceLevel);
      serviceMap.put(serviceLevel, number != null ? number + 1 : 1);
   }
   
   private static void incAddon(ServiceLevel serviceLevel, String addon, Map<ServiceLevel, Map<String, Integer>> addons) {
      Map<String, Integer> addonMap = addons.get(serviceLevel);
      if (addonMap == null) {
         addonMap = new HashMap<>();
         addons.put(serviceLevel, addonMap);
      }
      Integer count = addonMap.get(addon);
      addonMap.put(addon, count != null ? count + 1 : 1);
   }
   
   private int count(Collection<Integer> numbers) {
      int total = 0;
      for (Integer number : numbers) {
         total = total + (number != null ? number : 0);
      }
      return total;
   }
}

