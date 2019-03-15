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
package com.iris.common.subsystem.lawnngarden.model.schedules;

import static com.iris.messages.type.IrrigationTransitionEvent.ATTR_CONTROLLER;
import static com.iris.messages.type.IrrigationTransitionEvent.ATTR_DURATION;
import static com.iris.messages.type.IrrigationTransitionEvent.ATTR_RETRYCOUNT;
import static com.iris.messages.type.IrrigationTransitionEvent.ATTR_STARTTIME;
import static com.iris.messages.type.IrrigationTransitionEvent.ATTR_STATUS;
import static com.iris.messages.type.IrrigationTransitionEvent.ATTR_TIMEOFDAY;
import static com.iris.messages.type.IrrigationTransitionEvent.ATTR_ZONE;

import java.util.Date;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenTypeUtil;
import com.iris.common.subsystem.lawnngarden.util.Mapifiable;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.address.Address;
import com.iris.type.handler.TypeHandlerImpl;

public class Transition implements Mapifiable {

   public enum TransitionStatus { APPLIED, PENDING, RETRYING, FAILED }

   private final Address controller;
   private final Date startTime;
   private final TimeOfDay timeOfDay;
   private final String zone;
   private final TransitionStatus status;
   private final int retryCount;
   private final int duration;

   private Transition(Address controller, Date startTime, TimeOfDay timeOfDay, String zone, TransitionStatus status, int retryCount, int duration) {
      this.controller = controller;
      this.startTime = startTime;
      this.timeOfDay = timeOfDay;
      this.zone = zone;
      this.status = status;
      this.retryCount = retryCount;
      this.duration = duration;
   }

   public Address controller() {
      return controller;
   }

   public Date startTime() {
      return startTime;
   }

   public TimeOfDay timeOfDay() {
      return timeOfDay;
   }

   public String zone() {
      return zone;
   }

   public TransitionStatus status() {
      return status;
   }

   public int retryCount() {
      return retryCount;
   }

   public int duration() {
      return duration;
   }

   @Override
   public Map<String, Object> mapify() {
      ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String,Object>builder()
            .put(ATTR_DURATION, duration)
            .put(ATTR_RETRYCOUNT, retryCount)
            .put(ATTR_STATUS, status.name())
            .put(ATTR_TIMEOFDAY, timeOfDay.toString())
            .put(ATTR_ZONE, zone);

      if(controller != null) {
         builder.put(ATTR_CONTROLLER, controller.getRepresentation());
      }

      if(startTime != null) {
         builder.put(ATTR_STARTTIME, startTime.getTime());
      }

      return builder.build();
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((controller == null) ? 0 : controller.hashCode());
      result = prime * result + duration;
      result = prime * result + retryCount;
      result = prime * result
            + ((startTime == null) ? 0 : startTime.hashCode());
      result = prime * result + ((status == null) ? 0 : status.hashCode());
      result = prime * result
            + ((timeOfDay == null) ? 0 : timeOfDay.hashCode());
      result = prime * result + ((zone == null) ? 0 : zone.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      Transition other = (Transition) obj;
      if (controller == null) {
         if (other.controller != null)
            return false;
      } else if (!controller.equals(other.controller))
         return false;
      if (duration != other.duration)
         return false;
      if (retryCount != other.retryCount)
         return false;
      if (startTime == null) {
         if (other.startTime != null)
            return false;
      } else if (!startTime.equals(other.startTime))
         return false;
      if (status != other.status)
         return false;
      if (timeOfDay == null) {
         if (other.timeOfDay != null)
            return false;
      } else if (!timeOfDay.equals(other.timeOfDay))
         return false;
      if (zone == null) {
         if (other.zone != null)
            return false;
      } else if (!zone.equals(other.zone))
         return false;
      return true;
   }

   public static class Builder {
      private Address controller;
      private Date startTime;
      private TimeOfDay timeOfDay;
      private String zone;
      private TransitionStatus status;
      private int retryCount = 0;
      private int duration;

      private Builder() {
      }

      public Builder withController(Address controller) {
         this.controller = controller;
         return this;
      }

      public Builder withStartTime(Date startTime) {
         this.startTime = startTime;
         return this;
      }

      public Builder withTimeOfDay(TimeOfDay timeOfDay) {
         this.timeOfDay = timeOfDay;
         return this;
      }

      public Builder withZone(String zone) {
         this.zone = zone;
         return this;
      }

      public Builder withStatus(TransitionStatus status) {
         this.status = status;
         return this;
      }

      public Builder withRetryCount(int retryCount) {
         this.retryCount = retryCount;
         return this;
      }

      public Builder withDuration(int duration) {
         this.duration = duration;
         return this;
      }

      public Transition build() {
         Preconditions.checkNotNull(timeOfDay);
         Preconditions.checkNotNull(zone);
         Preconditions.checkNotNull(status);
         Preconditions.checkArgument(duration >= 1);

         int retries = retryCount;
         if(status == TransitionStatus.PENDING || status == TransitionStatus.APPLIED) {
            retries = 0;
         }

         Date tmpStartTime = startTime == null ? null : (Date) startTime.clone();
         return new Transition(controller, tmpStartTime, timeOfDay, zone, status, retries, duration);
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(Transition event) {
      Builder builder = builder();
      if(builder != null) {
         builder
            .withController(event.controller())
            .withDuration(event.duration())
            .withRetryCount(event.retryCount())
            .withStartTime(event.startTime())
            .withStatus(event.status())
            .withTimeOfDay(event.timeOfDay())
            .withZone(event.zone());
      }
      return builder;
   }

   @SuppressWarnings("serial")
   private static class TypeHandler extends TypeHandlerImpl<Transition> {

      private TypeHandler() {
         super(Transition.class, Map.class);
      }

      @SuppressWarnings("unchecked")
      @Override
      protected Transition convert(Object value) {
         Map<String,Object> map = (Map<String, Object>) value;
         return Transition.builder()
               .withController(LawnNGardenTypeUtil.address(map.get(ATTR_CONTROLLER)))
               .withDuration(LawnNGardenTypeUtil.integer(map.get(ATTR_DURATION)))
               .withRetryCount(LawnNGardenTypeUtil.integer(map.get(ATTR_RETRYCOUNT)))
               .withStartTime(LawnNGardenTypeUtil.date(map.get(ATTR_STARTTIME)))
               .withStatus(LawnNGardenTypeUtil.INSTANCE.coerce(TransitionStatus.class, map.get(ATTR_STATUS)))
               .withTimeOfDay(LawnNGardenTypeUtil.timeOfDay(map.get(ATTR_TIMEOFDAY)))
               .withZone(LawnNGardenTypeUtil.string(map.get(ATTR_ZONE)))
               .build();
      }
   }

   public static TypeHandler typeHandler() {
      return new TypeHandler();
   }

}

