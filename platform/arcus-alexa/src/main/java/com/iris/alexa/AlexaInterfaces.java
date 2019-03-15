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
package com.iris.alexa;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.messages.type.AlexaCapProperties;
import com.iris.messages.type.AlexaCapability;

public enum AlexaInterfaces {
   ;

   public static final String RESPONSE_NAMESPACE = "Alexa";
   public static final String RESPONSE_NAME = "Response";
   public static final String RESPONSE_ERROR = "ErrorResponse";
   public static final String RESPONSE_DEFERRED = "DeferredResponse";
   public static final String REQUEST_REPORTSTATE = "ReportState";
   public static final String RESPONSE_REPORTSTATE = "StateReport";
   public static final String EVENT_REPORT = "ChangeReport";

   public interface Discovery {
      static final String NAMESPACE = "Alexa.Discovery";
      static final String REQUEST_DISCOVER = "Discover";
      static final String RESPONSE_DISCOVER = "Discover.Response";
   }

   public interface BrightnessController {
      static final String NAMESPACE = "Alexa.BrightnessController";
      static final String REQUEST_ADJUSTBRIGHTNESS = "AdjustBrightness";
      static final String ARG_BRIGHTNESSDELTA = "brightnessDelta";
      static final String REQUEST_SETBRIGHTNESS = "SetBrightness";
      static final String PROP_BRIGHTNESS = "brightness";

      static AlexaCapability createCapability(boolean allowProactive) {
         return capability(NAMESPACE, ImmutableList.of(PROP_BRIGHTNESS), allowProactive);
      }
   }

   public interface ColorController {
      static final String NAMESPACE = "Alexa.ColorController";
      static final String REQUEST_SETCOLOR = "SetColor";
      static final String PROP_COLOR = "color";

      static AlexaCapability createCapability(boolean allowProactive) {
         return capability(NAMESPACE, ImmutableList.of(PROP_COLOR), allowProactive);
      }
   }

   public interface ColorTemperatureController {
      static final String NAMESPACE = "Alexa.ColorTemperatureController";
      static final String REQUEST_DECREASECOLORTEMPERATURE = "DecreaseColorTemperature";
      static final String REQUEST_INCREASECOLORTEMPERATURE = "IncreaseColorTemperature";
      static final String REQUEST_SETCOLORTEMPERATURE = "SetColorTemperature";
      static final String PROP_COLORTEMPERATUREINKELVIN = "colorTemperatureInKelvin";

      static AlexaCapability createCapability(boolean allowProactive) {
         return capability(NAMESPACE, ImmutableList.of(PROP_COLORTEMPERATUREINKELVIN), allowProactive);
      }
   }

   public interface LockController {
      static final String NAMESPACE = "Alexa.LockController";
      static final String REQUEST_LOCK = "Lock";
      static final String REQUEST_UNLOCK = "Unlock";
      static final String PROP_LOCKSTATE = "lockState";
      static final String STATE_JAMMED = "JAMMED";

      static AlexaCapability createCapability(boolean allowProactive) {
         return capability(NAMESPACE, ImmutableList.of(PROP_LOCKSTATE), allowProactive);
      }
   }

   public interface PercentageController {
      static final String NAMESPACE = "Alexa.PercentageController";
      static final String REQUEST_SETPERCENTAGE = "SetPercentage";
      static final String REQUEST_ADJUSTPERCENTAGE = "AdjustPercentage";
      static final String ARG_PERCENTAGEDELTA = "percentageDelta";
      static final String PROP_PERCENTAGE = "percentage";

      static AlexaCapability createCapability(boolean allowProactive) {
         return capability(NAMESPACE, ImmutableList.of(PROP_PERCENTAGE), allowProactive);
      }
   }

   public interface PowerController {
      static final String NAMESPACE = "Alexa.PowerController";
      static final String REQUEST_TURNON = "TurnOn";
      static final String REQUEST_TURNOFF = "TurnOff";
      static final String PROP_POWERSTATE = "powerState";

      static AlexaCapability createCapability(boolean allowProactive) {
         return capability(NAMESPACE, ImmutableList.of(PROP_POWERSTATE), allowProactive);
      }
   }

   public interface SceneController {
      static final String NAMESPACE = "Alexa.SceneController";
      static final String REQUEST_ACTIVATE = "Activate";
      static final String RESPONSE_ACTIVATE = "ActivationStarted";

      static final AlexaCapability CAPABILITY = sceneCapability();
   }

   public interface TemperatureSensor {
      static final String NAMESPACE = "Alexa.TemperatureSensor";
      static final String PROP_TEMPERATURE = "temperature";

      static AlexaCapability createCapability(boolean allowProactive) {
         return capability(NAMESPACE, ImmutableList.of(PROP_TEMPERATURE), allowProactive);
      }
   }

   public interface ThermostatController {
      static final String NAMESPACE = "Alexa.ThermostatController";
      static final String REQUEST_SETTARGETTEMPERATURE = "SetTargetTemperature";
      static final String REQUEST_ADJUSTTARGETTEMPERATURE = "AdjustTargetTemperature";
      static final String ARG_TARGETSETPOINTDELTA = "targetSetpointDelta";
      static final String REQUEST_SETTHERMOSTATMODE = "SetThermostatMode";
      static final String PROP_TARGETSETPOINT = "targetSetpoint";
      static final String PROP_LOWERSETPOINT = "lowerSetpoint";
      static final String PROP_UPPERSETPOINT = "upperSetpoint";
      static final String PROP_THERMOSTATMODE = "thermostatMode";

      static AlexaCapability createCapability(boolean allowProactive) {
         return capability(
            NAMESPACE,
            ImmutableList.of(
               PROP_LOWERSETPOINT,
               PROP_TARGETSETPOINT,
               PROP_THERMOSTATMODE,
               PROP_UPPERSETPOINT
            ),
            allowProactive
         );
      }
   }

   public interface Authorization {
      static final String NAMESPACE = "Alexa.Authorization";
      static final String REQUEST_ACCEPTGRANT = "AcceptGrant";
      static final String RESPONSE_ACCEPTGRANT = "AcceptGrant.Response";
      static final String ARG_GRANT = "grant";
      static final String ARG_GRANTEE = "grantee";
   }

   public interface EndpointHealth {
      static final String NAMESPACE = "Alexa.EndpointHealth";
      static final String PROP_CONNECTIVITY = "connectivity";

      static AlexaCapability createCapability(boolean allowProactive) {
         return capability(
            NAMESPACE,
            ImmutableList.of(PROP_CONNECTIVITY),
            allowProactive
         );
      }
   }

   private static AlexaCapability capability(String iface, List<String> props, boolean allowProactive) {
      AlexaCapability cap = new AlexaCapability();
      cap.setType("AlexaInterface");
      cap.setInterface(iface);
      cap.setVersion("3");
      cap.setProperties(properties(props, allowProactive).toMap());
      return cap;
   }

   private static AlexaCapProperties properties(List<String> supported, boolean allowProactive) {
      AlexaCapProperties props = new AlexaCapProperties();
      props.setRetrievable(true);
      props.setProactivelyReported(allowProactive);
      props.setSupported(supported.stream().map(prop -> ImmutableMap.<String,Object>of("name", prop)).collect(Collectors.toList()));
      return props;
   }

   private static AlexaCapability sceneCapability() {
      AlexaCapability cap = capability("Alexa.SceneController", ImmutableList.of(), false);
      AlexaCapProperties props = new AlexaCapProperties();
      props.setSupportsDeactivation(false);
      props.setProactivelyReported(false);
      cap.setProperties(props.toMap());
      return cap;
   }


}

