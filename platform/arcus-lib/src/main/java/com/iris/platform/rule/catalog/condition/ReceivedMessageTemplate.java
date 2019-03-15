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
package com.iris.platform.rule.catalog.condition;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.trigger.ReceivedMessageCondition;
import com.iris.messages.address.Address;
import com.iris.platform.rule.catalog.template.TemplatedValue;

/**
 * 
 */
public class ReceivedMessageTemplate extends TriggerTemplate {
   
   private TemplatedValue<Predicate<Address>> source;
   private TemplatedValue<Predicate<String>> messageType;
   private TemplatedValue<Predicate<Map<String, Object>>>attributes;

   public ReceivedMessageTemplate(TemplatedValue<Predicate<Address>> source, TemplatedValue<Predicate<String>> messageType, TemplatedValue<Predicate<Map<String, Object>>> attributes) {
      this.source = source;
      this.messageType = messageType;
      this.attributes = attributes;
   }
   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkState(source != null, "must specify a source");
      Preconditions.checkState(messageType != null, "must specify a message type");
      
      Predicate<String> messageTypePred = messageType.apply(values);
      Predicate<Address> fromPred = source.apply(values);
      Predicate<Map<String,Object>>attributesPred = attributes.apply(values);
      
      ReceivedMessageCondition condition = new ReceivedMessageCondition(messageTypePred,fromPred,attributesPred);
      return condition;
   }

   @Override
   public String toString() {
	   return "ReceivedMessageTemplate [source=" + source + ", messageType="
			   + messageType + ", attributes=" + attributes + "]";
   }
}

