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
package com.iris.alexa.message;

import org.eclipse.jdt.annotation.Nullable;

public final class Header {

   public static Header v2(String messageId, String name, String namespace) {
      return new Header(messageId, name, namespace, "2", null);
   }

   public static Header v3(String messageId, String name, String namespace, @Nullable String correlationToken) {
      return new Header(messageId, name, namespace, "3", correlationToken);
   }

   private final String messageId;
   private final String name;
   private final String namespace;
   private final String payloadVersion;
   private @Nullable final String correlationToken;

   public Header(
      String messageId,
      String name,
      String namespace,
      String payloadVersion,
      @Nullable String correlationToken
   ) {
      this.messageId = messageId;
      this.name = name;
      this.namespace = namespace;
      this.payloadVersion = payloadVersion;
      this.correlationToken = correlationToken;
   }

   public String getMessageId() {
      return messageId;
   }

   public String getName() {
      return name;
   }

   public String getNamespace() {
      return namespace;
   }

   public String getPayloadVersion() {
      return payloadVersion;
   }

   public boolean isV2() {
      return "2".equals(payloadVersion);
   }

   public boolean isV3() {
      return "3".equals(payloadVersion);
   }

   @Nullable
   public String getCorrelationToken() {
      return correlationToken;
   }

   @Override
   public String toString() {
      return "Header{" +
         "messageId='" + messageId + '\'' +
         ", name='" + name + '\'' +
         ", namespace='" + namespace + '\'' +
         ", payloadVersion='" + payloadVersion + '\'' +
         ", correlationToken='" + correlationToken + '\'' +
         '}';
   }
}

