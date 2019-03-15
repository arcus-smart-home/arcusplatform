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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BillingInfoRequest {
   private Map<String, String> mappings = new HashMap<String, String>();

   public final void setCardNumber(String cardNumber) {
      mappings.put("number", cardNumber);
   }
   public final void setMonth(Integer month) {
      mappings.put("month",month.toString());
   }
   public final void setYear(Integer year) {
      mappings.put("year", year.toString());
   }
   public final void setFirstName(String firstName) {
      mappings.put("first_name",firstName);
   }
   public final void setLastName(String lastName) {
      mappings.put("last_name",lastName);
   }
   public final void setVerificationValue(String verificationValue) {
      mappings.put("cvv",verificationValue);
   }
   public final void setAddress1(String address1) {
      mappings.put("address1",address1);
   }
   public final void setAddress2(String address2) {
      mappings.put("address2",address2);
   }
   public final void setCity(String city) {
      mappings.put("city",city);
   }
   public final void setState(String state) {
      mappings.put("state",state);
   }
   public final void setPostalCode(String zipCode) {
      mappings.put("postal_code",zipCode);
   }
   public final void setCountry(String country) {
      mappings.put("country",country);
   }
   public final void setVatNumber(String vatNumber) {
      mappings.put("vat_number",vatNumber);
   }
   public final void setPublicKey(String publicKey) {
      mappings.put("key",publicKey);
   }
   public Map<String, String> getMappings() {
      return Collections.unmodifiableMap(mappings);
   }
}

