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
package com.iris.voice.alexa.reporting;

import java.util.List;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.alexa.AlexaInterfaces;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ColorCapability;
import com.iris.messages.capability.ColorTemperatureCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.LightCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.type.AlexaColor;
import com.iris.messages.type.AlexaPropertyReport;
import com.iris.messages.type.AlexaTemperature;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.voice.context.VoiceContext;

@Mocks({VoiceContext.class})
public class TestPropertyReporter extends IrisMockTestCase {

   @Inject
   private VoiceContext context;

   private Model colorBulb;
   private Model lock;
   private Model fan;
   private Model thermostat;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      colorBulb = new SimpleModel();
      colorBulb.setAttribute(
         Capability.ATTR_CAPS,
         ImmutableSet.of(
            SwitchCapability.NAMESPACE,
            DimmerCapability.NAMESPACE,
            LightCapability.NAMESPACE,
            ColorCapability.NAMESPACE,
            ColorTemperatureCapability.NAMESPACE,
            DeviceConnectionCapability.NAMESPACE,
            DeviceAdvancedCapability.NAMESPACE
         )
      );
      colorBulb.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      colorBulb.setAttribute(DimmerCapability.ATTR_BRIGHTNESS, 50);
      colorBulb.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLOR);
      colorBulb.setAttribute(ColorCapability.ATTR_SATURATION, 100);
      colorBulb.setAttribute(ColorCapability.ATTR_HUE, 120);
      colorBulb.setAttribute(ColorTemperatureCapability.ATTR_COLORTEMP, 3000);
      colorBulb.setAttribute(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
      colorBulb.setAttribute(DeviceAdvancedCapability.ATTR_PROTOCOL, "ZIGB");

      lock = new SimpleModel();
      lock.setAttribute(
         Capability.ATTR_CAPS,
         ImmutableSet.of(
            DoorLockCapability.NAMESPACE,
            DeviceConnectionCapability.NAMESPACE,
            DeviceAdvancedCapability.NAMESPACE
         )
      );
      lock.setAttribute(DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_LOCKED);
      lock.setAttribute(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
      lock.setAttribute(DeviceAdvancedCapability.ATTR_PROTOCOL, "ZWAV");

      fan = new SimpleModel();
      fan.setAttribute(
         Capability.ATTR_CAPS,
         ImmutableSet.of(
            SwitchCapability.NAMESPACE,
            FanCapability.NAMESPACE,
            DeviceConnectionCapability.NAMESPACE,
            DeviceAdvancedCapability.NAMESPACE
         )
      );
      fan.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON);
      fan.setAttribute(FanCapability.ATTR_SPEED, 2);
      fan.setAttribute(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
      fan.setAttribute(DeviceAdvancedCapability.ATTR_PROTOCOL, "ZWAV");

      thermostat = new SimpleModel();
      thermostat.setAttribute(
         Capability.ATTR_CAPS,
         ImmutableSet.of(
            TemperatureCapability.NAMESPACE,
            ThermostatCapability.NAMESPACE,
            DeviceConnectionCapability.NAMESPACE,
            DeviceAdvancedCapability.NAMESPACE
         )
      );
      thermostat.setAttribute(TemperatureCapability.ATTR_TEMPERATURE, 21.00);
      thermostat.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL);
      thermostat.setAttribute(ThermostatCapability.ATTR_COOLSETPOINT, 21.11);
      thermostat.setAttribute(ThermostatCapability.ATTR_HEATSETPOINT, 19.00);
      thermostat.setAttribute(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
      thermostat.setAttribute(DeviceAdvancedCapability.ATTR_PROTOCOL, "ZWAV");
   }

   @Test
   public void testColorBulbInColorMode() {
      EasyMock.expect(context.isHubOffline()).andReturn(false);
      replay();
      List<AlexaPropertyReport> reports = PropertyReporter.report(context, colorBulb);
      verify();
      assertEquals(4, reports.size());
      assertReportsContains(AlexaInterfaces.PowerController.PROP_POWERSTATE, SwitchCapability.STATE_ON, reports);
      assertReportsContains(AlexaInterfaces.BrightnessController.PROP_BRIGHTNESS, 50, reports);

      AlexaColor c = new AlexaColor();
      c.setHue(120.0);
      c.setSaturation(1.0);
      c.setBrightness(0.5);

      assertReportsContains(AlexaInterfaces.ColorController.PROP_COLOR, c.toMap(), reports);
      assertReportsContains(AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY, ImmutableMap.of("value", "OK"), reports);
   }

   @Test
   public void testColorBulbInColorTempMode() {
      colorBulb.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_COLORTEMP);
      EasyMock.expect(context.isHubOffline()).andReturn(false);
      replay();
      List<AlexaPropertyReport> reports = PropertyReporter.report(context, colorBulb);
      verify();
      assertEquals(4, reports.size());
      assertReportsContains(AlexaInterfaces.PowerController.PROP_POWERSTATE, SwitchCapability.STATE_ON, reports);
      assertReportsContains(AlexaInterfaces.BrightnessController.PROP_BRIGHTNESS, 50, reports);
      assertReportsContains(AlexaInterfaces.ColorTemperatureController.PROP_COLORTEMPERATUREINKELVIN, 3000, reports);
      assertReportsContains(AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY, ImmutableMap.of("value", "OK"), reports);
   }

   @Test
   public void testColorBulbInNormalMode() {
      colorBulb.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_NORMAL);
      EasyMock.expect(context.isHubOffline()).andReturn(false);
      replay();
      List<AlexaPropertyReport> reports = PropertyReporter.report(context, colorBulb);
      verify();
      assertEquals(3, reports.size());
      assertReportsContains(AlexaInterfaces.PowerController.PROP_POWERSTATE, SwitchCapability.STATE_ON, reports);
      assertReportsContains(AlexaInterfaces.BrightnessController.PROP_BRIGHTNESS, 50, reports);
      assertReportsContains(AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY, ImmutableMap.of("value", "OK"), reports);
   }

   @Test
   public void testDeviceOffline() {
      colorBulb.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_NORMAL);
      colorBulb.setAttribute(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
      EasyMock.expect(context.isHubOffline()).andReturn(false);
      replay();
      List<AlexaPropertyReport> reports = PropertyReporter.report(context, colorBulb);
      verify();
      assertEquals(3, reports.size());
      assertReportsContains(AlexaInterfaces.PowerController.PROP_POWERSTATE, SwitchCapability.STATE_ON, reports);
      assertReportsContains(AlexaInterfaces.BrightnessController.PROP_BRIGHTNESS, 50, reports);
      assertReportsContains(AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY, ImmutableMap.of("value", "UNREACHABLE"), reports);
   }

   @Test
   public void testDeviceOfflineHubOffline() {
      colorBulb.setAttribute(LightCapability.ATTR_COLORMODE, LightCapability.COLORMODE_NORMAL);
      EasyMock.expect(context.isHubOffline()).andReturn(true);
      replay();
      List<AlexaPropertyReport> reports = PropertyReporter.report(context, colorBulb);
      verify();
      assertEquals(3, reports.size());
      assertReportsContains(AlexaInterfaces.PowerController.PROP_POWERSTATE, SwitchCapability.STATE_ON, reports);
      assertReportsContains(AlexaInterfaces.BrightnessController.PROP_BRIGHTNESS, 50, reports);
      assertReportsContains(AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY, ImmutableMap.of("value", "UNREACHABLE"), reports);
   }

   @Test
   public void testLock() {
      EasyMock.expect(context.isHubOffline()).andReturn(false);
      replay();
      List<AlexaPropertyReport> reports = PropertyReporter.report(context, lock);
      verify();
      assertEquals(2, reports.size());
      assertReportsContains(AlexaInterfaces.LockController.PROP_LOCKSTATE, DoorLockCapability.LOCKSTATE_LOCKED, reports);
      assertReportsContains(AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY, ImmutableMap.of("value", "OK"), reports);
   }

   @Test
   public void testLockJammed() {
      lock.setAttribute(DeviceAdvancedCapability.ATTR_ERRORS, ImmutableMap.of("WARN_JAM", "jammed"));
      EasyMock.expect(context.isHubOffline()).andReturn(false);
      replay();
      List<AlexaPropertyReport> reports = PropertyReporter.report(context, lock);
      verify();
      assertEquals(2, reports.size());
      assertReportsContains(AlexaInterfaces.LockController.PROP_LOCKSTATE, "JAMMED", reports);
      assertReportsContains(AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY, ImmutableMap.of("value", "OK"), reports);
   }

   @Test
   public void testFan() {
      EasyMock.expect(context.isHubOffline()).andReturn(false);
      replay();
      List<AlexaPropertyReport> reports = PropertyReporter.report(context, fan);
      verify();
      assertEquals(3, reports.size());
      assertReportsContains(AlexaInterfaces.PowerController.PROP_POWERSTATE, SwitchCapability.STATE_ON, reports);
      assertReportsContains(AlexaInterfaces.PercentageController.PROP_PERCENTAGE, 67, reports);
      assertReportsContains(AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY, ImmutableMap.of("value", "OK"), reports);
   }

   @Test
   public void testFanOff() {
      fan.setAttribute(SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF);
      EasyMock.expect(context.isHubOffline()).andReturn(false);
      replay();
      List<AlexaPropertyReport> reports = PropertyReporter.report(context, fan);
      verify();
      assertEquals(3, reports.size());
      assertReportsContains(AlexaInterfaces.PowerController.PROP_POWERSTATE, SwitchCapability.STATE_OFF, reports);
      assertReportsContains(AlexaInterfaces.PercentageController.PROP_PERCENTAGE, 0, reports);
      assertReportsContains(AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY, ImmutableMap.of("value", "OK"), reports);
   }

   @Test
   public void testThermostatCool() {

      AlexaTemperature sp = new AlexaTemperature();
      sp.setValue(21.11);
      sp.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaTemperature temp = new AlexaTemperature();
      temp.setValue(21.00);
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);

      EasyMock.expect(context.isHubOffline()).andReturn(false);
      replay();
      List<AlexaPropertyReport> reports = PropertyReporter.report(context, thermostat);
      verify();
      assertEquals(4, reports.size());
      assertReportsContains(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE, ThermostatCapability.HVACMODE_COOL, reports);
      assertReportsContains(AlexaInterfaces.ThermostatController.PROP_TARGETSETPOINT, sp.toMap(), reports);
      assertReportsContains(AlexaInterfaces.TemperatureSensor.PROP_TEMPERATURE, temp.toMap(), reports);
      assertReportsContains(AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY, ImmutableMap.of("value", "OK"), reports);
   }

   @Test
   public void testThermostatHeat() {

      thermostat.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT);

      AlexaTemperature sp = new AlexaTemperature();
      sp.setValue(19.00);
      sp.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaTemperature temp = new AlexaTemperature();
      temp.setValue(21.00);
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);

      EasyMock.expect(context.isHubOffline()).andReturn(false);
      replay();
      List<AlexaPropertyReport> reports = PropertyReporter.report(context, thermostat);
      verify();
      assertEquals(4, reports.size());
      assertReportsContains(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE, ThermostatCapability.HVACMODE_HEAT, reports);
      assertReportsContains(AlexaInterfaces.ThermostatController.PROP_TARGETSETPOINT, sp.toMap(), reports);
      assertReportsContains(AlexaInterfaces.TemperatureSensor.PROP_TEMPERATURE, temp.toMap(), reports);
      assertReportsContains(AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY, ImmutableMap.of("value", "OK"), reports);
   }

   @Test
   public void testThermostatAuto() {

      thermostat.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO);

      AlexaTemperature lowerSp = new AlexaTemperature();
      lowerSp.setValue(19.00);
      lowerSp.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaTemperature upperSp = new AlexaTemperature();
      upperSp.setValue(21.11);
      upperSp.setScale(AlexaTemperature.SCALE_CELSIUS);

      AlexaTemperature temp = new AlexaTemperature();
      temp.setValue(21.00);
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);

      EasyMock.expect(context.isHubOffline()).andReturn(false);
      replay();
      List<AlexaPropertyReport> reports = PropertyReporter.report(context, thermostat);
      verify();
      assertEquals(5, reports.size());
      assertReportsContains(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE, ThermostatCapability.HVACMODE_AUTO, reports);
      assertReportsContains(AlexaInterfaces.ThermostatController.PROP_LOWERSETPOINT, lowerSp.toMap(), reports);
      assertReportsContains(AlexaInterfaces.ThermostatController.PROP_UPPERSETPOINT, upperSp.toMap(), reports);
      assertReportsContains(AlexaInterfaces.TemperatureSensor.PROP_TEMPERATURE, temp.toMap(), reports);
      assertReportsContains(AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY, ImmutableMap.of("value", "OK"), reports);
   }

   @Test
   public void testThermostatOff() {

      thermostat.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF);

      AlexaTemperature temp = new AlexaTemperature();
      temp.setValue(21.00);
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);

      EasyMock.expect(context.isHubOffline()).andReturn(false);
      replay();
      List<AlexaPropertyReport> reports = PropertyReporter.report(context, thermostat);
      verify();
      assertEquals(3, reports.size());
      assertReportsContains(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE, ThermostatCapability.HVACMODE_OFF, reports);
      assertReportsContains(AlexaInterfaces.TemperatureSensor.PROP_TEMPERATURE, temp.toMap(), reports);
      assertReportsContains(AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY, ImmutableMap.of("value", "OK"), reports);
   }

   @Test
   public void testThermostatEco() {

      thermostat.setAttribute(ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_ECO);

      AlexaTemperature temp = new AlexaTemperature();
      temp.setValue(21.00);
      temp.setScale(AlexaTemperature.SCALE_CELSIUS);

      EasyMock.expect(context.isHubOffline()).andReturn(false);
      replay();
      List<AlexaPropertyReport> reports = PropertyReporter.report(context, thermostat);
      verify();
      assertEquals(3, reports.size());
      assertReportsContains(AlexaInterfaces.ThermostatController.PROP_THERMOSTATMODE, ThermostatCapability.HVACMODE_ECO, reports);
      assertReportsContains(AlexaInterfaces.TemperatureSensor.PROP_TEMPERATURE, temp.toMap(), reports);
      assertReportsContains(AlexaInterfaces.EndpointHealth.PROP_CONNECTIVITY, ImmutableMap.of("value", "OK"), reports);
   }

   private void assertReportsContains(String property, Object value, List<AlexaPropertyReport> reports) {
      for(AlexaPropertyReport report : reports) {
         if(report.getName().equals(property)) {
            assertEquals(property + " is not expected value", value, report.getValue());
            return;
         }
      }
      fail("no report for " + property + " found");
   }
}

