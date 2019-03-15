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
package com.iris.platform.services.account.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.notification.ServiceUpateNotifications;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.platform.subscription.SubscriptionUpdateException;
import com.iris.platform.subscription.SubscriptionUpdater;

public class UpdateServicePlanHandler implements ContextualRequestMessageHandler<Account> {
   private final Logger logger = LoggerFactory.getLogger(UpdateServicePlanHandler.class);

   public static final String PURCHASE_LINK_4G_MODEM = "4g.modem.purchase.link";
   
   @Inject(optional=true) 
   @Named(PURCHASE_LINK_4G_MODEM) String modemPurchaseLink = "https://";  

   private static final String MISSING_PLACE_ID_ERR = "missing.argument.placeID";
   private static final String MISSING_PLACE_ID_MSG = "Missing required place ID.";

   private final PlaceDAO placeDao;
   private final SubscriptionUpdater subscriptionUpdater;
   private final PlatformMessageBus platformBus;

   @Inject
   public UpdateServicePlanHandler(PlaceDAO placeDao, 
         SubscriptionUpdater subscriptionUpdater, 
         PlatformMessageBus platformBus) {
      this.placeDao = placeDao;
      this.subscriptionUpdater = subscriptionUpdater;
      this.platformBus = platformBus;
   }

   @Override
   public String getMessageType() {
     return AccountCapability.UpdateServicePlanRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Account context, PlatformMessage msg) {
      Preconditions.checkArgument(context != null, "No account context was provided.");

      
      if (context.getPlaceIDs() == null || context.getPlaceIDs().isEmpty()) {
         logger.debug("Should have at least one place ID associated, showing null or empty for Account [{}]", context.getId());
         throw new IllegalStateException("No PlaceID's found associated to account.");
      }

      MessageBody request = msg.getValue();

      logger.debug("Update Service Plan Message {}", request);

      String placeIDString = AccountCapability.UpdateServicePlanRequest.getPlaceID(request);
      if (Strings.isNullOrEmpty(placeIDString)) {
         return ErrorEvent.fromCode(
               MISSING_PLACE_ID_ERR,
               MISSING_PLACE_ID_MSG);
      }
      UUID placeId = UUID.fromString(placeIDString);

      String serviceLevelString = AccountCapability.UpdateServicePlanRequest.getServiceLevel(request);
      ServiceLevel serviceLevel = StringUtils.isEmpty(serviceLevelString) ? null : ServiceLevel.fromString(serviceLevelString);

      Map<String, Object> msgAddons = AccountCapability.UpdateServicePlanRequest.getAddons(request);
      
      if ((msgAddons == null || msgAddons.isEmpty()) && StringUtils.isEmpty(serviceLevelString)) {
         // Do nothing as no changes where specified.
         return AccountCapability.UpdateServicePlanResponse.instance();
      }

      Place currentPlace = placeDao.findById(placeId);

      if (currentPlace == null) {
         throw new ErrorEventException(Errors.CODE_INVALID_REQUEST, "The place designated by " + placeId + " could not be found.");
      }

      if(ServiceLevel.isPromon(serviceLevel) && !ServiceLevel.isPromon(currentPlace.getServiceLevel())) {
    	  throw new ErrorEventException(Errors.CODE_INVALID_REQUEST, "Can only manually update to a PROMON service level if the place is already PROMON.");
      }

      Map<String, Boolean> addons = mapAddons(msgAddons, currentPlace);

      try {
         if (subscriptionUpdater.updateSubscription(context, currentPlace, serviceLevel, addons)) {
            // If changes are made notify the user.
            sendNotification(platformBus, context.getOwner(), currentPlace, currentPlace.getServiceLevel(), serviceLevel);
         }
      } catch(SubscriptionUpdateException sue) {
         logger.error("Error updating subscription", sue);
         return sue.toError();
      }
      
      return AccountCapability.UpdateServicePlanResponse.instance();
   }

   private void sendNotification(PlatformMessageBus bus, UUID personId, Place place, ServiceLevel oldServiceLevel, ServiceLevel newServiceLevel) {
      switch (ServiceLevelChange.getServiceLevelChange(oldServiceLevel, newServiceLevel)){
         case DOWNGRADED_PREMIUM_BASIC:
            ServiceUpateNotifications.ACCOUNT_DOWNGRADED_PREMIUM_BASIC.notification(platformBus, AccountCapability.NAMESPACE, personId, place);
            break;
         case DOWNGRADED_PRO_BASIC:
            ServiceUpateNotifications.ACCOUNT_DOWNGRADED_PRO_BASIC.notification(platformBus,AccountCapability.NAMESPACE, personId, place);
            break;
         case DOWNGRADED_PRO_PREMIUM:
            ServiceUpateNotifications.ACCOUNT_DOWNGRADED_PRO_PREMIUM.notification(platformBus,AccountCapability.NAMESPACE, personId, place);
            break;
         case UPGRADED_PREMIUM:
            ServiceUpateNotifications.ACCOUNT_UPGRADED_PREMIUM.notification(platformBus,AccountCapability.NAMESPACE, personId,place);
            break;            
         default:
            ServiceUpateNotifications.SERVICE_UPDATED.notification(platformBus,AccountCapability.NAMESPACE, personId, place);
      }
   }
   
   private Map<String, Boolean> mapAddons(Map<String, Object> addons, Place place) {
      Map<String, Boolean> updatedMap = addons != null
            ? addons.entrySet().stream().collect(Collectors.toMap((e) -> e.getKey(), (e) -> (Boolean)e.getValue()))
            : new HashMap<>();
      Set<String> placeAddons = place.getServiceAddons();
      if (placeAddons != null) {
         for (String addon : placeAddons) {
            if (updatedMap.get(addon) == null) {
               updatedMap.put(addon, true);
            }
         }
      }
      return updatedMap;
   }
   
   private enum ServiceLevelChange {
      DOWNGRADED_PRO_BASIC, DOWNGRADED_PREMIUM_BASIC, UPGRADED_PREMIUM, DOWNGRADED_PRO_PREMIUM, UPGRADED_PRO, DEFAULT;
      
      public static ServiceLevelChange getServiceLevelChange(ServiceLevel oldServiceLevel, ServiceLevel newServiceLevel) {
         if (ServiceLevel.isPromon(oldServiceLevel) && ServiceLevel.BASIC.equals(newServiceLevel)){
            return DOWNGRADED_PRO_BASIC;
         }
         
         if (ServiceLevel.isPremiumNotPromon(oldServiceLevel) && ServiceLevel.BASIC.equals(newServiceLevel)){
            return DOWNGRADED_PREMIUM_BASIC;
         }  
         
         if (ServiceLevel.BASIC.equals(oldServiceLevel) && ServiceLevel.isPremiumNotPromon(newServiceLevel)){
            return UPGRADED_PREMIUM;
         }         
         
         if (ServiceLevel.isPromon(oldServiceLevel) && ServiceLevel.isPremiumNotPromon(newServiceLevel)){
            return DOWNGRADED_PRO_PREMIUM;
         }
         
         if (ServiceLevel.BASIC.equals(oldServiceLevel) && ServiceLevel.isPromon(newServiceLevel)){
            return UPGRADED_PRO;
         }          
         
         return DEFAULT;
      }
   }
   
   
}

