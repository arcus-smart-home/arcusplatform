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
package com.iris.platform.subscription;

import com.iris.messages.ErrorEvent;
import com.iris.messages.errors.Errors;

public class SubscriptionUpdateException extends Exception {

   public SubscriptionUpdateException() {
      super();
   }

   public SubscriptionUpdateException(String message, Throwable cause,
         boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }

   public SubscriptionUpdateException(String message, Throwable cause) {
      super(message, cause);
   }

   public SubscriptionUpdateException(String message) {
      super(message);
   }

   public SubscriptionUpdateException(Throwable cause) {
      super(cause);
   }

   public ErrorEvent toError() {
      if(getMessage() != null && getMessage().contains("transactioncode")){
         String[] transactionMessage = getMessage().split(",");
         return Errors.fromCode(transactionMessage[0], transactionMessage[1]);
      } else if(getMessage() != null && getMessage().contains("apiexceptioncode")) {
         String [] transactionMessage = getMessage().split(",");
         return Errors.fromCode("recurly.apierror", transactionMessage[1]);
      }
      return ErrorEvent.fromCode("unable.to.update.recurly", "Billing information could not be updated.");
   }
}

