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
package com.iris.billing.client.model;

import java.util.Collections;
import java.util.Map;

public abstract class RecurlyModel implements BaseRecurlyModel {
   // Links to other activities within the account
//   addonHREF;
//   planHREF;
//   accountHREF;
//   billingInfoHREF;
//   invoiceHREF;
//   subscriptionHREF;
//   redemptionHREF;
//   transactionHREF;
//   adjustmentHREF;
//   reopenHREF;
//   cancelHREF;
//   terminateHREF;
//   postponeHREF;
//   refundHREF;

   // TODO: Add-in a "Link" element for start, prev, next links in the header?

	@Override
   public abstract String getTagName();

   @Override
   public Map<String, Object> getXMLMappings() {
      return Collections.<String, Object>emptyMap();
   }
   
   public abstract RecurlyModels<?> createContainer();
}

