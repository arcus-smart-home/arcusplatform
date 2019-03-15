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
package com.iris.platform.rule.catalog.condition.config;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.trigger.ReceivedMessageCondition;
import com.iris.messages.address.Address;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedExpression;

/**
 * 
 */
public class ReceivedMessageConfig implements ConditionConfig {
   
   public static final String TYPE = "received-message";
   
   private TemplatedExpression source;
   private TemplatedExpression messageType;
   // FIXME need attribute typing information on these values
   private Map<String, TemplatedExpression> attributes = new HashMap<>();

   public ReceivedMessageConfig() {
      
   }
   
   public ReceivedMessageConfig(
         TemplatedExpression sourceExpression,
         TemplatedExpression messageTypeExpression
   ) {
      this.source = sourceExpression;
      this.messageType = messageTypeExpression;
   }
   
   /**
    * @return the sourceExpression
    */
   public TemplatedExpression getSourceExpression() {
      return source;
   }

   /**
    * @param sourceExpression the sourceExpression to set
    */
   public void setSourceExpression(TemplatedExpression sourceExpression) {
      this.source = sourceExpression;
   }

   /**
    * @return the messageTypeExpression
    */
   public TemplatedExpression getMessageTypeExpression() {
      return messageType;
   }

   /**
    * @param messageTypeExpression the messageTypeExpression to set
    */
   public void setMessageTypeExpression(TemplatedExpression messageTypeExpression) {
      this.messageType = messageTypeExpression;
   }

   /**
    * @return the attributeExpressions
    */
   public Map<String, TemplatedExpression> getAttributeExpressions() {
      return attributes;
   }

   /**
    * @param attributeExpressions the attributeExpressions to set
    */
   public void setAttributeExpressions(Map<String, TemplatedExpression> attributeExpressions) {
      this.attributes = attributeExpressions == null ? new HashMap<>() : new HashMap<>(attributeExpressions);
   }
   
   public ReceivedMessageConfig addAttributeExpression(String value, TemplatedExpression attributeExpression) {
      this.attributes.put(value, attributeExpression);
      return this;
   }

   @Override
   public String getType() {
      return TYPE;
   }
   
   @Override
   public Condition generate(Map<String, Object> values) {
      Preconditions.checkState(messageType != null, "must specify a message type");
      
      Predicate<Address> fromPred = this.source == null ?
         Predicates.alwaysTrue() :
         Predicates.equalTo( FunctionFactory.toAddress(this.source.toTemplate(), values) );
      Predicate<String> messageTypePred = Predicates.equalTo( FunctionFactory.toString(this.messageType.toTemplate(), values) );
      Predicate<Map<String,Object>> attributesPred;
      if(this.attributes.isEmpty()) {
         attributesPred = Predicates.alwaysTrue();
      }
      else {
         Map<String, Predicate<? super Object>> matches = new HashMap<>();
         for(Map.Entry<String, TemplatedExpression> attributeAndExpression: this.attributes.entrySet()) {
            String attribute = attributeAndExpression.getKey();
            TemplatedExpression expression = attributeAndExpression.getValue();
            matches.put(
                  attribute,
                  // TODO more options than equalTo?
                  Predicates.equalTo( expression.toTemplate().apply(values) )
            );
         }
         attributesPred = FunctionFactory.containsAll(matches);
      }
      
      ReceivedMessageCondition condition = new ReceivedMessageCondition(messageTypePred,fromPred,attributesPred);
      return condition;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "ReceivedMessageConfig [source=" + source + ", messageType="
            + messageType + ", attributes=" + attributes + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((attributes == null) ? 0 : attributes.hashCode());
      result = prime * result
            + ((messageType == null) ? 0 : messageType.hashCode());
      result = prime * result + ((source == null) ? 0 : source.hashCode());
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ReceivedMessageConfig other = (ReceivedMessageConfig) obj;
      if (attributes == null) {
         if (other.attributes != null) return false;
      }
      else if (!attributes.equals(other.attributes)) return false;
      if (messageType == null) {
         if (other.messageType != null) return false;
      }
      else if (!messageType.equals(other.messageType)) return false;
      if (source == null) {
         if (other.source != null) return false;
      }
      else if (!source.equals(other.source)) return false;
      return true;
   }

}

