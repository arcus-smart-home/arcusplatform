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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.google.model.ExecutePayload;
import com.iris.google.model.QueryPayload;
import com.iris.google.model.Request;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ColorCapability;
import com.iris.messages.capability.ColorTemperatureCapability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.LightCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.service.GoogleService;
import com.iris.messages.type.GoogleColor;
import com.iris.messages.type.GoogleCommand;
import com.iris.messages.type.GoogleDevice;
import com.iris.messages.type.GoogleDeviceInfo;
import com.iris.messages.type.GoogleDeviceName;
import com.iris.messages.type.Population;
import com.iris.util.IrisUUID;

public class TestTransformers {

   private Model allCaps;
   private Model thermo;
   private Model unsupportedDev;
   private Model nonDevModel;
   private GoogleDeviceInfo devInfo;
   private GoogleDeviceName devName;

   @Before
   public void setUp() {
      allCaps = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE, DimmerCapability.NAMESPACE, ColorCapability.NAMESPACE, ColorTemperatureCapability.NAMESPACE, LightCapability.NAMESPACE)
            .put(DeviceCapability.ATTR_DEVTYPEHINT, Constants.DeviceTypeHint.LIGHT)
            .put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE)
            .put(ColorCapability.ATTR_HUE, 120)
            .put(ColorCapability.ATTR_SATURATION, 100)
            .put(DimmerCapability.ATTR_BRIGHTNESS, 100)
            .put(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF)
            .put(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_NORMAL)
            .put(ColorTemperatureCapability.ATTR_COLORTEMP, 2500)
            .put(ColorTemperatureCapability.ATTR_MINCOLORTEMP, 2000)
            .put(ColorTemperatureCapability.ATTR_MAXCOLORTEMP, 6500)
            .put(DeviceCapability.ATTR_PRODUCTID, "12345")
            .toModel();

      thermo = ModelFixtures.buildDeviceAttributes(ThermostatCapability.NAMESPACE, TemperatureCapability.NAMESPACE)
         .put(DeviceCapability.ATTR_DEVTYPEHINT, Constants.DeviceTypeHint.THERMOSTAT)
         .put(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE)
         .put(TemperatureCapability.ATTR_TEMPERATURE, 20.0)
         .put(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF)
         .put(ThermostatCapability.ATTR_SUPPORTSAUTO, true)
         .put(ThermostatCapability.ATTR_COOLSETPOINT, 21.1)
         .put(ThermostatCapability.ATTR_HEATSETPOINT, 18.89)
         .put(DeviceCapability.ATTR_PRODUCTID, "12345")
         .toModel();

      unsupportedDev = ModelFixtures.buildContactAttributes().toModel();

      nonDevModel = new SimpleModel(ModelFixtures.createPersonAttributes());

      devInfo = new GoogleDeviceInfo();
      devInfo.setManufacturer("Testitron");
      devInfo.setModel("test");

      devName = new GoogleDeviceName();
      devName.setName("foobar");
   }

   @Test
   public void testModelToTraitAttributes() {
      assertEquals(ImmutableMap.of(), Transformers.modelToTraitAttributes(null));
      assertEquals(ImmutableMap.of(), Transformers.modelToTraitAttributes(unsupportedDev));
      assertEquals(ImmutableMap.of(
            Constants.Attributes.ColorTemperature.TEMPERATURE_MIN_K, 2000,
            Constants.Attributes.ColorTemperature.TEMPERATURE_MAX_K, 6500
      ), Transformers.modelToTraitAttributes(allCaps));

      assertEquals(ImmutableMap.of(
         Constants.Attributes.TemperatureSetting.UNIT, "F",
         Constants.Attributes.TemperatureSetting.MODES, StringUtils.join(ImmutableList.of("off", "cool", "heat", "heatcool"), ',')
      ), Transformers.modelToTraitAttributes(thermo));

      thermo.setAttribute(ThermostatCapability.ATTR_SUPPORTSAUTO, false);
      assertEquals(ImmutableMap.of(
         Constants.Attributes.TemperatureSetting.UNIT, "F",
         Constants.Attributes.TemperatureSetting.MODES, StringUtils.join(ImmutableList.of("off", "cool", "heat"), ',')
      ), Transformers.modelToTraitAttributes(thermo));
   }

   @Test
   public void testCapabilityToTrait() {
      assertFalse(Transformers.capabilityToTrait(null).isPresent());
      assertEquals(Constants.Trait.ON_OFF, Transformers.capabilityToTrait(SwitchCapability.NAMESPACE).get());
      assertEquals(Constants.Trait.BRIGHTNESS, Transformers.capabilityToTrait(DimmerCapability.NAMESPACE).get());
      assertEquals(Constants.Trait.COLOR_SPECTRUM, Transformers.capabilityToTrait(ColorCapability.NAMESPACE).get());
      assertEquals(Constants.Trait.COLOR_TEMPERATURE, Transformers.capabilityToTrait(ColorTemperatureCapability.NAMESPACE).get());
      assertEquals(Constants.Trait.SCENE, Transformers.capabilityToTrait(SceneCapability.NAMESPACE).get());
      assertEquals(Constants.Trait.TEMPERATURE_SETTING, Transformers.capabilityToTrait(ThermostatCapability.NAMESPACE).get());
      assertFalse(Transformers.capabilityToTrait(ContactCapability.NAMESPACE).isPresent());
   }

   @Test
   public void testCapabilitiesToTraits() {
      assertEquals(ImmutableSet.of(), Transformers.capabilitiesToTraits(null));
      assertEquals(ImmutableSet.of(), Transformers.capabilitiesToTraits(ImmutableSet.of()));
      assertEquals(ImmutableSet.of(), Transformers.capabilitiesToTraits(ImmutableSet.of(ContactCapability.NAMESPACE)));
      assertEquals(ImmutableSet.of(
            Constants.Trait.BRIGHTNESS, Constants.Trait.COLOR_SPECTRUM, Constants.Trait.COLOR_TEMPERATURE, Constants.Trait.ON_OFF, Constants.Trait.TEMPERATURE_SETTING
      ), Transformers.capabilitiesToTraits(ImmutableSet.of(
            SwitchCapability.NAMESPACE, DimmerCapability.NAMESPACE, ColorCapability.NAMESPACE, ColorTemperatureCapability.NAMESPACE, ThermostatCapability.NAMESPACE
      )));
   }

   @Test
   public void testDevTypeHintToGoogleType() {
      assertEquals(Optional.empty(), Transformers.devTypeHintToGoogleType(null));
      assertEquals(Constants.Type.LIGHT, Transformers.devTypeHintToGoogleType(Constants.DeviceTypeHint.LIGHT).get());
      assertEquals(Constants.Type.LIGHT, Transformers.devTypeHintToGoogleType(Constants.DeviceTypeHint.DIMMER).get());
      assertEquals(Constants.Type.SWITCH, Transformers.devTypeHintToGoogleType(Constants.DeviceTypeHint.SWITCH).get());
      assertEquals(Constants.Type.THERMOSTAT, Transformers.devTypeHintToGoogleType(Constants.DeviceTypeHint.THERMOSTAT).get());
      assertEquals(Optional.empty(), Transformers.devTypeHintToGoogleType("Fan Control"));
   }

   @Test
   public void testModelToGoogleType() {
      assertEquals(Optional.empty(), Transformers.modelToGoogleType(null));
      assertEquals(Optional.empty(), Transformers.modelToGoogleType(nonDevModel));
      Model m = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE).toModel();
      m.setAttribute(DeviceCapability.ATTR_DEVTYPEHINT, Constants.DeviceTypeHint.LIGHT);
      assertEquals(Constants.Type.LIGHT, Transformers.modelToGoogleType(m).get());

      m = new SimpleModel(ModelFixtures.buildServiceAttributes(SceneCapability.NAMESPACE).create());
      assertEquals(Constants.Type.SCENE, Transformers.modelToGoogleType(m).get());
   }

   @Test
   public void testModelToDeviceInfo() {
      assertEquals(Optional.empty(), Transformers.modelToDeviceInfo(null));
      assertEquals(Optional.empty(), Transformers.modelToDeviceInfo(nonDevModel));
      assertDeviceInfo(devInfo, Transformers.modelToDeviceInfo(unsupportedDev).get());
   }

   @Test
   public void testModelToDeviceName() {
      assertEquals(Optional.empty(), Transformers.modelToDeviceName(null));
      assertEquals(Optional.empty(), Transformers.modelToDeviceName(nonDevModel));
      unsupportedDev.setAttribute(DeviceCapability.ATTR_NAME, "foobar");
      assertDeviceName(devName, Transformers.modelToDeviceName(unsupportedDev).get());

      Model m = new SimpleModel(ModelFixtures.buildServiceAttributes(SceneCapability.NAMESPACE).create());
      m.setAttribute(SceneCapability.ATTR_NAME, "foobar");

      assertDeviceName(devName, Transformers.modelToDeviceName(m).get());
   }

   @Test
   public void testModelToCustom() {
      assertEquals(ImmutableMap.of(), Transformers.modelToCustomData(nonDevModel));

      assertEquals(ImmutableMap.of(
            Transformers.PRODUCT_ID, "12345",
            Transformers.TYPE_HINT, "Light"
      ), Transformers.modelToCustomData(allCaps));

      Model m = new SimpleModel(ModelFixtures.buildServiceAttributes(SceneCapability.NAMESPACE).create());
      assertEquals(ImmutableMap.of(
            Transformers.TYPE_HINT, SceneCapability.NAMESPACE
      ), Transformers.modelToCustomData(m));
   }

   @Test
   public void testModelToColor() {
      assertEquals(Optional.empty(), Transformers.modelToColor(null));
      assertEquals(Optional.empty(), Transformers.modelToColor(nonDevModel));

      GoogleColor color = new GoogleColor();
      color.setTemperature(2500);
      color.setSpectrumRGB(65280);

      assertColor(color, Transformers.modelToColor(allCaps).get());
   }

   @Test
   public void testModelToStateMap() {
      assertEquals(ImmutableMap.of(Constants.States.ERROR_CODE, Constants.Error.DEVICE_NOT_FOUND),
            Transformers.modelToStateMap(null, false, true, null));

      GoogleColor color = new GoogleColor();
      color.setSpectrumRGB(65280);
      color.setTemperature(2500);

      Map<String,Object> expected = ImmutableMap.of(
            Constants.States.ONLINE, true,
            Constants.States.OnOff.ON, false,
            Constants.States.Brightness.BRIGHTNESS, 100,
            Constants.States.Color.COLOR, color.toMap()
      );

      assertEquals(expected, Transformers.modelToStateMap(allCaps, false, true, null));

      expected = ImmutableMap.of(
         Constants.States.ONLINE, true,
         Constants.States.TemperatureSetting.MODE, "off",
         Constants.States.TemperatureSetting.TEMPERATURE_AMBIENT, 20.0
      );

      assertEquals(expected, Transformers.modelToStateMap(thermo, false, true, null));

      thermo.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);

      expected = ImmutableMap.of(
         Constants.States.ONLINE, true,
         Constants.States.TemperatureSetting.MODE, "heatcool",
         Constants.States.TemperatureSetting.TEMPERATURE_AMBIENT, 20.0,
         Constants.States.TemperatureSetting.TEMPERATURE_SET_HIGH, 21.1,
         Constants.States.TemperatureSetting.TEMPERATURE_SET_LOW, 18.89
      );

      assertEquals(expected, Transformers.modelToStateMap(thermo, false, true, null));

      thermo.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL);
      expected = ImmutableMap.of(
         Constants.States.ONLINE, true,
         Constants.States.TemperatureSetting.MODE, "cool",
         Constants.States.TemperatureSetting.TEMPERATURE_AMBIENT, 20.0,
         Constants.States.TemperatureSetting.TEMPERATURE_SET, 21.1
      );

      assertEquals(expected, Transformers.modelToStateMap(thermo, false, true, null));

      thermo.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT);
      expected = ImmutableMap.of(
         Constants.States.ONLINE, true,
         Constants.States.TemperatureSetting.MODE, "heat",
         Constants.States.TemperatureSetting.TEMPERATURE_AMBIENT, 20.0,
         Constants.States.TemperatureSetting.TEMPERATURE_SET, 18.89
      );

      assertEquals(expected, Transformers.modelToStateMap(thermo, false, true, null));
   }

   @Test
   public void testModelToDevice() {
      assertEquals(Optional.empty(), Transformers.modelToDevice(null, true, null, true));
      assertEquals(Optional.empty(), Transformers.modelToDevice(nonDevModel, true, null, true));
      assertEquals(Optional.empty(), Transformers.modelToDevice(allCaps, true, null, true));

      allCaps.setAttribute(DeviceCapability.ATTR_NAME, "foobar");

      GoogleDevice dev = new GoogleDevice();
      dev.setId(allCaps.getAddress().getRepresentation());
      dev.setName(devName.toMap());
      dev.setDeviceInfo(devInfo.toMap());
      dev.setAttributes(ImmutableMap.of(
            Constants.Attributes.ColorTemperature.TEMPERATURE_MIN_K, 2000,
            Constants.Attributes.ColorTemperature.TEMPERATURE_MAX_K, 6500
      ));
      dev.setTraits(ImmutableSet.of(Constants.Trait.BRIGHTNESS, Constants.Trait.COLOR_SPECTRUM, Constants.Trait.COLOR_TEMPERATURE, Constants.Trait.ON_OFF));
      dev.setType(Constants.Type.LIGHT);
      dev.setWillReportState(true);
      dev.setCustomData(ImmutableMap.of(Transformers.PRODUCT_ID, "12345", Transformers.TYPE_HINT, "Light"));

      assertDevice(dev, Transformers.modelToDevice(allCaps, true, null, true).get());
   }

   @Test
   public void testAddColorAttrs_Temperature() {
      ImmutableMap.Builder<String,Object> builder = ImmutableMap.builder();

      try {
         Transformers.addColorAttrs(allCaps, builder, null);
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         Transformers.addColorAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Color.COLOR, ImmutableMap.of()));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      GoogleColor color = new GoogleColor();

      try {
         color.setTemperature(1999);
         Transformers.addColorAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Color.COLOR, color.toMap()));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         color.setTemperature(6501);
         Transformers.addColorAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Color.COLOR, color.toMap()));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         Model noColorTemp = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE, DimmerCapability.NAMESPACE, ColorCapability.NAMESPACE)
               .put(DeviceCapability.ATTR_DEVTYPEHINT, Constants.DeviceTypeHint.LIGHT)
               .toModel();
         Transformers.addColorAttrs(noColorTemp, builder, ImmutableMap.of(Constants.States.Color.COLOR, color.toMap()));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.NOT_SUPPORTED);
      }

      color.setTemperature(null);

      // start in normal
      color.setTemperature(3000);
      Transformers.addColorAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Color.COLOR, color.toMap()));

      assertEquals(ImmutableMap.of(
            ColorTemperatureCapability.ATTR_COLORTEMP, 3000,
            LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLORTEMP
      ), builder.build());

      // start in color mode
      builder = ImmutableMap.builder();
      allCaps.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLOR);
      Transformers.addColorAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Color.COLOR, color.toMap()));

      assertEquals(ImmutableMap.of(
         ColorTemperatureCapability.ATTR_COLORTEMP, 3000,
         LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLORTEMP
      ), builder.build());

      // already in temperature mode
      builder = ImmutableMap.builder();
      allCaps.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLORTEMP);
      Transformers.addColorAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Color.COLOR, color.toMap()));

      assertEquals(ImmutableMap.of(
         ColorTemperatureCapability.ATTR_COLORTEMP, 3000
      ), builder.build());
   }

   @Test
   public void testAddColorAttrs_RGB() {
      ImmutableMap.Builder<String,Object> builder = ImmutableMap.builder();

      GoogleColor color = new GoogleColor();

      try {
         color.setSpectrumRGB(-1);
         Transformers.addColorAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Color.COLOR, color.toMap()));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         color.setSpectrumRGB(16777216);
         Transformers.addColorAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Color.COLOR, color.toMap()));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         Model noColor = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE, DimmerCapability.NAMESPACE, ColorTemperatureCapability.NAMESPACE)
               .put(DeviceCapability.ATTR_DEVTYPEHINT, Constants.DeviceTypeHint.LIGHT)
               .toModel();
         Transformers.addColorAttrs(noColor, builder, ImmutableMap.of(Constants.States.Color.COLOR, color.toMap()));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.NOT_SUPPORTED);
      }

      // start in normal
      color.setSpectrumRGB(16777215);
      Transformers.addColorAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Color.COLOR, color.toMap()));

      assertEquals(ImmutableMap.of(
         ColorCapability.ATTR_HUE, 0,
         ColorCapability.ATTR_SATURATION, 0,
         LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLOR
      ), builder.build());

      // start in color temp mode
      builder = ImmutableMap.builder();
      allCaps.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLORTEMP);
      Transformers.addColorAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Color.COLOR, color.toMap()));

      assertEquals(ImmutableMap.of(
         ColorCapability.ATTR_HUE, 0,
         ColorCapability.ATTR_SATURATION, 0,
         LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLOR
      ), builder.build());

      // already in color mode
      builder = ImmutableMap.builder();
      allCaps.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLOR);
      Transformers.addColorAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Color.COLOR, color.toMap()));

      assertEquals(ImmutableMap.of(
         ColorCapability.ATTR_HUE, 0,
         ColorCapability.ATTR_SATURATION, 0
      ), builder.build());
   }

   @Test
   public void testAddBrightnessAttrs() {
      ImmutableMap.Builder<String,Object> builder = ImmutableMap.builder();

      try {
         Transformers.addBrightnessAttrs(allCaps, builder, null);
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         Transformers.addBrightnessAttrs(allCaps, builder, ImmutableMap.of());
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         Transformers.addBrightnessAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Brightness.BRIGHTNESS, -1));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         Transformers.addBrightnessAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Brightness.BRIGHTNESS, 101));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         Model noDim = ModelFixtures.buildDeviceAttributes(SwitchCapability.NAMESPACE, ColorCapability.NAMESPACE, ColorTemperatureCapability.NAMESPACE)
               .toModel();
         Transformers.addBrightnessAttrs(noDim, builder, ImmutableMap.of(Constants.States.Brightness.BRIGHTNESS, 100));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.NOT_SUPPORTED);
      }

      // starts in off
      Transformers.addBrightnessAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Brightness.BRIGHTNESS, 50));
      assertEquals(ImmutableMap.of(DimmerCapability.ATTR_BRIGHTNESS, 50, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON), builder.build());

      // 0 while off should set to 1
      builder = ImmutableMap.builder();
      Transformers.addBrightnessAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Brightness.BRIGHTNESS, 0));
      assertEquals(ImmutableMap.of(DimmerCapability.ATTR_BRIGHTNESS, 1), builder.build());

      // 0 while on should set to 1 and turn off
      allCaps.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      builder = ImmutableMap.builder();
      Transformers.addBrightnessAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Brightness.BRIGHTNESS, 0));
      assertEquals(ImmutableMap.of(DimmerCapability.ATTR_BRIGHTNESS, 1, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF), builder.build());

      // already on should just adjust the brightness
      allCaps.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      builder = ImmutableMap.builder();
      Transformers.addBrightnessAttrs(allCaps, builder, ImmutableMap.of(Constants.States.Brightness.BRIGHTNESS, 50));
      assertEquals(ImmutableMap.of(DimmerCapability.ATTR_BRIGHTNESS, 50), builder.build());
   }

   @Test
   public void testAddOnOffAttrs() {
      ImmutableMap.Builder<String,Object> builder = ImmutableMap.builder();

      try {
         Transformers.addOnOffAttrs(allCaps, builder, null);
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         Transformers.addOnOffAttrs(allCaps, builder, ImmutableMap.of());
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         Model noSwitch = ModelFixtures.buildDeviceAttributes(DimmerCapability.NAMESPACE, ColorCapability.NAMESPACE, ColorTemperatureCapability.NAMESPACE)
               .toModel();
         Transformers.addOnOffAttrs(noSwitch, builder, ImmutableMap.of(Constants.States.OnOff.ON, true));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.NOT_SUPPORTED);
      }

      Transformers.addOnOffAttrs(allCaps, builder, ImmutableMap.of(Constants.States.OnOff.ON, true));

      assertEquals(ImmutableMap.of(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON), builder.build());
   }

   @Test
   public void testAddThermostatAttrs() {
      ImmutableMap.Builder<String,Object> builder = ImmutableMap.builder();

      try {
         Transformers.addThermostatAttrs(thermo, builder, null);
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         Transformers.addThermostatAttrs(thermo, builder, ImmutableMap.of());
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         Transformers.addThermostatAttrs(allCaps, builder, ImmutableMap.of(Constants.States.OnOff.ON, true));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.NOT_SUPPORTED);
      }

      try {
         Transformers.addThermostatAttrs(thermo, builder, ImmutableMap.of(
            Commands.SetMode.arg_mode, Constants.TemperatureSettingMode.cool.name(),
            Commands.TemperatureSetPoint.arg_temperature, 0.0
         ));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         Transformers.addThermostatAttrs(thermo, builder, ImmutableMap.of(
            Commands.SetMode.arg_mode, Constants.TemperatureSettingMode.heat.name(),
            Commands.TemperatureSetPoint.arg_temperature, 36.0
         ));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.VALUE_OUT_OF_RANGE);
      }

      try {
         Transformers.addThermostatAttrs(thermo, builder, ImmutableMap.of(
            Commands.SetMode.arg_mode, Constants.TemperatureSettingMode.heatcool.name(),
            Commands.TemperatureSetRange.arg_temperature_high, 20.0,
            Commands.TemperatureSetRange.arg_temperature_low,19.0
         ));
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.RANGE_TOO_CLOSE);
      }

      builder = ImmutableMap.builder();

      Transformers.addThermostatAttrs(thermo, builder, ImmutableMap.of(Commands.SetMode.arg_mode, Constants.TemperatureSettingMode.heatcool.name()));
      assertEquals(ImmutableMap.of(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO), builder.build());

      builder = ImmutableMap.builder();

      Transformers.addThermostatAttrs(thermo, builder, ImmutableMap.of(
         Commands.SetMode.arg_mode, Constants.TemperatureSettingMode.heatcool.name(),
         Commands.TemperatureSetPoint.arg_temperature, 23.89
      ));

      Map<String,Object> attrs = builder.build();
      assertEquals(ThermostatCapability.HVACMODE_AUTO, attrs.get(ThermostatCapability.ATTR_HVACMODE));
      assertTrue(((Double) attrs.get(ThermostatCapability.ATTR_COOLSETPOINT)) > 23.89);
      assertTrue(((Double) attrs.get(ThermostatCapability.ATTR_HEATSETPOINT)) < 23.89);



   }

   @Test
   public void testCommandToMessageBodyDeviceNotSupported() {
      try {
         Transformers.commandToMessageBody(
               new SimpleModel(ModelFixtures.createPersonAttributes()),
               false,
               Commands.OnOff.name,
               ImmutableMap.of(),
               true,
               null);
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.DEVICE_NOT_FOUND);
      }
   }

   @Test
   public void testCommandToMessageBodyOffline() {
      allCaps.setAttribute(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
      try {
         Transformers.commandToMessageBody(
               allCaps,
               false,
               Commands.OnOff.name,
               ImmutableMap.of(),
               true,
               null);
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.DEVICE_OFFLINE);
      }
   }

   @Test
   public void testCommandToMessageBodyCommandNotSupported() {
      try {
         Transformers.commandToMessageBody(
               allCaps,
               false,
               "MadeUpCommand",
               ImmutableMap.of(),
               true,
               null);
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.NOT_SUPPORTED);
      }
      try {
         Transformers.commandToMessageBody(
               allCaps,
               false,
               Commands.ActivateScene.name,
               ImmutableMap.of(),
               true,
               null);
      } catch(Throwable t) {
         assertErrorEvent(t, Constants.Error.NOT_SUPPORTED);
      }
   }

   @Test
   public void testCommandToMessageBody() {
      assertMessageBody(
            MessageBody.buildMessage(
                  Capability.CMD_SET_ATTRIBUTES,
                  ImmutableMap.of(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON)
            ),
            Transformers.commandToMessageBody(
                  allCaps,
                  false,
                  Commands.OnOff.name,
                  ImmutableMap.of(Commands.OnOff.arg_on, true),
                  true,
                  null
            ).get()
      );

      assertMessageBody(
            MessageBody.buildMessage(
                  Capability.CMD_SET_ATTRIBUTES,
                  ImmutableMap.of(DimmerCapability.ATTR_BRIGHTNESS, 50, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON)
            ),
            Transformers.commandToMessageBody(
                  allCaps,
                  false,
                  Commands.BrightnessAbsolute.name,
                  ImmutableMap.of(Commands.BrightnessAbsolute.arg_brightness, 50),
                  true,
                  null
            ).get()
      );

      GoogleColor color = new GoogleColor();
      color.setTemperature(3000);

      assertMessageBody(
            MessageBody.buildMessage(
                  Capability.CMD_SET_ATTRIBUTES,
                  ImmutableMap.of(ColorTemperatureCapability.ATTR_COLORTEMP, 3000, LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLORTEMP)
            ),
            Transformers.commandToMessageBody(
                  allCaps,
                  false,
                  Commands.ColorAbsolute.name,
                  ImmutableMap.of(Commands.ColorAbsolute.arg_color, color.toMap()),
                  true,
                  null
            ).get()
      );

      color.setTemperature(null);
      color.setSpectrumRGB(16711680);

      assertMessageBody(
            MessageBody.buildMessage(
                  Capability.CMD_SET_ATTRIBUTES,
                  ImmutableMap.of(ColorCapability.ATTR_HUE, 0, LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLOR)
            ),
            Transformers.commandToMessageBody(
                  allCaps,
                  false,
                  Commands.ColorAbsolute.name,
                  ImmutableMap.of(Commands.ColorAbsolute.arg_color, color.toMap()),
                  true,
                  null
            ).get()
      );

      Model m = new SimpleModel(ModelFixtures.buildServiceAttributes(SceneCapability.NAMESPACE).create());
      m.setAttribute(SceneCapability.ATTR_NAME, "foobar");
      assertMessageBody(
            SceneCapability.FireRequest.instance(),
            Transformers.commandToMessageBody(
                  m,
                  false,
                  Commands.ActivateScene.name,
                  ImmutableMap.of(),
                  true,
                  null
            ).get()
      );
   }

   @Test
   public void testRequestToMessage() {
      Request req = new Request();
      Request.Input input = new Request.Input();
      input.setIntent(Constants.Intents.SYNC);
      req.setInputs(ImmutableList.of(input));
      UUID id = IrisUUID.randomUUID();
      String population = Population.NAME_GENERAL;
      assertEquals(
            GoogleService.SyncRequest.NAME,
            Transformers.requestToMessage(req, id, population, 100).getMessageType()
      );

      input = new Request.Input();
      input.setIntent(Constants.Intents.QUERY);
      req.setInputs(ImmutableList.of(input));

      assertEquals(
            GoogleService.QueryRequest.NAME,
            Transformers.requestToMessage(req, id, population, 100).getMessageType()
      );

      input = new Request.Input();
      input.setIntent(Constants.Intents.EXECUTE);
      req.setInputs(ImmutableList.of(input));

      assertEquals(
            GoogleService.ExecuteRequest.NAME,
            Transformers.requestToMessage(req, id, population, 100).getMessageType()
      );

      try {
         input = new Request.Input();
         input.setIntent("WrongIntent");
         req.setInputs(ImmutableList.of(input));
         Transformers.requestToMessage(req, id, population, 100);
         fail("exception should have been thrown");
      } catch(ErrorEventException eee) {
         assertEquals(Constants.Error.NOT_SUPPORTED, eee.getCode());
      }
   }

   @Test
   public void testInputToQueryMessage() {
      assertMessageBody(
            GoogleService.QueryRequest.builder().withAddresses(ImmutableSet.of()).build(),
            Transformers.inputToQueryMessage(null)
      );

      String addr = Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation();
      QueryPayload payload = new QueryPayload();
      payload.setDevices(ImmutableList.of(
            ImmutableMap.of("id", addr)
      ));

      assertMessageBody(
            GoogleService.QueryRequest.builder().withAddresses(ImmutableSet.of(addr)).build(),
            Transformers.inputToQueryMessage(Transformers.GSON.toJsonTree(payload, QueryPayload.class))
      );
   }

   @Test
   public void testInputToExecuteMessage() {
      MessageBody expected = GoogleService.ExecuteRequest.builder().withCommands(ImmutableList.of()).build();
      assertMessageBody(
            expected,
            Transformers.inputToExecuteMessage(null)
      );

      ExecutePayload payload = new ExecutePayload();
      assertMessageBody(
            expected,
            Transformers.inputToExecuteMessage(Transformers.GSON.toJsonTree(payload, ExecutePayload.class))
      );

      payload.setCommands(ImmutableList.of());

      assertMessageBody(
            expected,
            Transformers.inputToExecuteMessage(Transformers.GSON.toJsonTree(payload, ExecutePayload.class))
      );

      ExecutePayload.Command command = new ExecutePayload.Command();
      payload.setCommands(ImmutableList.of(command));

      assertMessageBody(
            expected,
            Transformers.inputToExecuteMessage(Transformers.GSON.toJsonTree(payload, ExecutePayload.class))
      );

      command.setDevices(ImmutableList.of());

      assertMessageBody(
            expected,
            Transformers.inputToExecuteMessage(Transformers.GSON.toJsonTree(payload, ExecutePayload.class))
      );

      String addr = Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation();
      command.setDevices(ImmutableList.of(ImmutableMap.of("id", addr)));

      assertMessageBody(
            expected,
            Transformers.inputToExecuteMessage(Transformers.GSON.toJsonTree(payload, ExecutePayload.class))
      );

      command.setExecution(ImmutableList.of());

      assertMessageBody(
            expected,
            Transformers.inputToExecuteMessage(Transformers.GSON.toJsonTree(payload, ExecutePayload.class))
      );

      ExecutePayload.Execution exec = new ExecutePayload.Execution();
      exec.setCommand(Commands.OnOff.name);
      exec.setParams(ImmutableMap.of("on", true));

      command.setExecution(ImmutableList.of(exec));

      GoogleCommand c = new GoogleCommand();
      c.setAddresses(ImmutableSet.of(addr));
      c.setParams(ImmutableMap.of("on", true));
      c.setCommand(Commands.OnOff.name);

      expected = GoogleService.ExecuteRequest.builder().withCommands(ImmutableList.of(c.toMap())).build();
      assertMessageBody(
            expected,
            Transformers.inputToExecuteMessage(Transformers.GSON.toJsonTree(payload, ExecutePayload.class))
      );
   }

   private void assertDeviceInfo(GoogleDeviceInfo expected, GoogleDeviceInfo actual) {
      if(expected == null) {
         assertNull(actual);
      }
      assertEquals(expected.toMap(), actual.toMap());
   }

   private void assertDeviceName(GoogleDeviceName expected, GoogleDeviceName actual) {
      if(expected == null) {
         assertNull(actual);
      }
      assertEquals(expected.toMap(), actual.toMap());
   }

   private void assertColor(GoogleColor expected, GoogleColor actual) {
      if(expected == null) {
         assertNull(actual);
      }
      assertEquals(expected.toMap(), actual.toMap());
   }

   private void assertDevice(GoogleDevice expected, GoogleDevice actual) {
      if(expected == null) {
         assertNull(actual);
      }
      assertEquals(expected.toMap(), actual.toMap());
   }

   private void assertErrorEvent(Throwable t, String code) {
      if(!(t instanceof ErrorEventException)) {
         fail("expected error event exception");
      }
      assertEquals(code, ((ErrorEventException) t).getCode());
   }

   private void assertMessageBody(MessageBody expected, MessageBody actual) {
      assertEquals(expected.getMessageType(), actual.getMessageType());
      assertEquals(expected.getAttributes(), actual.getAttributes());
   }

}

