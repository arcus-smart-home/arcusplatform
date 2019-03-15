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
package com.iris.platform.billing.server.recurly;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.billing.client.BillingClient;
import com.iris.billing.client.model.Invoice;
import com.iris.billing.webhooks.model.ClosedInvoiceNotification;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.service.AccountService;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;

@Singleton
public class ClosedInvoiceWebhookHandler implements WebhookHandler<ClosedInvoiceNotification> {
   private static final String INVOICE_STATE_FAILED = "failed";
   private static final String SERVICE_NAME="recurly.webhook.handler";
   private static final IrisMetricSet METRICS = IrisMetrics.metrics(SERVICE_NAME);
   private final Counter INVOICE_FAILURE_COUNTER = METRICS.counter("delinquent.invoice.count");
   private static final Logger LOGGER = LoggerFactory.getLogger(ClosedInvoiceWebhookHandler.class);
   
   private final BillingClient billingClient;
   private final PlatformMessageBus platformBus;
   
   @Inject(optional = true)
   @Named(value = "billing.timeout")
   private int billingTimeout = 30;
   
   @Inject
   public ClosedInvoiceWebhookHandler(BillingClient billingClient, PlatformMessageBus platformBus) {
      this.billingClient = billingClient;
      this.platformBus = platformBus;
   }
   
   @Override
   public void handleWebhook(ClosedInvoiceNotification notification) throws Exception {
      if (notification.getInvoice().getState().equals(INVOICE_STATE_FAILED)) {
         String invoiceNumber = Integer.toString(notification.getInvoice().getInvoiceNumber().getValue());
         // Recurly recommends not taking significant action based on a webhook call without first confirming the state
         // of the resource with a separate API call
         if (confirmInvoiceFailed(invoiceNumber)) {
            LOGGER.info("processing failed payement {}",notification);
            publishInvoiceFailedEvent(notification.getAccount().getAccountCode());
         }
      }
   }
   
   private boolean confirmInvoiceFailed(String invoiceNumber) {
      Invoice invoice;
      try {
         invoice = billingClient.getInvoice(invoiceNumber).get(billingTimeout, SECONDS);
      } catch (Exception e) {
         LOGGER.error("Exception getting invoice", e);
         throw new RuntimeException("Exception getting invoice", e);
      }
      return invoice.getState().equals(INVOICE_STATE_FAILED);
   }
   
   private void publishInvoiceFailedEvent(String accountNumber){
      INVOICE_FAILURE_COUNTER.inc();
      
      MessageBody body=AccountCapability.DelinquentAccountEventRequest.builder()
         .withAccountId(accountNumber)
         .build();
      
      PlatformMessage event = PlatformMessage.buildEvent(body, Address.platformService(accountNumber, AccountService.NAMESPACE))
            .to(Address.platformService(accountNumber, AccountService.NAMESPACE))
            .withTimeToLive(86400000)
            .create();
      
      platformBus.send(event);
    }
}

