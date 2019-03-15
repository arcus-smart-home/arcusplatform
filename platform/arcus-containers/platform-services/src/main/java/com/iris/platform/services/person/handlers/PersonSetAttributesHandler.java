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
package com.iris.platform.services.person.handlers;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.billing.client.BillingClient;
import com.iris.billing.client.model.request.AccountRequest;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Person;
import com.iris.messages.services.PlatformConstants;
import com.iris.platform.services.AbstractSetAttributesPlatformMessageHandler;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.ParsedEmail;

@Singleton
public class PersonSetAttributesHandler extends AbstractSetAttributesPlatformMessageHandler<Person> {
   private static final Logger logger = LoggerFactory.getLogger(PersonSetAttributesHandler.class);

   private final AccountDAO accountDao;
   private final PersonDAO personDao;
   private final BillingClient client;
   private final PersonPlaceAssocDAO personPlaceAssocDao;

   @Inject(optional = true)
   @Named(value = "billing.timeout")
   private int billingTimeout = 30;

   @Inject
   public PersonSetAttributesHandler(
         CapabilityRegistry capabilityRegistry,
         BeanAttributesTransformer<Person> personTransformer,
         AccountDAO accountDao,
         PersonDAO personDao,
         PersonPlaceAssocDAO personPlaceAssocDao,
         BillingClient client,
         PlatformMessageBus platformBus,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      super(capabilityRegistry, personTransformer, platformBus, populationCacheMgr);
      this.accountDao = accountDao;
      this.personDao = personDao;
      this.client = client;
      this.personPlaceAssocDao = personPlaceAssocDao;
   }

   @Override
   protected void afterSave(Person context, Map<String, Object> oldAttributes) {
      boolean emailUpdated = oldAttributes != null && oldAttributes.containsKey(PersonCapability.ATTR_EMAIL);
      boolean mobileUpdated = oldAttributes != null && !StringUtils.isEmpty( (String) oldAttributes.get(PersonCapability.ATTR_MOBILENUMBER) );
      Account account = getAccount(context);
      boolean accountOwner = isAccountOwner(context, account);

      if(emailUpdated) {
         String oldEmail = (String)oldAttributes.get(PersonCapability.ATTR_EMAIL);

         // keep ReCurly in sync IF this is the account owner
         if(accountOwner) {
            personDao.setUpdateFlag(context.getId(), true);

            if(hasBillingSubscriptions(context)){
               updateRecurlyEmail(context);
            }
            personDao.updatePersonAndEmail(context, oldEmail);
            personDao.setUpdateFlag(context.getId(), false);
         }
         else {
            personDao.updatePersonAndEmail(context, oldEmail);
         }
         if(context.getHasLogin() && Account.AccountState.COMPLETE.equals(account.getState())){
            platformBus.send(createEmailChangeNotification(context, oldEmail, NotificationCapability.NotifyRequest.PRIORITY_LOW));
            platformBus.send(createEmailChangeNotification(context, oldEmail, NotificationCapability.NotifyRequest.PRIORITY_MEDIUM));
         }
      }
      else {
         personDao.update(context);
      }

      if(mobileUpdated) {
         String oldMobileNumber = (String)oldAttributes.get(PersonCapability.ATTR_MOBILENUMBER);
         PlatformMessage msg  = Notifications.builder()
               .withSource(Address.platformService(PlatformConstants.SERVICE_PEOPLE))
               .withPersonId(context.getId())
               .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
               .withMsgKey(Notifications.MobileNumberChanged.KEY)
               .addMsgParam(Notifications.MobileNumberChanged.PARAM_OLDMOBILENUMBER, oldMobileNumber)
               .create();
         platformBus.send(msg);
      }
   }

   @Override
   protected void save(Person bean) {
      // no-op actual work is done in afterSave so that
      // we have access to the old email address
   }

   protected boolean isAccountOwner(Person person) {
      if(!person.getHasLogin() || person.getAccountId() == null) {
         return false;
      }

      com.iris.messages.model.Account account = accountDao.findById(person.getAccountId());
      return Objects.equals(account.getOwner(), person.getId());
   }
   
   protected Account getAccount(Person person) {
   	return accountDao.findById(person.getAccountId());
   }
   
   protected boolean isAccountOwner(Person person, Account account) {
      if(!person.getHasLogin() || person.getAccountId() == null) {
         return false;
      }

      return Objects.equals(account.getOwner(), person.getId());
   }

   protected boolean hasBillingSubscriptions(Person person) {
      Account account = accountDao.findById(person.getAccountId());
      return (account.getSubscriptionIDs() != null && !account.getSubscriptionIDs().isEmpty());
   }
   
   
   /**
    * Send the ValueChangeEvent for every place this person belongs to
    */
   @Override
   protected void sendValueChangeEvent(Person context, PlatformMessage request, Map<String, Object> changes)
   {
      Set<UUID> placeIds = personPlaceAssocDao.findPlaceIdsByPerson(context.getId());
      if(!CollectionUtils.isEmpty(placeIds)) {
         placeIds.stream().forEach(placeId -> sendValueChangeEventForPlace(request, changes, placeId.toString()));
      }else{
         logger.warn("This should not happen, but person [{}] has no places associated with it.  Possibly index out of sync", context.getId());
         super.sendValueChangeEvent(context, request, changes);
      }      
   }

   private PlatformMessage createEmailChangeNotification(Person context, String oldEmail, String priority) {
      return Notifications.builder()
               .withSource(Address.platformService(PlatformConstants.SERVICE_PEOPLE))
               .withPersonId(context.getId())
               .withPriority(priority)
               .withMsgKey(Notifications.EmailChanged.KEY)
               .addMsgParam(Notifications.EmailChanged.PARAM_OLD_EMAIL, oldEmail)
               .create();
   }

   private void updateRecurlyEmail(Person context) {
      logger.debug("Changing email in ReCurly for primary account holder, account: [{}] person: [{}]", context.getAccountId(), context.getId());

      // If the email can't be parsed, then it is considered invalid.
      String newEmail = context.getEmail();
      ParsedEmail parsedEmail = ParsedEmail.parse(newEmail);
      if (!parsedEmail.isValid()) {
         throw new ErrorEventException(Errors.invalidParam(PersonCapability.ATTR_EMAIL));
      }

      AccountRequest request = new AccountRequest();
      request.setAccountID(context.getAccountId().toString());
      request.setEmail(newEmail);

      ListenableFuture<com.iris.billing.client.model.Account> accountFuture = client.updateAccount(request);

      try {
         accountFuture.get(billingTimeout, TimeUnit.SECONDS);
      }
      catch(Exception e) {
         throw new ErrorEventException( Errors.fromException(e) );
      }
   }
}

