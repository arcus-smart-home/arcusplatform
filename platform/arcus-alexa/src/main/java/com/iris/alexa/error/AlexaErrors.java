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
package com.iris.alexa.error;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.MessageBody;
import com.iris.messages.service.AlexaService.AlexaErrorEvent;
import com.iris.messages.type.AlexaTemperature;
import com.iris.messages.type.AlexaValidRange;

public enum AlexaErrors {
   ;

   public static final String TYPE_BRIDGE_UNREACHABLE = "BRIDGE_UNREACHABLE";
   public static final String TYPE_ENDPOINT_BUSY = "ENDPOINT_BUSY";
   public static final String TYPE_ENDPOINT_LOW_POWER = "ENDPOINT_LOW_POWER";
   public static final String TYPE_ENDPOINT_UNREACHABLE = "ENDPOINT_UNREACHABLE";
   public static final String TYPE_EXPIRED_AUTHORIZATION_CREDENTIAL = "EXPIRED_AUTHORIZATION_CREDENTIAL";
   public static final String TYPE_FIRMWARE_OUT_OF_DATE = "FIRMWARE_OUT_OF_DATE";
   public static final String TYPE_HARDWARE_MALFUNCTION = "HARDWARE_MALFUNCTION";
   public static final String TYPE_INTERNAL_ERROR = "INTERNAL_ERROR";
   public static final String TYPE_INVALID_AUTHORIZATION_CREDENTIAL = "INVALID_AUTHORIZATION_CREDENTIAL";
   public static final String TYPE_INVALID_DIRECTIVE = "INVALID_DIRECTIVE";
   public static final String TYPE_INVALID_VALUE = "INVALID_VALUE";
   public static final String TYPE_NO_SUCH_ENDPOINT = "NO_SUCH_ENDPOINT";
   public static final String TYPE_NOT_SUPPORTED_IN_CURRENT_MODE = "NOT_SUPPORTED_IN_CURRENT_MODE";
   public static final String TYPE_RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
   public static final String TYPE_VALUE_OUT_OF_RANGE = "VALUE_OUT_OF_RANGE";
   public static final String TYPE_TEMPERATURE_VALUE_OUT_OF_RANGE = "TEMPERATURE_VALUE_OUT_OF_RANGE";
   public static final String TYPE_REQUESTED_SETPOINTS_TOO_CLOSE = "REQUESTED_SETPOINTS_TOO_CLOSE";
   public static final String TYPE_THERMOSTAT_IS_OFF = "THERMOSTAT_IS_OFF";
   public static final String TYPE_UNSUPPORTED_THERMOSTAT_MODE = "UNSUPPORTED_THERMOSTAT_MODE";
   public static final String TYPE_DUAL_SETPOINTS_UNSUPPORTED = "DUAL_SETPOINTS_UNSUPPORTED";
   public static final String TYPE_TRIPLE_SETPOINTS_UNSUPPORTED = "TRIPLE_SETPOINTS_UNSUPPORTED";
   public static final String TYPE_UNWILLING_TO_SET_VALUE = "UNWILLING_TO_SET_VALUE";
   public static final String TYPE_ACCEPT_GRANT_FAILED = "ACCEPT_GRANT_FAILED";

   public static final String PROP_VALIDRANGE = "validRange";

   public static final MessageBody BRIDGE_UNREACHABLE = AlexaErrorEvent.builder()
      .withType(TYPE_BRIDGE_UNREACHABLE)
      .withMessage("Iris hub is offline, please check its power and connectivity.")
      .build();

   public static final MessageBody ENDPOINT_BUSY = AlexaErrorEvent.builder()
      .withType(TYPE_ENDPOINT_BUSY)
      .withMessage("The endpoint is busy and cannot complete the request.")
      .build();

   public static final MessageBody ENDPOINT_LOW_POWER = AlexaErrorEvent.builder()
      .withType(TYPE_ENDPOINT_LOW_POWER)
      .withMessage("The endpoint does not have sufficient battery to complete request.")
      .build();

   public static final MessageBody ENDPOINT_UNREACHABLE = AlexaErrorEvent.builder()
      .withType(TYPE_ENDPOINT_UNREACHABLE)
      .withMessage("The endpoint is offline, please check its batteries and connectivity.")
      .build();

   public static final MessageBody EXPIRED_AUTHORIZATION_CREDENTIAL = AlexaErrorEvent.builder()
      .withType(TYPE_EXPIRED_AUTHORIZATION_CREDENTIAL)
      .withMessage("Authorization credentials have expired.")
      .build();

   public static final MessageBody FIRMWARE_OUT_OF_DATE = AlexaErrorEvent.builder()
      .withType(TYPE_FIRMWARE_OUT_OF_DATE)
      .withMessage("Iris hub or endpoint require a firmware update to complete request.")
      .build();

   public static final MessageBody HARDWARE_MALFUNCTION = AlexaErrorEvent.builder()
      .withType(TYPE_HARDWARE_MALFUNCTION)
      .withMessage("Iris hub or endpoint are experiencing a hardware error preventing request completion.")
      .build();

   public static final MessageBody INTERNAL_ERROR = AlexaErrorEvent.builder()
      .withType(TYPE_INTERNAL_ERROR)
      .withMessage("An unexpected error prevented request completion.")
      .build();

   public static final MessageBody INVALID_AUTHORIZATION_CREDENTIAL = AlexaErrorEvent.builder()
      .withType(TYPE_INVALID_AUTHORIZATION_CREDENTIAL)
      .withMessage("Authorization credential was invalid or not provided.")
      .build();

   private static final MessageBody INVALID_DIRECTIVE_DEFAULT = AlexaErrorEvent.builder()
      .withType(TYPE_INVALID_DIRECTIVE)
      .withMessage("Directive was malformed.")
      .build();

   public static MessageBody missingArgument(String argument) {
      return AlexaErrorEvent.builder()
         .withType(TYPE_INVALID_DIRECTIVE)
         .withMessage("Request is missing the required argument " + argument + '.')
         .build();
   }

   public static MessageBody invalidDirective(@Nullable String message) {
      if(message == null) {
         return INVALID_DIRECTIVE_DEFAULT;
      }
      return AlexaErrorEvent.builder()
         .withType(TYPE_INVALID_DIRECTIVE)
         .withMessage(message)
         .build();
   }

   public static MessageBody unsupportedDirective(String directive) {
      return invalidDirective(directive + " is not supported.");
   }

   public static final MessageBody INVALID_VALUE_DEFAULT = AlexaErrorEvent.builder()
      .withType(TYPE_INVALID_VALUE)
      .withMessage("Request had an argument with an invalid value.")
      .build();

   public static MessageBody invalidValue(String argument, Object value) {
      return AlexaErrorEvent.builder()
         .withType(TYPE_INVALID_VALUE)
         .withMessage(String.valueOf(value) + " is not a valid value for " + argument + '.')
         .build();
   }

   public static final MessageBody NO_SUCH_ENDPOINT = AlexaErrorEvent.builder()
      .withType(TYPE_NO_SUCH_ENDPOINT)
      .withMessage("Endpoint no longer exists, please forget it in the Alexa companion application.")
      .build();

   public static MessageBody notSupportedInCurrentMode(String mode) {
      return AlexaErrorEvent.builder()
         .withType(TYPE_NOT_SUPPORTED_IN_CURRENT_MODE)
         .withMessage("Request cannot be completed in mode " + mode + '.')
         .withPayload(ImmutableMap.of("currentDeviceMode", mode))
         .build();
   }

   public static final MessageBody RATE_LIMIT_EXCEEDED = AlexaErrorEvent.builder()
      .withType(TYPE_RATE_LIMIT_EXCEEDED)
      .withMessage("Request cannot be completed because the rate limit has been exceeded.")
      .build();

   public static MessageBody valueOutOfRange(String argument, Number value, Number min, Number max) {
      AlexaValidRange range = new AlexaValidRange();
      range.setMinimumValue(min);
      range.setMaximumValue(max);
      return AlexaErrorEvent.builder()
         .withType(TYPE_VALUE_OUT_OF_RANGE)
         .withMessage(argument + " cannot be set to " + value + '.')
         .withPayload(ImmutableMap.of(PROP_VALIDRANGE, range.toMap()))
         .build();
   }

   public static MessageBody temperatureValueOutOfRange(double value, double min, double max) {
      AlexaTemperature minTemp = new AlexaTemperature();
      minTemp.setValue(min);
      minTemp.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaTemperature maxTemp = new AlexaTemperature();
      maxTemp.setValue(max);
      maxTemp.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaValidRange range = new AlexaValidRange();
      range.setMinimumValue(minTemp.toMap());
      range.setMaximumValue(maxTemp.toMap());

      return AlexaErrorEvent.builder()
         .withType(TYPE_TEMPERATURE_VALUE_OUT_OF_RANGE)
         .withMessage("The requested temperature of " + value + " is out of range.")
         .withPayload(ImmutableMap.of(PROP_VALIDRANGE, range.toMap()))
         .build();
   }

   public static MessageBody requestedSetpointsTooClose(double delta) {
      AlexaTemperature minDelta = new AlexaTemperature();
      minDelta.setScale(AlexaTemperature.SCALE_CELSIUS);
      minDelta.setValue(delta);

      return AlexaErrorEvent.builder()
         .withType(TYPE_REQUESTED_SETPOINTS_TOO_CLOSE)
         .withMessage("The requested setpoints are too close together")
         .withPayload(ImmutableMap.of("minimumTemperatureDelta", minDelta.toMap()))
         .build();
   }

   public static final MessageBody THERMOSTAT_IS_OFF = AlexaErrorEvent.builder()
      .withType(TYPE_THERMOSTAT_IS_OFF)
      .withMessage("The thermostat is off and cannot be turned on.")
      .build();

   public static MessageBody unsupportedThermostatMode(String mode) {
      return AlexaErrorEvent.builder()
         .withType(TYPE_UNSUPPORTED_THERMOSTAT_MODE)
         .withMessage(mode + " is not a supported thermostat mode.")
         .build();
   }

   public static MessageBody dualSetpointsUnsupported(String mode) {
      return AlexaErrorEvent.builder()
         .withType(TYPE_DUAL_SETPOINTS_UNSUPPORTED)
         .withMessage("Dual setpoints are not supported in " + mode + '.')
         .build();
   }

   public static MessageBody tripleSetpointsUnsupported(String mode) {
      return AlexaErrorEvent.builder()
         .withType(TYPE_TRIPLE_SETPOINTS_UNSUPPORTED)
         .withMessage("Triple setpoints are not supported in " + mode + '.')
         .build();
   }

   public static final MessageBody UNWILLING_TO_SET_VALUE = AlexaErrorEvent.builder()
      .withType(TYPE_UNWILLING_TO_SET_VALUE)
      .withMessage("Unwilling to set the thermostat value.")
      .build();

   public static final MessageBody ACCEPT_GRANT_FAILED = AlexaErrorEvent.builder()
      .withType(TYPE_ACCEPT_GRANT_FAILED)
      .withMessage("Unable to acquire tokens for proactive reporting.")
      .build();
}

