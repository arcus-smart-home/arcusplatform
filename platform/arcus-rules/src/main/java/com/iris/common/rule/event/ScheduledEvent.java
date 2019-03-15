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
package com.iris.common.rule.event;

import java.util.Date;

/**
 * An event that is scheduled to run at a given time. 
 */
public class ScheduledEvent extends RuleEvent {
   // mostly included for debugging / testing
   private final long scheduledTimestamp;
   
   public ScheduledEvent() {
      this(System.currentTimeMillis());
   }
   
   public ScheduledEvent(long timestamp) {
      this.scheduledTimestamp = timestamp;
   }

   @Override
   public RuleEventType getType() {
      return RuleEventType.SCHEDULED_EVENT;
   }
   
   public long getScheduledTimestamp() {
      return scheduledTimestamp;
   }

   @Override
   public String toString() {
      return "ScheduledEvent [scheduledTimestamp=" + new Date(scheduledTimestamp) + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + (int) (scheduledTimestamp ^ (scheduledTimestamp >>> 32));
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ScheduledEvent other = (ScheduledEvent) obj;
      if (scheduledTimestamp != other.scheduledTimestamp) return false;
      return true;
   }

}

