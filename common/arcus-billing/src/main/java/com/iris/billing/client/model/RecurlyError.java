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

import java.util.Map;


public class RecurlyError extends RecurlyModel {
   public static class Tags {
      private Tags() {}
      public static final String TAG_NAME = "error";
      public static final String ERROR_FIELD = "field";
      public static final String ERROR_SYMBOL = "symbol";
      public static final String ERROR_LANGUAGE = "lang";
      public static final String ERROR_DESCRIPTION_TAG = "description";
   }

   private String errorField;
   private String errorText;
   private String errorSymbol;
   private String language;

   public final String getErrorField() {
      return errorField;
   }

   public final void setErrorField(String errorField) {
      this.errorField = errorField;
   }

   public final String getErrorText() {
      return errorText;
   }

   public final void setErrorText(String errorText) {
      this.errorText = errorText;
   }

   public final String getErrorSymbol() {
      return errorSymbol;
   }

   public final void setErrorSymbol(String errorSymbol) {
      this.errorSymbol = errorSymbol;
   }

   public final String getLanguage() {
      return language;
   }

   public final void setLanguage(String language) {
      this.language = language;
   }

   @Override
   public Map<String, Object> getXMLMappings() {
      return null;
   }

   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }
   
   @Override
   public RecurlyModels<?> createContainer() {
      RecurlyErrors errors = new RecurlyErrors();
      errors.add(this);
      return errors;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("RecurlyError [transactionError=")
      .append(", transactionErrorDetails=")
      .append(", transaction=")
      .append(", errorField=").append(errorField)
      .append(", errorText=").append(errorText).append(", errorSymbol=")
      .append(errorSymbol).append(", language=").append(language)
      .append("]");
      return builder.toString();
   }
}

