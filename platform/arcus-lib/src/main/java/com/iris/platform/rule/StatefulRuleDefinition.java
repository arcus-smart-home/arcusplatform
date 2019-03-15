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
package com.iris.platform.rule;

import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.common.rule.condition.Condition;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.condition.config.ConditionConfig;


public class StatefulRuleDefinition extends RuleDefinition {
   private ConditionConfig condition;
   private ActionConfig action;
   
   public StatefulRuleDefinition() {
   }

   @Override
   public Condition createCondition(RuleEnvironment environment) {
      return condition.generate(getVariables());
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.RuleDefinition#createAction(com.iris.platform.rule.RuleEnvironment)
    */
   @Override
   public StatefulAction createAction(RuleEnvironment environment) {
      return action.createAction(getVariables());
   }

   /**
    * @return the condition
    */
   public ConditionConfig getCondition() {
      return condition;
   }

   /**
    * @param condition the condition to set
    */
   public void setCondition(ConditionConfig condition) {
      this.condition = condition;
   }

   /**
    * @return the action
    */
   public ActionConfig getAction() {
      return action;
   }

   /**
    * @param action the action to set
    */
   public void setAction(ActionConfig action) {
      this.action = action;
   }

   @Override
   public StatefulRuleDefinition copy() {
      return (StatefulRuleDefinition) super.copy();
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      return copy();
   }

   @Override
   public String toString() {
      return "RuleDefinition [placeId=" + getPlaceId() + ", sequenceId="
            + getSequenceId() + ", created=" + getCreated() + ", modified=" + getModified()
            + ", name=" + getName() + ", description=" + getDescription()
            + ", action=" + action + ", suspended=" + isSuspended()
            + ", disabled=" + isDisabled() + ", ruleTemplate=" + getRuleTemplate()
            + ", variables=" + getVariables() + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((action == null) ? 0 : action.hashCode());
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
      StatefulRuleDefinition other = (StatefulRuleDefinition) obj;
      if (action == null) {
         if (other.action != null) return false;
      }
      else if (!action.equals(other.action)) return false;
      return true;
   }

}

