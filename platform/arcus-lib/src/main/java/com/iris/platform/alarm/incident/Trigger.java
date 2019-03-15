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
package com.iris.platform.alarm.incident;

import java.util.Date;

import com.google.common.base.Preconditions;
import com.iris.common.alarm.AlertType;
import com.iris.messages.address.Address;
import com.iris.messages.model.Copyable;
import com.iris.messages.type.IncidentTrigger;

public class Trigger implements Copyable<Trigger>{

   public enum Event {
      MOTION, CONTACT, GLASS, KEYPAD, SMOKE, CO, RULE, LEAK, BEHAVIOR, VERIFIED_ALARM
   }

   private final Address source;
   private final Date time;
   private final boolean signalled;
   private final AlertType alarm;
   private final Event event;

   private Trigger(
      Address source,
      Date time,
      boolean signalled,
      AlertType alarm,
      Event event
   ) {
      this.source = source;
      this.time = time;
      this.signalled = signalled;
      this.alarm = alarm;
      this.event = event;
   }

   public Address getSource() {
      return source;
   }

   public AlertType getAlarm() {
      return alarm;
   }

   public boolean isSignalled() {
      return signalled;
   }

   public Event getEvent() {
      return event;
   }

   public Date getTime() {
      return time == null ? null : (Date) time.clone();
   }

   @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alarm == null) ? 0 : alarm.hashCode());
		result = prime * result + ((event == null) ? 0 : event.hashCode());
		result = prime * result + (signalled ? 1231 : 1237);
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((time == null) ? 0 : time.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(equalsOtherThanSignalled(obj)) {
			return (signalled == ((Trigger)obj).signalled);
		}else{
			return false;
		}		
	}
	
	/**
	 * Same as equals() except ignoring the signalled field.
	 * @param obj
	 * @return
	 */
	public boolean equalsOtherThanSignalled(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Trigger other = (Trigger) obj;
		if (alarm != other.alarm)
			return false;
		if (event != other.event)
			return false;		
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (time == null) {
			if (other.time != null)
				return false;
		} else if (!time.equals(other.time))
			return false;
		return true;
	}
	
	@Override
	public Trigger copy() {
		try{
			return (Trigger) clone();
		}
		catch (CloneNotSupportedException e){
			throw new RuntimeException(e);
		}
	}

	public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(Trigger t) {
      return new Builder(t);
   }

   public static Builder builder(IncidentTrigger t) {
      return new Builder(t);
   }

   public static class Builder {

      private Address source;
      private Date time;
      private boolean signalled = false;
      private AlertType alarm;
      private Event event;

      private Builder() {

      }

      private Builder(Trigger t) {
         if(t != null) {
            source = t.source;
            time = t.getTime();
            signalled = t.signalled;
            alarm = t.alarm;
            event = t.event;
         }
      }

      private Builder(IncidentTrigger t) {
         if(t != null) {
            source = Address.fromString(t.getSource());
            time = t.getTime() == null ? null : (Date) t.getTime().clone();
            alarm = AlertType.valueOf(t.getAlarm());
            event = Trigger.Event.valueOf(t.getEvent());
         }
      }

      public Builder withSource(Address source) {
         this.source = source;
         return this;
      }

      public Builder withSource(String source) {
         Preconditions.checkNotNull(source, "source cannot be null");
         return withSource(Address.fromString(source));
      }

      public Builder withTime(Date time) {
         this.time = time == null ? null : (Date) time.clone();
         return this;
      }

      public Builder withSignalled(boolean signalled) {
         this.signalled = signalled;
         return this;
      }

      public Builder withAlarm(AlertType alarm) {
         this.alarm = alarm;
         return this;
      }

      public Builder withEvent(Event event) {
         this.event = event;
         return this;
      }

      public Trigger build() {
         Preconditions.checkNotNull(source, "source cannot be null");
         Preconditions.checkNotNull(time, "time cannot be null");
         Preconditions.checkNotNull(alarm, "alarm cannot be null");
         Preconditions.checkNotNull(event, "event cannot be null");
         return new Trigger(source, time, signalled, alarm, event);
      }

   }

	
}

