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
package com.iris.common.rule.action.stateful;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.event.RuleEvent;

/**
 * 
 */
public class SequentialActionList extends BaseStatefulAction {
   private static final String KEY_NEXT_INDEX = "nextIndex";

   public static final String NAME = "first";

   private List<Entry> actions;
   private volatile Entry next; // TODO serialize this
   
   private SequentialActionList(List<Entry> actions) {
      this.actions = actions;
   }
   
   private void updateActionState(ActionContext context){
      if(next!=null){
         context.setVariable(KEY_NEXT_INDEX, next.getIndex());
      }
      else{
         context.setVariable(KEY_NEXT_INDEX, null);
      }
   }
   private void restoreActionState(ActionContext context){
      Integer nextIndex = context.getVariable(KEY_NEXT_INDEX,Integer.class);
      if(nextIndex!=null){
         next=actions.get(nextIndex);
      }
   }   
   
   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public void activate(ActionContext context) {
      restoreActionState(context);
      for(SequentialActionList.Entry action:actions){
         try{
            action.activate(context);
         }
         catch(Exception e){
            context.logger().warn("error activating action in list",e);
         }
      }
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
   public boolean isSatisfiable(ActionContext context) {
      for(SequentialActionList.Entry action:actions){
         if(!action.isSatisfiable(context)){
            return false;
         }
      }
      return true;
   }

   @Override
   public ActionState execute(ActionContext context) {
      return runNext(context, 0);
   }
   
   @Override
   public ActionState keepFiring(ActionContext context, RuleEvent event, boolean conditionMatches) {
      if(this.next == null) {
         updateActionState(context);
         return ActionState.IDLE;
      }
      if(this.next.keepFiring(context, event, conditionMatches) == ActionState.FIRING) {
         updateActionState(context);
         return ActionState.FIRING;
      }
      ActionState returnState = runNext(context, this.next.getIndex() + 1);
      updateActionState(context);
      return returnState;
   }

   @Override
   public String toString() {
      return getDescription();
   }

   protected ActionState runNext(ActionContext context, int index) {
      int size = this.actions.size();
      for(int i = index; i < size; i++) {
         this.next = this.actions.get(i);
         if(this.next.execute(context) == ActionState.FIRING) {
            return ActionState.FIRING;
         }
      }
      this.next = null;
      return ActionState.IDLE;
   }   
   
   private static class Entry implements Serializable {
      private int index;
      private StatefulAction action;
      private Function<ActionContext, ActionContext> wrapper;
      
      private Entry(int index, StatefulAction action, Function<ActionContext, ActionContext> wrapper) {
         this.index = index;
         this.action = action;
         this.wrapper = wrapper;
      }

      public int getIndex() {
         return index;
      }
      
      public String getDescription() {
         return action.getDescription();
      }
      
      public boolean isSatisfiable(ActionContext context){
         return action.isSatisfiable(context);
      }
      
      public void activate(ActionContext context) {
         try {
            ActionContext wrapped = wrapper.apply(context);
            action.activate(wrapped);
         }
         catch(Exception e) {
            context.logger().warn("Error activating action [{}]", action.getDescription(), e);
         }
      }

      public ActionState execute(ActionContext context) {
         try {
            ActionContext wrapped = wrapper.apply(context);
            return action.execute(wrapped);
         }
         catch(Exception e) {
            context.logger().warn("Error executing action [{}]", action.getDescription(), e);
            return ActionState.IDLE;
         }
      }
      
      public ActionState keepFiring(ActionContext context, RuleEvent event, boolean conditionMatches) {
         try {
            ActionContext wrapped = wrapper.apply(context);
            return action.keepFiring(wrapped, event, conditionMatches);
         }
         catch(Exception e) {
            context.logger().warn("Error continuing action [{}]", action.getDescription(), e);
            return ActionState.IDLE;
         }
      }
      
   }
   
   public static class Builder {
      private List<Entry> actions = new ArrayList<Entry>();
      
      public Builder addAction(StatefulAction action) {
         return addAction(action,overrideFunction(actions.size()));
      }
      
      public Builder addAction(StatefulAction action, @Nullable Function<ActionContext, ActionContext> wrapper) {
         Preconditions.checkNotNull(action, "action may not be null");
         if(wrapper == null) {
            wrapper = Functions.<ActionContext>identity();
         }
         actions.add(new Entry(actions.size(), action, wrapper));
         return this;
      }
      
      public boolean isEmpty() {
         return actions.isEmpty();
      }
      
      public SequentialActionList build() {
         Preconditions.checkState(!actions.isEmpty(), "Must specify at least one action");
         return new SequentialActionList(actions);
      }
      
      private Function<ActionContext,ActionContext>overrideFunction(final int index){
         return new Function<ActionContext,ActionContext>(){
            @Override
            public ActionContext apply(ActionContext context) {
               return context.override(String.valueOf(index));
            }
         };
      }
   }
}

