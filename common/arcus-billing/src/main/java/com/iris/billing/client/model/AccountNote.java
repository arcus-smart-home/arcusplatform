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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AccountNote extends RecurlyModel {
   public static class Tags {
      private Tags() {}
      public static final String URL_RESOURCE = "/notes";

      public static final String TAG_NAME = "note";
      
      public static final String MESSAGE = "message";
      public static final String CREATED_AT = "created_at";
   }

   private String message;
   private Date createdAt;

   private Map<String, String> storedValues = new HashMap<String, String>();

   public final String getMessage() {
      return message;
   }

   public final Date getCreatedAt() {
      return createdAt;
   }

   public final void setMessage(String message) {
      this.message = message;
      storedValues.put(Tags.MESSAGE, message);
   }

   public final void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

   @Override
   public String getTagName() {
      return Tags.TAG_NAME;
   }
   
   @Override
   public RecurlyModels<?> createContainer() {
      AccountNotes accountNotes = new AccountNotes();
      accountNotes.add(this);
      return accountNotes;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("AccountNote [message=").append(message)
      .append(", createdAt=").append(createdAt).append("]");
      return builder.toString();
   }
}

