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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Account;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;

/**
 * Billing-free subscription updater. All service level changes are applied
 * directly to the place record without any external billing provider calls.
 */
@Singleton
public class SubscriptionUpdaterImpl implements SubscriptionUpdater {
   private static final Logger logger = LoggerFactory.getLogger(SubscriptionUpdaterImpl.class);
   private final PlaceDAO placeDao;
   private final PlatformMessageBus platformBus;

   @Inject
   public SubscriptionUpdaterImpl(PlaceDAO placeDao, PlatformMessageBus platformBus) {
      this.placeDao = placeDao;
      this.platformBus = platformBus;
   }

   @Override
   public boolean updateSubscription(Account account, Place place, ServiceLevel serviceLevel, Map<String, Boolean> addons, boolean sendNotifications) throws SubscriptionUpdateException {
      ServiceLevel currentServiceLevel = place.getServiceLevel();

      if (currentServiceLevel == null && serviceLevel == null) {
         throw new IllegalArgumentException("If there is no current service level for the place, then a service level must be specified.");
      }

      if (serviceLevel == null) {
         serviceLevel = currentServiceLevel;
      }

      Set<String> currentAddons = place.getServiceAddons();
      if (serviceLevel == currentServiceLevel && areAddonsTheSame(addons, currentAddons)) {
         return false;
      }

      Place placeFromDb = placeDao.findById(place.getId());
      placeFromDb.setServiceLevel(serviceLevel);
      if (addons != null) {
         placeFromDb.setServiceAddons(addons.entrySet().stream()
               .filter(Map.Entry::getValue)
               .map(Map.Entry::getKey)
               .collect(Collectors.toSet()));
      }
      placeDao.save(placeFromDb);

      if (sendNotifications) {
         Map<String, Object> changes = new HashMap<>();
         if (serviceLevel != currentServiceLevel) {
            changes.put(PlaceCapability.ATTR_SERVICELEVEL, serviceLevel.name());
         }
         if (!Objects.equals(placeFromDb.getServiceAddons(), currentAddons)) {
            changes.put(PlaceCapability.ATTR_SERVICEADDONS, placeFromDb.getServiceAddons());
         }
         broadcastPlaceValueChange(place, changes);
      }

      return true;
   }

   @Override
   public boolean updateSubscriptions(Account account, ServiceLevel currentServiceLevel, ServiceLevel newServiceLevel, boolean sendNotifications) throws SubscriptionUpdateException {
      if (newServiceLevel == null) {
         throw new IllegalArgumentException("Service level must be specified.");
      }

      boolean changed = false;
      for (java.util.UUID placeId : account.getPlaceIDs()) {
         Place place = placeDao.findById(placeId);
         if (place != null && place.getServiceLevel() == currentServiceLevel) {
            place.setServiceLevel(newServiceLevel);
            placeDao.save(place);
            changed = true;

            if (sendNotifications) {
               Map<String, Object> changes = new HashMap<>();
               changes.put(PlaceCapability.ATTR_SERVICELEVEL, newServiceLevel.name());
               broadcastPlaceValueChange(place, changes);
            }
         }
      }
      return changed;
   }

   @Override
   public boolean updateSubscriptions(Account account, Set<Place> places, ServiceLevel serviceLevel, boolean sendNotifications) throws SubscriptionUpdateException {
      if (serviceLevel == null) {
         throw new IllegalArgumentException("Service level must be specified.");
      }

      boolean changed = false;
      for (Place place : places) {
         if (place.getServiceLevel() != serviceLevel) {
            Place placeFromDb = placeDao.findById(place.getId());
            placeFromDb.setServiceLevel(serviceLevel);
            placeDao.save(placeFromDb);
            changed = true;

            if (sendNotifications) {
               Map<String, Object> changes = new HashMap<>();
               changes.put(PlaceCapability.ATTR_SERVICELEVEL, serviceLevel.name());
               broadcastPlaceValueChange(place, changes);
            }
         }
      }
      return changed;
   }

   @Override
   public void removeSubscriptionForPlace(Account account, Place place) throws SubscriptionUpdateException {
      // No billing provider to update — place deletion handles cleanup
   }

   @Override
   public void processDelinquentAccount(Account account) throws SubscriptionUpdateException {
      // No billing provider — accounts cannot become delinquent
   }

   private boolean areAddonsTheSame(Map<String, Boolean> newAddons, Set<String> addons) {
      if ((newAddons == null || newAddons.isEmpty()) && (addons == null || addons.isEmpty())) {
         return true;
      }
      if (newAddons == null || newAddons.isEmpty() || addons == null || addons.isEmpty()) {
         return false;
      }
      Set<String> newAddonsThatAreTrue = newAddons.entrySet().stream()
            .filter(Map.Entry::getValue)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
      return newAddonsThatAreTrue.equals(addons);
   }

   private void broadcastPlaceValueChange(Place currentPlace, Map<String, Object> attrs) {
      PlatformMessage broadcast = PlatformMessage.buildBroadcast(
            MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, attrs),
            Address.platformService(currentPlace.getId(), PlaceCapability.NAMESPACE))
            .withPlaceId(currentPlace.getId())
            .withPopulation(currentPlace.getPopulation())
            .create();
      platformBus.send(broadcast);
   }
}
