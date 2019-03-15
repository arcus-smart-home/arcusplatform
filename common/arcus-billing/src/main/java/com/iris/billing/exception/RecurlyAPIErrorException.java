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
package com.iris.billing.exception;

import com.iris.billing.client.model.RecurlyErrors;

public class RecurlyAPIErrorException extends BaseException {
   private final String errorCode;
   private final RecurlyErrors errors;

   // To support android we can only use "Exception(String, Throwable)"
   public RecurlyAPIErrorException(String code) {
      super("[" + code + "]: ", null);

      this.errors = new RecurlyErrors();
      this.errorCode = code;
   }

   public RecurlyAPIErrorException(String code, String message) {
      super("[" + code + "]: " + message, null);

      this.errors = new RecurlyErrors();
      this.errorCode = code;
   }

   public RecurlyAPIErrorException(String code, RecurlyErrors errors) {
      super("[" + code + "]: ", null);

      this.errorCode = code;
      this.errors = errors;
   }

   public RecurlyErrors getErrors() {
      return errors;
   }

   public String getCode() {
      return errorCode;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("RecurlyAPIErrorException [errorCode=").append(errorCode)
            .append(", errors=").append(errors).append("]");
      return builder.toString();
   }
}

