/*
 * Copyright 2026 Arcus Project
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.nio.ByteOrder;
import java.util.List;
import java.util.Set;

import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.iris.driver.DeviceDriver;
import com.iris.driver.groovy.control.ControlProtocolPlugin;
import com.iris.driver.groovy.devicesettings.DeviceSettingsPlugin;
import com.iris.driver.groovy.pin.PinManagementPlugin;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.groovy.reflex.ReflexPlugin;
import com.iris.driver.groovy.scheduler.SchedulerPlugin;
import com.iris.driver.groovy.zwave.ZWaveProtocolPlugin;
import com.iris.driver.reflex.ReflexAction;
import com.iris.driver.reflex.ReflexActionSetAttribute;
import com.iris.driver.reflex.ReflexDriverDFA;
import com.iris.messages.capability.LeakH2OCapability;
import com.iris.protocol.zwave.Protocol;
import com.iris.protocol.zwave.ZWaveExternalProtocol;

/**
 * Runs the Z-Wave leak sensor drivers' hub reflexes against the frames the
 * sensors send, through the same DFA the hub matches with. The hub alarm only
 * sees a leak sensor whose state the hub keeps, so a report these miss never
 * reaches the water alarm.
 */
public class ZWaveLeakSensorReflexTest extends GroovyDriverTestCase {
   private static final int NODE = 3;

   private static final int NOTIFICATION = 0x71;
   private static final int NOTIFICATION_REPORT = 0x05;

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

   private static final int SENSOR_BINARY = 0x30;
   private static final int SENSOR_BINARY_REPORT = 0x03;
   private static final int BASIC = 0x20;
   private static final int BASIC_SET = 0x01;
   private static final int BASIC_REPORT = 0x03;
   private static final int BATTERY = 0x80;
   private static final int BATTERY_REPORT = 0x03;

   private static final String ZSE42 = "ZW_Zooz_Zse42_WaterLeakXsSensor.driver";
   private static final String ZSE30 = "ZW_Zooz_LeakSensor_2_4.driver";
   private static final String DOME = "ZW_Dome_LeakSensor_2_4.driver";
   private static final String WATER_SENSOR_6 = "ZW_Aeotec_WaterSensor6.driver";
   private static final String AEON = "ZW_Aeon_WaterSensor.driver";

   @Test
   public void ringFloodSensorBaseline() throws Exception {
      // The reflexes the other drivers copy, against the frames a ZSE42 sent
      assertEquals(LeakH2OCapability.STATE_LEAK, hubState("ZW_Ring_FloodFreezeSensor.driver", notification(0x05, 0x02)));
      assertEquals(LeakH2OCapability.STATE_SAFE, hubState("ZW_Ring_FloodFreezeSensor.driver", notification(0x05, 0x00)));
   }

   @Test
   public void zse42NotificationsSetTheHubsLeakState() throws Exception {
      assertLeakAndSafe(ZSE42, notification(0x05, 0x02), notification(0x05, 0x00));
      assertDriverChecksApplied(ZSE42);
      assertIgnored(ZSE42);
   }

   @Test
   public void zse30NotificationsAndBinaryReportsSetTheHubsLeakState() throws Exception {
      assertLeakAndSafe(ZSE30, notification(0x05, 0x02), notification(0x05, 0x00));
      assertBinaryReports(ZSE30);
      assertDriverChecksApplied(ZSE30);
      assertIgnored(ZSE30);
   }

   @Test
   public void domeNotificationsAndBinaryReportsSetTheHubsLeakState() throws Exception {
      assertLeakAndSafe(DOME, notification(0x05, 0x02), notification(0x05, 0x00));
      assertBinaryReports(DOME);
      assertDriverChecksApplied(DOME);
      assertIgnored(DOME);
   }

   @Test
   public void waterSensor6NotificationsBinaryAndBasicReportsSetTheHubsLeakState() throws Exception {
      assertLeakAndSafe(WATER_SENSOR_6, notification(0x05, 0x02), notification(0x05, 0x00));
      assertBinaryReports(WATER_SENSOR_6);
      // Basic Report V1, and V2 with target value and duration
      assertLeakAndSafe(WATER_SENSOR_6, frame(BASIC, BASIC_REPORT, 0xFF), frame(BASIC, BASIC_REPORT, 0x00));
      assertLeakAndSafe(WATER_SENSOR_6, frame(BASIC, BASIC_REPORT, 0xFF, 0xFF, 0x00), frame(BASIC, BASIC_REPORT, 0x00, 0x00, 0x00));
      assertIgnored(WATER_SENSOR_6);
   }

   @Test
   public void aeonBasicAndBinaryReportsSetTheHubsLeakState() throws Exception {
      assertLeakAndSafe(AEON, frame(BASIC, BASIC_REPORT, 0xFF), frame(BASIC, BASIC_REPORT, 0x00));
      assertLeakAndSafe(AEON, frame(BASIC, BASIC_SET, 0xFF), frame(BASIC, BASIC_SET, 0x00));
      assertBinaryReports(AEON);
      assertIgnored(AEON);
   }

   private void assertBinaryReports(String driverFile) throws Exception {
      // Sensor Binary V1, and V2 with the water sensor type
      assertLeakAndSafe(driverFile, frame(SENSOR_BINARY, SENSOR_BINARY_REPORT, 0xFF), frame(SENSOR_BINARY, SENSOR_BINARY_REPORT, 0x00));
      assertLeakAndSafe(driverFile, frame(SENSOR_BINARY, SENSOR_BINARY_REPORT, 0xFF, 0x06), frame(SENSOR_BINARY, SENSOR_BINARY_REPORT, 0x00, 0x06));
   }

   private void assertLeakAndSafe(String driverFile, byte[] leak, byte[] safe) throws Exception {
      assertEquals(LeakH2OCapability.STATE_LEAK, hubState(driverFile, leak));
      assertEquals(LeakH2OCapability.STATE_SAFE, hubState(driverFile, safe));
   }

   /** Notifications the driver's own handler refuses, so the hub must refuse them too. */
   private void assertDriverChecksApplied(String driverFile) throws Exception {
      assertNull(hubState(driverFile, frame(NOTIFICATION, NOTIFICATION_REPORT, 0, 0, 0, 0x00, 0x05, 0x02, 0)));
      assertNull(hubState(driverFile, frame(NOTIFICATION, NOTIFICATION_REPORT, 0x01, 0, 0, 0xFF, 0x05, 0x02, 0)));
      assertNull(hubState(driverFile, frame(NOTIFICATION, NOTIFICATION_REPORT, 0, 0x01, 0, 0xFF, 0x05, 0x00, 0)));
   }

   /** Frames that say nothing about water don't touch the hub's state. */
   private void assertIgnored(String driverFile) throws Exception {
      assertNull(hubState(driverFile, notification(0x07, 0x02)));
      assertNull(hubState(driverFile, notification(0x05, 0xFE)));
      assertNull(hubState(driverFile, frame(BATTERY, BATTERY_REPORT, 0x21)));
   }

   /** A Notification Report as the sensors send it: no V1 alarm, status on, no event parameters. */
   private static byte[] notification(int type, int event) throws Exception {
      return frame(NOTIFICATION, NOTIFICATION_REPORT, 0, 0, 0, 0xFF, type, event, 0);
   }

   /** The leak state a frame sets on the hub, or null if it sets none. */
   private @Nullable String hubState(String driverFile, byte[] frame) throws Exception {
      List<ReflexAction> actions = actions(driverFile, frame);
      if (actions == null) {
         return null;
      }
      for (ReflexAction action : actions) {
         if (action instanceof ReflexActionSetAttribute && LeakH2OCapability.ATTR_STATE.equals(((ReflexActionSetAttribute) action).getAttr())) {
            return String.valueOf(((ReflexActionSetAttribute) action).getValue());
         }
      }
      return null;
   }

   private @Nullable List<ReflexAction> actions(String driverFile, byte[] frame) throws Exception {
      DeviceDriver driver = factory.load(driverFile);
      ReflexDriverDFA dfa = driver.getDefinition().getReflexes().getDfa();
      assertNotNull(driverFile + " has no hub reflexes", dfa);
      return dfa.getDfa().matching(frame);
   }

   /** A command from the sensor as the hub's reflexes see it. */
   private static byte[] frame(int commandClass, int command, int... payload) throws Exception {
      byte[] bytes = new byte[payload.length];
      for (int i = 0; i < payload.length; i++) {
         bytes[i] = (byte) payload[i];
      }
      Protocol.Command cmd = Protocol.Command.builder()
         .setNodeId((byte) NODE)
         .setCommandClassId((byte) commandClass)
         .setCommandId((byte) command)
         .setPayload(bytes)
         .create();
      Protocol.Message msg = Protocol.Message.builder()
         .setType(Protocol.Command.ID)
         .setPayload(ByteOrder.BIG_ENDIAN, cmd)
         .create();
      return ZWaveExternalProtocol.INSTANCE.createSerializer().serialize(msg);
   }
}
