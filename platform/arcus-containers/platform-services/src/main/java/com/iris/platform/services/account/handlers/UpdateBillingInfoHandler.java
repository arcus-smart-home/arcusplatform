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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.billing.client.BillingClient;
import com.iris.billing.client.model.BillingInfo;
import com.iris.billing.client.model.RecurlyError;
import com.iris.billing.exception.RecurlyAPIErrorException;
import com.iris.billing.exception.TransactionErrorException;
import com.iris.core.dao.AccountDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;

@Singleton
public class UpdateBillingInfoHandler implements ContextualRequestMessageHandler<Account> {
   public static final String MISSING_ARGUMENT_ERR = "argument.error";
   public static final String MISSING_ARGUMENT_MSG = "Must supply at least one argument.";

   private final AccountDAO accountDao;
   private final BillingClient client;
   private final PlatformMessageBus platformBus;

   @Inject(optional = true)
   @Named(value = "billing.timeout")
   private int billingTimeout = 30;

   @Inject
   public UpdateBillingInfoHandler(AccountDAO accountDAO, BillingClient billingClient, PlatformMessageBus platformBus) {
      this.accountDao = accountDAO;
      this.client = billingClient;
      this.platformBus = platformBus;
   }

   @Override
   public String getMessageType() {
      return "account:UpdateBillingInfo";
   }

   @Override
   public MessageBody handleRequest(Account context, PlatformMessage msg) {
   	if (context == null) {
      	return ErrorEvent.fromCode(
               SignupTransitionHandler.ARGUMENT_ERROR,
               "The account is required");
      }

   	MessageBody request = msg.getValue();
      BillingInfo billingInfoRequest = createBillingInfo(request);

      if(billingInfoRequest == null) {
         return ErrorEvent.fromCode(
               MISSING_ARGUMENT_ERR,
               MISSING_ARGUMENT_MSG);
      }

      try {
         BillingInfo response = client
               .createOrUpdateBillingInfoNonCC(context.getId().toString(), billingInfoRequest)
               .get(billingTimeout, TimeUnit.SECONDS);

         context.setBillingCity(response.getCity());
         context.setBillingFirstName(response.getFirstName());
         context.setBillingLastName(response.getLastName());
         context.setBillingState(response.getState());
         context.setBillingStreet1(response.getAddress1());
         context.setBillingStreet2(response.getAddress2());
         context.setBillingZip(response.getZip());

         accountDao.save(context);

         notification(context.getOwner());

         return MessageBody.emptyMessage();
      } catch (Exception ex) {
         if (ex.getCause() instanceof RecurlyAPIErrorException) {

            RecurlyError e = ((RecurlyAPIErrorException) ex.getCause()).getErrors().get(0);
            return Errors.fromCode(e.getErrorSymbol(), e.getErrorText());
         } else if (ex.getCause() instanceof TransactionErrorException) {

            TransactionErrorException e = (TransactionErrorException) ex.getCause();
            return Errors.fromCode(e.getErrorCode(), e.getCustomerMessage());
         } else {
            return Errors.fromException(ex);
         }
      }
   }

   private void notification(UUID personId) {
      PlatformMessage msg = Notifications.builder()
            .withPersonId(personId)
            .withSource(Address.platformService(AccountCapability.NAMESPACE))
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .withMsgKey(Notifications.CCUpdated.KEY)
            .create();
      platformBus.send(msg);
   }

   // We can update any one of these fields in combination or together, if we have at least 1 we're ok to call out.
   public BillingInfo createBillingInfo(MessageBody request) {
      boolean haveInfo = false;
      BillingInfo retValue = new BillingInfo();

      String billingFirstName = (String) request.getAttributes().get("account:billingFirstName");
      if (!Strings.isNullOrEmpty(billingFirstName)) {
         haveInfo = true;
         retValue.setFirstName(billingFirstName);
      }

      String billingLastName = (String) request.getAttributes().get("account:billingLastName");
      if (!Strings.isNullOrEmpty(billingLastName)) {
         haveInfo = true;
         retValue.setLastName(billingLastName);
      }

      String billingStreet1 = (String) request.getAttributes().get("account:billingStreet1");
      if (!Strings.isNullOrEmpty(billingStreet1)) {
         haveInfo = true;
         retValue.setAddress1(billingStreet1);
      }

      String billingStreet2 = (String) request.getAttributes().get("account:billingStreet2");
      if (!Strings.isNullOrEmpty(billingStreet2)) {
         haveInfo = true;
         retValue.setAddress2(billingStreet2);
      }

      String billingCity = (String) request.getAttributes().get("account:billingCity");
      if (!Strings.isNullOrEmpty(billingCity)) {
         haveInfo = true;
         retValue.setCity(billingCity);
      }

      String billingState = (String) request.getAttributes().get("account:billingState");
      if (!Strings.isNullOrEmpty(billingState)) {
         haveInfo = true;
         retValue.setState(billingState);
      }

      String billingZip = (String) request.getAttributes().get("account:billingZip");
      if (!Strings.isNullOrEmpty(billingZip)) {
         haveInfo = true;
         retValue.setZip(billingZip);
      }

      return haveInfo ? retValue : null;
   }
}

