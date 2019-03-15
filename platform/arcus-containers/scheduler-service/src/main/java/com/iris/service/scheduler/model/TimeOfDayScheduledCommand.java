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
package com.iris.service.scheduler.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.iris.common.rule.time.TimeOfDay;
import com.iris.common.time.DayOfWeek;
import com.iris.messages.errors.Errors;
import com.iris.messages.type.TimeOfDayCommand;

public class TimeOfDayScheduledCommand extends ScheduledCommand {
   
   public enum Mode {
      ABSOLUTE,
      SUNRISE,
      SUNSET;
   }

   public static TimeOfDayScheduledCommand fromMap(Map<String, Object> attributes) {
      TimeOfDayCommand raw = new TimeOfDayCommand(attributes);
      
      TimeOfDayScheduledCommand command = new TimeOfDayScheduledCommand();
      command.setId(raw.getId());
      command.setScheduleId(raw.getScheduleId());
      if(!StringUtils.isEmpty(raw.getMessageType())) {
         command.setMessageType(raw.getMessageType());
      }
      if(raw.getAttributes() != null) {
         command.setAttributes(raw.getAttributes());
      }
      if(raw.getDays() != null) {
         command.setDays(toDays(raw.getDays()));
      }
      if(!StringUtils.isEmpty(raw.getMode())) {
         command.setMode(Mode.valueOf(raw.getMode()));
      }
      if(!StringUtils.isEmpty(raw.getTime())) {
         command.setTime(TimeOfDay.fromString(raw.getTime()));
      }
      if(raw.getOffsetMinutes() != null) {
         command.setOffsetMinutes(raw.getOffsetMinutes());
      }
      
      return command;
   }
   
   private static Set<DayOfWeek> toDays(Set<String> days) {
      EnumSet<DayOfWeek> values = EnumSet.noneOf(DayOfWeek.class);
      if(days == null || days.isEmpty()) {
         return values;
      }
      for(String day: days) {
         values.add(DayOfWeek.fromAbbr(day));
      }
      return values;
   }

   private Set<DayOfWeek> days;
   private Mode mode;
   private TimeOfDay time;
   private Integer offsetMinutes; 
   
   public TimeOfDayScheduledCommand() {
      // TODO Auto-generated constructor stub
   }

   /**
    * @return the days
    */
   public Set<DayOfWeek> getDays() {
      return days;
   }

   /**
    * @param days the days to set
    */
   public void setDays(Set<DayOfWeek> days) {
      this.days = days;
   }

   /**
    * @return the mode
    */
   public Mode getMode() {
      return mode;
   }

   /**
    * @param mode the mode to set
    */
   public void setMode(Mode mode) {
      this.mode = mode;
   }
   
   public boolean isAbsolute() {
      return mode == Mode.ABSOLUTE;
   }

   /**
    * @return the time
    */
   public TimeOfDay getTime() {
      return time;
   }

   /**
    * @param time the time to set
    */
   public void setTime(TimeOfDay time) {
      this.time = time;
   }

   /**
    * @return the offsetMinutes
    */
   public Integer getOffsetMinutes() {
      return offsetMinutes;
   }

   /**
    * @param offsetMinutes the offsetMinutes to set
    */
   public void setOffsetMinutes(Integer offsetMinutes) {
      this.offsetMinutes = offsetMinutes;
   }

   public void validate() {
      Errors.assertRequiredParam(this.getScheduleId(), TimeOfDayCommand.ATTR_SCHEDULEID);
      if(this.getMode() == null || this.getMode() == Mode.ABSOLUTE) {
         Errors.assertRequiredParam(this.getTime(), TimeOfDayCommand.ATTR_TIME);
         Errors.assertValidRequest(this.getOffsetMinutes() == null || this.getOffsetMinutes() == 0, "May not specify non-zero offsetMinutes when mode is ABSOLUTE or unspecified.");
      }
      else {
         Errors.assertRequiredParam(this.getOffsetMinutes(), TimeOfDayCommand.ATTR_OFFSETMINUTES);
         Errors.assertValidRequest(this.getTime() == null, "May not specify time when mode is SUNRISE / SUNSET.");
      }
      Errors.assertRequiredParam(this.getDays(), TimeOfDayCommand.ATTR_DAYS);
      Errors.assertRequiredParam(this.getAttributes(), TimeOfDayCommand.ATTR_ATTRIBUTES);
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "TimeOfDayScheduledCommand [days=" + days + ", mode=" + mode
            + ", time=" + time + ", offsetMinutes=" + offsetMinutes + "]";
   }

   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((days == null) ? 0 : days.hashCode());
      result = prime * result + ((mode == null) ? 0 : mode.hashCode());
      result = prime * result
            + ((offsetMinutes == null) ? 0 : offsetMinutes.hashCode());
      result = prime * result + ((time == null) ? 0 : time.hashCode());
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
      TimeOfDayScheduledCommand other = (TimeOfDayScheduledCommand) obj;
      if (days == null) {
         if (other.days != null) return false;
      }
      else if (!days.equals(other.days)) return false;
      if (mode != other.mode) return false;
      if (offsetMinutes == null) {
         if (other.offsetMinutes != null) return false;
      }
      else if (!offsetMinutes.equals(other.offsetMinutes)) return false;
      if (time == null) {
         if (other.time != null) return false;
      }
      else if (!time.equals(other.time)) return false;
      return true;
   }

}

