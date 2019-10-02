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

import com.google.common.util.concurrent.ListenableFuture;
import com.iris.billing.client.model.*;
import com.iris.billing.client.model.request.AccountRequest;
import com.iris.billing.client.model.request.AdjustmentRequest;
import com.iris.billing.client.model.request.InvoiceRefundRequest;
import com.iris.billing.client.model.request.SubscriptionRequest;

import java.util.Date;

public class NoopBillingClient implements BillingClient {
    @Override
    public ListenableFuture<Subscriptions> createSubscriptionForAccount(String accountID, SubscriptionRequest subscriptionRequest) {
        return null;
    }

    @Override
    public ListenableFuture<Subscriptions> createSubscriptionForAccount(AccountRequest account, SubscriptionRequest subscription) {
        return null;
    }

    @Override
    public ListenableFuture<Subscriptions> previewSubscriptionChange(String accountID, SubscriptionRequest subscriptionRequest) {
        return null;
    }

    @Override
    public ListenableFuture<Subscriptions> getSubscriptionsForAccount(String accountID) {
        return null;
    }

    @Override
    public ListenableFuture<Subscription> cancelSubscription(String subscriptionID) {
        return null;
    }

    @Override
    public ListenableFuture<Subscription> terminateSubscription(String subscriptionID, RefundType refundType) {
        return null;
    }

    @Override
    public ListenableFuture<Subscription> reactiviateSubscription(String subscriptionID) {
        return null;
    }

    @Override
    public ListenableFuture<Subscriptions> updateSubscription(SubscriptionRequest subscriptionRequest) {
        return null;
    }

    @Override
    public ListenableFuture<Account> updateAccount(AccountRequest account) {
        return null;
    }

    @Override
    public ListenableFuture<Account> getAccount(String accountID) {
        return null;
    }

    @Override
    public ListenableFuture<Account> createAccount(AccountRequest account) {
        return null;
    }

    @Override
    public ListenableFuture<Boolean> closeAccount(String accountID) {
        return null;
    }

    @Override
    public ListenableFuture<Account> reopenAccount(String accountID) {
        return null;
    }

    @Override
    public ListenableFuture<AccountNotes> getNotesForAccount(String accountID) {
        return null;
    }

    @Override
    public ListenableFuture<AccountBalance> getAccountBalance(String accountID) {
        return null;
    }

    @Override
    public ListenableFuture<Plans> getPlans() {
        return null;
    }

    @Override
    public ListenableFuture<PlanAddons> getPlanAddons(String planCode) {
        return null;
    }

    @Override
    public ListenableFuture<BillingInfo> getBillingInfoForAccount(String accountID) {
        return null;
    }

    @Override
    public ListenableFuture<BillingInfo> createOrUpdateBillingInfoForAccount(String accountID, String billingToken) {
        return null;
    }

    @Override
    public ListenableFuture<BillingInfo> createOrUpdateBillingInfoNonCC(String accountID, BillingInfo billingInfo) {
        return null;
    }

    @Override
    public ListenableFuture<Boolean> clearBillingInfoFromAccount(String accountID) {
        return null;
    }

    @Override
    public ListenableFuture<Adjustments> getAdjustmentsForAccount(String accountID) {
        return null;
    }

    @Override
    public ListenableFuture<Transactions> getTransactionsForAccount(String accountID) {
        return null;
    }

    @Override
    public ListenableFuture<Transactions> getTransactionsForAccount(String accountID, Date beginTime, Date endTime) {
        return null;
    }

    @Override
    public ListenableFuture<Invoices> getInvoicesForAccount(String accountID) {
        return null;
    }

    @Override
    public ListenableFuture<Invoice> getInvoice(String invoiceID) {
        return null;
    }

    @Override
    public ListenableFuture<Subscription> getSubscriptionDetails(String subscriptionID) {
        return null;
    }

    @Override
    public ListenableFuture<Adjustment> issueCredit(String accountID, AdjustmentRequest adjustmentRequest) {
        return null;
    }

    @Override
    public ListenableFuture<Invoice> issueInvoiceRefund(String accountID, InvoiceRefundRequest invoiceRefundRequest) {
        return null;
    }

    @Override
    public ListenableFuture<Accounts> getAccounts() {
        return null;
    }
}
