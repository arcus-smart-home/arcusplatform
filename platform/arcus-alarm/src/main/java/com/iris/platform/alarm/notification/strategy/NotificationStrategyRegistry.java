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
package com.iris.platform.alarm.notification.strategy;

import java.util.List;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.alarm.AlertType;
import com.iris.messages.address.Address;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.platform.alarm.incident.Trigger;

@Singleton
public class NotificationStrategyRegistry {

   private static final NotificationStrategy NOOP = new NotificationStrategy() {
      @Override
      public void execute(Address incidentAddress, UUID placeId, List<Trigger> triggers) {
      }

      @Override
      public boolean cancel(Address incidentAddress, UUID placeId, Address cancelledBy, List<String> alarms) {
         return true;
      }

      @Override
      public void acknowledge(Address incidentAddress, AlertType type) {
      }		
   };

   private final BasicNotificationStrategy basic;
   private final PremiumNotificationStrategy premium;

   @Inject
   public NotificationStrategyRegistry(BasicNotificationStrategy basic, PremiumNotificationStrategy premium) {
      this.basic = basic;
      this.premium = premium;
   }

   public NotificationStrategy forPlace(Place place) {
      if (ServiceLevel.isJustBasic(place.getServiceLevel())) {
         return this.basic;
      }

      if (ServiceLevel.isPremiumNotPromon(place.getServiceLevel())) {
         return this.premium;
      }

      return NOOP;
   }
}

