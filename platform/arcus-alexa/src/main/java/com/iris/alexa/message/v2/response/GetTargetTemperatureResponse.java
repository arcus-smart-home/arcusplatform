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

public class GetTargetTemperatureResponse implements ResponsePayload {

   private DoubleValue targetTemperature;
   private DoubleValue coolingTargetTemperature;
   private DoubleValue heatingTargetTemperature;
   private StringValue temperatureMode;
   private String friendlyName;
   private String applianceResponseTimestamp;

   @Override
   public String getNamespace() {
      return QUERY_NAMESPACE;
   }

   public DoubleValue getTargetTemperature() {
      return targetTemperature;
   }

   public void setTargetTemperature(DoubleValue targetTemperature) {
      this.targetTemperature = targetTemperature;
   }

   public DoubleValue getCoolingTargetTemperature() {
      return coolingTargetTemperature;
   }

   public void setCoolingTargetTemperature(DoubleValue coolingTargetTemperature) {
      this.coolingTargetTemperature = coolingTargetTemperature;
   }

   public DoubleValue getHeatingTargetTemperature() {
      return heatingTargetTemperature;
   }

   public void setHeatingTargetTemperature(DoubleValue heatingTargetTemperature) {
      this.heatingTargetTemperature = heatingTargetTemperature;
   }

   public StringValue getTemperatureMode() {
      return temperatureMode;
   }

   public void setTemperatureMode(StringValue temperatureMode) {
      this.temperatureMode = temperatureMode;
   }

   public String getFriendlyName() {
      return friendlyName;
   }

   public void setFriendlyName(String friendlyName) {
      this.friendlyName = friendlyName;
   }

   public String getApplianceResponseTimestamp() {
      return applianceResponseTimestamp;
   }

   public void setApplianceResponseTimestamp(String applianceResponseTimestamp) {
      this.applianceResponseTimestamp = applianceResponseTimestamp;
   }
}

