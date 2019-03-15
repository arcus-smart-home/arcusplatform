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
package com.iris.protocol.ipcd.definition.context;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefinitionContext {
   public static final String VIRTUAL_TYPE = "*";
   
   private Map<String, IpcdObject> messages;
   private List<IpcdObject> commands;
   private List<IpcdObject> responses;
   private List<IpcdEnum> enums;
   private List<IpcdObject> types;

   public IpcdObject getCommandDefinition() {
      return messages.get("command");
   }
   
   public IpcdObject getResponseDefinition() {
      return messages.get("response");
   }

   public Collection<IpcdObject> getMessages() {
      return messages.values();
   }
   public void setMessages(List<IpcdObject> messageList) {
      messages = new LinkedHashMap<>(messageList.size());
      for (IpcdObject message : messageList) {
         messages.put(message.getName(), message);
      }
   }
   public List<IpcdObject> getCommands() {
      return commands;
   }
   public void setCommands(List<IpcdObject> commands) {
      this.commands = commands;
   }
   public List<IpcdObject> getResponses() {
      return responses;
   }
   public List<IpcdEnum> getEnums() {
      return enums;
   }
   public void setEnums(List<IpcdEnum> enums) {
      this.enums = enums;
   }

   public void setResponses(List<IpcdObject> responses) {
      this.responses = responses;
   }
   public List<IpcdObject> getTypes() {
      return types;
   }
   public void setTypes(List<IpcdObject> types) {
      this.types = types;
   }
}

