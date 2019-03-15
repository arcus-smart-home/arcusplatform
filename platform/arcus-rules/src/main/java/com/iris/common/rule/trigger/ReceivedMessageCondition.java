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
package com.iris.common.rule.trigger;

import java.util.Map;

import com.google.common.base.Predicate;
import com.iris.common.rule.condition.ConditionContext;
import com.iris.common.rule.event.MessageReceivedEvent;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;

@SuppressWarnings("serial")
public class ReceivedMessageCondition extends SimpleTrigger {

   private Predicate<Address> from;
   private Predicate<String> messageType;
   private Predicate<Map<String, Object>> attributes;

   @Override
   public boolean handlesEventsOfType(RuleEventType type) {
      return RuleEventType.MESSAGE_RECEIVED.equals(type);
   }

   public ReceivedMessageCondition(Predicate<String> messageType, Predicate<Address> from, Predicate<Map<String, Object>> attributes) {
      this.from = from;
      this.messageType = messageType;
      this.attributes = attributes;
   }

   @Override
   public boolean isSatisfiable(ConditionContext context) {
      //If the from address still exists, we are satisfiable. 
      for(Model model:context.getModels()){
         if(from.apply(model.getAddress())){
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean shouldTrigger(ConditionContext context, RuleEvent event) {
      if (!(event instanceof MessageReceivedEvent)) {
         return false;
      }
      MessageReceivedEvent mre = (MessageReceivedEvent) event;
      PlatformMessage message = mre.getMessage();
      context.logger().trace("MessageType: {} Source: {} Attributes: {}", message.getMessageType(), message.getSource(), message.getValue().getAttributes());
      boolean typeOK = messageType.apply(message.getMessageType());
      if(!typeOK){
         return false;
      }
      boolean fromOK = from.apply(mre.getMessage().getSource());
      boolean attributesOK = attributes.apply(mre.getMessage().getValue().getAttributes());
      boolean triggerRule = typeOK && fromOK & attributesOK;
      context.logger().debug("Rule triggered: {} source matches:{} attributes match:{}",triggerRule,fromOK,attributesOK);
      return triggerRule;
   }
}

