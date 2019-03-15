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

import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.Person;
import com.iris.population.PlacePopulationCacheManager;

@Singleton
public class AccountActivateHandler implements ContextualRequestMessageHandler<Account> {

   public final static String ERROR_INVALID_STATE = "account.state.invalid";
   private final static String ERROR_MSG_OWNER_NAME_MISSING = "Must specify account owner's name";
   private final static String ERROR_MSG_OWNER_MISSING = "Must have an account owner";
   
   private final AccountDAO accountDao;
   private final PlatformMessageBus platformBus;
   private final PersonDAO personDao;
   private final PlacePopulationCacheManager populationCacheMgr;
   
   @Inject
   public AccountActivateHandler(AccountDAO accountDao, 
   		PersonDAO personDao, 
   		PlatformMessageBus platformBus,
   		PlacePopulationCacheManager populationCacheMgr) {
      this.accountDao = accountDao;
      this.personDao = personDao;
      this.platformBus = platformBus;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public String getMessageType() {
      return AccountCapability.ActivateRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Account context, PlatformMessage msg) {
      String curState = context.getState();
      if(Account.AccountState.COMPLETE.equalsIgnoreCase(curState)) {
         return AccountCapability.ActivateResponse.instance();
      }
      
      validateAccount(context);
      
      context.setState(Account.AccountState.COMPLETE);
      accountDao.save(context);
      if(curState == null || Account.AccountState.SIGN_UP_1.equalsIgnoreCase(curState)) {
         AccountStateTransitionUtils.sendAccountCreatedNotification(context.getOwner(), platformBus);
      }
      sendValueChange(context);
      return AccountCapability.ActivateResponse.instance();
   }

   /**
    * Send a value for the account state change to every place associated with the account
    * @param context
    */
   private void sendValueChange(Account context)
   {
      Set<UUID> placeIds = context.getPlaceIDs();      
      if(!CollectionUtils.isEmpty(placeIds)) {
         MessageBody msgBody = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE,
            ImmutableMap.<String, Object>of(AccountCapability.ATTR_STATE, context.getState()));
         Address source = Address.fromString(context.getAddress());
         placeIds.forEach(placeId -> {
            PlatformMessage message = PlatformMessage.buildBroadcast(msgBody, source)
               .withPlaceId(placeId)
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
               .create();
            platformBus.send(message);
         });
         
      }
   }

   /**
    * Make sure the account has sufficient data to be marked as COMPLETE
    * @param context
    */
   private void validateAccount(Account context)
   {
      //for now, just make sure owner has name
      UUID ownerId = context.getOwner();
      if(ownerId == null) {
         throw new ErrorEventException(Errors.fromCode(ERROR_INVALID_STATE, ERROR_MSG_OWNER_MISSING));
      }else {
         Person owner = personDao.findById(ownerId);
         if(owner == null) {
            throw new ErrorEventException(Errors.fromCode(ERROR_INVALID_STATE, ERROR_MSG_OWNER_MISSING));
         }else{
            if(StringUtils.isBlank(owner.getFirstName()) || StringUtils.isBlank(owner.getLastName())) {
               throw new ErrorEventException(Errors.fromCode(ERROR_INVALID_STATE, ERROR_MSG_OWNER_NAME_MISSING));
            }
         }
      }
      
   }
}

