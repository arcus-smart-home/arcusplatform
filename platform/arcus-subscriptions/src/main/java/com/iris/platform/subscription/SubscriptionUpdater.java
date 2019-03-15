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

import java.util.Map;
import java.util.Set;

import com.iris.messages.model.Account;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;

public interface SubscriptionUpdater {
   //Return true if the subscription was changed.
   default boolean updateSubscription(Account account, Place place, ServiceLevel serviceLevel, Map<String, Boolean> addons) throws SubscriptionUpdateException {
      return updateSubscription(account, place, serviceLevel, addons, true);
   }
   //  FIXME turn this into a bean
   boolean updateSubscription(
         Account account,
         Place place, ServiceLevel serviceLevel,
         Map<String, Boolean> addons,
         boolean sendNotifications
   ) throws SubscriptionUpdateException;
   
   //Return true if the subscription was changed.
   default boolean updateSubscriptions(Account account, ServiceLevel currentServiceLevel, ServiceLevel newServiceLevel) throws SubscriptionUpdateException {
      return updateSubscriptions(account, currentServiceLevel, newServiceLevel, true);
   }
   //  FIXME turn this into a bean
   boolean updateSubscriptions(
         Account account,
         ServiceLevel currentServiceLevel, ServiceLevel newServiceLevel,
         boolean sendNotifications
   ) throws SubscriptionUpdateException;
   
   //Return true if the subscription was changed.
   default boolean updateSubscriptions(Account account, Set<Place> places, ServiceLevel serviceLevel) throws SubscriptionUpdateException {
      return updateSubscriptions(account, places, serviceLevel, true);
   }
   //  FIXME turn this into a bean
   boolean updateSubscriptions(
         Account account,
         Set<Place> places, ServiceLevel serviceLevel,
         boolean sendNotifications
   ) throws SubscriptionUpdateException;
   
   void removeSubscriptionForPlace(Account account, Place place) throws SubscriptionUpdateException;
   void processDelinquentAccount(Account account) throws SubscriptionUpdateException;

}

