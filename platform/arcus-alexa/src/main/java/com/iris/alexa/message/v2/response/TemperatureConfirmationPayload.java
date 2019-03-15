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
package com.iris.alexa.message.v2.response;

import com.iris.alexa.message.v2.DoubleValue;
import com.iris.alexa.message.v2.StringValue;

public abstract class TemperatureConfirmationPayload implements ResponsePayload {

   public static class PreviousState {
      private DoubleValue targetTemperature;
      private StringValue mode;

      public DoubleValue getTargetTemperature() {
         return targetTemperature;
      }

      public void setTargetTemperature(DoubleValue targetTemperature) {
         this.targetTemperature = targetTemperature;
      }

      public StringValue getMode() {
         return mode;
      }

      public void setMode(StringValue mode) {
         this.mode = mode;
      }

      @Override
      public String toString() {
         return "PreviousState [targetTemperature=" + targetTemperature
               + ", mode=" + mode + ']';
      }

   }

   private DoubleValue targetTemperature;
   private StringValue temperatureMode;
   private PreviousState previousState;

   public DoubleValue getTargetTemperature() {
      return targetTemperature;
   }

   public void setTargetTemperature(DoubleValue targetTemperature) {
      this.targetTemperature = targetTemperature;
   }

   public StringValue getTemperatureMode() {
      return temperatureMode;
   }

   public void setTemperatureMode(StringValue temperatureMode) {
      this.temperatureMode = temperatureMode;
   }

   public PreviousState getPreviousState() {
      return previousState;
   }

   public void setPreviousState(PreviousState previousState) {
      this.previousState = previousState;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [targetTemperature="
            + targetTemperature + ", temperatureMode=" + temperatureMode
            + ", previousState=" + previousState + ']';
   }



}

