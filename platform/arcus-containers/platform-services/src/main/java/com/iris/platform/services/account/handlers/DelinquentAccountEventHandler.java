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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.ContextualEventMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.model.Account;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.services.PlatformConstants;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.subscription.IrisSubscription;
import com.iris.platform.subscription.SubscriptionManager;
import com.iris.platform.subscription.SubscriptionUpdater;

public class DelinquentAccountEventHandler implements ContextualEventMessageHandler<Account> {
   private final static String SERVICE_NAME = "delinquentaccount.event.handler";
   private final Logger LOGGER = LoggerFactory.getLogger(DelinquentAccountEventHandler.class);
   private static final IrisMetricSet METRICS = IrisMetrics.metrics(SERVICE_NAME);
   private final Counter FAILURE_TOTAL_COUNTER = METRICS.counter("failure.count");
   private static final int ONE_DAY_MS = 24 * 60 * 60 * 1000;

   @Inject(optional = true)
   @Named(value = "delinquentaccount.notification.ttl")
   private int NOTIFICATION_TTL = ONE_DAY_MS;

   private final PlaceDAO placeDao;
   private final SubscriptionUpdater subscriptionUpdater;
   private final PlatformMessageBus platformBus;
   private final SubscriptionManager subscriptionManager;

   @Inject
   public DelinquentAccountEventHandler(PlaceDAO placeDao, SubscriptionUpdater subscriptionUpdater, PlatformMessageBus platformBus, SubscriptionManager subscriptionManager) {
      this.placeDao = placeDao;
      this.subscriptionUpdater = subscriptionUpdater;
      this.platformBus = platformBus;
      this.subscriptionManager = subscriptionManager;
   }

   @Override
   public void handleEvent(Account account, PlatformMessage message) {
      Preconditions.checkArgument(account != null, "No account context was provided.");
      try{
         // get current service level.
         IrisSubscription irisSubscription = this.getIrisSubscription(account);
         
         String messageKey =
               irisSubscription.getServiceLevels().parallelStream().anyMatch(serv -> serv.isPromon()) ?
               Notifications.DelinquentAccountPro.KEY : 
               Notifications.DelinquentAccount.KEY;
         
         updateSubscription(account);
         notification(account, messageKey);
      }catch (Exception e){
         FAILURE_TOTAL_COUNTER.inc();
         LOGGER.error("Error processing delinquent account", e);
      }
   }

   private void updateSubscription(Account account) throws Exception {
      subscriptionUpdater.processDelinquentAccount(account);
   }

   private void notification(Account account, String messageKey) {

      PlatformMessage msg = Notifications.builder()
            .withPersonId(account.getOwner())
            .withSource(Address.platformService(PlatformConstants.SERVICE_PEOPLE))
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .withMsgKey(messageKey)
            .withTimeToLive(NOTIFICATION_TTL)
            .create();

      platformBus.send(msg);
   }

   @Override
   public String getEventType() {
      return AccountCapability.DelinquentAccountEventRequest.NAME;
   }

   IrisSubscription getIrisSubscription(Account account) {
      Set<UUID> placeIds = account.getPlaceIDs();

      Set<Place> places = new HashSet<>();

      for (UUID placeId : placeIds){
         Place place = this.placeDao.findById(placeId);

         if (place != null) {
            places.add(place);
         }
      }
      return subscriptionManager.extractSubscription(places);
   }

}

