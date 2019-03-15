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
package com.iris.platform.services.account.billing;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.billing.client.BillingClient;
import com.iris.billing.client.model.BillingInfo;
import com.iris.billing.client.model.RecurlyCurrencyFormats;
import com.iris.billing.client.model.RecurlyError;
import com.iris.billing.client.model.Subscriptions;
import com.iris.billing.client.model.request.AccountRequest;
import com.iris.billing.client.model.request.SubscriptionRequest;
import com.iris.billing.exception.RecurlyAPIErrorException;
import com.iris.billing.exception.TransactionErrorException;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Account;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.messages.model.ServiceLevel;

@Singleton
public class BillingAccountCreator implements BillingErrorCodes {

   private static final Logger logger = LoggerFactory.getLogger(BillingAccountCreator.class);

   private final AccountDAO accountDao;
   private final PlaceDAO placeDao;
   private final PersonDAO personDao;
   private final BillingClient client;
   private final PlatformMessageBus platformBus;

   @Inject(optional = true)
   @Named(value = "billing.timeout")
   private int billingTimeout = 30;

   @Inject
   public BillingAccountCreator(AccountDAO accountDao, PlaceDAO placeDao, PersonDAO personDao, BillingClient client, PlatformMessageBus platformBus) {
      this.accountDao = accountDao;
      this.placeDao = placeDao;
      this.personDao = personDao;
      this.client = client;
      this.platformBus = platformBus;
   }

   public void createBillingAccount(Account account, String billingToken, UUID placeId) throws Exception {
      createBillingAccount(account, billingToken, placeId, ServiceLevel.PREMIUM, null);
   }

   public void createBillingAccount(Account account, String billingToken, UUID placeId, ServiceLevel targetLevel, Date trialEnd) throws Exception {
      if (account == null) {
         throw new ErrorEventException(MISSING_ACCOUNT_ERR, MISSING_ACCOUNT_MSG);
      }

      if (Strings.isNullOrEmpty(billingToken)) {
         throw new ErrorEventException(MISSING_BILLING_TOKEN_ERR, MISSING_BILLING_TOKEN_MSG);
      }

      if (placeId == null) {
         throw new ErrorEventException(MISSING_PLACE_ID_ERR, MISSING_PLACE_ID_MSG);
      }

      try {
         Place place = placeDao.findById(placeId);
         if (place == null) {
            throw new ErrorEventException(MISSING_PLACE_ID_DB_ERR, MISSING_PLACE_ID_DB_MSG);
         }

         if(!Objects.equal(account.getId(), place.getAccount())) {
            throw new ErrorEventException(INVALID_PLACE_ID_ERR, INVALID_PLACE_ID_MSG);
         }

         Person person = personDao.findById(account.getOwner());
         if(person == null) {
            throw new ErrorEventException(MISSING_ACCOUNT_OWNER_CODE, MISSING_ACCOUNT_OWNER_MSG);
         }

         // We can set other meta-data (email, First Name, Last Name, etc) on the account (in ReCurly)
         // if we want to, though nothing at this point has that information in a message.
         // If you login to the ReCurly console you'll see "Full Name: <UUID String>"
         // Not sure if this will have an impact on reporting.
         // Maybe use the billing name/address details for the "account" object in ReCurly (update when we get a hook?)
         AccountRequest accountRequest = new AccountRequest();
         accountRequest.setAccountID(account.getId().toString());
         accountRequest.setEmail(person.getEmail());
         accountRequest.setBillingTokenID(billingToken);

         // ReCurly will only create the account only if billing info is valid.
         // If there is an error, the client can call this over and over until they guess their card #
         // and ReCurly will only create the account if they can validate the card information.
         client.createAccount(accountRequest).get(billingTimeout, TimeUnit.SECONDS);

         // Get the newly created account's non-sensitive billing info to update our records.
         // If we rely on the web hook we can omit this call and update later. The
         // service that gets the hook will need to call out to ReCurly and get the info,
         // then update the DB. So we don't avoid calls to ReCurly, but we do save a DB trip here.
         BillingInfo billingInfo =
               client.getBillingInfoForAccount(account.getId().toString())
                        .get(billingTimeout, TimeUnit.SECONDS);

         account.setBillingCCLast4(billingInfo.getLastFour());
         account.setBillingCCType(billingInfo.getCardType());
         account.setBillingFirstName(billingInfo.getFirstName());
         account.setBillingLastName(billingInfo.getLastName());
         account.setBillingStreet1(billingInfo.getAddress1());
         account.setBillingStreet2(billingInfo.getAddress2());
         account.setBillingCity(billingInfo.getCity());
         account.setBillingState(billingInfo.getState());
         account.setBillingZip(billingInfo.getZip());
         account.setTrialEnd(trialEnd);

         accountDao.save(account); // Save the billing info.

         // Create the subscription for the account we just setup.
         SubscriptionRequest subscriptionRequest = new SubscriptionRequest();
         subscriptionRequest.setPlanCode(targetLevel.name().toLowerCase());
         subscriptionRequest.setCurrency(RecurlyCurrencyFormats.getDefaultCurrency());
         if(targetLevel == ServiceLevel.PREMIUM && trialEnd != null) {
            subscriptionRequest.setTrialEndsAt(trialEnd);
         }

         // What to do if this call fails?
         Subscriptions subs = client
               .createSubscriptionForAccount(account.getId().toString(), subscriptionRequest)
               .get(billingTimeout, TimeUnit.SECONDS);

         if(account.getTrialEnd() == null && subs.get(0).getTrialEndsAt() != null) {
            account.setTrialEnd(subs.get(0).getTrialEndsAt());
         }

         if (account.getPlaceIDs() == null || account.getPlaceIDs().isEmpty()) {
            account.setPlaceIDs(ImmutableSet.of(place.getId()));
         } else {
            Set<UUID> placeIDs = new HashSet<>(account.getPlaceIDs());
            placeIDs.add(place.getId());

            account.setPlaceIDs(placeIDs);
         }

         if (account.getSubscriptionIDs() == null) {
            account.setSubscriptionIDs(ImmutableMap.of(targetLevel, subs.get(0).getSubscriptionID()));
         } else {
            Map<ServiceLevel, String> serviceLevels = new HashMap<>(account.getSubscriptionIDs());
            serviceLevels.put(targetLevel, subs.get(0).getSubscriptionID());
            account.setSubscriptionIDs(serviceLevels);
         }
         accountDao.save(account); // Save the Service levels & ID's

         place.setServiceLevel(targetLevel);
         placeDao.save(place);
         broadcastPlaceValueChange(place, PlaceCapability.ATTR_SERVICELEVEL, targetLevel.toString());
      } catch (Exception ex) {
         if (ex.getCause() instanceof RecurlyAPIErrorException) {
            logger.debug("Recurly API Error Received: {}", ((RecurlyAPIErrorException)ex.getCause()).getErrors());

            // This is typically an error with the billing client itself (invalid XML)
            //
            // The only instance I can think of (that is action-able by the client) is "invalid_token"
            // responses when a client submits a token ReCurly can't find (expired or doesn't exist)

            // If this is a token error, the first instance will hold that.
            RecurlyError error = ((RecurlyAPIErrorException)ex.getCause()).getErrors().get(0);
            throw new ErrorEventException(error.getErrorSymbol(), error.getErrorText());
         } else if (ex.getCause() instanceof TransactionErrorException) {
            logger.debug("Transaction Error {}", ex);

            TransactionErrorException e = (TransactionErrorException) ex.getCause();
            throw new ErrorEventException(e.getErrorCode(), e.getCustomerMessage());
         } else {
            logger.debug("Error {}", ex);
            throw ex;
         }
      }
   }

   private void broadcastPlaceValueChange(Place currentPlace,String attrName,String value){
      PlatformMessage broadcast = PlatformMessage.buildBroadcast(
            MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, ImmutableMap.of(attrName,value)),
            Address.platformService(currentPlace.getId(), PlaceCapability.NAMESPACE))
            .withPlaceId(currentPlace.getId())
            .withPopulation(currentPlace.getPopulation())
            .create();
      platformBus.send(broadcast);
   }
}

