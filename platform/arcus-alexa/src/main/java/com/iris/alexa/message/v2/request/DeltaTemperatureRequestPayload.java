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

abstract class DeltaTemperatureRequestPayload extends ApplianceRequestPayload {

   private DoubleValue deltaTemperature;

   public DoubleValue getDeltaTemperature() {
      return deltaTemperature;
   }

   public void setDeltaTemperature(DoubleValue deltaTemperature) {
      this.deltaTemperature = deltaTemperature;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [deltaTemperature="
            + deltaTemperature + ", appliance=" + getAppliance() + ']';
   }

}

