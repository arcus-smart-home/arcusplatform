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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;
import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.common.rule.action.stateful.StatefulActionWrapper;
import com.iris.common.rule.condition.Condition;
import com.iris.io.java.JavaDeserializer;

/**
 * 
 */
public class LegacyRuleDefinition extends RuleDefinition {
   private List<Expression> expressions;

   /**
    * 
    */
   public LegacyRuleDefinition() {
   }

   @Override
   public Condition createCondition(RuleEnvironment environment) {
      Preconditions.checkArgument(!expressions.isEmpty(), "Must be at least one expression");
      
      Expression e = expressions.get(0);
      return JavaDeserializer.<Condition>getInstance().deserialize(e.getCondition());
   }

   @Override
   public StatefulAction createAction(RuleEnvironment environment) {
      Preconditions.checkArgument(!expressions.isEmpty(), "Must be at least one expression");
      int actionId = expressions.get(0).getActionId();
      ActionDefinition ad = environment.getAction(actionId);
      Preconditions.checkNotNull(ad, "Action " + actionId + " could not be loaded");
      
      Action action = JavaDeserializer.<Action>getInstance().deserialize(ad.getAction());
      return new StatefulActionWrapper(action);
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      return copy();
   }

   public void addExpression(byte[] condition, ActionDefinition action) {
      Preconditions.checkNotNull(action, "action may not be null");
      Preconditions.checkArgument(action.isPersisted(), "action must be persisted");
      getExpressions().add(new Expression(condition, action.getSequenceId()));
   }

   public void addExpression(byte[] condition, int actionSequenceId) {
      Preconditions.checkArgument(actionSequenceId > -1, "action must be persisted");
      getExpressions().add(new Expression(condition, actionSequenceId));
   }

   /**
    * @return the expressions
    */
   public List<Expression> getExpressions() {
      if(expressions == null) {
         expressions = new ArrayList<>();
      }
      return expressions;
   }

   /**
    * @param expressions the expressions to set
    */
   public void setExpressions(List<Expression> expressions) {
      this.expressions = expressions;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.RuleDefinition#copy()
    */
   @Override
   public LegacyRuleDefinition copy() {
      LegacyRuleDefinition copy = (LegacyRuleDefinition) super.copy();
      if(this.expressions != null) {
         copy.expressions = new ArrayList<>(this.expressions.size() + 1);
         for(Expression e: this.expressions) {
            Expression eCopy = new Expression(Arrays.copyOf(e.condition, e.condition.length), e.actionId);
            copy.expressions.add(eCopy);
         }
      }
      return copy;
   }

   @Override
   public String toString() {
      return "RuleDefinition [placeId=" + getPlaceId() + ", sequenceId="
            + getSequenceId() + ", created=" + getCreated() + ", modified=" + getModified()
            + ", name=" + getName() + ", description=" + getDescription()
            + ", expressions=" + expressions + ", suspended=" + isSuspended()
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
      result = prime * result
            + ((expressions == null) ? 0 : expressions.hashCode());
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
      LegacyRuleDefinition other = (LegacyRuleDefinition) obj;
      if (expressions == null) {
         if (other.expressions != null) return false;
      }
      else if (!expressions.equals(other.expressions)) return false;
      return true;
   }

   public static class Expression {
      private byte [] condition;
      // TODO collapse action directly into this field
      private int actionId;

      public Expression() {

      }

      public Expression(byte [] condition, int actionId) {
         this.condition = condition;
         this.actionId = actionId;
      }

      /**
       * @return the actionId
       */
      public int getActionId() {
         return actionId;
      }

      /**
       * @param actionId the actionId to set
       */
      public void setActionId(int actionId) {
         this.actionId = actionId;
      }

      /**
       * @return the condition
       */
      public byte[] getCondition() {
         return condition;
      }

      /**
       * @param condition the condition to set
       */
      public void setCondition(byte[] condition) {
         this.condition = condition;
      }

      /* (non-Javadoc)
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + actionId;
         result = prime * result + Arrays.hashCode(condition);
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
         Expression other = (Expression) obj;
         if (actionId != other.actionId) return false;
         if (!Arrays.equals(condition, other.condition)) return false;
         return true;
      }

   }

}

