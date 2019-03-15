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

import static com.iris.messages.type.IrrigationScheduleStatus.ATTR_CONTROLLER;
import static com.iris.messages.type.IrrigationScheduleStatus.ATTR_ENABLED;
import static com.iris.messages.type.IrrigationScheduleStatus.ATTR_MODE;
import static com.iris.messages.type.IrrigationScheduleStatus.ATTR_NEXTEVENT;
import static com.iris.messages.type.IrrigationScheduleStatus.ATTR_SKIPPEDUNTIL;

import java.util.Date;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenTypeUtil;
import com.iris.common.subsystem.lawnngarden.util.Mapifiable;
import com.iris.messages.address.Address;
import com.iris.type.handler.TypeHandlerImpl;

public class ScheduleStatus implements Mapifiable {

   private final ScheduleMode mode;
   private final Address controller;
   private final boolean enabled;
   private final Date skippedUntil;
   private final Transition nextTransition;

   private ScheduleStatus(ScheduleMode mode, Address controller, boolean enabled, Date skippedUntil, Transition nextTransition) {
      this.mode = mode;
      this.controller = controller;
      this.enabled = enabled;
      this.skippedUntil = skippedUntil;
      this.nextTransition = nextTransition;
   }

   public ScheduleMode mode() {
      return mode;
   }

   public Address controller() {
      return controller;
   }

   public boolean enabled() {
      return enabled;
   }

   public Date skippedUntil() {
      return skippedUntil;
   }

   public Transition nextTransition() {
      return nextTransition;
   }

   @Override
   public Map<String, Object> mapify() {
      ImmutableMap.Builder<String,Object> builder = ImmutableMap.<String,Object>builder()
            .put(ATTR_CONTROLLER, controller.getRepresentation())
            .put(ATTR_ENABLED, enabled)
            .put(ATTR_MODE, mode.name());

      if(nextTransition != null) {
         builder.put(ATTR_NEXTEVENT, nextTransition.mapify());
      }

      if(skippedUntil != null) {
         builder.put(ATTR_SKIPPEDUNTIL, skippedUntil.getTime());
      }

      return builder.build();
   }

   public static class Builder {

      private ScheduleMode mode;
      private Address controller;
      private boolean enabled = false;
      private Date skippedUntil;
      private Transition nextTransition;

      private Builder() {
      }

      public Builder withMode(ScheduleMode mode) {
         this.mode = mode;
         return this;
      }

      public Builder withController(Address controller) {
         this.controller = controller;
         return this;
      }

      public Builder withEnabled(boolean enabled) {
         this.enabled = enabled;
         return this;
      }

      public Builder withSkippedUntil(Date skippedUntil) {
         this.skippedUntil = skippedUntil;
         return this;
      }

      public Builder withNextTransition(Transition nextTransition) {
         this.nextTransition = nextTransition;
         return this;
      }

      public ScheduleStatus build() {
         Preconditions.checkNotNull(mode);
         Preconditions.checkNotNull(controller);
         Date skip = skippedUntil == null ? null : (Date) skippedUntil.clone();
         return new ScheduleStatus(mode, controller, enabled, skip, nextTransition);
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(ScheduleStatus status) {
      Builder builder = builder();
      if(status != null) {
         builder
            .withController(status.controller())
            .withEnabled(status.enabled())
            .withMode(status.mode())
            .withSkippedUntil(status.skippedUntil())
            .withNextTransition(status.nextTransition());
      }
      return builder;
   }

   @SuppressWarnings("serial")
   private static class TypeHandler extends TypeHandlerImpl<ScheduleStatus> {

      private TypeHandler() {
         super(ScheduleStatus.class, Map.class);
      }

      @SuppressWarnings("unchecked")
      @Override
      protected ScheduleStatus convert(Object value) {
         Map<String,Object> map = (Map<String, Object>) value;
         return ScheduleStatus.builder()
               .withController(LawnNGardenTypeUtil.address(map.get(ATTR_CONTROLLER)))
               .withEnabled(LawnNGardenTypeUtil.bool(map.get(ATTR_ENABLED)))
               .withMode(LawnNGardenTypeUtil.INSTANCE.coerce(ScheduleMode.class, map.get(ATTR_MODE)))
               .withNextTransition(LawnNGardenTypeUtil.transition(map.get(ATTR_NEXTEVENT)))
               .withSkippedUntil(LawnNGardenTypeUtil.date(map.get(ATTR_SKIPPEDUNTIL)))
               .build();
      }
   }

   public static TypeHandler typeHandler() {
      return new TypeHandler();
   }
}

