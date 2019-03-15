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
package com.iris.platform.services.account.billing;

public interface BillingErrorCodes {

   static final String MISSING_ACCOUNT_ERR = "missing.argument.accountID";
   static final String MISSING_ACCOUNT_MSG = "Missing required argument Account ID";

   static final String MISSING_BILLING_TOKEN_ERR = "missing.argument.billingToken";
   static final String MISSING_BILLING_TOKEN_MSG = "Missing required billing token.";

   static final String MISSING_PLACE_ID_ERR = "missing.argument.placeID";
   static final String MISSING_PLACE_ID_MSG = "Missing required place ID.";

   static final String MISSING_PLACE_ID_DB_ERR = "invalid.argument.placeID";
   static final String MISSING_PLACE_ID_DB_MSG = "Unable to locate requested place ID.";

   static final String MISSING_ACCOUNT_OWNER_CODE = "invalid.argument.owner";
   static final String MISSING_ACCOUNT_OWNER_MSG = "Unable to locate account owner";

   static final String INVALID_PLACE_ID_ERR = "invalid.argument.invalid_placeID";
   static final String INVALID_PLACE_ID_MSG = "Place given does not belong to this account";

}

