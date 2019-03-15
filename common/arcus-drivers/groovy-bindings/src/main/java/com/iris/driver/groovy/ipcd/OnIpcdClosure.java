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

import java.util.Map;

import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.ipcd.bindings.MessageTypeClosures;
import com.iris.protocol.ipcd.message.model.MessageType;
import com.iris.protocol.ipcd.message.model.StatusType;

import groovy.lang.Closure;

@SuppressWarnings("serial")
public class OnIpcdClosure extends BaseIpcdClosure {
   private final Map<String, Closure<Object>> properties;
   
   public OnIpcdClosure(EnvironmentBinding binding) {
      super(binding);
      properties = MessageTypeClosures.buildMessageTypeClosures(binding);
   }
   
   @Override
   public Object getProperty(String property) {
      Object o = property != null ? properties.get(property.toLowerCase()) : null;
      if(o != null) {
         return o;
      }
      return super.getProperty(property);
   }
   
   @Override
   public Object invokeMethod(String name, Object arguments) {
      Object o = properties.get(name);
      if(o != null && o instanceof Closure<?>) {
         return ((Closure<?>) o).call((Object[]) arguments);
      }
      return super.invokeMethod(name, arguments);
   }

   protected void doCall(MessageType messageType, String commandName, StatusType statusType, Closure<?> closure) {
      addHandler(messageType, commandName, statusType, closure);
   }
   
   protected void doCall(String messageType, String commandName, StatusType statusType, Closure<?> closure) {
      addHandler(MessageType.fromString(messageType), commandName, statusType, closure);
   }
   
   protected void doCall(MessageType messageType, String commandName, String statusType, Closure<?> closure) {
      addHandler(messageType, commandName, StatusType.fromString(statusType), closure);
   }
   
   protected void doCall(String messageType, String commandName, String statusType, Closure<?> closure) {
      addHandler(MessageType.fromString(messageType), commandName, StatusType.fromString(statusType), closure);
   }
   
   protected void doCall(MessageType messageType, String commandName, Closure<?> closure) {
      addHandler(messageType, commandName, null, closure);
   }
   
   protected void doCall(String messageType, String commandName, Closure<?> closure) {
      addHandler(MessageType.fromString(messageType), commandName, null, closure);
   }
   
   protected void doCall(String messageType, Closure<?> closure) {
      addHandler(MessageType.fromString(messageType), null, null, closure);
   }
   
   protected void doCall(MessageType messageType, Closure<?> closure) {
      addHandler(messageType, null, null, closure);
   }
   
   protected void doCall(Closure<?> closure) {
      addHandler(null, null, null, closure);
   }
}

