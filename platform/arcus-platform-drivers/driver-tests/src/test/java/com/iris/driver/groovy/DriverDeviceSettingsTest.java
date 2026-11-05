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
package com.iris.driver.groovy;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.groovy.control.ControlProtocolPlugin;
import com.iris.driver.groovy.devicesettings.DeviceSettingsPlugin;
import com.iris.driver.groovy.pin.PinManagementPlugin;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.groovy.reflex.ReflexPlugin;
import com.iris.driver.groovy.scheduler.SchedulerPlugin;
import com.iris.driver.groovy.zwave.ZWaveProtocolPlugin;
import com.iris.messages.capability.DeviceSettingsCapability;
import com.iris.messages.model.Device;
import com.iris.protocol.constants.ZwaveConstants;

/**
 * Loads production drivers and checks which settings each Z-Wave product gets.
 */
public class DriverDeviceSettingsTest extends GroovyDriverTestCase {

   @Override
   protected Set<GroovyDriverPlugin> getPlugins() {
      return ImmutableSet.of(
         new SchedulerPlugin(),
         new ControlProtocolPlugin(),
         new ZWaveProtocolPlugin(),
         new ReflexPlugin(),
         new PinManagementPlugin(),
         new DeviceSettingsPlugin()
      );
   }

   @Override
   protected void configure(Binder binder) {
      super.configure(binder);
      Multibinder.newSetBinder(binder, org.codehaus.groovy.control.customizers.CompilationCustomizer.class)
         .addBinding()
         .toInstance(new ImportCustomizer()
            .addImports("groovy.transform.Field")
            .addStaticStars("java.util.concurrent.TimeUnit"));
   }

   @Test
   public void ringExtenderGenerationsGetTheirOwnSettings() throws Exception {
      String driver = "ZW_Ring_RangeExtender.driver";
      assertEquals(
            Arrays.asList("batteryReportInterval", "supervisionTimeout", "supervisionRetries",
                  "supervisionBackOff", "supervisedBatteryReports"),
            paramIds(driver, 0x0401, 0x0101));
      assertEquals(
            Arrays.asList("heartbeatInterval", "retryLimit", "retryDelay", "ledIndicator", "supervisionReportTimeout"),
            paramIds(driver, 0x0401, 0x0301));
      assertEquals(Arrays.asList(), paramIds(driver, 0x0401, 0x0201));
   }

   @Test
   public void outdoorSwitchSettingsOnlyOn500S() throws Exception {
      String driver = "ZW_Jasco_OutdoorSmartSwitch40Amp.driver";
      List<String> s500 = Arrays.asList("powerFailure", "energyReportMode", "energyReportFrequency", "alternateExclusion");
      assertEquals(s500, paramIds(driver, 0x4F44, 0x3032));
      assertEquals(s500, paramIds(driver, 0x5042, 0x4004));
      assertEquals(Arrays.asList(), paramIds(driver, 0x4F44, 0x3031));
   }

   @Test
   public void schlageDeadboltSettings() throws Exception {
      List<String> expected = Arrays.asList("beeper", "vacationMode", "lockAndLeave", "alarmMode", "alertSensitivity",
            "tamperSensitivity", "kickSensitivity", "alarmLocalControls", "autoLock");
      assertEquals(expected, paramIds("ZW_Schlage_Touch_Deadbolt_2_6.driver", 0x0001, 0x0469));
      assertEquals(expected, paramIds("ZW_Schlage_Touch_Deadbolt_2_6.driver", 0x6341, 0x5044));
   }

   @Test
   public void plugInDimmerSettings() throws Exception {
      assertEquals(
            Arrays.asList("smoothLevelChanges", "zwaveDimSteps", "zwaveDimRate", "manualDimSteps", "manualDimRate",
                  "allOnOffDimSteps", "allOnOffDimRate"),
            paramIds("ZW_Jasco_28167_PlugInDimmer_2_7.driver", 0x5044, 0x3037));
   }

   @Test
   public void t6ProStopsAtParameter42() throws Exception {
      List<Map<String, Object>> params = params("ZW_Honeywell_T6Pro_Thermostat_2_4.driver", 0x0011, 0x0008);
      assertEquals(42, params.size());
      Map<String, Object> last = params.get(params.size() - 1);
      assertEquals("temperatureOffset", last.get("paramId"));
      assertEquals(42, last.get("zwaveParamNo"));
   }

   private List<String> paramIds(String driverFile, int productType, int productId) throws Exception {
      List<String> ids = new ArrayList<>();
      for (Map<String, Object> p : params(driverFile, productType, productId)) {
         ids.add((String) p.get("paramId"));
      }
      return ids;
   }

   @SuppressWarnings("unchecked")
   private List<Map<String, Object>> params(String driverFile, int productType, int productId) throws Exception {
      DeviceDriver driver = factory.load(driverFile);
      Device device = createDevice(driver);
      AttributeMap protocolAttrs = AttributeMap.newMap();
      protocolAttrs.set(AttributeKey.create(ZwaveConstants.ATTR_PRODUCTTYPE, Integer.class), productType);
      protocolAttrs.set(AttributeKey.create(ZwaveConstants.ATTR_PRODUCTID, Integer.class), productId);
      device.setProtocolAttributes(protocolAttrs);

      PlatformDeviceDriverContext context = new PlatformDeviceDriverContext(device, driver, mockPopulationCacheMgr);
      driver.handleDriverEvent(DriverEvent.createConnected(0), context);
      return (List<Map<String, Object>>) (List<?>) context.getAttributeValue(DeviceSettingsCapability.KEY_PARAMS);
   }
}
