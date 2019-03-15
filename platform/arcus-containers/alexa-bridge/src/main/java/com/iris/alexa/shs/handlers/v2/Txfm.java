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
package com.iris.alexa.shs.handlers.v2;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Precision;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.AlexaUtil;
import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Header;
import com.iris.alexa.message.v2.Appliance;
import com.iris.alexa.message.v2.Color;
import com.iris.alexa.message.v2.DoubleValue;
import com.iris.alexa.message.v2.IntValue;
import com.iris.alexa.message.v2.StringValue;
import com.iris.alexa.message.v2.error.DriverInternalError;
import com.iris.alexa.message.v2.error.ErrorInfo;
import com.iris.alexa.message.v2.error.ErrorPayloadException;
import com.iris.alexa.message.v2.error.UnableToGetValueError;
import com.iris.alexa.message.v2.error.UnableToSetValueError;
import com.iris.alexa.message.v2.error.UnsupportedOperationError;
import com.iris.alexa.message.v2.error.UnwillingToSetValueError;
import com.iris.alexa.message.v2.error.ValueOutOfRangeError;
import com.iris.alexa.message.v2.request.DecrementColorTemperatureRequest;
import com.iris.alexa.message.v2.request.DecrementPercentageRequest;
import com.iris.alexa.message.v2.request.DecrementTargetTemperatureRequest;
import com.iris.alexa.message.v2.request.DiscoverAppliancesRequest;
import com.iris.alexa.message.v2.request.GetLockStateRequest;
import com.iris.alexa.message.v2.request.GetTargetTemperatureRequest;
import com.iris.alexa.message.v2.request.GetTemperatureReadingRequest;
import com.iris.alexa.message.v2.request.IncrementColorTemperatureRequest;
import com.iris.alexa.message.v2.request.IncrementPercentageRequest;
import com.iris.alexa.message.v2.request.IncrementTargetTemperatureRequest;
import com.iris.alexa.message.v2.request.RequestPayload;
import com.iris.alexa.message.v2.request.SetColorRequest;
import com.iris.alexa.message.v2.request.SetColorTemperatureRequest;
import com.iris.alexa.message.v2.request.SetLockStateRequest;
import com.iris.alexa.message.v2.request.SetPercentageRequest;
import com.iris.alexa.message.v2.request.SetTargetTemperatureRequest;
import com.iris.alexa.message.v2.request.TurnOffRequest;
import com.iris.alexa.message.v2.request.TurnOnRequest;
import com.iris.alexa.message.v2.response.ColorTemperatureConfirmationPayload;
import com.iris.alexa.message.v2.response.DecrementColorTemperatureConfirmation;
import com.iris.alexa.message.v2.response.DecrementPercentageConfirmation;
import com.iris.alexa.message.v2.response.DecrementTargetTemperatureConfirmation;
import com.iris.alexa.message.v2.response.DiscoverAppliancesResponse;
import com.iris.alexa.message.v2.response.GetLockStateResponse;
import com.iris.alexa.message.v2.response.GetTargetTemperatureResponse;
import com.iris.alexa.message.v2.response.GetTemperatureReadingResponse;
import com.iris.alexa.message.v2.response.IncrementColorTemperatureConfirmation;
import com.iris.alexa.message.v2.response.IncrementPercentageConfirmation;
import com.iris.alexa.message.v2.response.IncrementTargetTemperatureConfirmation;
import com.iris.alexa.message.v2.response.ResponsePayload;
import com.iris.alexa.message.v2.response.SetColorConfirmation;
import com.iris.alexa.message.v2.response.SetColorTemperatureConfirmation;
import com.iris.alexa.message.v2.response.SetLockStateConfirmation;
import com.iris.alexa.message.v2.response.SetPercentageConfirmation;
import com.iris.alexa.message.v2.response.SetTargetTemperatureConfirmation;
import com.iris.alexa.message.v2.response.TemperatureConfirmationPayload;
import com.iris.alexa.message.v2.response.TurnOffConfirmation;
import com.iris.alexa.message.v2.response.TurnOnConfirmation;
import com.iris.alexa.shs.ShsMetrics;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaCapability;
import com.iris.messages.type.AlexaColor;
import com.iris.messages.type.AlexaEndpoint;
import com.iris.messages.type.AlexaPropertyReport;
import com.iris.messages.type.AlexaTemperature;

class Txfm {

   private Txfm() {
   }

   final PlatformMessage txfmRequest(AlexaMessage message, UUID placeId, String population, int ttl) {
      return PlatformMessage.buildRequest(
         txfmRequestBody(message),
         AlexaUtil.ADDRESS_BRIDGE,
         Address.platformService(AlexaService.NAMESPACE)
      )
      .withTimeToLive(ttl)
      .withCorrelationId(message.getHeader().getMessageId())
      .withPlaceId(placeId)
      .withPopulation(population)
      .create();
   }

   @SuppressWarnings("unchecked")
   MessageBody txfmRequestBody(AlexaMessage message) {
      PayloadTxfm txfm = payloads.get(message.getHeader().getName());
      return AlexaService.ExecuteRequest.builder()
         .withDirective(txfm.executeDirective(message))
         .withCorrelationToken(message.getHeader().getCorrelationToken())
         .withArguments(txfm.requestToArgs((RequestPayload) message.getPayload()))
         .withTarget(extractRequestTarget(message))
         .withAllowDeferred(false)
         .build();
   }

   private String extractRequestTarget(AlexaMessage message) {
      Appliance app = ((RequestPayload) message.getPayload()).getAppliance();
      return AlexaUtil.endpointIdToAddress(app.getApplianceId());
   }

   final AlexaMessage transformResponse(AlexaMessage request, PlatformMessage response) {
      MessageBody body = response.getValue();
      if(AlexaService.AlexaErrorEvent.NAME.equals(body.getMessageType())) {
         throw new AlexaException(body);
      }
      PayloadTxfm txfm = payloads.get(request.getHeader().getName());
      ResponsePayload payload = txfm.responseToPayload(body);
      Header header = Header.v2(response.getCorrelationId(), payload.getName(), payload.getNamespace());
      return new AlexaMessage(header, payload);
   }

   //-------------------------------------------------------------------------------------------------------------------
   // Internal Interface for transforming the payload of the messages
   //-------------------------------------------------------------------------------------------------------------------

   private interface PayloadTxfm {

      default Map<String, Object> requestToArgs(RequestPayload payload) {
         return ImmutableMap.of();
      }

      String executeDirective(AlexaMessage message);
      ResponsePayload responseToPayload(MessageBody response);
   }

   //-------------------------------------------------------------------------------------------------------------------
   // Mapping of all supported message payloads to their transformers
   //-------------------------------------------------------------------------------------------------------------------

   private static final Map<String, PayloadTxfm> payloads = ImmutableMap.<String, PayloadTxfm>builder()
      //----------------------------------------------------------------------------------------------------------------
      // discover appliances
      .put(DiscoverAppliancesRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public String executeDirective(AlexaMessage message) {
            return AlexaInterfaces.Discovery.REQUEST_DISCOVER;
         }

         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            DiscoverAppliancesResponse rspPayload = new DiscoverAppliancesResponse();
            List<Map<String, Object>> endpoints = AlexaService.DiscoverResponse.getEndpoints(response);
            if(endpoints == null) {
               endpoints = ImmutableList.of();
            }
            rspPayload.setDiscoveredAppliances(
               endpoints.stream()
                  .map(m -> applianceFromEndpoint(new AlexaEndpoint(m)))
                  .collect(Collectors.toList())
            );
            return rspPayload;
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // turn on
      .put(TurnOnRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public String executeDirective(AlexaMessage message) {
            Appliance app = ((RequestPayload) message.getPayload()).getAppliance();
            return
               AlexaUtil.isSceneAddress(app.getApplianceId()) ?
               AlexaInterfaces.SceneController.REQUEST_ACTIVATE :
               AlexaInterfaces.PowerController.REQUEST_TURNON;
         }

         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            return new TurnOnConfirmation();
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // turn off
      .put(TurnOffRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public String executeDirective(AlexaMessage message) {
            return AlexaInterfaces.PowerController.REQUEST_TURNOFF;
         }

         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            return new TurnOffConfirmation();
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // set percentage
      .put(SetPercentageRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public Map<String, Object> requestToArgs(RequestPayload payload) {
            SetPercentageRequest setPerc = (SetPercentageRequest ) payload;
            if(isFan(payload.getAppliance())) {
               return ImmutableMap.of(AlexaInterfaces.PercentageController.PROP_PERCENTAGE, (int) setPerc.getPercentageState().getValue());
            }
            return ImmutableMap.of(AlexaInterfaces.BrightnessController.PROP_BRIGHTNESS, (int) setPerc.getPercentageState().getValue());
         }

         @Override
         public String executeDirective(AlexaMessage message) {
            Appliance app = ((RequestPayload) message.getPayload()).getAppliance();
            return
               isFan(app) ?
               AlexaInterfaces.PercentageController.REQUEST_SETPERCENTAGE :
               AlexaInterfaces.BrightnessController.REQUEST_SETBRIGHTNESS;
         }

         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            return new SetPercentageConfirmation();
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // increment percentage
      .put(IncrementPercentageRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public Map<String, Object> requestToArgs(RequestPayload payload) {
            IncrementPercentageRequest incPerc = (IncrementPercentageRequest) payload;
            if(isFan(payload.getAppliance())) {
               return ImmutableMap.of(AlexaInterfaces.PercentageController.ARG_PERCENTAGEDELTA, (int) incPerc.getDeltaPercentage().getValue());
            }
            return ImmutableMap.of(AlexaInterfaces.BrightnessController.ARG_BRIGHTNESSDELTA, (int) incPerc.getDeltaPercentage().getValue());
         }

         @Override
         public String executeDirective(AlexaMessage message) {
            Appliance app = ((RequestPayload) message.getPayload()).getAppliance();
            return
               isFan(app) ?
               AlexaInterfaces.PercentageController.REQUEST_ADJUSTPERCENTAGE :
               AlexaInterfaces.BrightnessController.REQUEST_ADJUSTBRIGHTNESS;
         }

         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            return new IncrementPercentageConfirmation();
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // decrement percentage
      .put(DecrementPercentageRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public Map<String, Object> requestToArgs(RequestPayload payload) {
            DecrementPercentageRequest decPerc = (DecrementPercentageRequest) payload;
            if(isFan(payload.getAppliance())) {
               return ImmutableMap.of(AlexaInterfaces.PercentageController.ARG_PERCENTAGEDELTA, -1 * (int) decPerc.getDeltaPercentage().getValue());
            }
            return ImmutableMap.of(AlexaInterfaces.BrightnessController.ARG_BRIGHTNESSDELTA, -1 * (int) decPerc.getDeltaPercentage().getValue());
         }

         @Override
         public String executeDirective(AlexaMessage message) {
            Appliance app = ((RequestPayload) message.getPayload()).getAppliance();
            return
               isFan(app) ?
               AlexaInterfaces.PercentageController.REQUEST_ADJUSTPERCENTAGE :
               AlexaInterfaces.BrightnessController.REQUEST_ADJUSTBRIGHTNESS;
         }

         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            return new DecrementPercentageConfirmation();
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // set lock state
      .put(SetLockStateRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public String executeDirective(AlexaMessage message) {
            return AlexaInterfaces.LockController.REQUEST_LOCK;
         }

         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            SetLockStateConfirmation confirmation = new SetLockStateConfirmation();
            confirmation.setLockState(getStateFromExecuteResponse(response, true));
            return confirmation;
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // get lock state
      .put(GetLockStateRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public String executeDirective(AlexaMessage message) {
            return AlexaInterfaces.REQUEST_REPORTSTATE;
         }

         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            GetLockStateResponse rspPayload = new GetLockStateResponse();
            rspPayload.setLockState(getStateFromExecuteResponse(response, false));
            return rspPayload;
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // set target temperature
      .put(SetTargetTemperatureRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public String executeDirective(AlexaMessage message) {
            return SetTargetTemperatureRequest.class.getSimpleName();
         }

         @Override
         public Map<String, Object> requestToArgs(RequestPayload payload) {
            SetTargetTemperatureRequest req = (SetTargetTemperatureRequest) payload;
            return ImmutableMap.of("targetTemperature", req.getTargetTemperature().getValue());
         }

         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            return thermostatResponse(response, SetTargetTemperatureConfirmation::new);
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // increment target temperature
      .put(IncrementTargetTemperatureRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public String executeDirective(AlexaMessage message) {
            return IncrementTargetTemperatureRequest.class.getSimpleName();
         }

         @Override
         public Map<String, Object> requestToArgs(RequestPayload payload) {
            IncrementTargetTemperatureRequest req = (IncrementTargetTemperatureRequest) payload;
            return ImmutableMap.of("deltaTemperature", req.getDeltaTemperature().getValue());
         }

         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            return thermostatResponse(response, IncrementTargetTemperatureConfirmation::new);
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // decrement target temperature
      .put(DecrementTargetTemperatureRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public String executeDirective(AlexaMessage message) {
            return DecrementTargetTemperatureRequest.class.getSimpleName();
         }

         @Override
         public Map<String, Object> requestToArgs(RequestPayload payload) {
            DecrementTargetTemperatureRequest req = (DecrementTargetTemperatureRequest) payload;
            return ImmutableMap.of("deltaTemperature", req.getDeltaTemperature().getValue());
         }

         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            return thermostatResponse(response, DecrementTargetTemperatureConfirmation::new);
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // get temperature reading
      .put(GetTemperatureReadingRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public String executeDirective(AlexaMessage message) {
            return AlexaInterfaces.REQUEST_REPORTSTATE;
         }

         @SuppressWarnings("unchecked")
         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            List<Map<String, Object>> properties = AlexaService.ExecuteResponse.getProperties(response);
            if(properties == null) {
               throw new ErrorPayloadException(new DriverInternalError());
            }
            AlexaPropertyReport rep = null;
            for(Map<String, Object> prop : properties) {
               AlexaPropertyReport propRep = new AlexaPropertyReport(prop);
               if(AlexaInterfaces.TemperatureSensor.PROP_TEMPERATURE.equals(propRep.getName())) {
                  rep = propRep;
                  break;
               }
            }

            if(rep == null) {
               throw new ErrorPayloadException(new DriverInternalError());
            }

            AlexaTemperature at = new AlexaTemperature((Map<String, Object>) rep.getValue());
            GetTemperatureReadingResponse rspPayload = new GetTemperatureReadingResponse();
            rspPayload.setApplianceResponseTimestamp(rep.getTimeOfSample());
            rspPayload.setTemperatureReading(new DoubleValue(at.getValue()));
            return rspPayload;
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // get target temperature
      .put(GetTargetTemperatureRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public String executeDirective(AlexaMessage message) {
            return AlexaInterfaces.REQUEST_REPORTSTATE;
         }

         @SuppressWarnings("unchecked")
         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            List<Map<String, Object>> properties = AlexaService.ExecuteResponse.getProperties(response);
            if(properties == null) {
               throw new ErrorPayloadException(new DriverInternalError());
            }
            String mode = null;
            Double coolSp = null;
            Double heatSp = null;
            Double targetSp = null;

            for(Map<String, Object> prop : properties) {
               AlexaPropertyReport propRep = new AlexaPropertyReport(prop);
               if(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE.equals(propRep.getName())) {
                  mode = (String) propRep.getValue();
               }
               if(AlexaInterfaces.ThermostatController.PROP_UPPERSETPOINT.equals(propRep.getName())) {
                  AlexaTemperature at = new AlexaTemperature((Map<String, Object>) propRep.getValue());
                  coolSp = at.getValue();
               }
               if(AlexaInterfaces.ThermostatController.PROP_LOWERSETPOINT.equals(propRep.getName())) {
                  AlexaTemperature at = new AlexaTemperature((Map<String, Object>) propRep.getValue());
                  heatSp = at.getValue();
               }
               if(AlexaInterfaces.ThermostatController.PROP_TARGETSETPOINT.equals(propRep.getName())) {
                  AlexaTemperature at = new AlexaTemperature((Map<String, Object>) propRep.getValue());
                  targetSp = at.getValue();

               }
            }

            if(mode == null) {
               throw new ErrorPayloadException(new DriverInternalError());
            }

            GetTargetTemperatureResponse rspPayload = new GetTargetTemperatureResponse();
            rspPayload.setTemperatureMode(new StringValue(mode));
            switch(mode) {
               case ThermostatCapability.HVACMODE_AUTO:
                  if(coolSp == null || heatSp == null) {
                     throw new ErrorPayloadException(new DriverInternalError());
                  }
                  rspPayload.setCoolingTargetTemperature(new DoubleValue(coolSp));
                  rspPayload.setHeatingTargetTemperature(new DoubleValue(heatSp));
                  break;
               case ThermostatCapability.HVACMODE_COOL:
                  if(targetSp == null) {
                     throw new ErrorPayloadException(new DriverInternalError());
                  }
                  rspPayload.setCoolingTargetTemperature(new DoubleValue(targetSp));
                  break;
               case ThermostatCapability.HVACMODE_HEAT:
                  if(targetSp == null) {
                     throw new ErrorPayloadException(new DriverInternalError());
                  }
                  rspPayload.setHeatingTargetTemperature(new DoubleValue(targetSp));
                  break;
               default: /* no op */
            }

            return rspPayload;
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // set color
      .put(SetColorRequest.class.getSimpleName(), new PayloadTxfm() {
         private AlexaColor alexaColor(Color c) {
            AlexaColor ac = new AlexaColor();
            ac.setSaturation(c.getSaturation());
            ac.setHue(c.getHue());
            ac.setBrightness(c.getBrightness());
            return ac;
         }

         private Color color(AlexaColor ac) {
            Color c = new Color();
            c.setBrightness(ac.getBrightness());
            c.setHue(ac.getHue());
            c.setSaturation(ac.getSaturation());
            return c;
         }

         @Override
         public String executeDirective(AlexaMessage message) {
            return AlexaInterfaces.ColorController.REQUEST_SETCOLOR;
         }

         @Override
         public Map<String, Object> requestToArgs(RequestPayload payload) {
            SetColorRequest req = (SetColorRequest) payload;
            Color c = req.getColor();
            if(c == null) {
               throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.ColorController.PROP_COLOR));
            }
            return ImmutableMap.of(AlexaInterfaces.ColorController.PROP_COLOR, alexaColor(c).toMap());
         }

         @SuppressWarnings("unchecked")
         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            List<Map<String, Object>> properties = AlexaService.ExecuteResponse.getProperties(response);
            if(properties == null) {
               throw new ErrorPayloadException(new DriverInternalError());
            }
            Map<String, Object> color = null;
            for(Map<String, Object> prop : properties) {
               AlexaPropertyReport propRep = new AlexaPropertyReport(prop);
               if(AlexaInterfaces.ColorController.PROP_COLOR.equals(propRep.getName())) {
                  color = (Map<String, Object>) propRep.getValue();
                  break;
               }
            }
            if(color == null) {
               throw new ErrorPayloadException(new DriverInternalError());
            }
            SetColorConfirmation confirmation = new SetColorConfirmation();
            confirmation.setColor(color(new AlexaColor(color)));
            return confirmation;
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // set color temperature
      .put(SetColorTemperatureRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public String executeDirective(AlexaMessage message) {
            return AlexaInterfaces.ColorTemperatureController.REQUEST_SETCOLORTEMPERATURE;
         }

         @Override
         public Map<String, Object> requestToArgs(RequestPayload payload) {
            SetColorTemperatureRequest req = (SetColorTemperatureRequest) payload;
            IntValue colorTemp = req.getColorTemperature();
            return ImmutableMap.of(AlexaInterfaces.ColorTemperatureController.PROP_COLORTEMPERATUREINKELVIN, colorTemp.getValue());
         }

         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            return colorTempResponse(response, SetColorTemperatureConfirmation::new);
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // increment color temperature
      .put(IncrementColorTemperatureRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public String executeDirective(AlexaMessage message) {
            return AlexaInterfaces.ColorTemperatureController.REQUEST_INCREASECOLORTEMPERATURE;
         }

         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            return colorTempResponse(response, IncrementColorTemperatureConfirmation::new);
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // decrement color temperature
      .put(DecrementColorTemperatureRequest.class.getSimpleName(), new PayloadTxfm() {
         @Override
         public String executeDirective(AlexaMessage message) {
            return AlexaInterfaces.ColorTemperatureController.REQUEST_DECREASECOLORTEMPERATURE;
         }

         @Override
         public ResponsePayload responseToPayload(MessageBody response) {
            return colorTempResponse(response, DecrementColorTemperatureConfirmation::new);
         }
      })
      .build();

   //-------------------------------------------------------------------------------------------------------------------
   // Percentage Helpers
   //-------------------------------------------------------------------------------------------------------------------

   private static boolean isFan(Appliance app) {
      Map<String, String> details = app.getAdditionalApplianceDetails();
      if(details == null) {
         return false;
      }
      return details.containsKey(FanCapability.ATTR_MAXSPEED);
   }

   //-------------------------------------------------------------------------------------------------------------------
   // Lock Helpers
   //-------------------------------------------------------------------------------------------------------------------

   private static final String JAMMED_CODE = "DEVICE_JAMMED";
   private static final String JAMMED_MSG = "Door lock is jammed.";

   private static String getStateFromExecuteResponse(MessageBody response, boolean set) {
      List<Map<String, Object>> properties = AlexaService.ExecuteResponse.getProperties(response);
      if(properties == null) {
         throw new ErrorPayloadException(new DriverInternalError());
      }
      String state = null;
      for(Map<String, Object> prop : properties) {
         AlexaPropertyReport propRep = new AlexaPropertyReport(prop);
         if(AlexaInterfaces.LockController.PROP_LOCKSTATE.equals(propRep.getName())) {
            state = (String) propRep.getValue();
            break;
         }
      }

      if(state == null) {
         throw new ErrorPayloadException(new DriverInternalError());
      }

      if(AlexaInterfaces.LockController.STATE_JAMMED.equals(state)) {
         if(set) {
            throw new ErrorPayloadException(new UnableToSetValueError(JAMMED_CODE, JAMMED_MSG));
         }
         throw new ErrorPayloadException(new UnableToGetValueError(JAMMED_CODE, JAMMED_MSG));
      }

      return state;
   }

   //-------------------------------------------------------------------------------------------------------------------
   // Thermostat Helpers
   // These rely on the fact that all three of the methods have the same return attributes.
   //-------------------------------------------------------------------------------------------------------------------

   @SuppressWarnings("unchecked")
   private static <T extends TemperatureConfirmationPayload> T thermostatResponse(MessageBody response, Supplier<T> factory) {
      Map<String, Object> payload = (Map<String, Object>) AlexaService.ExecuteResponse.getPayload(response);

      Boolean result = (Boolean) payload.get(ThermostatCapability.SetIdealTemperatureResponse.ATTR_RESULT);
      String mode = (String) payload.get(ThermostatCapability.SetIdealTemperatureResponse.ATTR_HVACMODE);
      if(Boolean.FALSE.equals(result)) {

         if(ThermostatCapability.HVACMODE_OFF.equals(mode)) {
            throw new ErrorPayloadException(new UnwillingToSetValueError("ThermostatIsOff", "The requested operation is unsafe because it requires changing the mode."));
         }

         throw new ErrorPayloadException(
            new ValueOutOfRangeError(
               Precision.round((Double) payload.get(ThermostatCapability.SetIdealTemperatureResponse.ATTR_MINSETPOINT), 2),
               Precision.round((Double) payload.get(ThermostatCapability.SetIdealTemperatureResponse.ATTR_MAXSETPOINT), 2)
            )
         );
      }

      Double prevIdeal = ((Number) payload.get(ThermostatCapability.SetIdealTemperatureResponse.ATTR_PREVIDEALTEMP)).doubleValue();
      Double idealTempSet = ((Number) payload.get(ThermostatCapability.SetIdealTemperatureResponse.ATTR_IDEALTEMPSET)).doubleValue();

      TemperatureConfirmationPayload.PreviousState prevState = new TemperatureConfirmationPayload.PreviousState();
      prevState.setMode(new StringValue(mode));
      prevState.setTargetTemperature(new DoubleValue(prevIdeal));
      T confirm = factory.get();
      confirm.setTargetTemperature(new DoubleValue(idealTempSet));
      confirm.setTemperatureMode(prevState.getMode());
      confirm.setPreviousState(prevState);
      return confirm;
   }

   //-------------------------------------------------------------------------------------------------------------------
   // Color Temperature Helpers
   //-------------------------------------------------------------------------------------------------------------------

   private static <T extends ColorTemperatureConfirmationPayload> T colorTempResponse(MessageBody response, Supplier<T> factory) {
      List<Map<String, Object>> properties = AlexaService.ExecuteResponse.getProperties(response);
      if(properties == null) {
         throw new ErrorPayloadException(new DriverInternalError());
      }
      Number value = null;
      for(Map<String, Object> prop : properties) {
         AlexaPropertyReport propRep = new AlexaPropertyReport(prop);
         if(AlexaInterfaces.ColorTemperatureController.PROP_COLORTEMPERATUREINKELVIN.equals(propRep.getName())) {
            value = (Number) propRep.getValue();
            break;
         }
      }
      if(value == null) {
         throw new ErrorPayloadException(new DriverInternalError());
      }
      T confirmation = factory.get();
      confirmation.setColorTemperature(new IntValue(value.intValue()));
      return confirmation;
   }

   //-------------------------------------------------------------------------------------------------------------------
   // Discover Helpers
   //-------------------------------------------------------------------------------------------------------------------

   private static Appliance applianceFromEndpoint(AlexaEndpoint endpoint) {
      Appliance a = new Appliance();
      a.setActions(actions(endpoint.getCapabilities()));
      a.setAdditionalApplianceDetails(endpoint.getCookie());
      a.setApplianceId(endpoint.getEndpointId());
      a.setFriendlyDescription(endpoint.getDescription());
      a.setFriendlyName(endpoint.getFriendlyName());
      a.setReachable(endpoint.getOnline());
      a.setManufacturerName(endpoint.getManufacturerName());
      a.setModelName(endpoint.getModel());
      a.setVersion("1");
      a.setApplianceTypes(applianceTypes(endpoint.getDisplayCategories()));
      return a;
   }

   private static final Map<String, Set<String>> actionMap = ImmutableMap.<String, Set<String>>builder()
      .put(
         AlexaInterfaces.BrightnessController.NAMESPACE,
         ImmutableSet.of("setPercentage", "incrementPercentage", "decrementPercentage")
      )
      .put(
         AlexaInterfaces.ColorController.NAMESPACE,
         ImmutableSet.of("setColor")
      )
      .put(
         AlexaInterfaces.ColorTemperatureController.NAMESPACE,
         ImmutableSet.of("setColorTemperature", "incrementColorTemperature", "decrementColorTemperature")
      )
      .put(
         AlexaInterfaces.LockController.NAMESPACE, ImmutableSet.of("getLockState", "setLockState")
      )
      .put(
         AlexaInterfaces.PercentageController.NAMESPACE,
         ImmutableSet.of("setPercentage", "incrementPercentage", "decrementPercentage")
      )
      .put(
         AlexaInterfaces.PowerController.NAMESPACE,
         ImmutableSet.of("turnOn", "turnOff")
      )
      .put(
         AlexaInterfaces.SceneController.NAMESPACE,
         ImmutableSet.of("turnOn")
      )
      .put(
         AlexaInterfaces.TemperatureSensor.NAMESPACE,
         ImmutableSet.of("getTemperatureReading")
      )
      .put(
         AlexaInterfaces.ThermostatController.NAMESPACE,
         ImmutableSet.of("setTargetTemperature", "incrementTargetTemperature", "decrementTargetTemperature", "getTargetTemperature")
      )
      .build();

   private static Set<String> actions(List<Map<String, Object>> v3Capabilities) {
      Set<String> actions = new HashSet<>();
      v3Capabilities.forEach(m -> {
         String iface = (String) m.get(AlexaCapability.ATTR_INTERFACE);
         if(iface != null) {
            actions.addAll(actionMap.getOrDefault(iface, ImmutableSet.of()));

         }
      });
      return actions;
   }

   private static Set<Appliance.Type> applianceTypes(Set<String> v3Categories) {
      return v3Categories.stream()
         .map(Appliance.Type::valueOf)
         .collect(Collectors.toSet());
   }

   //-------------------------------------------------------------------------------------------------------------------
   // DiscoverAppliance Transformer
   //-------------------------------------------------------------------------------------------------------------------

   private static final Txfm discoverAppliances = new Txfm() {
      @Override
      MessageBody txfmRequestBody(AlexaMessage message) {
         return AlexaService.DiscoverRequest.instance();
      }
   };

   //-------------------------------------------------------------------------------------------------------------------
   // Fallback that covers the majority of requests
   //-------------------------------------------------------------------------------------------------------------------

   private static final Txfm fallback = new Txfm();

   //-------------------------------------------------------------------------------------------------------------------
   // Factory Method
   //-------------------------------------------------------------------------------------------------------------------

   static Txfm transformerFor(AlexaMessage message) {
      if(DiscoverAppliancesRequest.class.getSimpleName().equals(message.getHeader().getName())) {
         return discoverAppliances;
      }
      if(payloads.containsKey(message.getHeader().getName())) {
         return fallback;
      }
      ShsMetrics.incInvalidDirective();
      throw new ErrorPayloadException(new UnsupportedOperationError());
   }
}

