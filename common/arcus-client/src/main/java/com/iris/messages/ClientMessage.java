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
package com.iris.messages;

import java.util.HashMap;
import java.util.Map;

public class ClientMessage {

   private static final String SOURCE_PROP = "source";
   private static final String DESTINATION_PROP = "destination";
   private static final String CORRELATIONID_PROP = "correlationId";
   private static final String ISREQUEST_PROP = "isRequest";

   public static class Builder {
      private final Map<String,Object> builderHeaders = new HashMap<>();
      private MessageBody builderPayload;

      public Builder withHeaders(Map<String,Object> headers) {
         builderHeaders.putAll(headers);
         return this;
      }

      public Builder withHeader(String name, Object value) {
         builderHeaders.put(name, value);
         return this;
      }

      public Builder withSource(String source) {
         return withHeader(SOURCE_PROP, source);
      }

      public Builder withDestination(String destination) {
         return withHeader(DESTINATION_PROP, destination);
      }

      public Builder withCorrelationId(String correlationId) {
         return withHeader(CORRELATIONID_PROP, correlationId);
      }

      public Builder isRequest(boolean isRequest) {
         return withHeader(ISREQUEST_PROP, isRequest);
      }

      public Builder withPayload(MessageBody payload) {
         builderPayload = payload;
         return this;
      }

      public ClientMessage create() {
         return new ClientMessage(builderHeaders, builderPayload);
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   private final Map<String,Object> headers;
   private final MessageBody payload;

   private ClientMessage(Map<String,Object> headers, MessageBody payload) {
      this.headers = headers;
      this.payload = payload;
   }

   public String getType() {
      return payload != null ? payload.getMessageType() : null;
   }

   public Map<String, Object> getHeaders() {
      return headers;
   }

   public MessageBody getPayload() {
      return payload;
   }

   public String getSource() {
      return (String) headers.get(SOURCE_PROP);
   }

   public String getCorrelationId() {
      return (String) headers.get(CORRELATIONID_PROP);
   }

   public String getDestination() {
      return (String) headers.get(DESTINATION_PROP);
   }

   public boolean isRequest() {
      Object obj = headers.get(ISREQUEST_PROP);
      if (obj instanceof String) {
         return Boolean.valueOf((String) obj);
      }
         
      return (boolean) headers.get(ISREQUEST_PROP);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((headers == null) ? 0 : headers.hashCode());
      result = prime * result + ((payload == null) ? 0 : payload.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      ClientMessage other = (ClientMessage) obj;
      if (headers == null) {
         if (other.headers != null)
            return false;
      } else if (!headers.equals(other.headers))
         return false;
      if (payload == null) {
         if (other.payload != null)
            return false;
      } else if (!payload.equals(other.payload))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "ClientMessage [headers=" + headers
            + ", payload=" + payload + "]";
   }
}

