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
package com.iris.platform.scene.resolver;

import java.util.List;
import java.util.Map;

import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.action.SendAction;
import com.iris.messages.address.Address;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Model;
import com.iris.messages.type.ActionSelector;
import com.iris.model.predicate.Predicates;

public class SecurityAlarmResolver extends BaseResolver {
   private final Predicate<Model> isSecurityAlarmAvailable =
         com.google.common.base.Predicates.and(
               Predicates.isA(SecuritySubsystemCapability.NAMESPACE),
               Predicates.attributeEquals(SubsystemCapability.ATTR_AVAILABLE, true)
         );

   public SecurityAlarmResolver() {
      super("security", "Set Security Alarm", "security");
   }

   /* (non-Javadoc)
    * @see com.iris.platform.scene.resolver.BaseResolver#resolve(com.iris.common.rule.action.ActionContext, com.iris.messages.model.Model)
    */
   @Override
   protected List<ActionSelector> resolve(ActionContext context, Model model) {
      if(!isSecurityAlarmAvailable.apply(model)) {
         return ImmutableList.of();
      }
         
      ActionSelector selector = new ActionSelector();
      selector.setType(ActionSelector.TYPE_LIST);
      selector.setName("alarm-state");
      selector.setValue(
            ImmutableList.of(
                  ImmutableList.of(SecuritySubsystemCapability.ALARMMODE_ON, "Arm On"),
                  ImmutableList.of(SecuritySubsystemCapability.ALARMMODE_PARTIAL, "Arm Partial"),
                  ImmutableList.of(SecuritySubsystemCapability.ALARMMODE_OFF, "Disarm")
            )
      );
      return ImmutableList.of(selector);
   }

   @Override
   public Action generate(ActionContext context, Address target, Map<String, Object> variables) {
      String value = (String) variables.get("alarm-state");
      String type;
      String mode=null;;
      switch(value) {
      case SecuritySubsystemCapability.ALARMMODE_ON:
         type = SecuritySubsystemCapability.ArmBypassedRequest.NAME;
         mode=SecuritySubsystemCapability.ALARMMODE_ON;
         break;
      case SecuritySubsystemCapability.ALARMMODE_PARTIAL:
         type = SecuritySubsystemCapability.ArmBypassedRequest.NAME;
         mode=SecuritySubsystemCapability.ALARMMODE_PARTIAL;
         break;
      case SecuritySubsystemCapability.ALARMMODE_OFF:
         type = SecuritySubsystemCapability.DisarmRequest.NAME;
         break;
         
      default:
         throw new ErrorEventException(Errors.invalidParam("alarm-state"));
      }
      Map<String,Object>args=ImmutableMap.of();
      if(mode!=null){
         args=ImmutableMap.of(SecuritySubsystemCapability.ArmBypassedRequest.ATTR_MODE,mode);
      }
      return new SendAction(type, Functions.constant(target),args);
   }

}

