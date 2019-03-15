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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class RecurlyCurrencyFormats {
   private static final String DEFAULT_CURRENCY = "USD";
   private static List<String> currencyFormats = new ArrayList<String>();

   protected RecurlyCurrencyFormats() {}

   public static final List<String> getCurrencyFormats() {
      if (currencyFormats.isEmpty()) {
         populateList();
      }
      return Collections.unmodifiableList(currencyFormats);
   }

   public static final boolean isValidCurrency(String currency) {
      if (currencyFormats.isEmpty()) {
         populateList();
      }
      return currencyFormats.contains(currency);
   }

   public static String getDefaultCurrency() {
      return RecurlyCurrencyFormats.DEFAULT_CURRENCY;
   }

   private static final void populateList() {
      currencyFormats.add("USD");
      currencyFormats.add("AUD");
      currencyFormats.add("CAD");
      currencyFormats.add("EUR");
      currencyFormats.add("GBP");
      currencyFormats.add("CZK");
      currencyFormats.add("DKK");
      currencyFormats.add("HUF");
      currencyFormats.add("NOK");
      currencyFormats.add("NZD");
      currencyFormats.add("PLN");
      currencyFormats.add("SGD");
      currencyFormats.add("SEK");
      currencyFormats.add("CHF");
      currencyFormats.add("ZAR");
   }
}

