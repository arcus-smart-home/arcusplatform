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
package com.iris.driver.groovy.control;

import com.iris.driver.metadata.ProtocolEventMatcher;

public class ControlProtocolEventMatcher extends ProtocolEventMatcher {
   private String messageType;

   public String getMessageType() {
      return messageType;
   }

   public void setMessageType(String messageType) {
      this.messageType = messageType;
   }

   public boolean matchesAnyMessageType() {
      return messageType == null;
   }

   @Override
   public String toString() {
      return "ControlProtocolEventMatcher [messageType=" + messageType + "]";
   }
}

