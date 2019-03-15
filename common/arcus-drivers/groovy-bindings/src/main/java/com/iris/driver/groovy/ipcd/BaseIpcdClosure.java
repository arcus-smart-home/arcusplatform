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

import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.message.model.MessageType;
import com.iris.protocol.ipcd.message.model.StatusType;

import groovy.lang.Closure;

@SuppressWarnings("serial")
public abstract class BaseIpcdClosure extends Closure<Object> {
   protected final EnvironmentBinding binding;
   
   protected BaseIpcdClosure(EnvironmentBinding binding) {
      super(binding);
      this.binding = binding;
      setResolveStrategy(TO_SELF);
   }
   
   @Override
   public void setProperty(String property, Object newValue) {
      throw new UnsupportedOperationException("Message type objects cannot have properties set");
   }

   protected void addHandler(MessageType messageType, String commandName, StatusType statusType, Closure<?> closure) {
      IpcdProtocolEventMatcher matcher = new IpcdProtocolEventMatcher();
      matcher.setProtocolName(IpcdProtocol.NAMESPACE);
      matcher.setStatusType(statusType);
      matcher.setMessageType(messageType);
      matcher.setCommandName(commandName);
      matcher.setHandler(DriverBinding.wrapAsHandler(closure));
      binding.getBuilder().addEventMatcher(matcher);
   }
}

