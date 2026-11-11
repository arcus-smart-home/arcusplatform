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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.devicesettings.DeviceSettingsContext;
import com.iris.driver.devicesettings.DeviceSettingsParamDefinition;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.control.ControlProtocolPlugin;
import com.iris.driver.groovy.devicesettings.DeviceSettingsPlugin;
import com.iris.driver.groovy.pin.PinManagementPlugin;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.groovy.reflex.ReflexPlugin;
import com.iris.driver.groovy.scheduler.SchedulerPlugin;
import com.iris.driver.groovy.zwave.ZWaveProtocolPlugin;
import com.iris.io.json.JSON;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceSettingsCapability;
import com.iris.messages.capability.IndicatorCapability;
import com.iris.messages.model.Device;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.constants.ZwaveConstants;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;
import com.iris.protocol.zwave.model.ZWaveNode;
import com.iris.util.TypeMarker;

/**
 * Loads production drivers and checks which settings each Z-Wave product gets.
 */
public class DriverDeviceSettingsTest extends GroovyDriverTestCase {

   private static final String DRIVER_DIR = "../../arcus-containers/driver-services/src/main/resources";

   /** Clients put labels beside controls, so they need to be short */
   private static final int MAX_LABEL_LENGTH = 32;

   /** A driver's settings block starts a line, unlike a mention of it in a comment */
   private static final Pattern SETTINGS_BLOCK = Pattern.compile("^deviceSettings\\b", Pattern.MULTILINE);

   /** Every setting of the driver loaded last, whatever product it's limited to */
   private DeviceSettingsContext loadedSettings;

   @Override
   protected Set<GroovyDriverPlugin> getPlugins() {
      return ImmutableSet.of(
         new SchedulerPlugin(),
         new ControlProtocolPlugin(),
         new ZWaveProtocolPlugin(),
         new ReflexPlugin(),
         new PinManagementPlugin(),
         new DeviceSettingsPlugin() {
            @Override
            public void postProcessEnvironment(EnvironmentBinding binding) {
               if (binding instanceof DriverBinding) {
                  loadedSettings = ((DriverBinding) binding).getBuilder().getDeviceSettingsContext();
               }
               super.postProcessEnvironment(binding);
            }
         }
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
   public void everySettingHasAShortLabel() throws Exception {
      File[] files = new File(DRIVER_DIR).listFiles((dir, name) -> name.endsWith(".driver"));
      assertTrue("no drivers in " + new File(DRIVER_DIR).getAbsolutePath(), files != null && files.length > 0);
      int drivers = 0;
      List<String> problems = new ArrayList<>();
      for (File file : files) {
         if (!SETTINGS_BLOCK.matcher(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).find()) {
            continue;
         }
         loadedSettings = null;
         factory.load(file.getName());
         assertNotNull(file.getName() + " has no deviceSettings block", loadedSettings);
         assertFalse(file.getName() + " has no settings", loadedSettings.getParams().isEmpty());
         drivers++;
         for (DeviceSettingsParamDefinition param : loadedSettings.getParams()) {
            String label = param.getLabel();
            if (label == null) {
               problems.add(file.getName() + " " + param.getParamId() + ": no label");
            } else if (label.length() > MAX_LABEL_LENGTH) {
               problems.add(file.getName() + " " + param.getParamId() + ": label longer than " + MAX_LABEL_LENGTH);
            }
         }
      }
      assertTrue("found no driver with a deviceSettings block", drivers > 0);
      assertEquals(Collections.emptyList(), problems);
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
      assertEquals(42L, last.get("zwaveParamNo"));
   }

   // A deploy that changes a driver's settings without a version bump is no upgrade, so the
   // driver starting is what brings the change to the attribute devices saved before it
   @Test
   @SuppressWarnings({ "unchecked", "rawtypes" })
   public void startingTheDriverReplacesSettingsSavedBeforeLabels() throws Exception {
      DeviceDriver driver = factory.load("ZW_Zooz_Zen77_DimmerSwitch.driver");
      PlatformDeviceDriverContext context = new PlatformDeviceDriverContext(createDevice(driver), driver, mockPopulationCacheMgr);
      Map<String, Object> savedBeforeLabels = new HashMap<>();
      savedBeforeLabels.put("paramId", "paddleOrientation");
      savedBeforeLabels.put("name", "paddleOrientation");
      savedBeforeLabels.put("type", "enum");
      context.setAttributeValue((AttributeKey) DeviceSettingsCapability.KEY_PARAMS, Collections.singletonList(savedBeforeLabels));
      context.clearDirty();

      driver.onRestored(context);

      List<Map<String, Object>> params = (List<Map<String, Object>>) (List<?>) context.getAttributeValue(DeviceSettingsCapability.KEY_PARAMS);
      assertEquals("Paddle orientation", params.get(0).get("name"));
      assertEquals(loadedSettings.getParams().size(), params.size());
      // saved with the device's next commit
      assertTrue(context.getDirtyAttributes().containsKey(DeviceSettingsCapability.KEY_PARAMS));
   }

   // what the same driver saved, loaded back as driver-services loads it, isn't saved again
   @Test
   @SuppressWarnings({ "unchecked", "rawtypes" })
   public void startingTheDriverLeavesUpToDateSettingsAlone() throws Exception {
      DeviceDriver driver = factory.load("ZW_Zooz_Zen77_DimmerSwitch.driver");
      PlatformDeviceDriverContext context = new PlatformDeviceDriverContext(createDevice(driver), driver, mockPopulationCacheMgr);
      driver.onRestored(context);
      String saved = JSON.toJson(context.getAttributeValue(DeviceSettingsCapability.KEY_PARAMS));
      Object loaded = JSON.fromJson(saved, TypeMarker.wrap(DeviceSettingsCapability.KEY_PARAMS.getType()));
      context.setAttributeValue((AttributeKey) DeviceSettingsCapability.KEY_PARAMS, loaded);
      context.clearDirty();

      driver.onRestored(context);

      assertFalse(context.getDirtyAttributes().containsKey(DeviceSettingsCapability.KEY_PARAMS));
   }

   // The ZEN77, like most drivers with settings, handles configuration reports itself, and its
   // handler comes first here, as in driver-services, since the Z-Wave plugin runs first
   @Test
   public void reportReachesTheSettingsAndTheDriver() throws Exception {
      DeviceDriver driver = factory.load("ZW_Zooz_Zen77_DimmerSwitch.driver");
      PlatformDeviceDriverContext context = new PlatformDeviceDriverContext(createDevice(driver), driver, mockPopulationCacheMgr);
      context.setConnected();

      // LED on when the switch is on
      driver.handleProtocolMessage(configurationReport(2, 1), context);

      assertEquals(1L, context.getVariable("devsettings:ledMode"));
      assertEquals(Boolean.TRUE, context.getAttributeValue(IndicatorCapability.KEY_INVERTED));
   }

   /** A one-byte configuration report from the device */
   private static ProtocolMessage configurationReport(int paramNo, int value) {
      ZWaveCommandClass configuration = ZWaveAllCommandClasses.getClass("configuration");
      ZWaveCommand command = new ZWaveCommand(configuration.number, configuration.commandsByName.get("report").commandNumber,
            new byte[] { (byte) paramNo, 1, (byte) value });
      ZWaveCommandMessage message = new ZWaveCommandMessage();
      message.setDevice(new ZWaveNode((byte) 0x02));
      message.setCommand(command);
      return ProtocolMessage.builder()
            .from(Address.protocolAddress("ZWAV", new byte[] { 0x02 }))
            .to(Address.broadcastAddress())
            .withPayload(ZWaveProtocol.INSTANCE, message)
            .create();
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
