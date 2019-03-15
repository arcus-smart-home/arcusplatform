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
package com.iris.core.notification;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.model.Place;

public enum ServiceUpateNotifications {
   SERVICE_UPDATED("account.service.updated","place"), 
   ACCOUNT_DOWNGRADED_PRO_BASIC("account.downgraded.pro.basic","place"),
   ACCOUNT_DOWNGRADED_PRO_PREMIUM("account.downgraded.pro.premium","place"), 
   ACCOUNT_DOWNGRADED_PREMIUM_BASIC("account.downgraded.premium.basic","place"),
   ACCOUNT_UPGRADED_PREMIUM("account.upgraded.premium","place"),
   ACCOUNT_UPGRADED_PRO("account.upgraded.pro","modemPurchaseLink");

   private final String key;
   private final String parameter;
   
   private ServiceUpateNotifications(String key, String parameter) {
      this.key = key;
      this.parameter = parameter;
   }
   
   public String getKey() {
      return key;
   }

   public String getParameter() {
      return parameter;
   }
   
   private static String getPlaceParam(Place place) {
      return StringUtils.isEmpty(place.getName()) ? "unnamed" : place.getName();
   }   

   public void notification(PlatformMessageBus bus, String source, UUID personId, Place place) {
      PlatformMessage msg = Notifications.builder()
            .withPersonId(personId)
            .withPlaceId(place.getId())
            .withPopulation(place.getPopulation())
            .withSource(Address.platformService(source))
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .withMsgKey(key)
            .addMsgParam(parameter,getPlaceParam(place))
            .create();
      bus.send(msg);
   }   
   
   public void notification(PlatformMessageBus bus, String source, UUID personId, Place place, String paramValue) {
      PlatformMessage msg = Notifications.builder()
            .withPersonId(personId)
            .withPlaceId(place.getId())
            .withPopulation(place.getPopulation())
            .withSource(Address.platformService(source))
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .withMsgKey(key)
            .addMsgParam(parameter,paramValue)
            .create();
      bus.send(msg);
   }   
}

