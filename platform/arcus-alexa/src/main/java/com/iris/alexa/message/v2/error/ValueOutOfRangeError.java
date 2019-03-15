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
package com.iris.alexa.message.v2.error;

public class ValueOutOfRangeError implements ErrorPayload {

   private double minimumValue;
   private double maximumValue;

   public ValueOutOfRangeError() {
   }

   public ValueOutOfRangeError(double minimumValue, double maximumValue) {
      this.minimumValue = minimumValue;
      this.maximumValue = maximumValue;

   }

   public double getMinimumValue() {
      return minimumValue;
   }

   public void setMinimumValue(double minimumValue) {
      this.minimumValue = minimumValue;
   }

   public double getMaximumValue() {
      return maximumValue;
   }

   public void setMaximumValue(double maximumValue) {
      this.maximumValue = maximumValue;
   }

   @Override
   public String toString() {
      return "ValueOutOfRangeError [minimumValue=" + minimumValue
            + ", maximumValue=" + maximumValue + ']';
   }

}

