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
package com.iris.billing.client;

import java.io.IOException;
import java.util.Date;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.iris.billing.client.model.Account;
import com.iris.billing.client.model.AccountBalance;
import com.iris.billing.client.model.AccountNotes;
import com.iris.billing.client.model.Accounts;
import com.iris.billing.client.model.Adjustment;
import com.iris.billing.client.model.Adjustments;
import com.iris.billing.client.model.BillingInfo;
import com.iris.billing.client.model.Invoice;
import com.iris.billing.client.model.Invoices;
import com.iris.billing.client.model.PlanAddons;
import com.iris.billing.client.model.Plans;
import com.iris.billing.client.model.Subscription;
import com.iris.billing.client.model.Subscriptions;
import com.iris.billing.client.model.Transactions;
import com.iris.billing.client.model.request.AccountRequest;
import com.iris.billing.client.model.request.AdjustmentRequest;
import com.iris.billing.client.model.request.InvoiceRefundRequest;
import com.iris.billing.client.model.request.SubscriptionRequest;
import com.iris.billing.exception.TransactionErrorException;

public interface BillingClient {

   public enum RefundType {
      FULL,
      PARTIAL,
      NONE
   }

   /**
    *
    * Create a new subscription for an account.  If the account is already subscribed to this,
    * you should {@link #updateSubscription(String, Subscription)} instead, as this will return an error.
    *
    * @param accountID
    * @param subscription
    * @return {@link Subscriptions} the customer is subscribed to.
    * @throws IOException
    */
   ListenableFuture<Subscriptions> createSubscriptionForAccount(String accountID, SubscriptionRequest subscriptionRequest);

   /**
    *
    * Create a new subscription for an account.  If the account is already subscribed to this,
    * you should {@link #updateSubscription(String, Subscription)} instead, as this will return an error.
    *
    * If you want to change attributes on the account as well use this.
    *
    */
   ListenableFuture<Subscriptions> createSubscriptionForAccount(AccountRequest account, SubscriptionRequest subscription);

   /**
    *
    * Preview a subscription change without "applying it"
    *
    * @param accountID
    * @param subscription
    * @return
    */
   ListenableFuture<Subscriptions> previewSubscriptionChange(String accountID, SubscriptionRequest subscriptionRequest);

   // TODO: Want to append ?state=STATE to get only subscriptions in that state available?
   /**
    *
    * Get an account's subscriptions
    *
    * @param accountID
    * @return
    */
   ListenableFuture<Subscriptions> getSubscriptionsForAccount(String accountID);

   /**
    *
    * Cancel an account's subscription.
    *
    * @param subscriptionID
    * @return
    */
   ListenableFuture<Subscription> cancelSubscription(String subscriptionID);

   /**
    *
    * Immediately cancel an account's subscription
    *
    * @param subscriptionID
    * @return
    */
   ListenableFuture<Subscription> terminateSubscription(String subscriptionID, RefundType refundType);

   /**
    *
    * Reactivate a previously cancelled/terminated subcsription
    *
    * @param subscriptionID
    * @return
    */
   ListenableFuture<Subscription> reactiviateSubscription(String subscriptionID);

   /**
    *
    * Update an account's subscriptions.
    *
    * @param subscriptionID
    * @param updatedSubscriptionDetails
    * @return
    */
   ListenableFuture<Subscriptions> updateSubscription(SubscriptionRequest subscriptionRequest);

   /**
    *
    * Updates an new account. You may optionally include billing information.
    * While not all fields need to be filled out to update an account the
    * account code is required, everything else is optional.
    *
    * Only fields that have values will be sent in the update request.
    *
    * @param account Populated Account Object
    *
    * @return Account that was updated, less billing information.
    * @throws {@link RecurlyAPIErrorException}, {@link TransactionErrorException}
    */
   ListenableFuture<Account> updateAccount(AccountRequest account);

   /**
    *
    * Gets an account's details.
    *
    * @param accountID
    *
    * @return Account details (No billing information)
    * @throws {@link RecurlyAPIErrorException}, {@link TransactionErrorException}
    */
   ListenableFuture<Account> getAccount(String accountID);

   /**
    *
    * Creates a new account. You may optionally include billing information.
    * If the user has nested billing info, the account will only be created
    * if the billing info passes validation.  If a retry is required, you must
    * re-populate the billing card/cvv numbers as they are cleared during this process.
    *
    * @param account Populated Account Object.
    *
    * @return Account that was created, less billing information.
    * @throws {@link RecurlyAPIErrorException}, {@link TransactionErrorException}
    */
   ListenableFuture<Account> createAccount(AccountRequest account);

   /**
    *
    * Marks an account as closed and cancels any active subscriptions.
    * Any saved billing information will also be permanently removed from the account.
    *
    * @param accountID
    *
    * @return true if the request was processed successfully
    * @throws {@link RecurlyAPIErrorException}
    */
   ListenableFuture<Boolean> closeAccount(String accountID);

   /**
    * Editing an account, creating a new transaction or subscription also reopens an account.
    * Reopening an account does not modify any previously canceled or expired subscriptions.
    *
    * The resulting object will be null if the account was successfully reopened.
    * {@see https://docs.recurly.com/api/accounts#reopen-account}
    * Returns a 204 status code.
    *
    * @param accountID
    *
    * @return null on success
    * @throws {@link RecurlyAPIErrorException}, {@link TransactionErrorException}
    */
   ListenableFuture<Account> reopenAccount(String accountID);

   /**
    *
    * Gets a list of the notes on an account sorted in descending order.
    *
    * @param accountID
    *
    * @return Returns a list of the notes on an account sorted in descending order.
    * @throws {@link RecurlyAPIErrorException}
    */
   ListenableFuture<AccountNotes> getNotesForAccount(String accountID);

   /**
    * Gets an account's balance and past due status.
    *
    * @param accountID
    *
    * @return account balance and past due status
    * @throws {@link RecurlyAPIErrorException}
    */
   ListenableFuture<AccountBalance> getAccountBalance(String accountID);

   /**
    *
    * Gets available Plans that a customer can subscribe to.
    *
    * @return list of available plans for subscription.
    * @throws {@link RecurlyAPIErrorException}
    */
   ListenableFuture<Plans> getPlans();

   /**
    *
    * Gets available Plan Addons that a customer can subscribe to for the {@code planCode} passed in.
    *
    * @return list of available plans for subscription.
    * @throws {@link RecurlyAPIErrorException}
    */
   ListenableFuture<PlanAddons> getPlanAddons(String planCode);

   ListenableFuture<BillingInfo> getBillingInfoForAccount(String accountID);

   ListenableFuture<BillingInfo> createOrUpdateBillingInfoForAccount(String accountID, String billingToken);

   ListenableFuture<BillingInfo> createOrUpdateBillingInfoNonCC(String accountID, BillingInfo billingInfo);

   ListenableFuture<Boolean> clearBillingInfoFromAccount(String accountID);

   ListenableFuture<Adjustments> getAdjustmentsForAccount(String accountID);

   ListenableFuture<Transactions> getTransactionsForAccount(String accountID);

   ListenableFuture<Transactions> getTransactionsForAccount(String accountID, @Nullable Date beginTime, @Nullable Date endTime);

   ListenableFuture<Invoices> getInvoicesForAccount(String accountID);

   ListenableFuture<Invoice> getInvoice(String invoiceID);

   ListenableFuture<Subscription> getSubscriptionDetails(String subscriptionID);

   ListenableFuture<Adjustment> issueCredit(String accountID, AdjustmentRequest adjustmentRequest);

   ListenableFuture<Invoice> issueInvoiceRefund(String accountID, InvoiceRefundRequest invoiceRefundRequest);

   ListenableFuture<Accounts> getAccounts();
}

