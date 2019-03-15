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
package com.iris.client.impl.netty;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class NettyWebsocket {
   private final URI uri;
   private final TextMessageHandler textHandler;
   private final List<Map.Entry<String, Object>> headers;
   private final int retryAttempts;
   private final int retryDelay;
   private final int maxFrameSize;
   
   protected NettyWebsocket(URI uri, 
         TextMessageHandler textHandler, 
         List<Map.Entry<String, Object>> headers,
         int retryAttempts,
         int retryDelay,
         int maxFrameSize) {
      this.uri = uri;
      this.textHandler = textHandler;
      this.headers = new ArrayList<>(headers);
      this.retryAttempts = retryAttempts;
      this.retryDelay = retryDelay;
      this.maxFrameSize = maxFrameSize;
   }

   public URI getUri() {
      return uri;
   }

   public TextMessageHandler getTextHandler() {
      return textHandler;
   }

   public List<Map.Entry<String, Object>> getHeaders() {
      return headers;
   }
   
   public int getRetryAttempts() {
      return retryAttempts;
   }

   public int getRetryDelay() {
      return retryDelay;
   }

   public int getMaxFrameSize() {
      return maxFrameSize;
   }

   public static Builder builder() {
      return new Builder();
   }
   
   public static class Builder {
      private final static Logger logger = LoggerFactory.getLogger(NettyWebsocket.Builder.class);
      private URI uri;
      private TextMessageHandler textHandler;
      private List<Map.Entry<String, Object>> headers;
      private int retryAttempts = 0;
      private int retryDelay = 1;
      private int maxFrameSize = 65535;
      
      protected Builder() {}
      
      public Builder uri(String uri) {
         try {
            this.uri = new URI(uri);
         } catch (URISyntaxException e) {
            logger.error("Attempting to make request to invalid URI [{}]", uri);
            throw new RuntimeException("Invalid URI for http request", e);
         }
         return this;
      }
      
      public Builder uri(URI uri) {
         this.uri = uri;
         return this;
      }
      
      public Builder setTextHandler(TextMessageHandler textHandler) {
         this.textHandler = textHandler;
         return this;
      }
      
      public Builder addHeader(String name, Object value) {
         if (headers == null) {
            headers = new ArrayList<>();
         }
         Map.Entry<String, Object> headerEntry = Maps.immutableEntry(name, value);
         headers.add(headerEntry);
         return this;
      }
      
      public Builder retryAttempts(int retryAttempts) {
         this.retryAttempts = retryAttempts;
         return this;
      }
      
      public Builder retryDelayInSeconds(int retryDelay) {
         this.retryDelay = retryDelay;
         return this;
      }
      
      public Builder maxFrameSize(int maxFrameSize) {
         this.maxFrameSize = maxFrameSize;
         return this;
      }
      
      public NettyWebsocket build() {
         return new NettyWebsocket(uri, textHandler, headers, retryAttempts, retryDelay, maxFrameSize);
      }
   }
}

