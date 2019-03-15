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
package com.iris.common.subsystem.climate;

import com.iris.messages.ErrorEvent;
import com.iris.messages.errors.Errors;

public class ClimateErrors {
   public static final String CODE_NOT_THERMOSTAT = "climate.not_thermostat";
   public static final String CODE_NOT_TEMPERATURE = "climate.not_temperature_device";
   public static final String CODE_NOT_HUMIDITY = "climate.not_humidity_device";
   
   public static ErrorEvent notThermostat(String address) {
      return Errors.fromCode(CODE_NOT_THERMOSTAT, "Device at address '" + address + "' is not a thermostat");
   }

   public static ErrorEvent notTemperatureDevice(String address) {
      return Errors.fromCode(CODE_NOT_TEMPERATURE, "Device at address '" + address + "' is not a temperature sensor");
   }

   public static ErrorEvent notHumidityDevice(String address) {
      return Errors.fromCode(CODE_NOT_HUMIDITY, "Device at address '" + address + "' is not a humidity sensor");
   }

}

