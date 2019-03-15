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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.billing.client.BillingClient;
import com.iris.billing.client.model.Adjustment;
import com.iris.billing.client.model.Invoice;
import com.iris.billing.client.model.Invoices;
import com.iris.billing.exception.BillingEntityNotFoundException;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.model.Account;

public class ListInvoicesHandler implements ContextualRequestMessageHandler<Account> {
   private static final Logger logger = LoggerFactory.getLogger(ListInvoicesHandler.class);
   private final BillingClient client;

   @Inject
   @Named(value = "invoice.url")
   private String invoiceUrl;

   @Inject(optional = true)
   @Named(value = "billing.timeout")
   private int billingTimeout = 30;

   @Inject
   public ListInvoicesHandler(BillingClient client) {
      this.client = client;
   }

   @Override
   public String getMessageType() {
      return AccountCapability.ListInvoicesRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Account context, PlatformMessage msg) {
      Preconditions.checkArgument(context != null, "The account is required");

      String accountId = context.getId().toString();

      ListenableFuture<Invoices> future = client.getInvoicesForAccount(accountId);
      Invoices invoices;
      try {
         invoices = future.get(billingTimeout, TimeUnit.SECONDS);
      }
      catch(Exception e) {
         if(e instanceof ExecutionException) {
            Throwable t = e.getCause();
            if(!(t instanceof BillingEntityNotFoundException)) {
               throw new RuntimeException("Cannot retrieve invoices from recurly for " + accountId, e);
            } else {
               logger.debug("no billing account for {}, returning empty invoices", accountId);
               return AccountCapability.ListInvoicesResponse.builder()
                     .withInvoices(Collections.emptyList())
                     .build();
            }
         }
         throw new RuntimeException("Cannot retrieve invoices from recurly for " + accountId, e);
      }

      List<Map<String, Object>> maps = new ArrayList<>();
      if (invoices != null) {
         for (Invoice invoice : invoices) {
            maps.add(convertInvoiceToAttributes(invoice, accountId));
         }
      }

      return AccountCapability.ListInvoicesResponse.builder()
                  .withInvoices(maps)
                  .build();
   }

   private Map<String, Object> convertInvoiceToAttributes(Invoice invoice, String accountId) {
      Map<String, Object> map = new HashMap<>();
      map.put("invoiceDate", invoice.getCreatedAt());
      map.put("totalAmount", convertCentStringToDollars(invoice.getTotalInCents()));
      map.put("invoiceAddress", invoiceUrl + "?in=" + invoice.getInvoiceNumber() + "&ac=" + accountId);
      map.put("state", invoice.getState());

      // This is for returning the description on refunds specifically. With a refund there is only one
      // adjustment and the description tells what invoice was refunded.
      List<Adjustment> adjustments = invoice.getAdjustments();
      if (adjustments != null && !adjustments.isEmpty()) {
         map.put("description", adjustments.get(0).getDescription());
      }
      return map;
   }

   private double convertCentStringToDollars(String cents) {
      return Integer.valueOf(cents)/100.0;
   }
}

