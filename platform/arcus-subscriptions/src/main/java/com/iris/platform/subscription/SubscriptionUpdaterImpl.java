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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.billing.client.BillingClient;
import com.iris.billing.client.BillingClient.RefundType;
import com.iris.billing.client.model.Constants;
import com.iris.billing.client.model.RecurlyError;
import com.iris.billing.client.model.RecurlyErrors;
import com.iris.billing.client.model.Subscription;
import com.iris.billing.client.model.SubscriptionAddon;
import com.iris.billing.client.model.Subscriptions;
import com.iris.billing.client.model.request.SubscriptionRequest;
import com.iris.billing.exception.RecurlyAPIErrorException;
import com.iris.billing.exception.TransactionErrorException;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.model.Account;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceAddon;
import com.iris.messages.model.ServiceLevel;

@Singleton
public class SubscriptionUpdaterImpl implements SubscriptionUpdater {
   private static final Logger logger = LoggerFactory.getLogger(SubscriptionUpdaterImpl.class);
   private AccountDAO accountDao;
   private PlaceDAO placeDao;
   private BillingClient billingClient;
   private SubscriptionManager subscriptionManager;
   private PlatformMessageBus platformBus;

   @Inject(optional = true)
   @Named(value = "billing.timeout")
   private int billingTimeout = 30;

   @Inject
   public SubscriptionUpdaterImpl(AccountDAO accountDao, PlaceDAO placeDao, BillingClient billingClient, SubscriptionManager subscriptionManager, PlatformMessageBus platformBus) {
      this.accountDao = accountDao;
      this.placeDao = placeDao;
      this.billingClient = billingClient;
      this.subscriptionManager = subscriptionManager;
      this.platformBus=platformBus;
   }

   @Override
   public boolean updateSubscription(Account account, Place place, ServiceLevel serviceLevel, Map<String, Boolean> addons, boolean sendNotifications) throws SubscriptionUpdateException {
      return updateSubscription(account, place, serviceLevel, addons, sendNotifications, false);
   }

   private boolean updateSubscription(Account account, Place place, ServiceLevel serviceLevel, Map<String, Boolean> addons, boolean sendNotifications, boolean deliquent) throws SubscriptionUpdateException {
      // Check to see if anything has changed and initialize missing data.
      ServiceLevel currentServiceLevel = place.getServiceLevel();
      
      if (currentServiceLevel == null && serviceLevel == null) {
         throw new IllegalArgumentException("If there is no current service level for the place, then a service level must be specified.");
      }

      if (serviceLevel == null) {
         // Use current service level if a new one isn't specified.
         serviceLevel = currentServiceLevel;
      }

      //normalize the addon value
      if(addons != null && !addons.isEmpty()) {
    	  Map<String, Boolean> normalizedAddons = new HashMap<>(addons.size());
    	  addons.forEach((k, v) -> {
    		  normalizedAddons.put(ServiceAddon.fromString(k).name(), v);
    	  });
    	  addons = normalizedAddons;
      }

      Set<String> currentAddons = place.getServiceAddons();
      if (areAddonsTheSame(addons, currentAddons) && serviceLevel == currentServiceLevel) {
         return false;
      }

      // Collect subscription data from all places associated with the account except the place
      // being updated.
      Collection<Place> places = subscriptionManager.collectPlacesForAccount(account, place.getId());
      IrisSubscription otherSubscriptions = subscriptionManager.extractSubscription(places);

      // Build new subscription model while merging in the updates.
      IrisSubscription updatedSubscription = IrisSubscription.builder()
                                                .setInitialSubscription(otherSubscriptions)
                                                .addSubscription(serviceLevel)
                                                .addAddon(serviceLevel, addons)
                                                .create();

      // Mark place record as being updated.
      placeDao.setUpdateFlag(place.getId(), true);

      logger.debug("Updated Subscription {}", updatedSubscription);

      if(account.hasBillingAccount()) {
         try {
            updateRecurly(account, updatedSubscription, deliquent);
         } catch (Exception e) {
            logger.error("Error updating subscription", e);
            throw new SubscriptionUpdateException(e);
         }

         // Update Account
         updateAccount(account.getId());
      } else if(updatedSubscription.getServiceLevels().parallelStream().anyMatch(level -> level.isPaid())) {
         throw new SubscriptionUpdateException("Service level cannot be updated to a pay account without a billing account");
      } else if(hasAddons(addons)) {
    	  throw new SubscriptionUpdateException("Service Addons are not allowed without a billing account");
      }

      // Update Place
      Place placeFromDb = placeDao.findById(place.getId());
      placeFromDb.setServiceLevel(serviceLevel);
      if (addons != null) {
         placeFromDb.setServiceAddons(addons.entrySet().stream()
               .filter((e) -> e.getValue())
               .map((e) -> e.getKey())
               .collect(Collectors.toSet()));
      }
      Place saved = placeDao.save(placeFromDb);

      // Mark Place record as being done.
      placeDao.setUpdateFlag(place.getId(), false);

      if(sendNotifications) {
         Map<String,Object> changes = new HashMap<>();
         if(saved.getServiceLevel() != currentServiceLevel) {
            changes.put(PlaceCapability.ATTR_SERVICELEVEL, saved.getServiceLevel().name());
         }
         if(!Objects.equals(saved.getServiceAddons(), currentAddons)) {
            changes.put(PlaceCapability.ATTR_SERVICEADDONS, saved.getServiceAddons());
         }
         broadcastPlaceValueChange(place, changes);
      }

      return true;
   }

   @Override
   public boolean updateSubscriptions(Account account, ServiceLevel currentServiceLevel, ServiceLevel newServiceLevel, boolean sendNotifications) throws SubscriptionUpdateException {
      Set<Place> places = account.getPlaceIDs().stream()
         .map(id -> placeDao.findById(id))
         .filter(p -> p != null && p.getServiceLevel() == currentServiceLevel)
         .collect(toSet());

      return updateSubscriptions(account, places, newServiceLevel, sendNotifications);
   }

   @Override
   public boolean updateSubscriptions(Account account, Set<Place> places, ServiceLevel serviceLevel, boolean sendNotifications) throws SubscriptionUpdateException {
      return updateSubscriptions(account, places, serviceLevel, sendNotifications, false);
   }

   private boolean updateSubscriptions(Account account, Set<Place> places, ServiceLevel serviceLevel, boolean sendNotifications, boolean deliquent) throws SubscriptionUpdateException {
      if (serviceLevel == null) {
         throw new IllegalArgumentException("Service level must be specified.");
      }

      // Short-circuit if service levels are all the same
      places = places.stream().filter(p -> p.getServiceLevel() != serviceLevel).collect(toSet());
      if (places.isEmpty()) {
         return false;
      }

      // Collect subscription data from all places associated with the account except the places
      // being updated.
      UUID[] placeIds = places.stream().map(Place::getId).toArray(UUID[]::new);
      Collection<Place> otherPlaces = subscriptionManager.collectPlacesForAccount(account, placeIds);
      IrisSubscription otherSubscriptions = subscriptionManager.extractSubscription(otherPlaces);

      // Build new subscription model while merging in the updates.
      IrisSubscription.Builder updatedSubscriptionBuilder = IrisSubscription.builder()
         .setInitialSubscription(otherSubscriptions);
      for (Place place : places) {
         updatedSubscriptionBuilder.addSubscription(serviceLevel);
         Map<String, Boolean> addons = place.getServiceAddons().stream().collect(toMap(identity(), a -> true));
         updatedSubscriptionBuilder.addAddon(serviceLevel, addons);
      }
      IrisSubscription updatedSubscription = updatedSubscriptionBuilder.create();

      // Mark place records as being updated.
      for (Place place : places) {
         placeDao.setUpdateFlag(place.getId(), true);
      }

      logger.debug("Updated Subscription {}", updatedSubscription);

      if (account.hasBillingAccount()) {
         try {
            updateRecurly(account, updatedSubscription, deliquent);
         } catch (Exception e) {
            logger.error("Error updating subscription", e);
            throw new SubscriptionUpdateException(e);
         }

         // Update Account
         updateAccount(account.getId());
      } else if (updatedSubscription.getServiceLevels().parallelStream().anyMatch(level -> level.isPaid())) {
         throw new SubscriptionUpdateException("Service level cannot be updated to " + serviceLevel + " without a billing account");
      }

      for (Place place : places) {
         // Update Place
         Place placeFromDb = placeDao.findById(place.getId());
         placeFromDb.setServiceLevel(serviceLevel);
         Place saved = placeDao.save(placeFromDb);

         // Mark Place record as being done.
         placeDao.setUpdateFlag(place.getId(), false);

         if (sendNotifications) {
            Map<String,Object> changes = new HashMap<>();
            changes.put(PlaceCapability.ATTR_SERVICELEVEL, saved.getServiceLevel().name());
            broadcastPlaceValueChange(place, changes);
         }
      }

      return true;
   }

   private boolean hasAddons(Map<String, Boolean> addons) {
	   if(addons != null && !addons.isEmpty()) {
		   for(Boolean value: addons.values()) {
			   if(Boolean.TRUE.equals(value)) {
			      return true;
			   }
		   }
	   }
	   return false;
   }

   @Override
   public void processDelinquentAccount(Account account) throws SubscriptionUpdateException{

      //TODO: WORKOUT REAL RULES HERE

      if(account.getPlaceIDs() != null){

         account.getPlaceIDs().stream().forEach(placeId->{
            Place place = placeDao.findById(placeId);
            try {
               Map<String,Boolean>addonsFalse=place.getServiceAddons().stream()
                     .collect(Collectors.toMap(String::toString,a->Boolean.FALSE));
               
               updateSubscription(account, place, ServiceLevel.BASIC,addonsFalse, true);
            } catch (Exception e) {
               logger.error("Error downgrading subscription for place on DelinquentAccount with place:"+place.getId(), e);
            }
         });

      }
   }


   @Override
   public void removeSubscriptionForPlace(Account account, Place place) throws SubscriptionUpdateException {

      // Collect subscription data from all places associated with the account except the place
      // being updated.
      Collection<Place> places = subscriptionManager.collectPlacesForAccount(account, place.getId());
      IrisSubscription newSubscription = subscriptionManager.extractSubscription(places);

      logger.debug("Updated Supscription {}", newSubscription);

      try {
         updateRecurly(account, newSubscription, false);
      } catch (Exception e) {
         logger.error("Error updating subscription", e);
         throw new SubscriptionUpdateException(e);
      }

      // Update Account
      updateAccount(account.getId());
   }

   private void updateAccount(UUID accountId) {
      Subscriptions recurlySubscriptions;
      try {
         recurlySubscriptions = billingClient.getSubscriptionsForAccount(accountId.toString())
               .get(billingTimeout, TimeUnit.SECONDS);
      } catch (Exception e) {
         logger.error("Exception getting subscriptions after update", e);
         throw new RuntimeException("Exception getting subscriptions after update", e);
      }

      // TODO: Check subscriptions to make sure all the information is correct.
      Map<ServiceLevel, String> subscriptionIds;
      if (recurlySubscriptions != null) {
          subscriptionIds = recurlySubscriptions.stream()
               .filter(s -> s.isActive())
               .collect(Collectors.toMap(Subscription::getServiceLevel, Subscription::getSubscriptionID));
      }
      else {
         subscriptionIds = Collections.emptyMap();
      }

      Account account = accountDao.findById(accountId);
      account.setSubscriptionIDs(subscriptionIds);
      accountDao.save(account);
   }

   private void updateRecurly(Account account, IrisSubscription subscription, boolean deliquent) throws Exception {
      Subscriptions recurlySubscriptions = billingClient.getSubscriptionsForAccount(account.getId().toString())
               .get(billingTimeout, TimeUnit.SECONDS);

      // Convert recurly subscriptions to a map and check for inconsistencies.
      Map<ServiceLevel, Subscription> subscriptionMap = new HashMap<>();
      Set<ServiceLevel> existingServiceLevels = new HashSet<>();
      if (recurlySubscriptions != null) {
         for (Subscription recurlySubscription : recurlySubscriptions) {
            ServiceLevel serviceLevel = recurlySubscription.getServiceLevel();
            if (serviceLevel == null) {
               throw new IllegalStateException("Invalid recurly subscription in account " + account.getId());
            }
            // Only worry about active and canceled subscriptions. Expired subscriptions need to be recreated.
            if (recurlySubscription.isActive() || recurlySubscription.isCanceled()) {
               if (existingServiceLevels.contains(serviceLevel)) {
                  throw new IllegalStateException("There should only be one recurly subscription for service level " + serviceLevel + " in account " + account.getId());
               }
               existingServiceLevels.add(serviceLevel);
               subscriptionMap.put(serviceLevel, recurlySubscription);
            }
         }
      }

      // Remove subscriptions which are no longer valid.
      removeSubscriptions(subscriptionMap, subscription, deliquent);

      // Add or update subscriptions
      createOrUpdateSubscriptions(account, subscriptionMap, subscription);
   }

   private Set<String> removeSubscriptions(Map<ServiceLevel, Subscription> recurlySubs, IrisSubscription irisSub, boolean deliquent) {
      Set<ServiceLevel> serviceLevels = irisSub.getAllSubscriptions().keySet();
      logger.debug("Iris Service Levels {}", serviceLevels);
      logger.debug("Recurly Service Levels {}", recurlySubs.keySet());
      Set<String> removedSubscriptionIds = new HashSet<>();
      for (ServiceLevel serviceLevel : recurlySubs.keySet()) {
         if (!serviceLevels.contains(serviceLevel)) {
            RefundType refund = RefundType.NONE;
            Subscription sub = recurlySubs.get(serviceLevel);
            Date trialEnd = sub.getTrialEndsAt();
            
            if(!deliquent && serviceLevel.isPaid() && (trialEnd == null || new Date().getTime() > trialEnd.getTime())) {
               refund = RefundType.PARTIAL;
            }
            String subscriptionId = recurlySubs.get(serviceLevel).getSubscriptionID();
            try {
               terminateSubscription(subscriptionId, refund);
               removedSubscriptionIds.add(subscriptionId);
            } catch(Exception e) {
               logger.error("Error cancelling subscription", e);
               throw new RuntimeException("Error cancelling subscription", e);
            }
         }
      }
      return removedSubscriptionIds;
   }

   private void terminateSubscription(final String subscriptionId, final RefundType refund) throws Exception {
      logger.debug("Call terminate subscription: {} with refund: {}", subscriptionId, refund);
      ListenableFuture<Subscription> future = billingClient.terminateSubscription(subscriptionId, refund);
      try {
         future.get(billingTimeout, TimeUnit.SECONDS);
      } catch(Exception e) {

         if(refund == RefundType.NONE) {
            throw e;
         }

         if (e.getCause() instanceof RecurlyAPIErrorException) {
            logger.debug("Recurly API Error Received: {}", ((RecurlyAPIErrorException)e.getCause()).getErrors());
            RecurlyErrors errs = ((RecurlyAPIErrorException)e.getCause()).getErrors();

            if(errs.stream().anyMatch((re) -> { return  re.getErrorSymbol().equals("refund_invalid") || re.getErrorSymbol().equals("invalid_transaction"); })) {
               logger.info("recurly reported refund_invalid or invalid_transaction - previously refunded {} refunding {}, retrying with no refund", refund, subscriptionId);
               terminateSubscription(subscriptionId, RefundType.NONE);
            } else {
               throw e;
            }

         } else {
            throw e;
         }
      }
   }

   private void createOrUpdateSubscriptions(
         Account account,
         Map<ServiceLevel, Subscription> recurlySubscriptions,
         IrisSubscription subscription) {
      for (ServiceLevel serviceLevel : subscription.getServiceLevels()) {
         Subscription recurlySubscription = recurlySubscriptions.get(serviceLevel);
         if (recurlySubscription != null && !recurlySubscription.isExpired()) {
            logger.debug("Got back subscription {} with state {}", recurlySubscription.getSubscriptionID(), recurlySubscription.getState());
            if (recurlySubscription.isCanceled()) {
               logger.debug("Reactivating Subscription {}", recurlySubscription.getSubscriptionID());
               ListenableFuture<Subscription> future = billingClient.reactiviateSubscription(recurlySubscription.getSubscriptionID());
               try {
                  Subscription sub = future.get(billingTimeout, TimeUnit.SECONDS);
                  logger.debug("Result from reactivation {}", sub);
               } catch (Exception e) {
                  logger.error("Error reactivating subscription", e);
                  if (e.getCause() instanceof TransactionErrorException){
                     throw new RuntimeException( "transactioncode: " + ((TransactionErrorException) e).getError().getErrorCode() + ",  "+((TransactionErrorException) e).getCustomerMessage());
                  }
                  throw new RuntimeException("Error reactivating subscription", e);
               }
            }
            SubscriptionRequest request = buildUpdateRequest(
                  recurlySubscription,
                  subscription.getNumberOfSubscriptions(serviceLevel),
                  subscription.getNumberOfAddons(serviceLevel));
            logger.debug("Call Update Subscription");
            ListenableFuture<Subscriptions> future = billingClient.updateSubscription(request);
            try {
               future.get(billingTimeout, TimeUnit.SECONDS);


            }
            catch (Exception e) {
               logger.error("Error while trying to update a subscription.", e);
              recurlyErrorProcessing(e);
               throw new RuntimeException("Error while trying to update a subscription.", e);
            }
         }
         else {
            SubscriptionRequest request = buildCreateRequest(
                  serviceLevel,
                  subscription.getNumberOfSubscriptions(serviceLevel),
                  subscription.getNumberOfAddons(serviceLevel),
                  account.getTrialEnd());
            logger.debug("Call Create Subscription");
            ListenableFuture<Subscriptions> future = billingClient.createSubscriptionForAccount(account.getId().toString(), request);
            try {
               future.get(billingTimeout, TimeUnit.SECONDS);

            } catch (Exception e) {
               logger.error("Error while trying to create a subscription.", e);
               recurlyErrorProcessing(e);
               throw new RuntimeException("Error while trying to create a subscription.", e);
            }
         }
      }
   }
   private void recurlyErrorProcessing(Exception e){
      if (e.getCause() instanceof RecurlyAPIErrorException) {
         if (e.getCause() instanceof RecurlyAPIErrorException) {
            RecurlyErrors errs = ((RecurlyAPIErrorException)e.getCause()).getErrors();
            RecurlyError error  = errs.stream()
                    .filter(re -> re.getErrorSymbol().equals("cannot_determine_destination_jurisdictions"))
                    .findFirst()
                    .get();
            if(error != null) {
               throw new RuntimeException( "apiexceptioncode: Issue Processing Subscription Update  ,  Invalid Billing Zip" );
            }

         }
      }
      else if (e.getCause() instanceof TransactionErrorException){
         throw new RuntimeException( "transactioncode: " + ((TransactionErrorException) e).getError().getErrorCode() + ",  "+((TransactionErrorException) e).getCustomerMessage());
      }
   }
   private SubscriptionRequest buildCreateRequest(ServiceLevel serviceLevel,
         Integer numberOfSubscriptions,
         Map<String, Integer> addons,
         Date trialEnd) {
      SubscriptionRequest request = new SubscriptionRequest();

      switch (serviceLevel) {
         case PREMIUM:
            request.setPlanCode(Constants.PLAN_CODE_PREMIUM);
            break;
         case PREMIUM_FREE:
            request.setPlanCode(Constants.PLAN_CODE_PREMIUM_FREE);
            break;
         case PREMIUM_PROMON:
            request.setPlanCode(Constants.PLAN_CODE_PREMIUM_PROMON);
            break;
         case PREMIUM_PROMON_FREE:
            request.setPlanCode(Constants.PLAN_CODE_PREMIUM_PROMON_FREE);
            break;
         case PREMIUM_ANNUAL:
            request.setPlanCode(Constants.PLAN_CODE_PREMIUM_ANNUAL);
            break;
         case PREMIUM_PROMON_ANNUAL:
            request.setPlanCode(Constants.PLAN_CODE_PREMIUM_PROMON_ANNUAL);
            break;
          default:
             request.setPlanCode(Constants.PLAN_CODE_BASIC);
      }
      request.setQuantity(numberOfSubscriptions);
      // TODO: This shouldn't be hardcoded.
      request.setCurrency(Constants.CURRENCY_USD);
      if (addons != null) {
         buildSubscriptionAddons(addons).stream().forEach((a) -> request.addSubscriptionAddon(a));
      }
      if(serviceLevel == ServiceLevel.PREMIUM && trialEnd != null) {
         request.setTrialEndsAt(trialEnd);
      }
      return request;
   }

   private SubscriptionRequest buildUpdateRequest(Subscription recurlySubscription,
         Integer numberOfSubscriptions,
         Map<String, Integer> addons) {
      SubscriptionRequest request = new SubscriptionRequest();
      request.setSubscriptionID(recurlySubscription.getSubscriptionID());
      request.setPlanCode(recurlySubscription.getPlanCode());
      if (numberOfSubscriptions != null) {
         request.setQuantity(numberOfSubscriptions);
      }
      if (addons != null) {
         buildSubscriptionAddons(addons).stream().forEach((a) -> request.addSubscriptionAddon(a));
      }
      return request;
   }

   private List<SubscriptionAddon> buildSubscriptionAddons(Map<String, Integer> addons) {
      return addons.entrySet().stream().map((e) -> SubscriptionAddon.buildAddonWithQuantity(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
   }

   private boolean areAddonsTheSame(Map<String, Boolean> newAddons, Set<String> addons) {
      if ((newAddons == null || newAddons.isEmpty()) && (addons == null || addons.isEmpty())) {
         return true;
      }
      if (newAddons == null || newAddons.isEmpty() || addons == null || addons.isEmpty()) {
         return false;
      }
      Set<String> newAddonsThatAreTrue = newAddons.entrySet().stream()
            .filter(e -> e.getValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
      return newAddonsThatAreTrue.equals(addons);
   }

   private void broadcastPlaceValueChange(Place currentPlace, Map<String,Object> attrs) {
      PlatformMessage broadcast = PlatformMessage.buildBroadcast(
            MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, attrs),
            Address.platformService(currentPlace.getId(), PlaceCapability.NAMESPACE))
            .withPlaceId(currentPlace.getId())
            .withPopulation(currentPlace.getPopulation())
            .create();
      platformBus.send(broadcast);
   }
}

