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
package com.iris.voice.alexa.handlers;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.Precision;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.AlexaUtil;
import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.bootstrap.ServiceLocator;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ColorCapability;
import com.iris.messages.capability.ColorTemperatureCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.LightCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DimmerModel;
import com.iris.messages.model.dev.DoorLockModel;
import com.iris.messages.model.dev.FanModel;
import com.iris.messages.model.dev.LightModel;
import com.iris.messages.model.dev.SwitchModel;
import com.iris.messages.model.dev.ThermostatModel;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaCause;
import com.iris.messages.type.AlexaColor;
import com.iris.messages.type.AlexaTemperature;
import com.iris.messages.type.AlexaThermostatMode;
import com.iris.platform.util.LazyReference;
import com.iris.voice.VoicePredicates;
import com.iris.voice.alexa.AlexaConfig;
import com.iris.voice.alexa.AlexaPredicates;

enum DirectiveTransformer {
   ;

   private static final String ARG_DELTATEMPERATURE = "deltaTemperature";

   interface Txfm {

      default Optional<MessageBody> txfmRequest(MessageBody req, Model curState, AlexaConfig config) {
         Map<String, Object> attributes = attributes(req, curState, config);
         if(attributes == null || attributes.isEmpty()) {
            return Optional.empty();
         }
         return Optional.of(MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attributes));
      }

      default Map<String, Object> txfmResponse(Model curState, @Nullable PlatformMessage respMsg, AlexaConfig config) {
         return ImmutableMap.of();
      }

      Map<String, Object> attributes(MessageBody req, Model curState, AlexaConfig config);

      default Set<String> completableDevAdvErrors() {
         return ImmutableSet.of();
      }

      default boolean allowCheat() {
         return true;
      }

      default boolean updateModelOnCheat() {
         return true;
      }

      default boolean deferred() {
         return false;
      }

   }

   private static final Map<String, Txfm> transformers = ImmutableMap.<String, Txfm>builder()

      //----------------------------------------------------------------------------------------------------------------
      // turn on
      .put(AlexaInterfaces.PowerController.REQUEST_TURNON, (req, curState, config) -> {
         if(SwitchModel.isStateON(curState)) {
            return ImmutableMap.of();
         }

         ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
            .put(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);

         // turning on a dimmer is supposed to go to 100% brightness for non-bulb dimmers
         if(curState.supports(DimmerCapability.NAMESPACE) && !curState.supports(LightCapability.NAMESPACE)) {
            int brightness = AlexaUtil.getFromModelOrInternalError(curState, DimmerCapability.ATTR_BRIGHTNESS, DimmerCapability.TYPE_BRIGHTNESS);
            if(brightness != 100) {
               builder.put(DimmerCapability.ATTR_BRIGHTNESS, 100);
            }
         }

         return builder.build();
      })

      //----------------------------------------------------------------------------------------------------------------
      // turn off
      .put(AlexaInterfaces.PowerController.REQUEST_TURNOFF, (req, curState, config) -> {
         if(SwitchModel.isStateOFF(curState)) {
            return ImmutableMap.of();
         }
         return ImmutableMap.of(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      })
      //----------------------------------------------------------------------------------------------------------------
      // adjust brightness
      .put(AlexaInterfaces.BrightnessController.REQUEST_ADJUSTBRIGHTNESS, (req, curState, config) -> {
         Map<String, Object> args = AlexaService.ExecuteRequest.getArguments(req);
         if(args == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.BrightnessController.ARG_BRIGHTNESSDELTA));
         }
         Number n = (Number) args.get(AlexaInterfaces.BrightnessController.ARG_BRIGHTNESSDELTA);
         if(n == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.BrightnessController.ARG_BRIGHTNESSDELTA));
         }
         int delta = n.intValue();
         if(delta < -100 || delta > 100) {
            throw new AlexaException(AlexaErrors.valueOutOfRange(AlexaInterfaces.BrightnessController.ARG_BRIGHTNESSDELTA, delta, -100, 100));
         }
         int brightness = AlexaUtil.getFromModelOrInternalError(curState, DimmerCapability.ATTR_BRIGHTNESS, DimmerCapability.TYPE_BRIGHTNESS);

         // alexa expects that increasing the brightness of a dimmer that is off will increment from 0
         if(SwitchModel.isStateOFF(curState)) {
            // if the dimming then this is a no op
            if(delta < 0) {
               return ImmutableMap.of();
            }
            brightness = 0;
         }
         int targetBrightness = brightness + delta;

         if(targetBrightness < 0) {
            targetBrightness = 0;
         } else if(targetBrightness > 100) {
            targetBrightness = 100;
         }
         return brightnessSetAttr(targetBrightness, curState);
      })
      //----------------------------------------------------------------------------------------------------------------
      // set brightness
      .put(AlexaInterfaces.BrightnessController.REQUEST_SETBRIGHTNESS, (req, curState, config) -> {
         Map<String, Object> args = AlexaService.ExecuteRequest.getArguments(req);
         if(args == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.BrightnessController.PROP_BRIGHTNESS));
         }
         Number n = (Number) args.get(AlexaInterfaces.BrightnessController.PROP_BRIGHTNESS);
         if(n == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.BrightnessController.PROP_BRIGHTNESS));
         }
         int brightness = n.intValue();
         if(brightness < 0 || brightness > 100) {
            throw new AlexaException(AlexaErrors.valueOutOfRange(AlexaInterfaces.BrightnessController.PROP_BRIGHTNESS, brightness, 0, 100));
         }
         return brightnessSetAttr(brightness, curState);
      })
      //----------------------------------------------------------------------------------------------------------------
      // set color
      .put(AlexaInterfaces.ColorController.REQUEST_SETCOLOR, (req, curState, config) -> {
         @SuppressWarnings("unchecked")
         Map<String, Object> args = AlexaService.ExecuteRequest.getArguments(req);
         if(args == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.ColorController.PROP_COLOR));
         }
         Map<String, Object> color = (Map<String, Object>) args.get(AlexaInterfaces.ColorController.PROP_COLOR);
         if(color == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.ColorController.PROP_COLOR));
         }
         AlexaColor target = new AlexaColor(color);

         // per the alexa test cases the brightness should not be adjusted if different from the current device settings
         // so just ignore it.

         if(target.getSaturation() == null) {
            throw new AlexaException(AlexaErrors.invalidDirective("Color property is missing saturation."));
         }
         if(target.getSaturation() < 0 || target.getSaturation() > 1.0) {
            throw new AlexaException(AlexaErrors.valueOutOfRange("color.saturation", target.getSaturation(), 0.0, 1.0));
         }
         if(target.getHue() == null) {
            throw new AlexaException(AlexaErrors.invalidDirective("Color property is missing hue."));
         }
         if(target.getHue() < 0 || target.getHue() > 360.0) {
            throw new AlexaException(AlexaErrors.valueOutOfRange("color.hue", target.getHue(), 0.0, 360.0));
         }

         int h = (int) Math.round(target.getHue());
         int s = (int) Math.round(target.getSaturation() * 100d);

         ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
         if(SwitchModel.isStateOFF(curState)) {
            builder.put(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
         }
         if(!LightModel.isColormodeCOLOR(curState)) {
            builder.put(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLOR);
         }
         int curHue = AlexaUtil.getFromModelOrInternalError(curState, ColorCapability.ATTR_HUE, ColorCapability.TYPE_HUE);
         if(!Objects.equals(h, curHue)) {
            builder.put(ColorCapability.ATTR_HUE, h);
         }
         int curSat = AlexaUtil.getFromModelOrInternalError(curState, ColorCapability.ATTR_SATURATION, ColorCapability.TYPE_SATURATION);
         if(!Objects.equals(s, curSat)) {
            builder.put(ColorCapability.ATTR_SATURATION, s);
         }

         return builder.build();
      })

      //----------------------------------------------------------------------------------------------------------------
      // decrease color temperature
      .put(AlexaInterfaces.ColorTemperatureController.REQUEST_DECREASECOLORTEMPERATURE, (req, curState, config) -> adjustColorTemp(curState, config, -1))

      //----------------------------------------------------------------------------------------------------------------
      // increase color temperature
      .put(AlexaInterfaces.ColorTemperatureController.REQUEST_INCREASECOLORTEMPERATURE, (req, curState, config) -> adjustColorTemp(curState, config, 1))

      //----------------------------------------------------------------------------------------------------------------
      // set color temperature
      .put(AlexaInterfaces.ColorTemperatureController.REQUEST_SETCOLORTEMPERATURE, (req, curState, config) -> {
         Map<String, Object> args = AlexaService.ExecuteRequest.getArguments(req);
         if(args == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.ColorTemperatureController.PROP_COLORTEMPERATUREINKELVIN));
         }
         Number n = (Number) args.get(AlexaInterfaces.ColorTemperatureController.PROP_COLORTEMPERATUREINKELVIN);
         if(n == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.ColorTemperatureController.PROP_COLORTEMPERATUREINKELVIN));
         }
         int targetTemp = n.intValue();
         int minTemp = AlexaUtil.getFromModelOrInternalError(curState, ColorTemperatureCapability.ATTR_MINCOLORTEMP, ColorTemperatureCapability.TYPE_MINCOLORTEMP);
         int maxTemp = AlexaUtil.getFromModelOrInternalError(curState, ColorTemperatureCapability.ATTR_MAXCOLORTEMP, ColorTemperatureCapability.TYPE_MAXCOLORTEMP);

         // per the alexa api, if a temperature isn't supported we should set to the nearest values
         if(targetTemp < minTemp) {
            targetTemp = minTemp;
         }
         if(targetTemp > maxTemp) {
            targetTemp = maxTemp;
         }

         return colorTempSetAttr(targetTemp, curState);
      })
      //----------------------------------------------------------------------------------------------------------------
      // set target temperature
      .put(AlexaInterfaces.ThermostatController.REQUEST_SETTARGETTEMPERATURE, new Txfm() {

         private Map<String, Object> handleTargetSp(AlexaTemperature temp, Model curState) {
            double target = normalizeTemp(temp);
            validateMinMax(curState, target);

            switch(ThermostatModel.getHvacmode(curState)) {
               case ThermostatCapability.HVACMODE_AUTO:
                  Pair<Double, Double> pair = calculateIdealRange(curState, target);
                  return thermostatSetAttr(pair.getRight(), pair.getLeft(), curState);
               case ThermostatCapability.HVACMODE_COOL:
                  return thermostatSetAttr(target, null, curState);
               case ThermostatCapability.HVACMODE_HEAT:
                  return thermostatSetAttr(null, target, curState);
               default:
                  return ImmutableMap.of();
            }
         }

         @Override
         public Map<String, Object> attributes(MessageBody req, Model curState, AlexaConfig config) {
            if(ThermostatModel.isHvacmodeOFF(curState)) {
               throw new AlexaException(AlexaErrors.THERMOSTAT_IS_OFF);
            }
            if(ThermostatModel.isHvacmodeECO(curState)) {
               throw new AlexaException(AlexaErrors.notSupportedInCurrentMode("OTHER"));
            }

            AlexaTemperature target = getTemp(req, AlexaInterfaces.ThermostatController.PROP_TARGETSETPOINT);
            if(target != null) {
               return handleTargetSp(target, curState);
            }

            if(!ThermostatModel.isHvacmodeAUTO(curState)) {
               throw new AlexaException(AlexaErrors.dualSetpointsUnsupported(ThermostatModel.getHvacmode(curState)));
            }

            AlexaTemperature lowerSp = getTemp(req, AlexaInterfaces.ThermostatController.PROP_LOWERSETPOINT);
            if(lowerSp == null) {
               throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.ThermostatController.PROP_LOWERSETPOINT));
            }
            AlexaTemperature upperSp = getTemp(req, AlexaInterfaces.ThermostatController.PROP_UPPERSETPOINT);
            if(upperSp == null) {
               throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.ThermostatController.PROP_UPPERSETPOINT));
            }
            return thermostatSetAttr(normalizeTemp(upperSp), normalizeTemp(lowerSp), curState);
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // adjust target temperature
      .put(AlexaInterfaces.ThermostatController.REQUEST_ADJUSTTARGETTEMPERATURE, (req, curState, config) -> {
         AlexaTemperature delta = getTemp(req, AlexaInterfaces.ThermostatController.ARG_TARGETSETPOINTDELTA);
         if(delta == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.ThermostatController.ARG_TARGETSETPOINTDELTA));
         }
         if(ThermostatModel.isHvacmodeOFF(curState)) {
            throw new AlexaException(AlexaErrors.THERMOSTAT_IS_OFF);
         }
         if(ThermostatModel.isHvacmodeECO(curState)) {
            throw new AlexaException(AlexaErrors.notSupportedInCurrentMode("OTHER"));
         }

         double curCool = AlexaUtil.getFromModelOrInternalError(curState, ThermostatCapability.ATTR_COOLSETPOINT, ThermostatCapability.TYPE_COOLSETPOINT);
         double curHeat = AlexaUtil.getFromModelOrInternalError(curState, ThermostatCapability.ATTR_HEATSETPOINT, ThermostatCapability.TYPE_HEATSETPOINT);
         double curTarget = (curCool + curHeat) / 2;

         double normalizedDelta = normalizeAdjustment(delta);
         double targetCool = curCool + normalizedDelta;
         double targetHeat = curHeat + normalizedDelta;
         double target = curTarget + normalizedDelta;

         switch(ThermostatModel.getHvacmode(curState)) {
            case ThermostatCapability.HVACMODE_COOL:
               return thermostatSetAttr(targetCool, null, curState);
            case ThermostatCapability.HVACMODE_HEAT:
               return thermostatSetAttr(null, targetHeat, curState);
            default:
               validateMinMax(curState, target);
               Pair<Double, Double> pair = calculateIdealRange(curState, target);
               return thermostatSetAttr(pair.getRight(), pair.getLeft(), curState);
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // set thermostat mode
      .put(AlexaInterfaces.ThermostatController.REQUEST_SETTHERMOSTATMODE, (req, curState, config) -> {
         Map<String, Object> args = AlexaService.ExecuteRequest.getArguments(req);
         if(args == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE));
         }
         @SuppressWarnings("unchecked")
         Map<String, Object> modeMap = (Map<String, Object>) args.get(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE);
         if(modeMap == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE));
         }
         AlexaThermostatMode mode = new AlexaThermostatMode(modeMap);

         Set<String> supportedModes = AlexaUtil.getFromModelOrInternalError(curState, ThermostatCapability.ATTR_SUPPORTEDMODES, ThermostatCapability.TYPE_SUPPORTEDMODES);
         if(!supportedModes.contains(mode.getValue())) {
            throw new AlexaException(AlexaErrors.unsupportedThermostatMode(mode.getValue()));
         }

         String curMode = AlexaUtil.getFromModelOrInternalError(curState, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.TYPE_HVACMODE);
         if(Objects.equals(mode.getValue(), curMode)) {
            return ImmutableMap.of();
         }
         return ImmutableMap.of(ThermostatCapability.ATTR_HVACMODE, mode.getValue());
      })
      //----------------------------------------------------------------------------------------------------------------
      // activate
      .put(AlexaInterfaces.SceneController.REQUEST_ACTIVATE, new Txfm() {
         @Override
         public Optional<MessageBody> txfmRequest(MessageBody req, Model curState, AlexaConfig config) {
            // null is ok here for the model because this is a scene
            if(!AlexaPredicates.supported(curState, null)) {
               throw new AlexaException(AlexaErrors.INVALID_VALUE_DEFAULT);
            }
            return Optional.of(SceneCapability.FireRequest.instance());
         }

         @Override
         public Map<String, Object> txfmResponse(Model curState, @Nullable PlatformMessage respMsg, AlexaConfig config) {
            AlexaCause cause = new AlexaCause();
            cause.setType(AlexaCause.TYPE_VOICE_INTERACTION);
            return ImmutableMap.of(
               "cause", cause.toMap(),
               "timestamp", ZonedDateTime.now(ZoneOffset.UTC).toString()
            );
         }

         @Override
         public Map<String, Object> attributes(MessageBody req, Model curState, AlexaConfig config) {
            return ImmutableMap.of();
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // lock
      .put(AlexaInterfaces.LockController.REQUEST_LOCK, new Txfm() {
         @Override
         public Map<String, Object> attributes(MessageBody req, Model curState, AlexaConfig config) {
            if(VoicePredicates.isLockJammed(curState)) {
               return ImmutableMap.of();
            }
            if(DoorLockModel.isLockstateLOCKING(curState) || DoorLockModel.isLockstateUNLOCKING(curState)) {
               throw new AlexaException(AlexaErrors.ENDPOINT_BUSY);
            }

            if(!DoorLockModel.isLockstateLOCKED(curState)) {
               return ImmutableMap.of(DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_LOCKED);
            }
            return ImmutableMap.of();
         }

         @Override
         public Set<String> completableDevAdvErrors() {
            return ImmutableSet.of(VoicePredicates.DEVADVERR_JAMMED);
         }

         @Override
         public boolean allowCheat() {
            return false;
         }

         @Override
         public boolean deferred() {
            return lockDeferredEnabled.get();
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // unlock
      .put(AlexaInterfaces.LockController.REQUEST_UNLOCK, new Txfm() {
         @Override
         public Map<String, Object> attributes(MessageBody req, Model curState, AlexaConfig config) {
            if(VoicePredicates.isLockJammed(curState)) {
               return ImmutableMap.of();
            }
            if(DoorLockModel.isLockstateLOCKING(curState) || DoorLockModel.isLockstateUNLOCKING(curState)) {
               throw new AlexaException(AlexaErrors.ENDPOINT_BUSY);
            }
            if(!DoorLockModel.isLockstateUNLOCKED(curState)) {
               return ImmutableMap.of(DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_UNLOCKED);
            }
            return ImmutableMap.of();
         }

         @Override
         public Set<String> completableDevAdvErrors() {
            return ImmutableSet.of(VoicePredicates.DEVADVERR_JAMMED);
         }

         @Override
         public boolean allowCheat() {
            return false;
         }

         @Override
         public boolean deferred() {
            return lockDeferredEnabled.get();
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // set percentage
      .put(AlexaInterfaces.PercentageController.REQUEST_SETPERCENTAGE, (req, curState, config) -> {
         Map<String, Object> args = AlexaService.ExecuteRequest.getArguments(req);
         if(args == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.PercentageController.PROP_PERCENTAGE));
         }
         Number n = (Number) args.get(AlexaInterfaces.PercentageController.PROP_PERCENTAGE);
         if(n == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.PercentageController.PROP_PERCENTAGE));
         }
         int target = n.intValue();
         if(target < 0 || target > 100) {
            throw new AlexaException(AlexaErrors.valueOutOfRange(AlexaInterfaces.PercentageController.PROP_PERCENTAGE, target, 0, 100));
         }
         int max = FanModel.getMaxSpeed(curState, 3);
         int speed = ceilFanSpeed(max, target);
         return fanSetAttr(speed, curState);
      })
      //----------------------------------------------------------------------------------------------------------------
      // adjust percentage
      .put(AlexaInterfaces.PercentageController.REQUEST_ADJUSTPERCENTAGE, (req, curState, config) -> {
         Map<String, Object> args = AlexaService.ExecuteRequest.getArguments(req);
         if(args == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.PercentageController.ARG_PERCENTAGEDELTA));
         }
         Number n = (Number) AlexaService.ExecuteRequest.getArguments(req).get(AlexaInterfaces.PercentageController.ARG_PERCENTAGEDELTA);
         if(n == null) {
            throw new AlexaException(AlexaErrors.missingArgument(AlexaInterfaces.PercentageController.ARG_PERCENTAGEDELTA));
         }
         int delta = n.intValue();
         if(delta < -100 || delta > 100) {
            throw new AlexaException(AlexaErrors.valueOutOfRange(AlexaInterfaces.PercentageController.ARG_PERCENTAGEDELTA, delta, -100, 100));
         }
         // alexa expects that increasing the percentage of a device that is off will increment from 0
         if(SwitchModel.isStateOFF(curState)) {
            // if the decreasing then this is a no op
            if(delta < 0) {
               return ImmutableMap.of();
            }
         }

         int max = FanModel.getMaxSpeed(curState, 3);
         int curSpeed = 0;
         if(SwitchModel.isStateON(curState)) {
            curSpeed = AlexaUtil.getFromModelOrInternalError(curState, FanCapability.ATTR_SPEED, FanCapability.TYPE_SPEED);
         }
         double interval = ((double) 100) / max;
         double percentage = (interval * curSpeed) + delta;
         int speed = delta >= 0 ? ceilFanSpeed(max, percentage) : floorFanSpeed(max, percentage);
         return fanSetAttr(speed, curState);
      })
      //----------------------------------------------------------------------------------------------------------------
      // report state
      .put(AlexaInterfaces.REQUEST_REPORTSTATE, new Txfm() {
         @Override
         public Optional<MessageBody> txfmRequest(MessageBody req, Model curState, AlexaConfig config) {
            return Optional.empty();
         }

         @Override
         public Map<String, Object> attributes(MessageBody req, Model curState, AlexaConfig config) {
            return ImmutableMap.of();
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // v2 set target temperature
      .put("SetTargetTemperatureRequest", new Txfm() {

         private static final String ARG_TARGETTEMPERATURE = "targetTemperature";

         @Override
         public Optional<MessageBody> txfmRequest(MessageBody req, Model curState, AlexaConfig config) {
            Map<String, Object> args = AlexaService.ExecuteRequest.getArguments(req);
            if(args == null) {
               throw new AlexaException(AlexaErrors.missingArgument(ARG_TARGETTEMPERATURE));
            }
            Number n = (Number) args.get(ARG_TARGETTEMPERATURE);
            if(n == null) {
               throw new AlexaException(AlexaErrors.missingArgument(ARG_TARGETTEMPERATURE));
            }
            return Optional.of(
               ThermostatCapability.SetIdealTemperatureRequest.builder()
                  .withTemperature(n.doubleValue())
                  .build()
            );
         }

         @Override
         public Map<String, Object> attributes(MessageBody req, Model curState, AlexaConfig config) {
            return ImmutableMap.of();
         }

         @Override
         public Map<String, Object> txfmResponse(Model curState, @Nullable PlatformMessage respMsg, AlexaConfig config) {
            if(respMsg == null) {
               throw new AlexaException(AlexaErrors.INTERNAL_ERROR);
            }
            return respMsg.getValue().getAttributes();
         }

         @Override
         public boolean updateModelOnCheat() {
            return false;
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // v2 increment target temperature
      .put("IncrementTargetTemperatureRequest", new Txfm() {
         @Override
         public Optional<MessageBody> txfmRequest(MessageBody req, Model curState, AlexaConfig config) {
            Map<String, Object> args = AlexaService.ExecuteRequest.getArguments(req);
            if(args == null) {
               throw new AlexaException(AlexaErrors.missingArgument(ARG_DELTATEMPERATURE));
            }
            Number n = (Number) args.get(ARG_DELTATEMPERATURE);
            if(n == null) {
               throw new AlexaException(AlexaErrors.missingArgument(ARG_DELTATEMPERATURE));
            }
            double delta = n.doubleValue();
            return Optional.of(
               ThermostatCapability.IncrementIdealTemperatureRequest.builder()
                  .withAmount(delta)
                  .build()
            );
         }

         @Override
         public Map<String, Object> attributes(MessageBody req, Model curState, AlexaConfig config) {
            return ImmutableMap.of();
         }

         @Override
         public Map<String, Object> txfmResponse(Model curState, @Nullable PlatformMessage respMsg, AlexaConfig config) {
            if(respMsg == null) {
               throw new AlexaException(AlexaErrors.INTERNAL_ERROR);
            }
            return respMsg.getValue().getAttributes();
         }

         @Override
         public boolean updateModelOnCheat() {
            return false;
         }
      })
      //----------------------------------------------------------------------------------------------------------------
      // v2 decrement target temperature
      .put("DecrementTargetTemperatureRequest", new Txfm() {
         @Override
         public Optional<MessageBody> txfmRequest(MessageBody req, Model curState, AlexaConfig config) {
            Map<String, Object> args = AlexaService.ExecuteRequest.getArguments(req);
            if(args == null) {
               throw new AlexaException(AlexaErrors.missingArgument(ARG_DELTATEMPERATURE));
            }
            Number n = (Number) args.get(ARG_DELTATEMPERATURE);
            if(n == null) {
               throw new AlexaException(AlexaErrors.missingArgument(ARG_DELTATEMPERATURE));
            }
            double delta = n.doubleValue();
            return Optional.of(
               ThermostatCapability.DecrementIdealTemperatureRequest.builder()
                  .withAmount(delta)
                  .build()
            );
         }

         @Override
         public Map<String, Object> attributes(MessageBody req, Model curState, AlexaConfig config) {
            return ImmutableMap.of();
         }

         @Override
         public Map<String, Object> txfmResponse(Model curState, @Nullable PlatformMessage respMsg, AlexaConfig config) {
            if(respMsg == null) {
               throw new AlexaException(AlexaErrors.INTERNAL_ERROR);
            }
            return respMsg.getValue().getAttributes();
         }

         @Override
         public boolean updateModelOnCheat() {
            return false;
         }
      })
      .build();

   //-------------------------------------------------------------------------------------------------------------------
   // Factory Method
   //-------------------------------------------------------------------------------------------------------------------

   static Txfm transformerFor(MessageBody req) {
      String directive = AlexaService.ExecuteRequest.getDirective(req);
      Txfm txfm = transformers.get(directive);
      if(txfm == null) {
         throw new AlexaException(AlexaErrors.invalidDirective(directive + " is not supported"));
      }
      return txfm;
   }

   //-------------------------------------------------------------------------------------------------------------------
   // Lock Util
   //-------------------------------------------------------------------------------------------------------------------

   // Lazy load the configuration for whether or not lock's use the deferred mechanism from the service locator.
   private static final LazyReference<Boolean> lockDeferredEnabled = new LazyReference<Boolean>() {
      @Override
      protected Boolean load() {
         return ServiceLocator.getInstance(AlexaConfig.class).isLockDeferredEnabled();
      }
   };

   //-------------------------------------------------------------------------------------------------------------------
   // Brightness Util
   //-------------------------------------------------------------------------------------------------------------------

   private static Map<String, Object> brightnessSetAttr(int targetBrightness, Model curState) {
      ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
      if(targetBrightness == 0) {
         if(!SwitchModel.isStateOFF(curState)) {
            builder.put(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
         }
         // bail out here because drivers won't set the brightness and thus alignment based on expecting brightness
         // to appear in value changes will fail.
         return builder.build();
      }
      if(targetBrightness > 0 && !SwitchModel.isStateON(curState)) {
         builder.put(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      }

      if(!Objects.equals(targetBrightness, DimmerModel.getBrightness(curState))) {
         builder.put(DimmerCapability.ATTR_BRIGHTNESS, targetBrightness);
      }
      return builder.build();
   }

   //-------------------------------------------------------------------------------------------------------------------
   // ColorTemp Util
   //-------------------------------------------------------------------------------------------------------------------

   private static Map<String, Object> adjustColorTemp(Model curState, AlexaConfig config, int multiplier) {
      if(!LightModel.isColormodeCOLORTEMP(curState)) {
         throw new AlexaException(AlexaErrors.notSupportedInCurrentMode(LightModel.isColormodeCOLOR(curState) ? "COLOR" : "OTHER"));
      }
      int curTemp = AlexaUtil.getFromModelOrInternalError(curState, ColorTemperatureCapability.ATTR_COLORTEMP, ColorTemperatureCapability.TYPE_COLORTEMP);
      return colorTempSetAttr(curTemp + (multiplier * config.getColorTempDelta()), curState);
   }

   private static Map<String, Object> colorTempSetAttr(int targetTemp, Model curState) {
      ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
      int minColor = AlexaUtil.getFromModelOrInternalError(curState, ColorTemperatureCapability.ATTR_MINCOLORTEMP, ColorTemperatureCapability.TYPE_MINCOLORTEMP);
      int maxColor = AlexaUtil.getFromModelOrInternalError(curState, ColorTemperatureCapability.ATTR_MAXCOLORTEMP, ColorTemperatureCapability.TYPE_MAXCOLORTEMP);
      int curTemp = AlexaUtil.getFromModelOrInternalError(curState, ColorTemperatureCapability.ATTR_COLORTEMP, ColorTemperatureCapability.TYPE_COLORTEMP);
      if(targetTemp < minColor) {
         targetTemp = minColor;
      } else if(targetTemp > maxColor) {
         targetTemp = maxColor;
      }
      if(targetTemp != curTemp) {
         builder.put(ColorTemperatureCapability.ATTR_COLORTEMP, targetTemp);
      }
      if(!LightModel.isColormodeCOLORTEMP(curState)) {
         builder.put(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLORTEMP);
      }
      if(!SwitchModel.isStateON(curState)) {
         builder.put(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      }
      return builder.build();
   }

   //-------------------------------------------------------------------------------------------------------------------
   // Thermostat Util
   //-------------------------------------------------------------------------------------------------------------------

   private static final double MIN_GAP = 3.0 * (5.0/9.0);

   @Nullable
   @SuppressWarnings("unchecked")
   private static AlexaTemperature getTemp(MessageBody req, String name) {
      Map<String, Object> args = AlexaService.ExecuteRequest.getArguments(req);
      if(args == null) {
         throw new AlexaException(AlexaErrors.missingArgument(name));
      }
      Map<String, Object> temp = (Map<String, Object>) args.get(name);
      return temp == null ? null : new AlexaTemperature(temp);
   }

   private static Map<String, Object> thermostatSetAttr(Double targetCool, Double targetHeat, Model curState) {
      double cool = AlexaUtil.getFromModelOrInternalError(curState, ThermostatCapability.ATTR_COOLSETPOINT, ThermostatCapability.TYPE_COOLSETPOINT);
      double heat = AlexaUtil.getFromModelOrInternalError(curState, ThermostatCapability.ATTR_HEATSETPOINT, ThermostatCapability.TYPE_HEATSETPOINT);
      ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

      if(targetCool != null && targetHeat != null && (targetCool - targetHeat) < MIN_GAP) {
         throw new AlexaException(AlexaErrors.requestedSetpointsTooClose(MIN_GAP));
      }

      if(targetCool != null) {
         validateMinMax(curState, targetCool);
         if(!tempsEqual(cool, targetCool)) {
            builder.put(ThermostatCapability.ATTR_COOLSETPOINT, targetCool);
         }
      }
      if(targetHeat != null) {
         validateMinMax(curState, targetHeat);
         if(!tempsEqual(heat, targetHeat)) {
            builder.put(ThermostatCapability.ATTR_HEATSETPOINT, targetHeat);
         }
      }
      return builder.build();
   }

   private static Pair<Double, Double> calculateIdealRange(Model m, Double target) {
      double minSetPoint = AlexaUtil.getFromModelOrInternalError(m, ThermostatCapability.ATTR_MINSETPOINT, ThermostatCapability.TYPE_MINSETPOINT);
      double maxSetPoint = AlexaUtil.getFromModelOrInternalError(m, ThermostatCapability.ATTR_MAXSETPOINT, ThermostatCapability.TYPE_MAXSETPOINT);
      double cool = AlexaUtil.getFromModelOrInternalError(m, ThermostatCapability.ATTR_COOLSETPOINT, ThermostatCapability.TYPE_COOLSETPOINT);
      double heat = AlexaUtil.getFromModelOrInternalError(m, ThermostatCapability.ATTR_HEATSETPOINT, ThermostatCapability.TYPE_HEATSETPOINT);
      double sep = MIN_GAP;
      if(cool - heat > MIN_GAP) {
         sep = cool - heat;
      }
      double newCool = target + (sep / 2);
      double newHeat = target - (sep / 2);
      if(newCool > maxSetPoint) {
         newCool = maxSetPoint;
         newHeat = newCool - sep;
      }
      if(newHeat < minSetPoint) {
         newHeat = minSetPoint;
         newCool = newHeat + sep;
      }
      return new ImmutablePair<>(newHeat, newCool);
   }

   private static double normalizeAdjustment(AlexaTemperature temp) {
      switch(temp.getScale()) {
         case AlexaTemperature.SCALE_FAHRENHEIT:
            return temp.getValue() * (5.0/9.0);
         default:
            return temp.getValue();
      }
   }

   private static boolean tempsEqual(double t1, double t2) {
      return Precision.equals(Precision.round(t1, 2), Precision.round(t2, 2));
   }

   private static double normalizeTemp(AlexaTemperature temp) {
      switch(temp.getScale()) {
         case AlexaTemperature.SCALE_FAHRENHEIT:
            return fToC(temp.getValue());
         default:
            return temp.getValue();
      }
   }

   private static double getMinSp(Model curState) {
      double minSetPoint = AlexaUtil.getFromModelOrInternalError(curState, ThermostatCapability.ATTR_MINSETPOINT, ThermostatCapability.TYPE_MINSETPOINT);
      String state = AlexaUtil.getFromModelOrInternalError(curState, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.TYPE_HVACMODE);
      switch(state) {
         case ThermostatCapability.HVACMODE_COOL:
            return minSetPoint + MIN_GAP;
         default:
            return minSetPoint;
      }
   }

   public static double getMaxSp(Model curState) {
      double maxSetPoint = AlexaUtil.getFromModelOrInternalError(curState, ThermostatCapability.ATTR_MAXSETPOINT, ThermostatCapability.TYPE_MAXSETPOINT);
      String state = AlexaUtil.getFromModelOrInternalError(curState, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.TYPE_HVACMODE);
      switch(state) {
         case ThermostatCapability.HVACMODE_HEAT:
            return maxSetPoint - MIN_GAP;
         default:
            return maxSetPoint;
      }
   }

   private static void validateMinMax(Model curState, double target) {
      double minSetPoint = Precision.round(getMinSp(curState), 2);
      double maxSetPoint = Precision.round(getMaxSp(curState), 2);
      double roundedTarget = Precision.round(target, 2);
      if(roundedTarget < minSetPoint || roundedTarget > maxSetPoint) {
         throw new AlexaException(AlexaErrors.temperatureValueOutOfRange(target, Precision.round(minSetPoint, 2), Precision.round(maxSetPoint, 2)));
      }
   }

   private static double fToC(double val) {
      return Precision.round((val-32.0) * (5.0/9.0), 2);
   }

   //-------------------------------------------------------------------------------------------------------------------
   // Fan Util
   //-------------------------------------------------------------------------------------------------------------------

   private static int ceilFanSpeed(int maxSpeed, double percentage) {
      double interval = ((double) 100) / maxSpeed;
      double level = percentage / interval;
      int speed = (int) Math.ceil(level);
      return speed > maxSpeed ? maxSpeed : speed;
   }

   private static int floorFanSpeed(int maxSpeed, double percentage) {
      double interval = ((double) 100) / maxSpeed;
      double level = percentage / interval;
      int speed = (int) Math.floor(level);
      return speed < 0 ? 0 : speed;
   }

   private static Map<String, Object> fanSetAttr(int targetSpeed, Model curState) {
      ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
      if(targetSpeed == 0) {
         if(!SwitchModel.isStateOFF(curState)) {
            builder.put(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
            return builder.build();
         }
         return ImmutableMap.of();
      }
      if(targetSpeed > 0 && !SwitchModel.isStateON(curState)) {
         builder.put(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      }
      int curSpeed = AlexaUtil.getFromModelOrInternalError(curState, FanCapability.ATTR_SPEED, FanCapability.TYPE_SPEED);
      if(!Objects.equals(targetSpeed, curSpeed)) {
         builder.put(FanCapability.ATTR_SPEED, targetSpeed);
      }
      return builder.build();
   }

}

