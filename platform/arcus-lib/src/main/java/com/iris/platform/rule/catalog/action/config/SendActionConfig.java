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
package com.iris.platform.rule.catalog.action.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.action.stateful.SendAction;
import com.iris.messages.address.Address;
import com.iris.platform.rule.catalog.function.FunctionFactory;
import com.iris.platform.rule.catalog.template.TemplatedExpression;
import com.iris.platform.rule.catalog.template.TemplatedValue;

public class SendActionConfig extends BaseActionConfig {
   public static final String TYPE = "send";
   
   private TemplatedExpression address;
   private Map<String, TemplatedExpression> attributes;
   private String sendActionType;
   private Set<String> availableContextVariables;

   public SendActionConfig(String sendActionType) {
      this.sendActionType = sendActionType;
      this.availableContextVariables = ImmutableSet.of();
      this.attributes=ImmutableMap.of();
   }

   public SendActionConfig(Set<String> availableContextVariables, String sendActionType) {
      this(sendActionType);
      this.availableContextVariables = availableContextVariables;
   }

   public Set<String> getAvailableContextVariables() {
      if(availableContextVariables==null){
         availableContextVariables=new HashSet<>();
      }
      return availableContextVariables;
   }

   public void setAvailableContextVariables(Set<String> availableContextVariables) {
      this.availableContextVariables = availableContextVariables;
   }

   /**
    * @return the address
    */
   public TemplatedExpression getAddress() {
      return address;
   }

   /**
    * @param address the address to set
    */
   public void setAddress(TemplatedExpression address) {
      this.address = address;
   }

   /**
    * @return the attributes
    */
   public Map<String, TemplatedExpression> getAttributes() {
      return attributes;
   }

   /**
    * @param attributes the attributes to set
    */
   public void setAttributes(Map<String, TemplatedExpression> attributes) {
      this.attributes = attributes;
   }

   @Override
   public String getType() {
      return TYPE;
   }
   
   @Override
   public SendAction createAction(Map<String, Object> variables) {
      Preconditions.checkState(address != null, "Must specify an address");
      
      Map<String, Function<ActionContext, Object>> attributes = new HashMap<>(getAttributes().size() + 1);
      for(Map.Entry<String, TemplatedExpression> attribute: this.attributes.entrySet()) {
         attributes.put(attribute.getKey(), FunctionFactory.toActionContextFunction(attribute.getValue().toTemplate()));
      }
      
      SendAction action = new SendAction(
            sendActionType,
            generateToAddress(),
            generatedStaticAttributes(),
            generateDynamicAttributes()
      );
      return action;
   }
   
   protected Function<ActionContext, Address> generateToAddress() {
      Preconditions.checkNotNull(address, "Must specify address");
      
      return FunctionFactory.toActionContextFunction(
            TemplatedValue.transform(
                  FunctionFactory.INSTANCE.getToAddress(), 
                  getAddress().toTemplate()
            )
      );
   }
   
   protected Map<String, Object> generatedStaticAttributes() {
      // not currently used
      return ImmutableMap.of();
   }
   
   protected Map<String, Function<ActionContext, Object>> generateDynamicAttributes() {
      Map<String, TemplatedExpression> expressions = getAttributes();
      Map<String, Function<ActionContext, Object>> attributes = new HashMap<>(getAttributes().size() + 1);
      for(Map.Entry<String, TemplatedExpression> attribute: expressions.entrySet()) {
         attributes.put(attribute.getKey(), FunctionFactory.toActionContextFunction(attribute.getValue().toTemplate()));
      }
      return attributes;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "SendActionConfig [address=" + address + ", attributes="
            + attributes + ", sendActionType=" + sendActionType
            + ", availableContextVariables=" + availableContextVariables + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((address == null) ? 0 : address.hashCode());
      result = prime * result
            + ((attributes == null) ? 0 : attributes.hashCode());
      result = prime
            * result
            + ((availableContextVariables == null) ? 0
                  : availableContextVariables.hashCode());
      result = prime * result
            + ((sendActionType == null) ? 0 : sendActionType.hashCode());
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
      SendActionConfig other = (SendActionConfig) obj;
      if (address == null) {
         if (other.address != null) return false;
      }
      else if (!address.equals(other.address)) return false;
      if (attributes == null) {
         if (other.attributes != null) return false;
      }
      else if (!attributes.equals(other.attributes)) return false;
      if (availableContextVariables == null) {
         if (other.availableContextVariables != null) return false;
      }
      else if (!availableContextVariables
            .equals(other.availableContextVariables)) return false;
      if (sendActionType == null) {
         if (other.sendActionType != null) return false;
      }
      else if (!sendActionType.equals(other.sendActionType)) return false;
      return true;
   }

}

