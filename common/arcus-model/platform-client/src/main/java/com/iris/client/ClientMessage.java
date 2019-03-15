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
package com.iris.client;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.client.event.ClientEventFactory;
import com.iris.client.event.DefaultClientEventFactory;

public class ClientMessage {
   private static final Logger logger = LoggerFactory.getLogger(ClientMessage.class);

   private static final String SOURCE_PROP = "source";
   private static final String DESTINATION_PROP = "destination";
   private static final String CORRELATIONID_PROP = "correlationId";
   private static final String ISREQUEST_PROP = "isRequest";

   // TODO make this injectable
   private static final ClientEventFactory factory = loadFactory();
   
   private static ClientEventFactory loadFactory() {
      try {
         return (ClientEventFactory) Class.forName("com.iris.client.GeneratedEventFactory").newInstance();
      }
      catch(Exception e) {
         logger.warn("Unable to load generated events, reverting to simple events");
         return new DefaultClientEventFactory();
      }
   }

   public static class Builder {
      private final Map<String,Object> builderHeaders = new HashMap<>();
      private String eventType;
      private Map<String, Object> attributes;

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
      
      public Builder withType(String type) {
         this.eventType = type;
         return this;
      }

      public Builder withAttributes(Map<String, Object> attributes) {
         if(this.attributes == null) {
            this.attributes = new HashMap<String, Object>();
         }
         if(attributes != null) {
            this.attributes.putAll(attributes);
         }
         return this;
      }

      public Builder withAttribute(String attribute, Object value) {
         if(this.attributes == null) {
            this.attributes = new HashMap<String, Object>();
         }
         this.attributes.put(attribute, value);
         return this;
      }
      
      public ClientMessage create() {
         return new ClientMessage(builderHeaders, factory.create(eventType, (String) builderHeaders.get(SOURCE_PROP), attributes));
      }
   }

   public static Builder builder() {
      return new Builder();
   }
   
   private final Map<String,Object> headers;
   private final ClientEvent event;

   private ClientMessage(Map<String,Object> headers, ClientEvent event) {
      this.headers = headers;
      this.event = event;
   }

   public String getType() {
      return event != null ? event.getType() : null;
   }

   public Map<String, Object> getHeaders() {
      return headers;
   }

   public ClientEvent getEvent() {
      return event;
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
      return (boolean) headers.get(ISREQUEST_PROP);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((headers == null) ? 0 : headers.hashCode());
      result = prime * result + ((event == null) ? 0 : event.hashCode());
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
      if (event == null) {
         if (other.event != null)
            return false;
      } else if (!event.equals(other.event))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "ClientMessage [headers=" + headers
            + ", payload=" + event + "]";
   }
}

