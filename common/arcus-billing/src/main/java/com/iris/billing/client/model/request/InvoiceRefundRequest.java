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
package com.iris.billing.client.model.request;

import com.iris.billing.client.model.Invoice;
import com.iris.billing.client.model.RecurlyModel;
import com.iris.billing.client.model.RecurlyModels;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InvoiceRefundRequest extends RecurlyModel {
    private String invoiceNumber;
    private String amountInCents;
    private String refundApplyOrder;

    private Map<String, Object> mappings = new HashMap<String, Object>();

    @Override
    public Map<String, Object> getXMLMappings() {
        return Collections.unmodifiableMap(mappings);
    }

    @Override
    public String getTagName() {
        return Invoice.Tags.TAG_NAME;
    }

    @Override
    public RecurlyModels<?> createContainer() {
        // There is no container for AdjustmentRequest
        return null;
    }

    public String getAmountInCents() {
        return amountInCents;
    }

    public void setAmountInCents(String amountInCents) {
        this.amountInCents = amountInCents;
        mappings.put("amount_in_cents", amountInCents);
    }

    public final String getInvoiceNumber() {
        return invoiceNumber;
    }

    public final void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getRefundApplyOrder() {
        return refundApplyOrder;
    }

    public void setRefundApplyOrder(String refundApplyOrder) {
        this.refundApplyOrder = refundApplyOrder;
        mappings.put("refund_apply_order", refundApplyOrder);
    }
}

