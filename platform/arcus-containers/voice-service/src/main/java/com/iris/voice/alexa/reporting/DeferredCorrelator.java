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
package com.iris.voice.alexa.reporting;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.DeviceAdvancedCapability;

public class DeferredCorrelator {

   private final String messageId;
   private final String correlationToken;
   private final Set<String> expectedAttributes;
   private final Set<String> completableDevAdvErrors;
   private final Date expiry;

   public DeferredCorrelator(String messageId, String correlationToken, Set<String> expectedAttributes, Set<String> completableDevAdvErrors, int timeoutSecs) {
      this.messageId = messageId;
      this.correlationToken = correlationToken;
      this.expectedAttributes = expectedAttributes;
      this.completableDevAdvErrors = completableDevAdvErrors;
      this.expiry = new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSecs));
   }

   public String getMessageId() {
      return messageId;
   }

   public String getCorrelationToken() {
      return correlationToken;
   }

   public synchronized boolean complete() {
      return expired() || expectedAttributes.isEmpty();
   }

   public synchronized boolean expired() {
      return new Date().after(expiry);
   }

   public synchronized boolean complete(MessageBody body) {
      expectedAttributes.removeAll(body.getAttributes().keySet());
      if(expectedAttributes.isEmpty()) {
         return true;
      }
      Map<String, String> errors = DeviceAdvancedCapability.getErrors(body);
      if(errors == null) {
         return false;
      }
      if(errors.isEmpty()) {
         return false;
      }
      if(!Sets.intersection(completableDevAdvErrors, errors.keySet()).isEmpty()) {
         expectedAttributes.clear();
         return true;
      }
      return false;
   }

   @Override
   public boolean equals(Object o) {
      if(this == o) return true;
      if(o == null || getClass() != o.getClass()) return false;

      DeferredCorrelator that = (DeferredCorrelator) o;

      if(messageId != null ? !messageId.equals(that.messageId) : that.messageId != null) return false;
      return correlationToken != null ? correlationToken.equals(that.correlationToken) : that.correlationToken == null;
   }

   @Override
   public int hashCode() {
      int result = messageId != null ? messageId.hashCode() : 0;
      result = 31 * result + (correlationToken != null ? correlationToken.hashCode() : 0);
      return result;
   }
}

