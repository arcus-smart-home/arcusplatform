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
package com.iris.google.model;

import com.google.gson.JsonElement;

import java.util.List;

public class Request {

   private String requestId;
   private List<Input> inputs;

   public String getRequestId() {
      return requestId;
   }

   public void setRequestId(String requestId) {
      this.requestId = requestId;
   }

   public List<Input> getInputs() {
      return inputs;
   }

   public void setInputs(List<Input> inputs) {
      this.inputs = inputs;
   }

   @Override
   public String toString() {
      return "Request{" +
            "requestId='" + requestId + '\'' +
            ", inputs=" + inputs +
            '}';
   }

   public static class Input {

      private String intent;
      private JsonElement payload;

      public String getIntent() {
         return intent;
      }

      public void setIntent(String intent) {
         this.intent = intent;
      }

      public JsonElement getPayload() {
         return payload;
      }

      public void setPayload(JsonElement payload) {
         this.payload = payload;
      }

      @Override
      public String toString() {
         return "Input{" +
               "intent='" + intent + '\'' +
               ", payload=" + payload +
               '}';
      }
   }
}

