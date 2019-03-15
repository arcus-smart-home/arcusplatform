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
package com.iris.platform.services.account;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AccountDAO;
import com.iris.core.platform.AbstractPlatformService;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.capability.AccountMigrationCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.messages.model.ServiceLevel;
import com.iris.platform.services.account.billing.BillingAccountCreator;

@Singleton
public class AccountMigrationService extends AbstractPlatformService {

   private static final String MISSING_SERVICE_ERR = "missing.argument.serviceLevel";
   private static final String MISSING_SERVICE_MSG = "Missing required argument service level";
   private static final String INVALID_SERVICE_ERR = "invalid.argument.serviceLevel";
   private static final String INVALID_SERVICE_MSG = "%s is not valid service level";

   private final AccountDAO accountDao;
   private final BillingAccountCreator billingAccountCreator;

   @Inject
   public AccountMigrationService(PlatformMessageBus platformBus, AccountDAO accountDao, BillingAccountCreator billingAccountCreator) {
      super(platformBus, AccountMigrationCapability.NAMESPACE);
      this.accountDao = accountDao;
      this.billingAccountCreator = billingAccountCreator;
   }

   @Override
   protected MessageBody handleRequest(PlatformMessage message) throws Exception {
      switch(message.getMessageType()) {
      case AccountMigrationCapability.MigrateBillingAccountRequest.NAME:
         return handleMigrate(message);
      default:
         return super.handleRequest(message);
      }
   }

   private MessageBody handleMigrate(PlatformMessage message) {
      Account account = getAccount(message.getDestination());
      MessageBody request = message.getValue();

      String serviceLevel = AccountMigrationCapability.MigrateBillingAccountRequest.getServiceLevel(request);
      if(StringUtils.isBlank(serviceLevel)) {
         return Errors.fromCode(MISSING_SERVICE_ERR, MISSING_SERVICE_MSG);
      }

      ServiceLevel level = null;
      try {
         level = ServiceLevel.valueOf(serviceLevel.toUpperCase());
      } catch(IllegalArgumentException iae) {
         return Errors.fromCode(INVALID_SERVICE_ERR, INVALID_SERVICE_MSG);
      }

      try {
         Date trialEnd = level == ServiceLevel.PREMIUM ?
                         new Date(System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS)) :
                         new Date();

         String placeId = AccountMigrationCapability.MigrateBillingAccountRequest.getPlaceID(request);
         this.billingAccountCreator.createBillingAccount(
               account,
               AccountMigrationCapability.MigrateBillingAccountRequest.getBillingToken(request),
               placeId == null ? null : UUID.fromString(placeId),
               level,
               trialEnd);

         return AccountMigrationCapability.MigrateBillingAccountResponse.instance();

      } catch(Exception e) {
         return Errors.fromException(e);
      }

   }

   private Account getAccount(Address address) {
      if(address instanceof PlatformServiceAddress) {
         Object id = ((PlatformServiceAddress) address).getId();
         if(id instanceof UUID) {
            return accountDao.findById((UUID) id);
         }
      }

      return null;
   }
}

