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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.iris.common.rule.action.stateful.SequentialActionList;
import com.iris.common.rule.action.stateful.StatefulAction;

public class ActionListConfig implements ActionConfig {
   public final static String TYPE = "actions";

   private List<ActionConfig> actionConfigs= new ArrayList<>();
   
   @Override
   public String getType() {
      return TYPE;
   }
   
   @Override
   public StatefulAction createAction(Map<String, Object> variables) {
      SequentialActionList.Builder bldr = new SequentialActionList.Builder();
      for(ActionConfig config:actionConfigs){
         bldr.addAction(config.createAction(variables));
      }
      return bldr.build();
   }
   
   public void addActionConfig(ActionConfig actionConfig){
      if (actionConfig != null) {
         actionConfigs.add(actionConfig);
      }
   }

   public boolean isEmpty() {
      return actionConfigs.isEmpty();
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((actionConfigs == null) ? 0 : actionConfigs.hashCode());
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
      ActionListConfig other = (ActionListConfig) obj;
      if (actionConfigs == null) {
         if (other.actionConfigs != null) return false;
      }
      else if (!actionConfigs.equals(other.actionConfigs)) return false;
      return true;
   }

   @Override
   public String toString() {
	   return "ActionListConfig [actionConfigs=" + actionConfigs + "]";
   }   
}

