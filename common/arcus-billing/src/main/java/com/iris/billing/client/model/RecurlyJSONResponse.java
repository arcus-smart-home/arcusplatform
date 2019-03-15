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
import java.util.List;

import com.google.common.base.Strings;

public class RecurlyJSONResponse {
   private ResponseParameters error;
   private String id;

   public boolean isError() {
      return Strings.isNullOrEmpty(id);
   }

   public String getID() {
      return id;
   }

   public List<String> getFields() {
      if (error != null) {
         return error.getFields();
      }

      return Collections.<String>emptyList();
   }

   public String getCode() {
      if (error != null) {
         return error.getCode();
      }

      return null;
   }

   public String getMessage() {
      if (error != null) {
         return error.getMessage();
      }

      return null;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("RecurlyJSONResponse [error=").append(error)
            .append(", id=").append(id).append("]");
      return builder.toString();
   }

   private class ResponseParameters {
      private String code;
      private String message;
      private List<String> fields;
      public final List<String> getFields() {
         return fields;
      }
      public final String getCode() {
         return code;
      }
      public final String getMessage() {
         return message;
      }
      @Override
      public String toString() {
         StringBuilder builder = new StringBuilder();
         builder.append("code=").append(code).append(", message=")
               .append(message).append(", fields=").append(fields);
         return builder.toString();
      }
   }
}

