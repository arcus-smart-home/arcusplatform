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

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.common.rule.action.stateful.ForEachModelAction;
import com.iris.common.rule.action.stateful.SequentialActionList;
import com.iris.common.rule.action.stateful.StatefulAction;
import com.iris.messages.model.Model;
import com.iris.model.query.expression.ExpressionCompiler;

public class ForEachModelActionConfig implements ActionConfig {
   public final static String TYPE = "for-each-model";

   private List<ActionConfig> actions;
   private String modelQuery;
   private String targetVariable;

   public String getTargetVariable() {
      return targetVariable;
   }

   public void setTargetVariable(String targetVariable) {
      this.targetVariable = targetVariable;
   }

   public List<ActionConfig> getActions() {
      return actions;
   }

   public void setActions(List<ActionConfig> actions) {
      this.actions = actions;
   }

   public String getModelQuery() {
      return modelQuery;
   }

   public void setModelQuery(String modelQuery) {
      this.modelQuery = modelQuery;
   }

   @Override
   public String getType() {
      return TYPE;
   }

   @Override
   public StatefulAction createAction(Map<String, Object> variables) {
      SequentialActionList.Builder alBuilder = new SequentialActionList.Builder();
      for (ActionConfig t : actions){
         alBuilder.addAction(t.createAction(variables));
      }
      Predicate<Model> queryPredicate = ExpressionCompiler.compile(modelQuery);
      ForEachModelAction action = new ForEachModelAction(
            alBuilder.build(), 
            Predicates.<Model>alwaysTrue(), // FIXME expose satisfiability to the XML layer by splitting up query into selector and condition
            queryPredicate, 
            targetVariable
      );
      return action;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((actions == null) ? 0 : actions.hashCode());
      result = prime * result
            + ((modelQuery == null) ? 0 : modelQuery.hashCode());
      result = prime * result
            + ((targetVariable == null) ? 0 : targetVariable.hashCode());
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
      ForEachModelActionConfig other = (ForEachModelActionConfig) obj;
      if (actions == null) {
         if (other.actions != null) return false;
      }
      else if (!actions.equals(other.actions)) return false;
      if (modelQuery == null) {
         if (other.modelQuery != null) return false;
      }
      else if (!modelQuery.equals(other.modelQuery)) return false;
      if (targetVariable == null) {
         if (other.targetVariable != null) return false;
      }
      else if (!targetVariable.equals(other.targetVariable)) return false;
      return true;
   }

   @Override
   public String toString() {
	   return "ForEachModelActionConfig [actions=" + actions + ", modelQuery="
			   + modelQuery + ", targetVariable=" + targetVariable + "]";
   }   
}

