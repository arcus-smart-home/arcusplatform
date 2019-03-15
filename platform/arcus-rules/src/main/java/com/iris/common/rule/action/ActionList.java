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
package com.iris.common.rule.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;

/**
 * 
 */
@SuppressWarnings("serial")
public class ActionList implements Action {
   public static final String NAME = "first";

   private List<Entry> actions;
   
   private ActionList(List<Entry> actions) {
      this.actions = actions;
   }
   
   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public String getDescription() {
      StringBuilder sb = 
            new StringBuilder(NAME)
               .append(" (")
               .append(actions.get(0).getDescription())
               .append(")");
      for(int i=1; i<actions.size(); i++) {
         sb
            .append(" then (")
            .append(actions.get(i).getDescription())
            .append(")");
      }
      return sb.toString();
   }

   @Override
   public void execute(ActionContext context) {
      for(Entry entry: actions) {
         entry.execute(context);
      }
   }
   
   @Override
   public String toString() {
      return getDescription();
   }

   private static class Entry implements Serializable {
      private Action action;
      private Function<ActionContext, ActionContext> wrapper;
      
      private Entry(Action action, Function<ActionContext, ActionContext> wrapper) {
         this.action = action;
         this.wrapper = wrapper;
      }

      public String getDescription() {
         return action.getDescription();
      }
      
      public void execute(ActionContext context) {
         try {
            ActionContext wrapped = wrapper.apply(context);
            action.execute(wrapped);
         }
         catch(Exception e) {
            context.logger().warn("Error executing action [{}]", action.getDescription(), e);
         }
      }
      
   }
   
   public static class Builder {
      private List<Entry> actions = new ArrayList<Entry>();
      
      public Builder addAction(Action action) {
         return addAction(action, Functions.<ActionContext>identity());
      }
      
      public Builder addAction(Action action, final Map<String, Object> variables) {
         return addAction(action, new Function<ActionContext, ActionContext>() {
            @Override
            public ActionContext apply(ActionContext input) {
               return input.override(variables);
            }
         });
      }
      
      public Builder addAction(Action action, @Nullable Function<ActionContext, ActionContext> wrapper) {
         Preconditions.checkNotNull(action, "action may not be null");
         if(wrapper == null) {
            wrapper = Functions.<ActionContext>identity();
         }
         actions.add(new Entry(action, wrapper));
         return this;
      }
      
      public boolean isEmpty() {
         return actions.isEmpty();
      }
      
      public ActionList build() {
         Preconditions.checkState(!actions.isEmpty(), "Must specify at least one action");
         return new ActionList(actions);
      }
   }
}

