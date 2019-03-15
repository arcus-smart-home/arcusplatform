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
package com.iris.driver.groovy.ipcd;

import com.iris.driver.metadata.ProtocolEventMatcher;
import com.iris.protocol.ipcd.message.model.MessageType;
import com.iris.protocol.ipcd.message.model.StatusType;

public class IpcdProtocolEventMatcher extends ProtocolEventMatcher {
   private MessageType messageType;
   private StatusType statusType;
   private String commandName;
   
   public boolean matchesAnyMessageType() {
      return messageType == null;
   }
   
   public boolean matchesAnyStatusType() {
      return statusType == null;
   }
   
   public boolean matchesAnyCommandName() {
      return commandName == null;
   }
   
   public MessageType getMessageType() {
      return messageType;
   }
   public void setMessageType(MessageType messageType) {
      this.messageType = messageType;
   }
   public StatusType getStatusType() {
      return statusType;
   }
   public void setStatusType(StatusType statusType) {
      this.statusType = statusType;
   }
   public String getCommandName() {
      return commandName;
   }
   public void setCommandName(String commandName) {
      this.commandName = commandName;
   }

   @Override
   public String toString() {
      return "IpcdProtocolEventMatcher [messageType=" + messageType
            + ", statusType=" + statusType + ", commandName=" + commandName
            + "]";
   }
   
   
}

