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
package com.iris.platform.rule.catalog.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.platform.rule.catalog.RuleTemplate;
import com.iris.platform.rule.catalog.action.config.ParameterConfig;
import com.iris.platform.rule.catalog.action.config.SendNotificationActionConfig;
import com.iris.platform.rule.catalog.template.TemplatedExpression;
import com.iris.platform.rule.catalog.template.TemplatedValue;

public class NotificationMessageTemplate extends BaseNotificationTemplate {
   private TemplatedValue<String> to;
   private TemplatedValue<String> priority;
   private String key;
   private List<Parameter> parameters = new ArrayList<>();
   private List<ParameterConfig> parameterConfigs = new ArrayList<>();

   public NotificationMessageTemplate(Set<String> contextVariables) {
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
   public TemplatedValue<String> getPriority() {
      return priority;
   }

   /**
    * @param method the method to set
    */
   public void setPriority(TemplatedValue<String> priority) {
      this.priority = priority;
   }
   
   public String getKey() {
      return key;
   }
   
   public void setKey(String key) {
      this.key = key;
   }
   
   public List<Parameter> getParameters() {
      return Collections.unmodifiableList(parameters);
   }

   public void setParameters(List<Parameter> parameters) {
      this.parameters.clear();
      this.parameters.addAll(parameters);
   }
   
   public List<ParameterConfig> getParameterConfigs() {
      return parameterConfigs;
   }

   public void setParameterConfigs(List<ParameterConfig> parameterConfigs) {
      this.parameterConfigs = parameterConfigs;
   }   

   @Override
   public SendNotificationActionConfig generateActionConfig(Map<String, Object> variables) {
      Address address = getToAddress(to,variables);
      
      SendNotificationActionConfig sac = new SendNotificationActionConfig(NotificationCapability.NotifyRequest.NAME);
      sac.setAddress(new TemplatedExpression(Address.platformService(NotificationCapability.NAMESPACE).getRepresentation()));
      sac.setAttributes(ImmutableMap.of(
                  NotificationCapability.NotifyRequest.ATTR_PRIORITY, new TemplatedExpression(priority.apply(variables)),
                  NotificationCapability.NotifyRequest.ATTR_PERSONID, new TemplatedExpression(String.valueOf(address.getId())),
                  NotificationCapability.NotifyRequest.ATTR_PLACEID, new TemplatedExpression(String.valueOf(variables.get(RuleTemplate.PLACE_ID))),
                  NotificationCapability.NotifyRequest.ATTR_MSGKEY, new TemplatedExpression(key)
      ));
      for(ParameterConfig parameter: parameterConfigs){
         sac.getParameterConfigs().add(parameter.interpolate(variables));
      }
      return sac;
   }
}

