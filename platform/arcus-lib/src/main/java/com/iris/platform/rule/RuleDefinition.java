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
package com.iris.platform.rule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.iris.common.rule.Rule;
import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.common.rule.condition.Condition;
import com.iris.messages.capability.RuleCapability;

/**
 * A representation of a {@link Rule} that may be persisted to
 * the database.
 */
public abstract class RuleDefinition extends BaseDefinition<RuleDefinition> {
   private boolean suspended;
   private boolean disabled;

   private String ruleTemplate;
   private Map<String,Object> variables = new HashMap<>();

   @Override
   public String getType() {
      return RuleCapability.NAMESPACE;
   }
   
   public abstract Condition createCondition(RuleEnvironment environment);
   
   public abstract StatefulAction createAction(RuleEnvironment environment);

   public boolean isActive() {
      return !suspended && !disabled;
   }

   /**
    * @return the suspended
    */
   public boolean isSuspended() {
      return suspended;
   }

   /**
    * @param suspended the suspended to set
    */
   public void setSuspended(boolean suspended) {
      this.suspended = suspended;
   }

   /**
    * @return the disabled
    */
   public boolean isDisabled() {
      return disabled;
   }

   /**
    * @param disabled the disabled to set
    */
   public void setDisabled(boolean disabled) {
      this.disabled = disabled;
   }

   public String getRuleTemplate() {
      return ruleTemplate;
   }

   public void setRuleTemplate(String ruleTemplate) {
      this.ruleTemplate = ruleTemplate;
   }

   public Map<String, Object> getVariables() {
      return Collections.unmodifiableMap(variables);
   }

   public void setVariables(Map<String, Object> variables) {
      this.variables = new HashMap<>(variables);
   }
   
   @Override
   public RuleDefinition copy() {
      try {
         RuleDefinition copy = (RuleDefinition) super.clone();
         copy.variables = this.variables != null ? new HashMap<>(this.variables) : null;
         return copy;
      }
      catch(CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (disabled ? 1231 : 1237);
      result = prime * result
            + ((ruleTemplate == null) ? 0 : ruleTemplate.hashCode());
      result = prime * result + (suspended ? 1231 : 1237);
      result = prime * result
            + ((variables == null) ? 0 : variables.hashCode());
      return result;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!super.equals(obj)) return false;
      if (getClass() != obj.getClass()) return false;
      RuleDefinition other = (RuleDefinition) obj;
      if (disabled != other.disabled) return false;
      if (ruleTemplate == null) {
         if (other.ruleTemplate != null) return false;
      }
      else if (!ruleTemplate.equals(other.ruleTemplate)) return false;
      if (suspended != other.suspended) return false;
      if (variables == null) {
         if (other.variables != null) return false;
      }
      else if (!variables.equals(other.variables)) return false;
      return true;
   }

}

