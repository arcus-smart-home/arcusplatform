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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.billing.client.BillingClient;
import com.iris.billing.client.model.Adjustment;
import com.iris.billing.client.model.Adjustments;
import com.iris.billing.exception.BillingEntityNotFoundException;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Account;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ListAdjustmentsHandler implements ContextualRequestMessageHandler<Account>  {
    private static final Logger logger = LoggerFactory.getLogger(ListInvoicesHandler.class);
    private final BillingClient client;

    @Inject(optional = true)
    @Named(value = "billing.timeout")
    //config shared with all other billing handlers
    private int billingTimeout = 30;

    @Inject
    public ListAdjustmentsHandler(BillingClient client) {
        this.client = client;
    }

    @Override
    public String getMessageType() {
        return AccountCapability.ListAdjustmentsRequest.NAME;
    }

    @Override
    public MessageBody handleRequest(Account context, PlatformMessage msg) {
        Preconditions.checkArgument(context != null, "The account is required");

        String accountId = context.getId().toString();
        ListenableFuture<Adjustments> future = client.getAdjustmentsForAccount(accountId);

        Adjustments adjustments;
        try {
            adjustments = future.get(billingTimeout, TimeUnit.SECONDS);
        }
        catch(Exception e) {
            if(e instanceof ExecutionException) {
                Throwable t = e.getCause();
                if(!(t instanceof BillingEntityNotFoundException)) {
                    throw new ErrorEventException(Errors.CODE_GENERIC, "Cannot retrieve adjustments from recurly for " + accountId);
                } else {
                    logger.debug("no billing account for {}, returning empty invoices", accountId);
                    return AccountCapability.ListAdjustmentsResponse.builder()
                            .withAdjustments(Collections.emptyList())
                            .build();
                }
            }
            throw new ErrorEventException(Errors.CODE_GENERIC, "Cannot retrieve adjustments from recurly for " + accountId);
        }


        List<Map<String, Object>> maps = new ArrayList<>();
        if (adjustments != null) {
            maps.addAll(adjustments.stream().map(this::convertAdjustmentToAttributes).collect(Collectors.toList()));
        }

        return AccountCapability.ListAdjustmentsResponse.builder()
                .withAdjustments(maps)
                .build();
    }
    private Map<String, Object> convertAdjustmentToAttributes(Adjustment adjustment) {
        Map<String, Object> map = new HashMap<>();

        //no transformer at this time since Adjustment comes from billing
        map.put(com.iris.messages.type.Adjustment.ATTR_ACCOUNTINGCODE, adjustment.getAccountingCode());
        map.put(com.iris.messages.type.Adjustment.ATTR_ADJUSTMENTID, adjustment.getAdjustmentID());
        map.put(com.iris.messages.type.Adjustment.ATTR_ADJUSTMENTTYPE, adjustment.getAdjustmentType());
        map.put(com.iris.messages.type.Adjustment.ATTR_CREATEDAT, adjustment.getCreatedAt());
        map.put(com.iris.messages.type.Adjustment.ATTR_DESCRIPTION, adjustment.getDescription());
        map.put(com.iris.messages.type.Adjustment.ATTR_DISCOUNTINDOLLARS, convertCentStringToDollars(adjustment.getDiscountInCents()));
        map.put(com.iris.messages.type.Adjustment.ATTR_ENDDATE, adjustment.getEndDate());
        map.put(com.iris.messages.type.Adjustment.ATTR_ORIGIN, adjustment.getOrigin());
        map.put(com.iris.messages.type.Adjustment.ATTR_PRODUCTCODE, adjustment.getProductCode());
        map.put(com.iris.messages.type.Adjustment.ATTR_STATE, adjustment.getState());
        map.put(com.iris.messages.type.Adjustment.ATTR_STARTDATE, adjustment.getStartDate());
        map.put(com.iris.messages.type.Adjustment.ATTR_TAXINDOLLARS, convertCentStringToDollars(adjustment.getTaxInCents()));
        map.put(com.iris.messages.type.Adjustment.ATTR_TOTALINDOLLARS,convertCentStringToDollars(adjustment.getTotalInCents()));
        map.put(com.iris.messages.type.Adjustment.ATTR_UNITINDOLLARS, convertCentStringToDollars(adjustment.getUnitAmountInCents()));
        map.put(com.iris.messages.type.Adjustment.ATTR_QUANTITY, StringUtils.isNotEmpty(adjustment.getQuantity())?new Integer (adjustment.getQuantity()): null);

        return map;


    }
    private Double convertCentStringToDollars(String cents) {

        if(StringUtils.isNotEmpty(cents)) {
            return Integer.valueOf(cents) / 100.0;
        }else {
            return null;
        }
    }

}

