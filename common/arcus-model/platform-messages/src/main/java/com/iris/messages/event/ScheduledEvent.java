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
package com.iris.messages.event;

import java.util.Date;

import com.iris.messages.address.Address;

/**
 * An event that is scheduled to run at a given time. 
 */
public class ScheduledEvent extends AddressableEvent {
   private final Address source;
   // mostly included for debugging / testing
   private final long scheduledTimestamp;
   
   public ScheduledEvent(Address source) {
      this(source, System.currentTimeMillis());
   }
   
   public ScheduledEvent(Address source, long timestamp) {
      this.source = source;
      this.scheduledTimestamp = timestamp;
   }

   @Override
   public Address getAddress() {
      return source;
   }
   
   public long getScheduledTimestamp() {
      return scheduledTimestamp;
   }

   @Override
   public String toString() {
      return "ScheduledEvent [source=" + source + ", scheduledTimestamp=" + new Date(scheduledTimestamp) + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + (int) (scheduledTimestamp ^ (scheduledTimestamp >>> 32));
      result = prime * result + ((source == null) ? 0 : source.hashCode());
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
      ScheduledEvent other = (ScheduledEvent) obj;
      if (scheduledTimestamp != other.scheduledTimestamp) return false;
      if (source == null) {
         if (other.source != null) return false;
      }
      else if (!source.equals(other.source)) return false;
      return true;
   }

}

