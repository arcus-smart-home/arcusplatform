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
package com.iris.common.subsystem.lawnngarden.model;

import static com.iris.messages.type.ZoneWatering.ATTR_CONTROLLER;
import static com.iris.messages.type.ZoneWatering.ATTR_DURATION;
import static com.iris.messages.type.ZoneWatering.ATTR_STARTTIME;
import static com.iris.messages.type.ZoneWatering.ATTR_TRIGGER;
import static com.iris.messages.type.ZoneWatering.ATTR_ZONE;
import static com.iris.messages.type.ZoneWatering.ATTR_ZONENAME;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenTypeUtil;
import com.iris.common.subsystem.lawnngarden.util.Mapifiable;
import com.iris.messages.address.Address;
import com.iris.type.handler.TypeHandlerImpl;

public class ZoneWatering implements Mapifiable {

   public enum Trigger { MANUAL, SCHEDULED };

   private final Address controller;
   private final String zone;
   private final String zoneName;
   private final Date startTime;
   private final Integer duration;
   private final Trigger trigger;

   private ZoneWatering(Address controller, String zone, String zoneName, Date startTime, Integer duration, Trigger trigger) {
      this.controller = controller;
      this.zone = zone;
      this.zoneName = zoneName;
      this.startTime = startTime;
      this.duration = duration;
      this.trigger = trigger;
   }

   public Address controller() {
      return controller;
   }

   public String zone() {
      return zone;
   }

   public Date startTime() {
      return startTime;
   }

   public Integer duration() {
      return duration;
   }

   public Trigger trigger() {
      return trigger;
   }

   @Override
   public Map<String, Object> mapify() {
      ImmutableMap.Builder<String,Object> builder = ImmutableMap.<String,Object>builder()
            .put(ATTR_CONTROLLER, controller.getRepresentation())
            .put(ATTR_STARTTIME, startTime.getTime())
            .put(ATTR_ZONE, zone)
            .put(ATTR_ZONENAME, this.zoneName);
      if(trigger != null) {
         builder.put(ATTR_TRIGGER, trigger.name());
      }
      if(duration != null) {
         builder.put(ATTR_DURATION, duration);
      }

      return builder.build();
   }

   public static class Builder {

      private Address controller;
      private String zone;
      private Date startTime;
      private Integer duration;
      private Trigger trigger;
      private String zoneName;

      private Builder() {
      }

      public Builder withController(Address controller) {
         this.controller = controller;
         return this;
      }

      public Builder withZone(String zone) {
         this.zone = zone;
         return this;
      }
      
      public Builder withZoneName(String zoneName) {
         this.zoneName = zoneName;
         return this;
      }

      public Builder withStartTime(Date startTime) {
         this.startTime = startTime;
         return this;
      }

      public Builder withDuration(Integer duration) {
         this.duration = duration;
         return this;
      }

      public Builder withTrigger(Trigger trigger) {
         this.trigger = trigger;
         return this;
      }

      public ZoneWatering build() {
         Preconditions.checkNotNull(controller);
         Preconditions.checkNotNull(zone);
         Preconditions.checkNotNull(startTime);
         
         if (StringUtils.isBlank(this.zoneName)) {
            this.zoneName = this.zone;
         }
         
         return new ZoneWatering(controller, zone, this.zoneName, (Date) startTime.clone(), duration, trigger);
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(ZoneWatering watering) {
      Builder builder = builder();
      if(watering != null) {
         builder
            .withController(watering.controller())
            .withDuration(watering.duration())
            .withStartTime(watering.startTime())
            .withTrigger(watering.trigger())
            .withZone(watering.zone());
      }
      return builder;
   }

   @SuppressWarnings("serial")
   private static class TypeHandler extends TypeHandlerImpl<ZoneWatering> {

      private TypeHandler() {
         super(ZoneWatering.class, Map.class);
      }

      @SuppressWarnings("unchecked")
      @Override
      protected ZoneWatering convert(Object value) {
         Map<String,Object> map = (Map<String,Object>) value;
         return ZoneWatering.builder()
               .withController(LawnNGardenTypeUtil.address(map.get(ATTR_CONTROLLER)))
               .withDuration(LawnNGardenTypeUtil.integer(map.get(ATTR_DURATION)))
               .withStartTime(LawnNGardenTypeUtil.date(map.get(ATTR_STARTTIME)))
               .withTrigger(LawnNGardenTypeUtil.INSTANCE.coerce(Trigger.class, map.get(ATTR_TRIGGER)))
               .withZone(LawnNGardenTypeUtil.string(map.get(ATTR_ZONE)))
               .withZoneName(LawnNGardenTypeUtil.string(map.get(ATTR_ZONENAME)))
               .build();
      }
   }

   public static TypeHandler typeHandler() {
      return new TypeHandler();
   }

}

