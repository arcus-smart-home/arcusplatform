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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Response;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.iris.billing.client.model.Account;
import com.iris.billing.client.model.AccountBalance;
import com.iris.billing.client.model.AccountNote;
import com.iris.billing.client.model.AccountNotes;
import com.iris.billing.client.model.Accounts;
import com.iris.billing.client.model.Adjustment;
import com.iris.billing.client.model.Adjustments;
import com.iris.billing.client.model.BaseRecurlyModel;
import com.iris.billing.client.model.BillingInfo;
import com.iris.billing.client.model.Invoice;
import com.iris.billing.client.model.Invoices;
import com.iris.billing.client.model.Plan;
import com.iris.billing.client.model.PlanAddon;
import com.iris.billing.client.model.PlanAddons;
import com.iris.billing.client.model.Plans;
import com.iris.billing.client.model.RecurlyErrors;
import com.iris.billing.client.model.Subscription;
import com.iris.billing.client.model.Subscriptions;
import com.iris.billing.client.model.Transaction;
import com.iris.billing.client.model.Transactions;
import com.iris.billing.client.model.request.AccountRequest;
import com.iris.billing.client.model.request.AdjustmentRequest;
import com.iris.billing.client.model.request.InvoiceRefundRequest;
import com.iris.billing.client.model.request.SubscriptionRequest;
import com.iris.billing.deserializer.RecurlyDeserializer;
import com.iris.billing.exception.BillingEntityNotFoundException;
import com.iris.billing.exception.RecurlyAPIErrorException;
import com.iris.billing.exception.TransactionErrorException;
import com.iris.billing.serializer.RecurlyObjectSerializer;
import com.iris.metrics.AsyncTimer;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.TaggingAsyncTimer;

import io.netty.handler.codec.http.QueryStringEncoder;

public class RecurlyClient implements BillingClient {
    private final String baseURL;
    private final String APIKey;
    private static final String ACCOUNT_ID_NULL = "Account ID cannot be null.";
    private static final String ACCOUNT_ID_NESTED_OBJ_NULL = "Account objects ID was null. Account ID cannot be null.";
    private static final String AMOUNT_NESTED_OBJ_NULL = "Amount in adjustment was null. Amount cannot be null.";
    private static final String AMOUNT_NESTED_OBJ_INVALID = "Amount in adjustment is invalid. Amount must be an integer.";
    private static final String AMOUNT_NESTED_OBJ_POSITIVE = "Amount in credit adjustment is positive. Amount must be negative.";
    private static final String INVOICE_ID_NULL = "Invoice ID cannot be null.";
    private static final String INVOICE_NUMBER_NESTED_OBJ_NULL = "Invoice Number in invoice refund was null. Invoice number cannot be null.";
    private static final String SUBSCRIPTION_ID_NULL = "Subscription ID cannot be null.";
    private static final String BILLING_TOKEN_NULL = "Billing Token Cannot Be Null";
    private static final String USER_AGENT = "Iris 0.2a";
    private static final String HEADER_API_VERSION = "X-Api-Version";
    private static final String APPLICATION_XML_NO_CHARSET = "application/xml";
    private static final String APPLICATION_XML_WITH_CHARSET = "application/xml; charset=utf-8";
    private static final String ISO_8601_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    private static final IrisMetricSet metrics = IrisMetrics.metrics("recurly");

    private final RecurlyDeserializer deserializer = new RecurlyDeserializer();
    private final AsyncHttpClient client;

    private final TaggingAsyncTimer requestTaggingAsyncTimer;

    public RecurlyClient(String baseURL, String apiKey) {
        if (Strings.isNullOrEmpty(apiKey)) {
            throw new IllegalArgumentException("APIKey cannot be null.");
        } else if (Strings.isNullOrEmpty(baseURL)) {
            throw new IllegalArgumentException("Base URL cannot be null.");
        }

        this.baseURL = baseURL;
        this.APIKey = "Basic " + BaseEncoding.base64().encode(apiKey.getBytes());
        client = new DefaultAsyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setMaxConnections(-1).build());

        requestTaggingAsyncTimer = metrics.taggingAsyncTimer("request");
    }

    @Override
    public ListenableFuture<Subscriptions> createSubscriptionForAccount(String accountID, SubscriptionRequest subscriptionRequest) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }

        Account a = new Account();
        a.setAccountID(accountID);
        RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();
        serializer.setRoot(subscriptionRequest);
        serializer.addNestedModel(a);
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "createSubscriptionForAccount");
        return requestAsyncTimer.time(doPOST("/subscriptions", Subscriptions.class, serializer));
    }

    @Override
    public ListenableFuture<Subscriptions> createSubscriptionForAccount(AccountRequest account, SubscriptionRequest subscription) {
        if (Strings.isNullOrEmpty(account.getAccountID())) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }
        RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();
        serializer.setRoot(subscription);
        serializer.addNestedModel(account);
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "createSubscriptionForAccount");
        return requestAsyncTimer.time(doPOST("/subscriptions", Subscriptions.class, serializer));
    }

    @Override
    public ListenableFuture<Subscriptions> previewSubscriptionChange(String accountID, SubscriptionRequest subscriptionRequest) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }
        RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();
        serializer.setRoot(subscriptionRequest);
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "previewSubscriptionChange");
        return requestAsyncTimer.time(
                doPOST(Subscription.Tags.URL_RESOURCE + "/" + subscriptionRequest.getSubscriptionID() + "/preview", Subscriptions.class, serializer));
    }

    @Override
    public ListenableFuture<Subscription> getSubscriptionDetails(String subscriptionID) {
        if (Strings.isNullOrEmpty(subscriptionID)) {
            throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL);
        }

        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "getSubscriptionDetails");
        return requestAsyncTimer.time(doGET(Subscription.Tags.URL_RESOURCE + "/" + subscriptionID, Subscription.class));
    }

    @Override
    public ListenableFuture<Subscriptions> getSubscriptionsForAccount(String accountID) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }

        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "getSubscriptionsForAccount");
        return requestAsyncTimer.time(
                doGET(Account.Tags.URL_RESOURCE + "/" + accountID + Subscription.Tags.URL_RESOURCE, Subscriptions.class));
    }

    @Override
    public ListenableFuture<Subscription> cancelSubscription(String subscriptionID) {
        if (Strings.isNullOrEmpty(subscriptionID)) {
            throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL);
        }
        RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "cancelSubscription");
        return requestAsyncTimer.time(
                doPUT(Subscription.Tags.URL_RESOURCE + "/" + subscriptionID + "/cancel", Subscription.class, serializer));
    }

    @Override
    public ListenableFuture<Subscription> terminateSubscription(String subscriptionID, RefundType refundType) {
        if (Strings.isNullOrEmpty(subscriptionID)) {
            throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL);
        }
        if(refundType == null) {
            refundType = RefundType.NONE;
        }
        RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "terminateSubscription");
        return requestAsyncTimer.time(
                doPUT(Subscription.Tags.URL_RESOURCE + "/" + subscriptionID + "/terminate?refund=" + refundType.name().toLowerCase(), Subscription.class, serializer));
    }

    @Override
    public ListenableFuture<Subscription> reactiviateSubscription(String subscriptionID) {
        if (Strings.isNullOrEmpty(subscriptionID)) {
            throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL);
        }
        RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "reactivateSubscription");
        return requestAsyncTimer.time(
                doPUT(Subscription.Tags.URL_RESOURCE + "/" + subscriptionID + "/reactivate", Subscription.class, serializer));
    }

    @Override
    public ListenableFuture<Subscriptions> updateSubscription(SubscriptionRequest subscriptionRequest) {
        if (Strings.isNullOrEmpty(subscriptionRequest.getSubscriptionID())) {
            throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL);
        }
        RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();
        serializer.setRoot(subscriptionRequest);
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "updateSubscription");
        return requestAsyncTimer.time(
                doPUT(Subscription.Tags.URL_RESOURCE + "/" + subscriptionRequest.getSubscriptionID(), Subscriptions.class, serializer));
    }

    @Override
    public ListenableFuture<Account> updateAccount(AccountRequest account) {
        if (account == null || Strings.isNullOrEmpty(account.getAccountID())) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }
        RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();
        serializer.setRoot(account);
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "updateAccount");
        return requestAsyncTimer.time(
                doPUT(Account.Tags.URL_RESOURCE + "/" + account.getAccountID(), Account.class, serializer));
    }

    @Override
    public ListenableFuture<Account> getAccount(String accountID) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "getAccount");
        return requestAsyncTimer.time(doGET(Account.Tags.URL_RESOURCE + "/" + accountID, Account.class));
    }

    @Override
    public ListenableFuture<Account> createAccount(AccountRequest account) {
        if (account == null || Strings.isNullOrEmpty(account.getAccountID())) {
            throw new IllegalArgumentException(ACCOUNT_ID_NESTED_OBJ_NULL);
        }
        RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();
        serializer.setRoot(account);
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "createAccount");
        return requestAsyncTimer.time(doPOST("/accounts", Account.class, serializer));
    }

    @Override
    public ListenableFuture<Boolean> closeAccount(String accountID) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }

        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "closeAccount");
        return requestAsyncTimer.time(doDELETE(Account.Tags.URL_RESOURCE + "/" + accountID));
    }

    @Override
    public ListenableFuture<Account> reopenAccount(String accountID) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }
        RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "reopenAccount");
        return requestAsyncTimer.time(
                doPUT(Account.Tags.URL_RESOURCE + "/" + accountID + "/reopen", Account.class, serializer));
    }

    @Override
    public ListenableFuture<AccountNotes> getNotesForAccount(String accountID) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }

        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "getNotesForAccount");
        return requestAsyncTimer.time(
                doGET(Account.Tags.URL_RESOURCE + accountID + AccountNote.Tags.URL_RESOURCE, AccountNotes.class));
    }

    @Override
    public ListenableFuture<AccountBalance> getAccountBalance(String accountID) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }

        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "getAccountBalance");
        return requestAsyncTimer.time(
                doGET(Account.Tags.URL_RESOURCE + "/" + accountID + AccountBalance.Tags.URL_RESOURCE, AccountBalance.class, "2.10"));
    }

    @Override
    public ListenableFuture<Plans> getPlans() {
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "getPlans");
        return requestAsyncTimer.time(doGET(Plan.Tags.URL_RESOURCE + "/", Plans.class));
    }

    @Override
    public ListenableFuture<PlanAddons> getPlanAddons(String planCode) {
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "getPlanAddons");
        return requestAsyncTimer.time(
                doGET(Plan.Tags.URL_RESOURCE + "/" + planCode + PlanAddon.Tags.URL_RESOURCE, PlanAddons.class));
    }

    @Override
    public ListenableFuture<BillingInfo> getBillingInfoForAccount(String accountID) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }

        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "getBillingInfoForAccount");
        return requestAsyncTimer.time(
                doGET(Account.Tags.URL_RESOURCE + "/" + accountID + BillingInfo.Tags.URL_RESOURCE, BillingInfo.class));
    }

    @Override
    public ListenableFuture<BillingInfo> createOrUpdateBillingInfoForAccount(String accountID, String billingToken) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        } else if (Strings.isNullOrEmpty(billingToken)) {
            throw new IllegalArgumentException(BILLING_TOKEN_NULL);
        }

        BillingInfo info = new BillingInfo();
        info.setTokenID(billingToken);
        RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();
        serializer.setRoot(info);
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "createOrUpdateBillingInfoForAccount");
        return requestAsyncTimer.time(
                doPUT(Account.Tags.URL_RESOURCE + "/" + accountID + BillingInfo.Tags.URL_RESOURCE, BillingInfo.class, serializer));
    }

    @Override
    public ListenableFuture<BillingInfo> createOrUpdateBillingInfoNonCC(String accountID, BillingInfo billingInfo) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }
        RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();
        serializer.setRoot(billingInfo);
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "createOrUpdateBillingInfoNonCC");
        return requestAsyncTimer.time(
                doPUT(Account.Tags.URL_RESOURCE + "/" + accountID + BillingInfo.Tags.URL_RESOURCE, BillingInfo.class, serializer));
    }

    @Override
    public ListenableFuture<Boolean> clearBillingInfoFromAccount(String accountID) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }

        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "clearBillingInfoFromAccount");
        return requestAsyncTimer.time(
                doDELETE(Account.Tags.URL_RESOURCE + "/" + accountID + BillingInfo.Tags.URL_RESOURCE));
    }

    @Override
    public ListenableFuture<Adjustments> getAdjustmentsForAccount(String accountID) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }

        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "getAdjustmentsForAccount");
        return requestAsyncTimer.time(
                doGET(Account.Tags.URL_RESOURCE + "/" + accountID + Adjustment.Tags.URL_RESOURCE, Adjustments.class));
    }

    @Override
    public ListenableFuture<Transactions> getTransactionsForAccount(String accountID) {
        return getTransactionsForAccount(accountID, null, null);
    }

    @Override
    public ListenableFuture<Transactions> getTransactionsForAccount(String accountID, @Nullable Date beginTime, @Nullable Date endTime) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }

        QueryStringEncoder encoder =
                new QueryStringEncoder(Account.Tags.URL_RESOURCE + "/" + accountID + Transaction.Tags.URL_RESOURCE);
        DateFormat df = new SimpleDateFormat(ISO_8601_DATE_TIME_FORMAT);
        if (beginTime != null) {
            encoder.addParam("begin_time", df.format(beginTime));
        }
        if (endTime != null) {
            encoder.addParam("end_time", df.format(endTime));
        }

        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "getTransactionsForAccount");
        return requestAsyncTimer.time(doGET(encoder.toString(), Transactions.class));
    }

    @Override
    public ListenableFuture<Invoices> getInvoicesForAccount(String accountID) {
        if (Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }

        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "getInvoicesForAccount");
        return requestAsyncTimer.time(
                doGET(Account.Tags.URL_RESOURCE + "/" + accountID + Invoice.Tags.URL_RESOURCE, Invoices.class));
    }

    @Override
    public ListenableFuture<Invoice> getInvoice(String invoiceID) {
        if (Strings.isNullOrEmpty(invoiceID)) {
            throw new IllegalArgumentException(INVOICE_ID_NULL);
        }

        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "getInvoice");
        return requestAsyncTimer.time(
                doGET(Invoice.Tags.URL_RESOURCE + "/" + invoiceID, Invoice.class));
    }

    @Override
    public ListenableFuture<Adjustment> issueCredit(String accountID, AdjustmentRequest adjustmentRequest) {
        if (adjustmentRequest == null || Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }

        if (Strings.isNullOrEmpty(adjustmentRequest.getAmountInCents())) {
            throw new IllegalArgumentException(AMOUNT_NESTED_OBJ_NULL);
        }

        Integer amount;

        try {
            amount = Integer.valueOf(adjustmentRequest.getAmountInCents());
        } catch(NumberFormatException nfe) {
            throw new IllegalArgumentException(AMOUNT_NESTED_OBJ_INVALID);
        }

        if (amount >= 0) {
            throw new IllegalArgumentException(AMOUNT_NESTED_OBJ_POSITIVE);
        }

        adjustmentRequest.setCurrency("USD");
        adjustmentRequest.setQuantity("1");
        RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();
        serializer.setRoot(adjustmentRequest);
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "issueCredit");
        return requestAsyncTimer.time(
                doPOST(Account.Tags.URL_RESOURCE + "/" + accountID + Adjustment.Tags.URL_RESOURCE, Adjustment.class, serializer));
    }

    @Override
    public ListenableFuture<Invoice> issueInvoiceRefund(String accountID, InvoiceRefundRequest invoiceRefundRequest) {
        if (invoiceRefundRequest == null || Strings.isNullOrEmpty(accountID)) {
            throw new IllegalArgumentException(ACCOUNT_ID_NULL);
        }

        if (Strings.isNullOrEmpty(invoiceRefundRequest.getInvoiceNumber())) {
            throw new IllegalArgumentException(INVOICE_NUMBER_NESTED_OBJ_NULL);
        }

        // we want the whole invoice amount back, so send an empty amount
        invoiceRefundRequest.setAmountInCents("");

        // this is the default, but in case they ever change it this is what we want
        invoiceRefundRequest.setRefundApplyOrder("credit");

        RecurlyObjectSerializer serializer = new RecurlyObjectSerializer();
        serializer.setRoot(invoiceRefundRequest);
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "issueInvoiceRefund");
        return requestAsyncTimer.time(
                doPOST(Invoice.Tags.URL_RESOURCE + "/" + invoiceRefundRequest.getInvoiceNumber() + "/refund", Invoice.class, serializer));
    }

    public ListenableFuture<Accounts> getAccounts() {
        AsyncTimer requestAsyncTimer = requestTaggingAsyncTimer.tag("op", "getAccounts");
        return requestAsyncTimer.time(doGET(Account.Tags.URL_RESOURCE, Accounts.class));
    }

    BoundRequestBuilder setupCommonSettings(BoundRequestBuilder builder, @Nullable String apiVersion) {
        builder.addHeader(HttpHeaders.AUTHORIZATION, APIKey)
                .addHeader(HttpHeaders.ACCEPT, APPLICATION_XML_NO_CHARSET)
                .addHeader(HttpHeaders.USER_AGENT, USER_AGENT);

        // Must include the "X-Api-Version" header for certain APIs (e.g. "Lookup Account Balance") that were added in a
        // later release, or else Recurly will return a 400 error.  For now, we include the header lazily, only for those
        // APIs that require it, until we have enough QA time in a future release to test a universal migration to the
        // latest Recurly version for all our calls.
        if (!isEmpty(apiVersion)) {
            builder.addHeader(HEADER_API_VERSION, apiVersion);
        }

        return builder;
    }

    BoundRequestBuilder post(String url, @Nullable String apiVersion) {
        return setupCommonSettings(client.preparePost(url), apiVersion).addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_XML_WITH_CHARSET);
    }

    BoundRequestBuilder get(String url, @Nullable String apiVersion) {
        return setupCommonSettings(client.prepareGet(url), apiVersion);
    }

    BoundRequestBuilder put(String url, @Nullable String apiVersion) {
        return setupCommonSettings(client.preparePut(url), apiVersion).addHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_XML_WITH_CHARSET);
    }

    BoundRequestBuilder delete(String url, @Nullable String apiVersion) {
        return setupCommonSettings(client.prepareDelete(url), apiVersion);
    }

    /*
     *  						POST/GET/PUT/DELETE Actual Method Calls.
     */

    protected <T extends BaseRecurlyModel> ListenableFuture<T> doPOST(String resourcePath, final Class<T> clazz, RecurlyObjectSerializer serializer) {
        return doPOST(resourcePath, clazz, serializer, null);
    }

    protected <T extends BaseRecurlyModel> ListenableFuture<T> doPOST(String resourcePath, final Class<T> clazz, RecurlyObjectSerializer serializer, @Nullable String apiVersion) {
        final SettableFuture<T> future = SettableFuture.create();

        try {
            post(baseURL + resourcePath, apiVersion)
                    .setBody(serializer.serialize())
                    .execute(new AsyncCompletionHandler<Void>() {
                        @Override
                        public Void onCompleted(Response response) throws Exception {
                            setResponse(response, future, clazz);
                            return null;
                        }
                    });
        } catch (Exception ex) {
            future.setException(ex);
        }

        return future;
    }

    protected <T extends BaseRecurlyModel> ListenableFuture<T> doGET(String resourcePath, final Class<T> clazz) {
        return doGET(resourcePath, clazz, null);
    }

    protected <T extends BaseRecurlyModel> ListenableFuture<T> doGET(String resourcePath, final Class<T> clazz, @Nullable String apiVersion) {
        final SettableFuture<T> future = SettableFuture.create();

        try {
            get(baseURL + resourcePath, apiVersion)
                    .execute(new AsyncCompletionHandler<Void>() {
                        @Override
                        public Void onCompleted(Response response) throws Exception {
                            setResponse(response, future, clazz);
                            return null;
                        }
                    });
        } catch (Exception ex) {
            future.setException(ex);
        }

        return future;
    }

    protected <T extends BaseRecurlyModel> ListenableFuture<T> doPUT(String resourcePath, final Class<T> clazz, RecurlyObjectSerializer serializer) {
        return doPUT(resourcePath, clazz, serializer, null);
    }

    protected <T extends BaseRecurlyModel> ListenableFuture<T> doPUT(String resourcePath, final Class<T> clazz, RecurlyObjectSerializer serializer, @Nullable String apiVersion) {
        final SettableFuture<T> future = SettableFuture.create();
        String data = null;
        try {
            data = serializer.serialize();
        } catch (IllegalArgumentException ex) {
            // Intended.
            // Didn't have data after all!
        }

        try {
            put(baseURL + resourcePath, apiVersion)
                    .setBody(data).execute(new AsyncCompletionHandler<Void>() {
                @Override
                public Void onCompleted(Response response) throws Exception {
                    setResponse(response, future, clazz);
                    return null;
                }
            });
        } catch (Exception ex) {
            future.setException(ex);
        }

        return future;
    }

    protected ListenableFuture<Boolean> doDELETE(String resourcePath) {
        return doDELETE(resourcePath, null);
    }

    protected ListenableFuture<Boolean> doDELETE(String resourcePath, @Nullable String apiVersion) {
        final SettableFuture<Boolean> future = SettableFuture.create();

        try {
            delete(baseURL + resourcePath, apiVersion)
                    .execute(new AsyncCompletionHandler<Void>() {
                        @Override
                        public Void onCompleted(Response response) throws Exception {
                            // TODO: Validate this is the only response type to a DELETE (save for an error instance...)
                            try {
                                if (response.getStatusCode() == 204) {
                                    future.set(true);
                                } else {
                                    if (response.getStatusCode() > 300) {
                                        parseErrorResponse(response, future);
                                    } else {
                                        future.set(false);
                                    }
                                }
                            } catch (Exception ex) {
                                future.setException(ex);
                            }
                            return null;
                        }
                    });
        } catch (Exception ex) {
            future.setException(ex);
        }

        return future;
    }

    private <T extends BaseRecurlyModel> void setResponse(Response response, SettableFuture<T> future,  Class<T> clazz) {
        try {
            if (response.getStatusCode() > 300) {
                parseErrorResponse(response, future);
            } else {
                future.set(deserializer.parse(response.getResponseBodyAsStream(), clazz));
            }
        } catch (Exception ex) {
            future.setException(ex);
        }
    }

    private void parseErrorResponse(final Response response, final SettableFuture<?> future) {
        try {
            RecurlyErrors errors = new RecurlyErrors();
            errors = deserializer.parse(response.getResponseBodyAsStream(), RecurlyErrors.class);
            if (!errors.isEmpty()) {
                // If we encountered a transaction error, the deserializer only puts that error in the object.
                // So we can safely call get(0) since the errors object is not empty.
                if (errors.hasTransactionError()) {
                    future.setException(new TransactionErrorException(errors.getTransactionErrors().get(0)));
                } else {
                    RecurlyAPIErrorException reculryException = new RecurlyAPIErrorException("APIError", errors);
                    if(response.getStatusCode()==404){
                        future.setException(new BillingEntityNotFoundException("Recurly account or subscription not found",reculryException));
                    }else{
                        future.setException(reculryException);
                    }
                }
            } else {
                future.setException(new UnknownError("Unknown error. Received " + response.getStatusCode() + " From Recurly"));
            }
        } catch (Exception e) {
            future.setException(e);
        }
    }
}

