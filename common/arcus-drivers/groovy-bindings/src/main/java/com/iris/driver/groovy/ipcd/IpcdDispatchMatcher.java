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

import org.apache.commons.lang3.StringUtils;

import com.iris.driver.metadata.EventMatcher;
import com.iris.protocol.ipcd.message.model.MessageType;

public class IpcdDispatchMatcher extends EventMatcher {
   private MessageType type;
   private String event;
   private String parameter;
   
   public boolean matchesAnyEvent() {
      return StringUtils.isEmpty(event);
   }
   
   public boolean matchesAnyParameter() {
      return StringUtils.isEmpty(parameter);
   }
   
   public MessageType getType() {
      return type;
   }
   public void setType(MessageType type) {
      this.type = type;
   }
   public String getEvent() {
      return event;
   }
   public void setEvent(String event) {
      this.event = event;
   }
   public String getParameter() {
      return parameter;
   }
   public void setParameter(String parameter) {
      this.parameter = parameter;
   }
}

