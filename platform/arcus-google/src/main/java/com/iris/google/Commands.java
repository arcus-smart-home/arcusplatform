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
package com.iris.google;

public interface Commands {

   interface OnOff {
      String name = "action.devices.commands.OnOff";
      String arg_on = "on";
   }

   interface BrightnessAbsolute {
      String name = "action.devices.commands.BrightnessAbsolute";
      String arg_brightness = "brightness";
   }

   interface ColorAbsolute {
      String name = "action.devices.commands.ColorAbsolute";
      String arg_color = "color";
   }

   interface ActivateScene {
      String name = "action.devices.commands.ActivateScene";
      String arg_deactivate = "deactivate";
   }

   interface TemperatureSetPoint {
      String name = "action.devices.commands.ThermostatTemperatureSetpoint";
      String arg_temperature = "thermostatTemperatureSetpoint";
   }

   interface TemperatureSetRange {
      String name = "action.devices.commands.ThermostatTemperatureSetRange";
      String arg_temperature_high = "thermostatTemperatureSetpointHigh";
      String arg_temperature_low = "thermostatTemperatureSetpointLow";
   }

   interface SetMode {
      String name = "action.devices.commands.ThermostatSetMode";
      String arg_mode = "thermostatMode";
   }

   interface SetThermostat {
      String name = "action.devices.commands.internal.SetThermostat";
   }

}

