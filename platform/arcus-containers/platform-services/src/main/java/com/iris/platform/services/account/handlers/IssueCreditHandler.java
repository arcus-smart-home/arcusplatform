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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.billing.client.BillingClient;
import com.iris.billing.client.model.RecurlyError;
import com.iris.billing.client.model.request.AdjustmentRequest;
import com.iris.billing.exception.RecurlyAPIErrorException;
import com.iris.billing.exception.TransactionErrorException;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class IssueCreditHandler implements ContextualRequestMessageHandler<Account> {
    private final Logger logger = LoggerFactory.getLogger(IssueCreditHandler.class);

    private static final String MISSING_ACCOUNT_ID_ERR = "missing.argument.accountID";
    private static final String MISSING_ACCOUNT_ID_MSG = "Missing required account ID.";

    private static final String MISSING_AMOUNT_ERR = "missing.argument.amountInCents";
    private static final String MISSING_AMOUNT_MSG = "Missing required amountInCents parameter.";

    private BillingClient client;
    private final PlatformMessageBus platformBus;

    @Inject(optional = true)
    @Named(value = "billing.timeout")
    private int billingTimeout = 30;

    @Inject
    public IssueCreditHandler(BillingClient client, PlatformMessageBus platformBus) {
        this.client = client;
        this.platformBus = platformBus;
    }

    @Override
    public String getMessageType() {
        return AccountCapability.IssueCreditRequest.NAME;
    }

    @Override
    public MessageBody handleRequest(Account context, PlatformMessage msg) {
        Preconditions.checkArgument(context != null, "No account context was provided.");

        if (context.getId() == null) {
            logger.debug("Should have an account ID, showing null or empty for Account [{}]", context.getId());
            throw new IllegalStateException("No account ID's found associated to account.");
        }

        MessageBody request = msg.getValue();

        logger.debug("Issue Credit Message {}", request);

        String amountString = AccountCapability.IssueCreditRequest.getAmountInCents(request);
        if (Strings.isNullOrEmpty(amountString)) {
            return ErrorEvent.fromCode(
                    MISSING_AMOUNT_ERR,
                    MISSING_AMOUNT_MSG);
        }

        String description = AccountCapability.IssueCreditRequest.getDescription(request);

        AdjustmentRequest adjustmentRequest = new AdjustmentRequest();
        adjustmentRequest.setAmountInCents(amountString);
        adjustmentRequest.setDescription(description);

        try {
            client.issueCredit(context.getId().toString(), adjustmentRequest).get(billingTimeout, TimeUnit.SECONDS);
        } catch (Exception ex) {
            if (ex.getCause() instanceof RecurlyAPIErrorException) {
                logger.debug("Recurly API Error Received: {}", ((RecurlyAPIErrorException)ex.getCause()).getErrors());

                // This is typically an error with the billing client itself (invalid XML)
                //
                // The only instance I can think of (that is action-able by the client) is "invalid_token"
                // responses when a client submits a token ReCurly can't find (expired or doesn't exist)

                // If this is a token error, the first instance will hold that.
                RecurlyError error = ((RecurlyAPIErrorException)ex.getCause()).getErrors().get(0);
                return Errors.fromCode(error.getErrorSymbol(), error.getErrorText());
            } else if (ex.getCause() instanceof TransactionErrorException) {
                logger.debug("Transaction Error {}", ex);

                TransactionErrorException e = (TransactionErrorException) ex.getCause();
                return Errors.fromCode(e.getErrorCode(), e.getCustomerMessage());
            } else {
                logger.debug("Error {}", ex);
                return Errors.fromException(ex);
            }
        }

        return AccountCapability.IssueCreditResponse.instance();
    }
}

