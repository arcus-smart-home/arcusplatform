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
package com.iris.alexa.message.v2.request;

import com.iris.alexa.message.v2.DoubleValue;

public class SetTargetTemperatureRequest extends ApplianceRequestPayload {

   private DoubleValue targetTemperature;

   public DoubleValue getTargetTemperature() {
      return targetTemperature;
   }

   public void setTargetTemperature(DoubleValue targetTemperature) {
      this.targetTemperature = targetTemperature;
   }

   @Override
   public String toString() {
      return "SetTargetTemperatureRequest [targetTemperature="
            + targetTemperature + ", appliance=" + getAppliance() + ']';
   }

}

