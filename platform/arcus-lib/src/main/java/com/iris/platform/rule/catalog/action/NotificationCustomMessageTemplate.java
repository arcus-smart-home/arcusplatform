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
package com.iris.platform.rule.catalog.action;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.platform.rule.catalog.RuleTemplate;
import com.iris.platform.rule.catalog.action.config.SendActionConfig;
import com.iris.platform.rule.catalog.template.TemplatedExpression;
import com.iris.platform.rule.catalog.template.TemplatedValue;

/**
 * 
 */
public class NotificationCustomMessageTemplate extends BaseNotificationTemplate {

   private TemplatedValue<String> to;
   private TemplatedValue<String> method;
   private TemplatedValue<String> message;
   
   public NotificationCustomMessageTemplate(Set<String> contextVariables) {
      super(contextVariables);
   }

   /**
    * @return the to
    */
   public TemplatedValue<String> getTo() {
      return to;
   }

   /**
    * @param to the to to set
    */
   public void setTo(TemplatedValue<String> to) {
      this.to = to;
   }

   /**
    * @return the method
    */
   public TemplatedValue<String> getMethod() {
      return method;
   }

   /**
    * @param method the method to set
    */
   public void setMethod(TemplatedValue<String> method) {
      this.method = method;
   }

   /**
    * @return the message
    */
   public TemplatedValue<String> getMessage() {
      return message;
   }

   /**
    * @param message the message to set
    */
   public void setMessage(TemplatedValue<String> message) {
      this.message = message;
   }

   
   @Override
   public SendActionConfig generateActionConfig(Map<String, Object> variables) {
      SendActionConfig config = new SendActionConfig(ImmutableSet.of(), NotificationCapability.NotifyCustomRequest.NAME);
      config.setAddress(new TemplatedExpression(Address.platformService(NotificationCapability.NAMESPACE).getRepresentation()));
      Address address = getToAddress(to,variables);
      config.setAttributes(ImmutableMap.of(
                  NotificationCapability.NotifyCustomRequest.ATTR_DISPATCHMETHOD, new TemplatedExpression(method.apply(variables)),
                  NotificationCapability.NotifyCustomRequest.ATTR_PERSONID, new TemplatedExpression(String.valueOf(address.getId())),
                  NotificationCapability.NotifyCustomRequest.ATTR_PLACEID, new TemplatedExpression(String.valueOf(variables.get(RuleTemplate.PLACE_ID))),
                  NotificationCapability.NotifyCustomRequest.ATTR_MSG, new TemplatedExpression(String.valueOf(message.apply(variables)))
      ));
      return config;
   }

}

