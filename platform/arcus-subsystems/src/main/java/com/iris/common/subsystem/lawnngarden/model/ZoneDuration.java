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

import static com.iris.messages.type.ZoneDuration.ATTR_DURATION;
import static com.iris.messages.type.ZoneDuration.ATTR_ZONE;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenTypeUtil;
import com.iris.common.subsystem.lawnngarden.util.Mapifiable;
import com.iris.type.handler.TypeHandlerImpl;

public class ZoneDuration implements Mapifiable {

   private final String zone;
   private final int duration;

   private ZoneDuration(String zone, int duration) {
      this.zone = zone;
      this.duration = duration;
   }

   public String zone() {
      return zone;
   }

   public int duration() {
      return duration;
   }

   @Override
   public Map<String, Object> mapify() {
      return ImmutableMap.<String,Object>of(
            ATTR_DURATION, duration,
            ATTR_ZONE, zone
      );
   }

   public static class Builder {
      private String zone;
      private int duration;

      private Builder() {
      }

      public Builder withZone(String zone) {
         this.zone = zone;
         return this;
      }

      public Builder withDuration(int duration) {
         this.duration = duration;
         return this;
      }

      public ZoneDuration build() {
         Preconditions.checkNotNull(zone);
         Preconditions.checkArgument(duration >= 1);
         return new ZoneDuration(zone, duration);
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(ZoneDuration zoneDuration) {
      Builder builder = builder();
      if(zoneDuration != null) {
         builder.withZone(zoneDuration.zone())
            .withDuration(zoneDuration.duration());
      }
      return builder;
   }

   @SuppressWarnings("serial")
   private static class TypeHandler extends TypeHandlerImpl<ZoneDuration> {

      private TypeHandler() {
         super(ZoneDuration.class, Map.class);
      }

      @SuppressWarnings("unchecked")
      @Override
      protected ZoneDuration convert(Object value) {
         Map<String,Object> map = (Map<String, Object>) value;
         return ZoneDuration.builder()
               .withDuration(LawnNGardenTypeUtil.integer(map.get(ATTR_DURATION)))
               .withZone(LawnNGardenTypeUtil.string(map.get(ATTR_ZONE)))
               .build();
      }
   }

   public static TypeHandler typeHandler() {
      return new TypeHandler();
   }
}

