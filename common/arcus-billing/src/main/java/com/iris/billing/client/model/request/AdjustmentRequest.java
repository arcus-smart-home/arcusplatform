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

import com.iris.billing.client.model.Adjustment;
import com.iris.billing.client.model.RecurlyModel;
import com.iris.billing.client.model.RecurlyModels;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AdjustmentRequest extends RecurlyModel {
	private String currency;
	private String amountInCents;
	private String description;
	private String quantity;

	private Map<String, Object> mappings = new HashMap<String, Object>();

	@Override
	public Map<String, Object> getXMLMappings() {
		return Collections.unmodifiableMap(mappings);
	}

	@Override
	public String getTagName() {
		return Adjustment.Tags.TAG_NAME;
	}
	
	@Override
   public RecurlyModels<?> createContainer() {
      // There is no container for AdjustmentRequest
      return null;
   }

   public final String getCurrency() {
		return currency;
	}

	public final void setCurrency(String currency) {
		this.currency = currency;
		mappings.put(Adjustment.Tags.CURRENCY_TYPE, currency);
	}

	public final String getAmountInCents() {
		return amountInCents;
	}

	public final void setAmountInCents(String amountInCents) {
		this.amountInCents = amountInCents;
		mappings.put(Adjustment.Tags.AMOUNT_IN_CENTS, amountInCents);
	}

	public final String getDescription() {
		return description;
	}

	public final void setDescription(String description) {
		this.description = description;
		mappings.put(Adjustment.Tags.DESCRIPTION, description);
	}

	public final String getQuantity() {
		return quantity;
	}

	public final void setQuantity(String quantity) {
		this.quantity = quantity;
		mappings.put(Adjustment.Tags.QTY, quantity);
	}
}

