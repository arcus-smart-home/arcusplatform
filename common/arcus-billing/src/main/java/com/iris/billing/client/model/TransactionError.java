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

import com.iris.messages.model.Copyable;

public class TransactionError extends RecurlyModel implements Copyable<TransactionError> {
   
   public static class Tags {
      private Tags() {}
      public static final String TAG_NAME = "transaction_error";

      public static final String CODE = "error_code";
      public static final String CATEGORY = "error_category";
      public static final String MERCHANT_MSG = "merchant_message";
      public static final String CUSTOMER_MSG = "customer_message";
   }

   private String errorCode;
   private String errorCategory;
   private String merchantMessage;
   private String customerMessage;
   
   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }
   
   @Override
   public RecurlyModels<?> createContainer() {
      // There is no container for TransactionError.
      return null;
   }


   public String getErrorCode() {
      return errorCode;
   }

   public void setErrorCode(String errorCode) {
      this.errorCode = errorCode;
   }

   public String getErrorCategory() {
      return errorCategory;
   }

   public void setErrorCategory(String errorCategory) {
      this.errorCategory = errorCategory;
   }

   public String getMerchantMessage() {
      return merchantMessage;
   }

   public void setMerchantMessage(String merchantMessage) {
      this.merchantMessage = merchantMessage;
   }

   public String getCustomerMessage() {
      return customerMessage;
   }

   public void setCustomerMessage(String customerMessage) {
      this.customerMessage = customerMessage;
   }

   @Override
   public TransactionError copy() {
      try {
         return (TransactionError)clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      TransactionError clone = new TransactionError();
      clone.setErrorCode(errorCode);
      clone.setErrorCategory(errorCategory);
      clone.setMerchantMessage(merchantMessage);
      clone.setCustomerMessage(customerMessage);
      return clone;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("TransactionError [errorCode=").append(errorCode)
      .append(", errorCategory=").append(errorCategory)
      .append(", merchantMessage=").append(merchantMessage)
      .append(", customerMessage=").append(customerMessage).append("]");
      return builder.toString();
   }
   
}

