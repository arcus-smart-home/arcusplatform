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

import com.google.inject.Inject;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import com.iris.platform.services.account.billing.BillingAccountCreator;

public class CreateBillingAccountHandler implements ContextualRequestMessageHandler<Account> {

   private final BillingAccountCreator billingAccountCreator;

   @Inject
   public CreateBillingAccountHandler(BillingAccountCreator billingAccountCreator) {
      this.billingAccountCreator = billingAccountCreator;
   }

   @Override
   public String getMessageType() {
      return AccountCapability.CreateBillingAccountRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(final Account context, PlatformMessage msg) {
      MessageBody request = msg.getValue();

      try {
         String placeId = AccountCapability.CreateBillingAccountRequest.getPlaceID(request);
         this.billingAccountCreator.createBillingAccount(
               context,
               AccountCapability.CreateBillingAccountRequest.getBillingToken(request),
               placeId == null ? null : UUID.fromString(placeId));
      } catch(Exception e) {
         return Errors.fromException(e);
      }
      return AccountCapability.CreateBillingAccountResponse.instance();
   }
}

