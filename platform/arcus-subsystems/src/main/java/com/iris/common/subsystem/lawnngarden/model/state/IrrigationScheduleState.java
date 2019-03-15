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
package com.iris.common.subsystem.lawnngarden.model.state;

import static com.iris.messages.type.IrrigationScheduleState.ATTR_CURRENTSTATE;
import static com.iris.messages.type.IrrigationScheduleState.ATTR_CONTROLLER;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.lawnngarden.util.LawnNGardenTypeUtil;
import com.iris.common.subsystem.lawnngarden.util.Mapifiable;
import com.iris.messages.address.Address;
import com.iris.type.handler.TypeHandlerImpl;

public class IrrigationScheduleState implements Mapifiable {

   public enum STATE {
      UPDATING, APPLIED, INITIAL, PAUSED
   };

   private final Address controller;
   private final STATE currentState;

   private IrrigationScheduleState(Address controller, STATE currentState) {
      super();
      this.controller = controller;
      this.currentState = currentState;
   }

   public Address controller() {
      return controller;
   }

   public STATE currentState() {
      return currentState;
   }

   @Override
   public Map<String, Object> mapify() {
      ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object> builder()
            .put(ATTR_CONTROLLER, controller.getRepresentation());
      if (currentState != null) {
         builder.put(ATTR_CURRENTSTATE, currentState.name());
      }

      return builder.build();
   }

   public static class Builder {

      private Address controller;
      private STATE currentState;

      private Builder() {
      }

      public Builder withController(Address controller) {
         this.controller = controller;
         return this;
      }

      public Builder withCurrentState(STATE currentState) {
         this.currentState = currentState;
         return this;
      }

      public IrrigationScheduleState build() {
         Preconditions.checkNotNull(controller);
         Preconditions.checkNotNull(currentState);
         return new IrrigationScheduleState(controller, currentState);
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public static Builder builder(IrrigationScheduleState irrigationState) {
      Builder builder = builder();
      if (irrigationState != null) {
         builder
               .withController(irrigationState.controller())
               .withCurrentState(irrigationState.currentState);
      }
      return builder;
   }

   @SuppressWarnings("serial")
   private static class TypeHandler extends TypeHandlerImpl<IrrigationScheduleState> {

      private TypeHandler() {
         super(IrrigationScheduleState.class, Map.class);
      }

      @SuppressWarnings("unchecked")
      @Override
      protected IrrigationScheduleState convert(Object value) {
         Map<String, Object> map = (Map<String, Object>) value;
         return IrrigationScheduleState.builder()
               .withController(LawnNGardenTypeUtil.address(map.get(ATTR_CONTROLLER)))
               .withCurrentState(LawnNGardenTypeUtil.INSTANCE.coerce(STATE.class, map.get(ATTR_CURRENTSTATE)))
               .build();
      }
   }

   public static TypeHandler typeHandler() {
      return new TypeHandler();
   }
}

