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

import com.iris.messages.address.Address;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.service.GoogleService;

public interface Constants {

   Address BRIDGE_ADDRESS = Address.bridgeAddress("GOOG");
   Address SERVICE_ADDRESS = Address.platformService(GoogleService.NAMESPACE);

   interface Defaults {
      int COLOR_TEMPERATURE_MIN = 2700;
      int COLOR_TEMPERATURE_MAX = 6500;
      int COLOR_RGB_MIN = 0;
      int COLOR_RGB_MAX = 16777215;
   }

   interface DeviceTypeHint {
      String THERMOSTAT = "Thermostat";
      String LIGHT = "Light";
      String SWITCH = "Switch";
      String DIMMER = "Dimmer";
   }

   interface Error {
      String AUTH_EXPIRED = "authExpired";
      String AUTH_FAILURE = "authFailure";
      String DEVICE_OFFLINE = "deviceOffline";
      String TIMEOUT = "timeout";
      String DEVICE_TURNED_OFF = "deviceTurnedOff";
      String DEVICE_NOT_FOUND = "deviceNotFound";
      String VALUE_OUT_OF_RANGE = "valueOutOfRange";
      String NOT_SUPPORTED = "notSupported";
      String PROTOCOL_ERROR = "protocolError";
      String UNKNOWN_ERROR = "unknownError";
      String IN_HEAT_OR_COOL = "inHeatOrCool";
      String IN_HEAT_COOL = "inHeatCool";
      String LOCKED_TO_RANGE = "lockedToRange";
      String RANGE_TOO_CLOSE = "rangeTooClose";
   }

   interface Status {
      String SUCCESS = "SUCCESS";
      String PENDING = "PENDING";
      String OFFLINE = "OFFLINE";
      String ERROR = "ERROR";
   }

   interface Type {
      String THERMOSTAT = "action.devices.types.THERMOSTAT";
      String LIGHT = "action.devices.types.LIGHT";
      String OUTLET = "action.devices.types.OUTLET";
      String SWITCH = "action.devices.types.SWITCH";
      String SCENE = "action.devices.types.SCENE";
   }

   interface Trait {
      String ON_OFF = "action.devices.traits.OnOff";
      String BRIGHTNESS = "action.devices.traits.Brightness";
      String COLOR_SPECTRUM = "action.devices.traits.ColorSpectrum";
      String COLOR_TEMPERATURE = "action.devices.traits.ColorTemperature";
      String SCENE = "action.devices.traits.Scene";
      String TEMPERATURE_SETTING = "action.devices.traits.TemperatureSetting";
   }

   interface Attributes {
      interface ColorTemperature {
         String TEMPERATURE_MIN_K = "TemperatureMinK";
         String TEMPERATURE_MAX_K = "TemperatureMaxK";
      }
      interface TemperatureSetting {
         String MODES = "availableThermostatModes";
         String UNIT = "thermostatTemperatureUnit";
      }
      interface Scene {
         String sceneReversible = "sceneReversible";
      }
   }

   interface States {
      String ONLINE = "online";
      String ERROR_CODE = "errorCode";
      String DEBUG_STRING = "debugString";

      interface OnOff {
         String ON = "on";
      }

      interface Brightness {
         String BRIGHTNESS = "brightness";
      }

      // covers both temperature and spectrum
      interface Color {
         String COLOR = "color";
      }

      interface TemperatureSetting {
         String MODE = "thermostatMode";
         String TEMPERATURE_SET = "thermostatTemperatureSetpoint";
         String TEMPERATURE_AMBIENT = "thermostatTemperatureAmbient";
         String TEMPERATURE_SET_HIGH = "thermostatTemperatureSetpointHigh";
         String TEMPERATURE_SET_LOW = "thermostatTemperatureSetpointLow";
         String HUMIDITY_AMBIENT = "thermostatHumidityAmbient";
      }
   }

   enum TemperatureSettingMode {
      off, cool, heat, heatcool;

      static TemperatureSettingMode fromIris(String mode) {
         switch(mode) {
            case ThermostatCapability.HVACMODE_AUTO: return heatcool;
            case ThermostatCapability.HVACMODE_COOL: return cool;
            case ThermostatCapability.HVACMODE_HEAT: return heat;
            case ThermostatCapability.HVACMODE_OFF: return off;
            default:
               throw new IllegalArgumentException("unsupported thermostat mode " + mode);
         }
      }

      String toIris() {
         switch(this) {
            case off: return ThermostatCapability.HVACMODE_OFF;
            case cool: return ThermostatCapability.HVACMODE_COOL;
            case heat: return ThermostatCapability.HVACMODE_HEAT;
            case heatcool: return ThermostatCapability.HVACMODE_AUTO;
            default:
               throw new IllegalArgumentException("unsupported mode " + this);
         }
      }
   }

   // don't want a dependency on arcus-protocol
   interface Protocol {
      String ZIGBEE = "ZIGB";
      String ZWAVE = "ZWAV";
      String SERCOMM = "SCOM";
   }

   interface Intents {
      String EXECUTE = "action.devices.EXECUTE";
      String SYNC = "action.devices.SYNC";
      String QUERY = "action.devices.QUERY";
   }

   interface Response {
      String ERROR_CODE = "errorCode";
      String DEBUG_STRING = "debugString";

      interface Sync {
         String DEVICES = "devices";
         String AGENT_USER_ID = "agentUserId";
      }

      interface Query {
         String DEVICES = "devices";
      }

      interface Execute {
         String COMMANDS = "commands";
      }
   }
}

