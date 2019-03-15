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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.iris.google.model.ExecutePayload;
import com.iris.google.model.QueryPayload;
import com.iris.google.model.Request;
import com.iris.google.model.Response;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ColorCapability;
import com.iris.messages.capability.ColorTemperatureCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.LightCapability;
import com.iris.messages.capability.RelativeHumidityCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.ColorModel;
import com.iris.messages.model.dev.ColorTemperatureModel;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.dev.DimmerModel;
import com.iris.messages.model.dev.LightModel;
import com.iris.messages.model.dev.RelativeHumidityModel;
import com.iris.messages.model.dev.SwitchModel;
import com.iris.messages.model.dev.TemperatureModel;
import com.iris.messages.model.dev.ThermostatModel;
import com.iris.messages.model.serv.SceneModel;
import com.iris.messages.service.GoogleService;
import com.iris.messages.type.GoogleColor;
import com.iris.messages.type.GoogleCommand;
import com.iris.messages.type.GoogleDevice;
import com.iris.messages.type.GoogleDeviceInfo;
import com.iris.messages.type.GoogleDeviceName;
import com.iris.prodcat.ProductCatalogEntry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Transformers {

   private Transformers() {}

   public static final Gson GSON = new GsonBuilder().create();

   public static final String PRODUCT_ID = "prodId";
   public static final String TYPE_HINT = "hint";

   public static Optional<GoogleDevice> modelToDevice(Model m, boolean whitelisted, @Nullable ProductCatalogEntry pm, boolean reportStateEnabled) {
      if(!Predicates.isSupportedModel(m, whitelisted, pm)) {
         return Optional.empty();
      }

      GoogleDevice dev = new GoogleDevice();
      modelToDeviceName(m).ifPresent((n) -> dev.setName(n.toMap()));
      dev.setId(m.getAddress().getRepresentation());
      modelToGoogleType(m).ifPresent(dev::setType);
      dev.setTraits(capabilitiesToTraits(m.getCapabilities()));
      dev.setAttributes(modelToTraitAttributes(m));
      modelToDeviceInfo(m).ifPresent((i) -> dev.setDeviceInfo(i.toMap()));
      dev.setCustomData(modelToCustomData(m));

      if(!reportStateEnabled || !StringUtils.isBlank(SceneModel.getName(m))) {
         dev.setWillReportState(false); // scenes don't have a state to update
      }
      else {
         dev.setWillReportState(true); // device states are updated via ReportState
      }

      if(dev.getName() == null || dev.getType() == null) {
         return Optional.empty();
      }

      return Optional.of(dev);
   }

   /**
    * Any new states added here will necessitate a change to GoogleProactiveReportHandler.interestingAttributes to ensure parity between sync and report state
    */
   public static Map<String,Object> modelToStateMap(Model m, boolean hubOffline, boolean whitelisted, @Nullable ProductCatalogEntry pm) {
      if(!Predicates.isSupportedModel(m, whitelisted, pm)) {
         return ImmutableMap.of(Constants.States.ERROR_CODE, Constants.Error.DEVICE_NOT_FOUND);
      }

      ImmutableMap.Builder<String,Object> builder = ImmutableMap.builder();
      builder.put(Constants.States.ONLINE, !Predicates.isDeviceOffline(m, hubOffline));
      if(m.supports(SwitchCapability.NAMESPACE)) {
         builder.put(Constants.States.OnOff.ON, Objects.equals(SwitchCapability.STATE_ON, SwitchModel.getState(m, SwitchCapability.STATE_OFF)));
      }
      if(m.supports(DimmerCapability.NAMESPACE)) {
         builder.put(Constants.States.Brightness.BRIGHTNESS, DimmerModel.getBrightness(m));
      }
      if(m.supports(ColorCapability.NAMESPACE) || m.supports(ColorTemperatureCapability.NAMESPACE)) {
         modelToColor(m).ifPresent((c) -> builder.put(Constants.States.Color.COLOR, c.toMap()));
      }
      if(m.supports(ThermostatCapability.NAMESPACE)) {
         String mode = ThermostatModel.getHvacmode(m);
         builder.put(Constants.States.TemperatureSetting.MODE, Constants.TemperatureSettingMode.fromIris(mode).name());
         builder.put(Constants.States.TemperatureSetting.TEMPERATURE_AMBIENT, TemperatureModel.getTemperature(m));
         switch(mode) {
            case ThermostatCapability.HVACMODE_AUTO:
               builder.put(Constants.States.TemperatureSetting.TEMPERATURE_SET_LOW, ThermostatModel.getHeatsetpoint(m));
               builder.put(Constants.States.TemperatureSetting.TEMPERATURE_SET_HIGH, ThermostatModel.getCoolsetpoint(m));
               break;
            case ThermostatCapability.HVACMODE_COOL:
               builder.put(Constants.States.TemperatureSetting.TEMPERATURE_SET, ThermostatModel.getCoolsetpoint(m));
               break;
            case ThermostatCapability.HVACMODE_HEAT:
               builder.put(Constants.States.TemperatureSetting.TEMPERATURE_SET, ThermostatModel.getHeatsetpoint(m));
               break;
            default: /* no op */
         }
      }
      if(m.supports(RelativeHumidityCapability.NAMESPACE)) {
         builder.put(Constants.States.TemperatureSetting.HUMIDITY_AMBIENT, RelativeHumidityModel.getHumidity(m));
      }

      return builder.build();
   }

   /**
    * Throws error event exception with one of the following codes:
    * <ul>
    * <li>Constants.Error.DEVICE_OFFLINE if the device is offline</li>
    * <li>Constants.Error.DEVICE_NOT_FOUND if the model is null or isn't supported by google</li>
    * <li>Constants.Error.VALUE_OUT_OF_RANGE if the value provided is out of range</li>
    * <li>Constants.Error.NOT_SUPPORTED if the command is not recognized or the device doesn't not support the required capability</li>
    * </ul>
    */
   public static Optional<MessageBody> commandToMessageBody(Model m, boolean hubOffline, String command, Map<String, Object> params, boolean whitelisted, @Nullable ProductCatalogEntry pm) throws ErrorEventException {

      if(!Predicates.isSupportedModel(m, whitelisted, pm)) {
         if(m.supports(SceneCapability.NAMESPACE)) {
            throw new ErrorEventException(Constants.Error.NOT_SUPPORTED, "scene contains unsupprorted operations");
         }
         throw new ErrorEventException(Constants.Error.DEVICE_NOT_FOUND, "device doesn't exist or support google home");
      }

      if(Predicates.isDeviceOffline(m, hubOffline)) {
         throw new ErrorEventException(Constants.Error.DEVICE_OFFLINE, m.getId() + " is offline");
      }

      if(params == null) {
         params = ImmutableMap.of();
      }

      String platformCommand = Capability.CMD_SET_ATTRIBUTES;

      ImmutableMap.Builder<String, Object> attrBuilder = ImmutableMap.builder();
      switch(command) {
         case Commands.OnOff.name:
            addOnOffAttrs(m, attrBuilder, params);
            break;
         case Commands.BrightnessAbsolute.name:
            addBrightnessAttrs(m, attrBuilder, params);
            break;
         case Commands.ColorAbsolute.name:
            addColorAttrs(m, attrBuilder, params);
            break;
         case Commands.ActivateScene.name:
            if(!m.supports(SceneCapability.NAMESPACE)) {
               throw new ErrorEventException(Constants.Error.NOT_SUPPORTED, command + " is not supported");
            }
            platformCommand = SceneCapability.FireRequest.NAME;
            break;
         case Commands.SetThermostat.name:
            addThermostatAttrs(m, attrBuilder, params);
            break;
         default:
            throw new ErrorEventException(Constants.Error.NOT_SUPPORTED, command + " is not supported");
      }

      Map<String,Object> attrs = attrBuilder.build();
      if(Objects.equals(Capability.CMD_SET_ATTRIBUTES, platformCommand) && attrs.isEmpty()) {
         return Optional.empty();
      }

      return Optional.of(MessageBody.buildMessage(platformCommand, attrs));
   }

   static void addOnOffAttrs(Model m, ImmutableMap.Builder<String, Object> attrs, Map<String, Object> params) {
      if(params == null) {
         params = ImmutableMap.of();
      }

      assertCapability(m, SwitchCapability.NAMESPACE);
      Boolean on = (Boolean) params.get(Commands.OnOff.arg_on);
      if(on == null) {
         throw new ErrorEventException(Constants.Error.VALUE_OUT_OF_RANGE, Commands.OnOff.arg_on + " must be true or false");
      }
      String newState = on ? SwitchCapability.STATE_ON : SwitchCapability.STATE_OFF;
      if(!Objects.equals(SwitchModel.getState(m), newState)) {
         attrs.put(SwitchCapability.ATTR_STATE, newState);
      }
   }

   static void addBrightnessAttrs(Model m, ImmutableMap.Builder<String, Object> attrs, Map<String, Object> params) {
      if(params == null) {
         params = ImmutableMap.of();
      }

      assertCapability(m, DimmerCapability.NAMESPACE);
      Number b = (Number) params.get(Commands.BrightnessAbsolute.arg_brightness);
      if(b == null || b.intValue() < 0 || b.intValue() > 100) {
         throw new ErrorEventException(Constants.Error.VALUE_OUT_OF_RANGE, Commands.BrightnessAbsolute.arg_brightness + " must be between 0 and 100");
      }

      if(b.intValue() == 0) {
         b = 1;
         if(!Objects.equals(SwitchModel.getState(m), SwitchCapability.STATE_OFF)) {
            attrs.put(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
         }
      } else if(b.intValue() > 0 && !Objects.equals(SwitchModel.getState(m), SwitchCapability.STATE_ON)) {
         attrs.put(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      }

      if(!Objects.equals(DimmerModel.getBrightness(m), b.intValue())) {
         attrs.put(DimmerCapability.ATTR_BRIGHTNESS, b);
      }
   }

   @SuppressWarnings("unchecked")
   static void addColorAttrs(Model m, ImmutableMap.Builder<String, Object> attrs, Map<String, Object> params) {
      if(params == null) {
         params = ImmutableMap.of();
      }

      GoogleColor color = new GoogleColor((Map<String, Object>) params.get(Commands.ColorAbsolute.arg_color));

      if(color.getTemperature() != null) {
         assertCapability(m, ColorTemperatureCapability.NAMESPACE);
      }

      if(color.getSpectrumRGB() != null) {
         assertCapability(m, ColorCapability.NAMESPACE);
      }

      if(color.getTemperature() == null && color.getSpectrumRGB() == null) {
         throw new ErrorEventException(Constants.Error.VALUE_OUT_OF_RANGE,
               Commands.ColorAbsolute.arg_color + " must have either " + GoogleColor.ATTR_TEMPERATURE + " or " + GoogleColor.ATTR_SPECTRUMRGB);
      }

      if(color.getTemperature() != null) {
         assertCapability(m, ColorTemperatureCapability.NAMESPACE);
         Integer min = ColorTemperatureModel.getMincolortemp(m, Constants.Defaults.COLOR_TEMPERATURE_MIN);
         Integer max = ColorTemperatureModel.getMaxcolortemp(m, Constants.Defaults.COLOR_TEMPERATURE_MAX);
         if((min != null && color.getTemperature() < min) || (max != null && color.getTemperature() > max)) {
            throw new ErrorEventException(Constants.Error.VALUE_OUT_OF_RANGE,
                  GoogleColor.ATTR_TEMPERATURE + " must be between " + min + " and " + max);
         }
         Integer colorTemp = color.getTemperature();
         if(!Objects.equals(LightModel.getColormode(m), LightCapability.COLORMODE_COLORTEMP)) {
            attrs.put(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLORTEMP);
         }
         if(!Objects.equals(ColorTemperatureModel.getColortemp(m), colorTemp)) {
            attrs.put(ColorTemperatureCapability.ATTR_COLORTEMP, color.getTemperature());
         }
      } else if(color.getSpectrumRGB() != null) {
         assertCapability(m, ColorCapability.NAMESPACE);
         if(color.getSpectrumRGB() < Constants.Defaults.COLOR_RGB_MIN || color.getSpectrumRGB() > Constants.Defaults.COLOR_RGB_MAX) {
            throw new ErrorEventException(Constants.Error.VALUE_OUT_OF_RANGE, GoogleColor.ATTR_SPECTRUMRGB + " must be between 0 and 16777215");
         }
         Colors.HSB hsb = new Colors.RGB(color.getSpectrumRGB()).toHSB();

         if(!Objects.equals(LightModel.getColormode(m), LightCapability.COLORMODE_COLOR)) {
            attrs.put(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLOR);
         }
         if(!Objects.equals(ColorModel.getHue(m), hsb.hue())) {
            attrs.put(ColorCapability.ATTR_HUE, hsb.hue());
         }
         if(!Objects.equals(ColorModel.getSaturation(m), hsb.saturationPercent())) {
            attrs.put(ColorCapability.ATTR_SATURATION, hsb.saturationPercent());
         }
         if(m.supports(DimmerCapability.NAMESPACE) && !Objects.equals(DimmerModel.getBrightness(m), hsb.brightnessPercent())) {
            attrs.put(DimmerCapability.ATTR_BRIGHTNESS, hsb.brightnessPercent());
         }
      }
   }

   static void addThermostatAttrs(Model m, ImmutableMap.Builder<String, Object> attrs, Map<String, Object> params) {
      if(params == null) {
         params = ImmutableMap.of();
      }

      assertCapability(m, ThermostatCapability.NAMESPACE);

      String newMode = (String) params.get(Commands.SetMode.arg_mode);
      String targetMode = newMode == null ? ThermostatModel.getHvacmode(m) : Constants.TemperatureSettingMode.valueOf(newMode.toLowerCase()).toIris();

      if(!Objects.equals(targetMode, ThermostatModel.getHvacmode(m))) {
         attrs.put(ThermostatCapability.ATTR_HVACMODE, targetMode);
      }

      Number tempNumber = (Number) params.get(Commands.TemperatureSetPoint.arg_temperature);
      Double temp = tempNumber == null ? null : tempNumber.doubleValue();

      switch(targetMode) {
         case ThermostatCapability.HVACMODE_OFF:
            return;
         case ThermostatCapability.HVACMODE_COOL:
            if(modeChangeOnlyTemp(temp)) { return; }
            assertSetPoint(temp);
            attrs.put(ThermostatCapability.ATTR_COOLSETPOINT, temp);
            break;
         case ThermostatCapability.HVACMODE_HEAT:
            if(modeChangeOnlyTemp(temp)) { return; }
            assertSetPoint(temp);
            attrs.put(ThermostatCapability.ATTR_HEATSETPOINT, temp);
            break;
         case ThermostatCapability.HVACMODE_AUTO:
            Number tempHighNumber = (Number) params.get(Commands.TemperatureSetRange.arg_temperature_high);
            Double tempHigh = tempHighNumber == null ? null : tempHighNumber.doubleValue();
            Number tempLowNumber = (Number) params.get(Commands.TemperatureSetRange.arg_temperature_low);
            Double tempLow = tempLowNumber == null ? null : tempLowNumber.doubleValue();

            // mode only change
            if(modeChangeOnlyTemp(temp) && modeChangeOnlyTemp(tempHigh) && modeChangeOnlyTemp(tempLow)) {
               return;
            }

            if((tempHigh != null && tempLow == null) || (tempHigh == null && tempLow != null)) {
               throw new ErrorEventException(
                  Constants.Error.VALUE_OUT_OF_RANGE,
                  Commands.TemperatureSetRange.name + " must have both " + Commands.TemperatureSetRange.arg_temperature_low + " and " + Commands.TemperatureSetRange.arg_temperature_high
               );
            }

            if(tempHigh == null && tempLow == null) {
               Pair<Double, Double> idealRange = calculateIdealRange(m, temp);
               tempLow = idealRange.getLeft();
               tempHigh = idealRange.getRight();
            } else {
               assertGap(tempLow, tempHigh);
            }
            attrs.put(ThermostatCapability.ATTR_HEATSETPOINT, tempLow);
            attrs.put(ThermostatCapability.ATTR_COOLSETPOINT, tempHigh);
            break;
         default:
            /* no op */
      }
   }

   private static boolean modeChangeOnlyTemp(Double temp) {
      // mode only change, google sends 3.073047532264324e-41 when just changing the mode
      return temp == null || (temp < 0.001 && temp > 0);
   }

   private static final double MIN_SET_POINT = fToC(35);
   private static final double MAX_SET_POINT = fToC(95);
   private static final double MIN_GAP = 3.0*(5.0/9.0);

   private static void assertSetPoint(Double value) {
      if(value < MIN_SET_POINT || value > MAX_SET_POINT) {
         throw new ErrorEventException(
            Constants.Error.VALUE_OUT_OF_RANGE,
            "set point must be between " + MIN_SET_POINT + " and " + MAX_SET_POINT
         );
      }
   }

   private static void assertGap(@NonNull Double min, @NonNull Double max) {
      double diff = Math.abs(max - min);
      if(diff < MIN_GAP) {
         throw new ErrorEventException(
            Constants.Error.RANGE_TOO_CLOSE,
            "set point range must have at least 3F degrees of separation"
         );
      }
   }

   private static Pair<Double, Double> calculateIdealRange(Model m, Double target) {
      Double cool = ThermostatModel.getCoolsetpoint(m);
      Double heat = ThermostatModel.getHeatsetpoint(m);
      Double sep = MIN_SET_POINT;
      if (cool != null && heat != null && (cool - heat) > MIN_GAP) {
         sep = cool - heat;
      }

      double newCool = target + (sep / 2);
      double newHeat = target - (sep / 2);
      assertSetPoint(newCool);
      assertSetPoint(newHeat);
      return new ImmutablePair<>(newHeat, newCool);
   }

   private static double fToC(double val) {
      return (val-32.0) * (5.0/9.0);
   }

   private static double cToF(double val) {
      return (val*(9.0/5.0)) + 32.0;
   }

   private static void assertCapability(Model m, String capability) {
      if(!m.supports(capability)) {
         throw new ErrorEventException(Constants.Error.NOT_SUPPORTED, m.getId() + " does not supported " + capability);
      }
   }

   static Optional<GoogleColor> modelToColor(Model m) {
      if(m == null) {
         return Optional.empty();
      }
      if(!m.supports(ColorTemperatureCapability.NAMESPACE) && !m.supports(ColorCapability.NAMESPACE)) {
         return Optional.empty();
      }

      GoogleColor color = new GoogleColor();
      if(m.supports(ColorTemperatureCapability.NAMESPACE)) {
         color.setTemperature(ColorTemperatureModel.getColortemp(m));
      }
      if(m.supports(ColorCapability.NAMESPACE)) {
         color.setSpectrumRGB(new Colors.HSB(
               ColorModel.getHue(m),
               ColorModel.getSaturation(m),
               m.supports(DimmerCapability.NAMESPACE) ? DimmerModel.getBrightness(m) : 100
         ).toRGB().toInt());
      }
      return Optional.of(color);
   }

   static Optional<GoogleDeviceName> modelToDeviceName(Model m) {
      if(m == null) {
         return Optional.empty();
      }

      if(m.supports(DeviceCapability.NAMESPACE)) {
         return createDeviceName(DeviceModel.getName(m));
      }

      if(m.supports(SceneCapability.NAMESPACE)) {
         return createDeviceName(SceneModel.getName(m));
      }

      return Optional.empty();
   }

   private static Optional<GoogleDeviceName> createDeviceName(String name) {
      if(StringUtils.isBlank(name)) {
         return Optional.empty();
      }
      GoogleDeviceName n = new GoogleDeviceName();
      n.setName(name);
      return Optional.of(n);
   }

   static Optional<GoogleDeviceInfo> modelToDeviceInfo(Model m) {
      if(m == null || !m.supports(DeviceCapability.NAMESPACE)) {
         return Optional.empty();
      }

      GoogleDeviceInfo info = new GoogleDeviceInfo();
      info.setManufacturer(DeviceModel.getVendor(m));
      info.setModel(DeviceModel.getModel(m));
      return Optional.of(info);
   }

   static Optional<String> modelToGoogleType(Model m) {
      if(m == null) {
         return Optional.empty();
      }
      if(m.supports(DeviceCapability.NAMESPACE)) {
         return devTypeHintToGoogleType(DeviceModel.getDevtypehint(m));
      }
      if(m.supports(SceneCapability.NAMESPACE)) {
         return Optional.of(Constants.Type.SCENE);
      }
      return Optional.empty();
   }

   static Map<String, Object> modelToCustomData(Model m) {
      ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
      if(m.supports(DeviceCapability.NAMESPACE)) {
         String val = DeviceModel.getProductId(m);
         if(!StringUtils.isBlank(val))  builder.put(PRODUCT_ID, val);
         val = DeviceModel.getDevtypehint(m);
         if(!StringUtils.isBlank(val))  builder.put(TYPE_HINT, val);
      } else if (m.supports(SceneCapability.NAMESPACE)) {
         builder.put(TYPE_HINT, SceneCapability.NAMESPACE);
      }
      return builder.build();
   }

   static Optional<String> devTypeHintToGoogleType(String devTypeHint) {
      if(StringUtils.isBlank(devTypeHint)) {
         return Optional.empty();
      }
      String type;
      switch(devTypeHint) {
         case Constants.DeviceTypeHint.DIMMER:
         case Constants.DeviceTypeHint.LIGHT:
            type = Constants.Type.LIGHT; break;
         case Constants.DeviceTypeHint.SWITCH:
            type = Constants.Type.SWITCH; break;
         case Constants.DeviceTypeHint.THERMOSTAT:
            type = Constants.Type.THERMOSTAT; break;
         default: type = null; break;
      }
      return Optional.ofNullable(type);
   }

   static Set<String> capabilitiesToTraits(Set<String> capabilities) {
      if(capabilities == null) {
         return ImmutableSet.of();
      }
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      capabilities.forEach((s) -> capabilityToTrait(s).ifPresent(builder::add));
      return builder.build();
   }

   static Optional<String> capabilityToTrait(String capability) {
      if(StringUtils.isBlank(capability)) {
         return Optional.empty();
      }
      String trait;
      switch(capability) {
         case SwitchCapability.NAMESPACE: trait = Constants.Trait.ON_OFF; break;
         case DimmerCapability.NAMESPACE: trait = Constants.Trait.BRIGHTNESS; break;
         case ColorCapability.NAMESPACE: trait = Constants.Trait.COLOR_SPECTRUM; break;
         case ColorTemperatureCapability.NAMESPACE: trait = Constants.Trait.COLOR_TEMPERATURE; break;
         case SceneCapability.NAMESPACE: trait = Constants.Trait.SCENE; break;
         case ThermostatCapability.NAMESPACE: trait = Constants.Trait.TEMPERATURE_SETTING; break;
         default: trait = null; break;
      }
      return Optional.ofNullable(trait);
   }

   static Map<String,Object> modelToTraitAttributes(Model m) {
      if(m != null) {
         if(m.supports(ColorTemperatureCapability.NAMESPACE)) {
            return ImmutableMap.of(
               Constants.Attributes.ColorTemperature.TEMPERATURE_MIN_K, ColorTemperatureModel.getMincolortemp(m, Constants.Defaults.COLOR_TEMPERATURE_MIN),
               Constants.Attributes.ColorTemperature.TEMPERATURE_MAX_K, ColorTemperatureModel.getMaxcolortemp(m, Constants.Defaults.COLOR_TEMPERATURE_MAX)
            );
         }
         if(m.supports(ThermostatCapability.NAMESPACE)) {
            return ImmutableMap.of(
               Constants.Attributes.TemperatureSetting.UNIT, "F",
               Constants.Attributes.TemperatureSetting.MODES, StringUtils.join(thermostatSupportedModes(m), ',')
            );
         }
         if(m.supports(SceneCapability.NAMESPACE)) {
            return ImmutableMap.of(Constants.Attributes.Scene.sceneReversible, false);
         }
      }
      return ImmutableMap.of();
   }

   private static List<String> thermostatSupportedModes(Model m) {
      if(m != null && m.supports(ThermostatCapability.NAMESPACE)) {
         ImmutableList.Builder<String> modeBuilder = new ImmutableList.Builder<>();
         modeBuilder.add(Constants.TemperatureSettingMode.off.name(), Constants.TemperatureSettingMode.cool.name(), Constants.TemperatureSettingMode.heat.name());
         if(ThermostatModel.getSupportsAuto(m, true)) {
            modeBuilder.add(Constants.TemperatureSettingMode.heatcool.name());
         }
         return modeBuilder.build();
      }
      return ImmutableList.of();
   }

   public static PlatformMessage requestToMessage(Request req, UUID placeId, String population, int ttl) {
      Preconditions.checkNotNull(req, "req must not be null");
      Preconditions.checkNotNull(req.getInputs(), "inputs must not be null");
      Preconditions.checkArgument(req.getInputs().size() == 1, "inputs must be of length 1");
      MessageBody body;
      Request.Input input = req.getInputs().get(0);
      switch(input.getIntent()) {
         case Constants.Intents.SYNC:
            body = inputToSyncMessage();
            break;
         case Constants.Intents.QUERY:
            body = inputToQueryMessage(input.getPayload());
            break;
         case Constants.Intents.EXECUTE:
            body = inputToExecuteMessage(input.getPayload());
            break;
         default:
            body = null;
            break;
      }
      if(body == null) {
         throw new ErrorEventException(Constants.Error.NOT_SUPPORTED, "unknown intent " + input.getIntent());
      }
      return PlatformMessage.buildRequest(body, Constants.BRIDGE_ADDRESS, Constants.SERVICE_ADDRESS)
            .withTimeToLive(ttl)
            .withCorrelationId(req.getRequestId())
            .withPlaceId(placeId)
            .withPopulation(population)
            .create();
   }

   public static Response messageToResponse(PlatformMessage msg) {
      Preconditions.checkNotNull(msg, "msg must not be null");
      Response r = new Response();
      r.setRequestId(msg.getCorrelationId());
      switch(msg.getMessageType()) {
         case GoogleService.SyncResponse.NAME:
            r.setPayload(
               ImmutableMap.of(
                  Constants.Response.Sync.DEVICES, GoogleService.SyncResponse.getDevices(msg.getValue()),
                  Constants.Response.Sync.AGENT_USER_ID, GoogleService.SyncResponse.getUserAgentId(msg.getValue())
               )
            );
            break;
         case GoogleService.QueryResponse.NAME:
            r.setPayload(ImmutableMap.of(Constants.Response.Query.DEVICES, GoogleService.QueryResponse.getDevices(msg.getValue())));
            break;
         case GoogleService.ExecuteResponse.NAME:
            r.setPayload(ImmutableMap.of(Constants.Response.Execute.COMMANDS, GoogleService.ExecuteResponse.getCommands(msg.getValue())));
            break;
         default:
            r.setPayload(ImmutableMap.of(Constants.Response.ERROR_CODE, Constants.Error.UNKNOWN_ERROR));
            break;
      }
      return r;
   }

   static MessageBody inputToSyncMessage() {
      return GoogleService.SyncRequest.instance();
   }

   static MessageBody inputToQueryMessage(JsonElement payload) {
      Set<String> addresses = ImmutableSet.of();
      if(payload != null) {
         QueryPayload query = GSON.fromJson(payload, QueryPayload.class);
         addresses = devicesToAddresses(query.devicesAsBeans());
      }
      return GoogleService.QueryRequest.builder()
            .withAddresses(addresses)
            .build();
   }

   static MessageBody inputToExecuteMessage(JsonElement payload) {
      List<GoogleCommand> commands = ImmutableList.of();
      if(payload != null) {
         ExecutePayload execute = GSON.fromJson(payload, ExecutePayload.class);
         if(execute.getCommands() != null) {
            ImmutableList.Builder<GoogleCommand> builder = ImmutableList.builder();
            execute.getCommands().forEach((c) -> addCommands(c, builder));
            commands = builder.build();
         }
      }
      return GoogleService.ExecuteRequest.builder()
            .withCommands(commands.stream().map(GoogleCommand::toMap).collect(Collectors.toList()))
            .build();
   }

   private static void addCommands(ExecutePayload.Command command, ImmutableList.Builder<GoogleCommand> commands) {
      if(command == null) {
         return;
      }
      if(command.getDevices() == null || command.getDevices().isEmpty()) {
         return;
      }
      if(command.getExecution() == null || command.getExecution().isEmpty()) {
         return;
      }
      for(ExecutePayload.Execution e : command.getExecution()) {
         GoogleCommand c = new GoogleCommand();
         c.setCommand(e.getCommand());
         c.setParams(e.getParams());
         c.setAddresses(devicesToAddresses(command.devicesAsBeans()));
         commands.add(c);
      }
   }

   private static Set<String> devicesToAddresses(List<GoogleDevice> devices) {
      if(devices == null || devices.isEmpty()) {
         return ImmutableSet.of();
      }
      return devices.stream().map(GoogleDevice::getId).collect(Collectors.toSet());
   }

}

