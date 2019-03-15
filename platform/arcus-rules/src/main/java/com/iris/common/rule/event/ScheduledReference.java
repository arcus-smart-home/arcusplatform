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
package com.iris.common.rule.event;

import java.util.Date;

import com.iris.common.rule.action.ActionContext;

public class ScheduledReference{
   
   private final String name;
   
   public ScheduledReference(String name) {
      this.name=name;
   }

   public boolean isReferencedEvent(ActionContext context, RuleEvent event) {
      if(event instanceof ScheduledEvent){
         return isReferencedEvent(context, (ScheduledEvent)event); 
      }
      return false;
   }
   
   public boolean isReferencedEvent(ActionContext context, ScheduledEvent event) {
      Date scheduleEvent=context.getVariable(name, Date.class);
      context.logger().debug("scheduledEvent-[{}] event-{}",scheduleEvent, event.getScheduledTimestamp());
      
      //still some jitter because we convert to and from dates and delays
      if(scheduleEvent!=null && (Math.abs(event.getScheduledTimestamp()-scheduleEvent.getTime()) <= 500)){
         context.setVariable(name,null);
         return true;
      }
      return false;
   }

   public void setTimeout(ActionContext context, long delayMs) {
      Date wakeup = new Date(context.getLocalTime().getTimeInMillis()+delayMs);
      setTimeout(context, wakeup);
   }
   
   public void setTimeout(ActionContext context, Date wakeup) {
      ScheduledEventHandle handle = context.wakeUpAt(wakeup);
      context.setVariable(name,wakeup);
   }

   public boolean restore(ActionContext context) {
      Date scheduleEvent=context.getVariable(name, Date.class);
      if(scheduleEvent!=null){
         context.logger().debug("A scheduled event was found and is being restored for {}",scheduleEvent);
         if(scheduleEvent.before(context.getLocalTime().getTime())){
            scheduleEvent=context.getLocalTime().getTime();
            context.setVariable(name,scheduleEvent);
         }
         context.wakeUpAt(scheduleEvent);
         return true;
      }
      return false;
   }
   
   public boolean hasScheduled(ActionContext context) {
      if(context.getVariable(name, Date.class)!=null){
         return true;
      }
      else{
         return false;
      }
   }
   
   public void cancel(ActionContext context){
      context.setVariable(name,null);
   }

}

