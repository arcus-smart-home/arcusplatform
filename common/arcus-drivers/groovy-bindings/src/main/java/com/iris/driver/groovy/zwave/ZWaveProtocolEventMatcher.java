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
/**
 * 
 */
package com.iris.driver.groovy.zwave;

import com.iris.driver.metadata.ProtocolEventMatcher;

/**
 *
 */
public class ZWaveProtocolEventMatcher extends ProtocolEventMatcher {
   private String messageType;
   private Byte commandClass;
   private Byte commandId;
   
   public String getMessageType() {
      return messageType;
   }
   
   public void setMessageType(String messageType) {
      this.messageType = messageType;
   }
   
   public boolean matchesAnyMessageType() {
      return messageType == null;
   }
   
   public Byte getCommandClass() {
      return commandClass;
   }
   
   public void setCommandClass(Byte commandClass) {
      this.commandClass = commandClass;
   }
   
   public boolean matchesAnyCommandClass() {
      return commandClass == null;
   }
   
   public Byte getCommandId() {
      return commandId;
   }
   
   public void setCommandId(Byte commandId) {
      this.commandId = commandId;
   }
   
   public boolean matchesAnyCommandId() {
      return commandId == null;
   }

   @Override
   public String toString() {
      return "ZWaveProtocolEventMatcher [messageType=" + (messageType !=null ? messageType : "*") 
            + ", commandClass=" + (commandClass != null ? "0x" + Integer.toHexString(commandClass & 0xff) : "*")
            + ", commandId=" + (commandId != null ? commandId : "*") + "]";
   }

}

