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
package com.iris.platform.history.appender;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.common.alarm.AlertType;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.ButtonCapability;
import com.iris.messages.capability.CarbonMonoxideCapability;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.GlassCapability;
import com.iris.messages.capability.LeakGasCapability;
import com.iris.messages.capability.LeakH2OCapability;
import com.iris.messages.capability.MotionCapability;
import com.iris.messages.capability.MotorizedDoorCapability;
import com.iris.messages.capability.PetDoorCapability;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.capability.TiltCapability;
import com.iris.messages.capability.ValveCapability;
import com.iris.messages.capability.WaterSoftenerCapability;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.alarm.incident.AlarmIncident.MonitoringState;
import com.iris.platform.alarm.incident.AlarmIncidentDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.appender.devvc.DeviceButtonAppender;
import com.iris.platform.history.appender.devvc.DeviceCOAppender;
import com.iris.platform.history.appender.devvc.DeviceContactAppender;
import com.iris.platform.history.appender.devvc.DeviceDimmerAppender;
import com.iris.platform.history.appender.devvc.DeviceDoorLockAppender;
import com.iris.platform.history.appender.devvc.DeviceFanAppender;
import com.iris.platform.history.appender.devvc.DeviceGasLeakAppender;
import com.iris.platform.history.appender.devvc.DeviceGlassAppender;
import com.iris.platform.history.appender.devvc.DeviceMotionAppender;
import com.iris.platform.history.appender.devvc.DeviceMotorizedDoorAppender;
import com.iris.platform.history.appender.devvc.DevicePetDoorAppender;
import com.iris.platform.history.appender.devvc.DevicePowerAppender;
import com.iris.platform.history.appender.devvc.DeviceSmokeAppender;
import com.iris.platform.history.appender.devvc.DeviceSwitchAppender;
import com.iris.platform.history.appender.devvc.DeviceThermostatAppender;
import com.iris.platform.history.appender.devvc.DeviceTiltAppender;
import com.iris.platform.history.appender.devvc.DeviceValveAppender;
import com.iris.platform.history.appender.devvc.DeviceWaterLeakAppender;
import com.iris.platform.history.appender.devvc.DeviceWaterSoftenerAppender;
import com.iris.test.Mocks;
import com.iris.test.Modules;
import com.iris.util.UnitConversion;

@Modules({TestDeviceAppenderModule.class})
@Mocks({AlarmIncidentDAO.class})
public class TestDeviceAppenders extends EventAppenderTestCase {

   private final static UUID INCIDENT_ID = UUID.fromString("2aeaf09e-2804-408e-960d-8218449e5a23");

   private final static Address ACTOR_ALARM_SUBSYSTEM = Address.platformService(AlarmSubsystemCapability.NAMESPACE);
   private final static Address ACTOR_NON_ALARM_SUBSYSTEM = Address.platformService(CareSubsystemCapability.NAMESPACE);
   private final static Address ACTOR_INCIDENT = Address.platformService(INCIDENT_ID, AlarmIncidentCapability.NAMESPACE);

   @Inject private AlarmIncidentDAO mockAlarmIncidentDao;

   @Inject private DeviceButtonAppender buttonAppender;
	@Inject private DevicePowerAppender powerAppender;
	@Inject private DeviceContactAppender contactAppender;
	@Inject private DeviceSwitchAppender switchAppender;
	@Inject private DeviceDimmerAppender dimmerAppender;
	@Inject private DeviceMotionAppender motionAppender;
	@Inject private DeviceSmokeAppender smokeAppender;
	@Inject private DeviceCOAppender coAppender;
	@Inject private DeviceGlassAppender glassAppender;
	@Inject private DeviceWaterLeakAppender waterLeakAppender;
	@Inject private DeviceGasLeakAppender gasLeakAppender;
	@Inject private DeviceMotorizedDoorAppender motorizedDoorAppender;
	@Inject private DeviceDoorLockAppender doorLockAppender;
	@Inject private DevicePetDoorAppender petdoorAppender;
	@Inject private DeviceFanAppender fanAppender;
	@Inject private DeviceValveAppender valveAppender;
	@Inject private DeviceTiltAppender titleAppender;
	@Inject private DeviceWaterSoftenerAppender softAppender;
	@Inject private DeviceThermostatAppender thermostatAppender;

	@Test public void testWaterSoftenerRecharge() {
		TestContext ctx = ctxbuild("watersoftener.recharging", true)
									.build();
		testValueChange(softAppender, WaterSoftenerCapability.ATTR_RECHARGESTATUS, WaterSoftenerCapability.RECHARGESTATUS_RECHARGING, ctx);
	}

	@Test public void testWaterSoftenerRechargePers() {
		TestContext ctx = ctxbuild("watersoftener.recharging.pers", true)
									.withValue(2, PERSON_NAME)
									.build();
		testValueChange(softAppender, ACTOR_PERSON, WaterSoftenerCapability.ATTR_RECHARGESTATUS, WaterSoftenerCapability.RECHARGESTATUS_RECHARGING, ctx);
	}

	@Test public void testWaterSoftenerRechargeRule() {
		TestContext ctx = ctxbuild("watersoftener.recharging.rule", true)
									.withValue(2,  RULE_NAME)
									.build();
		testValueChange(softAppender, ACTOR_RULE, WaterSoftenerCapability.ATTR_RECHARGESTATUS, WaterSoftenerCapability.RECHARGESTATUS_RECHARGING, ctx);
	}

	@Test public void testTitleUpright() {
		TestContext ctx = ctxbuild("tilt.upright", true).build();
		testValueChange(titleAppender, TiltCapability.ATTR_TILTSTATE, TiltCapability.TILTSTATE_UPRIGHT, ctx);
	}

	@Test public void testTitleFlat() {
		TestContext ctx = ctxbuild("tilt.flat", true).build();
		testValueChange(titleAppender, TiltCapability.ATTR_TILTSTATE, TiltCapability.TILTSTATE_FLAT, ctx);
	}

	@Test public void testValveOpen() {
		TestContext ctx = ctxbuild("valve.opened", true).build();
		testValueChange(valveAppender, ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_OPEN, ctx);
	}

	@Test public void testValveOpenPers() {
		TestContext ctx = ctxbuild("valve.opened.pers", true)
									.withValue(2,  PERSON_NAME)
									.build();
		testValueChange(valveAppender, ACTOR_PERSON, ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_OPEN, ctx);
	}

	@Test public void testValveOpenRule() {
		TestContext ctx = ctxbuild("valve.opened.rule", true)
									.withValue(2, RULE_NAME)
									.build();
		testValueChange(valveAppender, ACTOR_RULE, ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_OPEN, ctx);
	}

	@Test public void testValveClose() {
		TestContext ctx = ctxbuild("valve.closed", true).build();
		testValueChange(valveAppender, ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_CLOSED, ctx);
	}

	@Test public void testValveClosePers() {
		TestContext ctx = ctxbuild("valve.closed.pers", true)
									.withValue(2,  PERSON_NAME)
									.build();
		testValueChange(valveAppender, ACTOR_PERSON, ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_CLOSED, ctx);
	}

	@Test public void testValveCloseRule() {
		TestContext ctx = ctxbuild("valve.closed.rule", true)
									.withValue(2, RULE_NAME)
									.build();
		testValueChange(valveAppender, ACTOR_RULE, ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_CLOSED, ctx);
	}

	@Test public void testValveObstruct() {
		TestContext ctx = ctxbuild("valve.obstruction", true).build();
		testValueChange(valveAppender, ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_OBSTRUCTION, ctx);
	}

	@Test public void testValveObstructPers() {
		TestContext ctx = ctxbuild("valve.obstruction.pers", true)
									.withValue(2,  PERSON_NAME)
									.build();
		testValueChange(valveAppender, ACTOR_PERSON, ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_OBSTRUCTION, ctx);
	}

	@Test public void testValveObstructRule() {
		TestContext ctx = ctxbuild("valve.obstruction.rule", true)
									.withValue(2,  RULE_NAME)
									.build();
		testValueChange(valveAppender, ACTOR_RULE, ValveCapability.ATTR_VALVESTATE, ValveCapability.VALVESTATE_OBSTRUCTION, ctx);
	}

	@Test public void testFanSpeed() {
		String speed = "55";
		TestContext ctx = ctxbuild("fan.speed", true)
				.withValue(4,  speed)
				.build();
		testValueChange(fanAppender, FanCapability.ATTR_SPEED, speed, ctx);
	}

	@Test public void testFanSpeedPers() {
		String speed = "55";
		TestContext ctx = ctxbuild("fan.speed.pers", true)
									.withValue(2,  PERSON_NAME)
									.withValue(4,  speed)
									.build();
		testValueChange(fanAppender, ACTOR_PERSON, FanCapability.ATTR_SPEED, speed, ctx);
	}

	@Test public void testFanSpeedRule() {
		String speed = "55";
		TestContext ctx = ctxbuild("fan.speed.rule", true)
									.withValue(2,  RULE_NAME)
									.withValue(4,  speed)
									.build();
		testValueChange(fanAppender, ACTOR_RULE, FanCapability.ATTR_SPEED, speed, ctx);
	}

	@Test public void testDoorLocked() {
		TestContext ctx = ctxbuild("doorlock.locked", true)
									.build();
		testValueChange(doorLockAppender, DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_LOCKED, ctx);
	}	

	@Test public void testDoorLockedPers() {
		TestContext ctx = ctxbuild("doorlock.locked.pers", true)
									.withValue(2, PERSON_NAME)
									.build();
		testValueChange(doorLockAppender, ACTOR_PERSON, DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_LOCKED, ctx);
	}

	@Test public void testDoorLockedRule() {
		TestContext ctx = ctxbuild("doorlock.locked.rule", true)
									.withValue(2,  RULE_NAME)
									.build();
		testValueChange(doorLockAppender, ACTOR_RULE, DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_LOCKED, ctx);
	}

	@Test public void testDoorUnlocked() {
		TestContext ctx = ctxbuild("doorlock.unlocked", true).build();
		testValueChange(doorLockAppender, DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_UNLOCKED, ctx);
	}

	@Test public void testDoorUnlockedPers() {
		TestContext ctx = ctxbuild("doorlock.unlocked.pers", true)
									.withValue(2, PERSON_NAME)
									.build();
		testValueChange(doorLockAppender, ACTOR_PERSON, DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_UNLOCKED, ctx);
	}

	@Test public void testDoorUnlockedRule() {
		TestContext ctx = ctxbuild("doorlock.unlocked.rule", true)
									.withValue(2, RULE_NAME)
									.build();
		testValueChange(doorLockAppender, ACTOR_RULE, DoorLockCapability.ATTR_LOCKSTATE, DoorLockCapability.LOCKSTATE_UNLOCKED, ctx);
	}

	@Test public void testMotorizedDoorOpened() {
		TestContext ctx = ctxbuild("motdoor.opened", true).build();
		testValueChange(motorizedDoorAppender, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OPEN, ctx);
	}

	@Test public void testMotorizedDoorOpenedPers() {
		TestContext ctx = ctxbuild("motdoor.opened.pers", true)
									.withValue(2, PERSON_NAME)
									.build();
		testValueChange(motorizedDoorAppender, ACTOR_PERSON, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OPEN, ctx);
	}

	@Test public void testMotorizedDoorOpenedRule() {
		TestContext ctx = ctxbuild("motdoor.opened.rule", true)
									.withValue(2, RULE_NAME)
									.build();
		testValueChange(motorizedDoorAppender, ACTOR_RULE, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OPEN, ctx);
	}

	@Test public void testMotorizedDoorOpenedInst() {
		TestContext ctx = ctxbuild("motdoor.opened.inst", true)
									.withValue(1, INSTANCE_FENRIS)
									.build();
		testValueChange(motorizedDoorAppender, INSTANCE_FENRIS, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OPEN, ctx);
	}

	@Test public void testMotorizedDoorOpenedInstPers() {
		TestContext ctx = ctxbuild("motdoor.opened.inst.pers", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(2, PERSON_NAME)
									.build();
		testValueChange(motorizedDoorAppender, INSTANCE_FENRIS, ACTOR_PERSON, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OPEN, ctx);
	}

	@Test public void testMotorizedDoorOpenedInstRule() {
		TestContext ctx = ctxbuild("motdoor.opened.inst.rule", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(2, RULE_NAME)
									.build();
		testValueChange(motorizedDoorAppender, INSTANCE_FENRIS, ACTOR_RULE, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OPEN, ctx);
	}

	@Test public void testMotorizedDoorClosed() {
		TestContext ctx = ctxbuild("motdoor.closed", true).build();
		testValueChange(motorizedDoorAppender, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_CLOSED, ctx);
	}

	@Test public void testMotorizedDoorClosedPers() {
		TestContext ctx = ctxbuild("motdoor.closed.pers", true)
									.withValue(2, PERSON_NAME)
									.build();
		testValueChange(motorizedDoorAppender, ACTOR_PERSON, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_CLOSED, ctx);
	}

	@Test public void testMotorizedDoorClosedRule() {
		TestContext ctx = ctxbuild("motdoor.closed.rule", true)
									.withValue(2, RULE_NAME)
									.build();
		testValueChange(motorizedDoorAppender, ACTOR_RULE, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_CLOSED, ctx);
	}

	@Test public void testMotorizedDoorClosedInst() {
		TestContext ctx = ctxbuild("motdoor.closed.inst", true)
									.withValue(1, INSTANCE_FENRIS)
									.build();
		testValueChange(motorizedDoorAppender, INSTANCE_FENRIS, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_CLOSED, ctx);
	}

	@Test public void testMotorizedDoorClosedInstPers() {
		TestContext ctx = ctxbuild("motdoor.closed.inst.pers", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(2, PERSON_NAME)
									.build();
		testValueChange(motorizedDoorAppender, INSTANCE_FENRIS, ACTOR_PERSON, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_CLOSED, ctx);
	}

	@Test public void testMotorizedDoorClosedInstRule() {
		TestContext ctx = ctxbuild("motdoor.closed.inst.rule", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(2, RULE_NAME)
									.build();
		testValueChange(motorizedDoorAppender, INSTANCE_FENRIS, ACTOR_RULE, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_CLOSED, ctx);
	}

	@Test public void testMotorizedDoorObstruct() {
		TestContext ctx = ctxbuild("motdoor.obstruction", true).build();
		testValueChange(motorizedDoorAppender, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OBSTRUCTION, ctx);
	}

	@Test public void testMotorizedDoorObstructPers() {
		TestContext ctx = ctxbuild("motdoor.obstruction.pers", true)
									.withValue(2, PERSON_NAME)
									.build();
		testValueChange(motorizedDoorAppender, ACTOR_PERSON, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OBSTRUCTION, ctx);
	}

	@Test public void testMotorizedDoorObstructRule() {
		TestContext ctx = ctxbuild("motdoor.obstruction.rule", true)
									.withValue(2, RULE_NAME)
									.build();
		testValueChange(motorizedDoorAppender, ACTOR_RULE, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OBSTRUCTION, ctx);
	}

	@Test public void testMotorizedDoorObstructInst() {
		TestContext ctx = ctxbuild("motdoor.obstruction.inst", true)
									.withValue(1, INSTANCE_FENRIS)
									.build();
		testValueChange(motorizedDoorAppender, INSTANCE_FENRIS, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OBSTRUCTION, ctx);
	}

	@Test public void testMotorizedDoorObstructInstPers() {
		TestContext ctx = ctxbuild("motdoor.obstruction.inst.pers", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(2, PERSON_NAME)
									.build();
		testValueChange(motorizedDoorAppender, INSTANCE_FENRIS, ACTOR_PERSON, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OBSTRUCTION, ctx);
	}

	@Test public void testMotorizedDoorObstructInstRule() {
		TestContext ctx = ctxbuild("motdoor.obstruction.inst.rule", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(2, RULE_NAME)
									.build();
		testValueChange(motorizedDoorAppender, INSTANCE_FENRIS, ACTOR_RULE, MotorizedDoorCapability.ATTR_DOORSTATE, MotorizedDoorCapability.DOORSTATE_OBSTRUCTION, ctx);
	}

	@Test public void testGasLeakDetect() {
		TestContext ctx = ctxbuild("gasleak.detected", true).build();
		testValueChange(gasLeakAppender, LeakGasCapability.ATTR_STATE, LeakGasCapability.STATE_LEAK, ctx);
	}

	@Test public void testGasLeakNone() {
		TestContext ctx = ctxbuild("gasleak.none", false).build();
		testValueChange(gasLeakAppender, LeakGasCapability.ATTR_STATE, LeakGasCapability.STATE_SAFE, ctx);
	}

	@Test public void testWaterLeakDetect() {
		TestContext ctx = ctxbuild("waterleak.detected", true).build();
		testValueChange(waterLeakAppender, LeakH2OCapability.ATTR_STATE, LeakH2OCapability.STATE_LEAK, ctx);
	}

	@Test public void testWaterLeakSafe() {
		TestContext ctx = ctxbuild("waterleak.safe", false).build();
		testValueChange(waterLeakAppender, LeakH2OCapability.ATTR_STATE, LeakH2OCapability.STATE_SAFE, ctx);
	}

	@Test public void testGlassDetect() {
		TestContext ctx = ctxbuild("glassbreak.detected", true).build();
		testValueChange(glassAppender, GlassCapability.ATTR_BREAK, GlassCapability.BREAK_DETECTED, ctx);
	}

	@Test public void testGlassNone() {
		TestContext ctx = ctxbuild("glassbreak.none", false).build();
		testValueChange(glassAppender, GlassCapability.ATTR_BREAK, GlassCapability.BREAK_SAFE, ctx);
	}

	@Test public void testCoDetect() {
		TestContext ctx = ctxbuild("co.detected", true).build();
		testValueChange(coAppender, CarbonMonoxideCapability.ATTR_CO, CarbonMonoxideCapability.CO_DETECTED, ctx);
	}

	@Test public void testCoSafe() {
		TestContext ctx = ctxbuild("co.none", false).build();
		testValueChange(coAppender, CarbonMonoxideCapability.ATTR_CO, CarbonMonoxideCapability.CO_SAFE, ctx);
	}

	@Test public void testSmokeDetect() {
		TestContext ctx = ctxbuild("smoke.detected", true).build();
		testValueChange(smokeAppender, SmokeCapability.ATTR_SMOKE, SmokeCapability.SMOKE_DETECTED, ctx);
	}

	@Test public void testSmokeSafe() {
		TestContext ctx = ctxbuild("smoke.none", false).build();
		testValueChange(smokeAppender, SmokeCapability.ATTR_SMOKE, SmokeCapability.SMOKE_SAFE, ctx);
	}

	@Test public void testMotionDetect() {
		TestContext ctx = ctxbuild("motion.detected", false).build();
		testValueChange(motionAppender, MotionCapability.ATTR_MOTION, MotionCapability.MOTION_DETECTED, ctx);
	}

	@Test public void testMotionNone() {
		TestContext ctx = ctxbuild("motion.none", false).build();
		testValueChange(motionAppender, MotionCapability.ATTR_MOTION, MotionCapability.MOTION_NONE, ctx);
	}

	@Test public void testDimmerBright() {
		String brightness = "42";
		TestContext ctx = ctxbuild("dimmer.brightness", true).withValue(4, brightness).build();
		testValueChange(dimmerAppender, DimmerCapability.ATTR_BRIGHTNESS, brightness, ctx);
	}

	@Test public void testDimmerBrightPers() {
		String brightness = "42";
		TestContext ctx = ctxbuild("dimmer.brightness.pers", true)
									.withValue(2, PERSON_NAME)
									.withValue(4, brightness)
									.build();
		testValueChange(dimmerAppender, ACTOR_PERSON, DimmerCapability.ATTR_BRIGHTNESS, brightness, ctx);
	}

	@Test public void testDimmerBrightRule() {
		String brightness = "42";
		TestContext ctx = ctxbuild("dimmer.brightness.rule", true)
									.withValue(2, RULE_NAME)
									.withValue(4, brightness)
									.build();
		testValueChange(dimmerAppender, ACTOR_RULE, DimmerCapability.ATTR_BRIGHTNESS, brightness, ctx);
	}

	@Test public void testDimmerBrightInst() {
		String brightness = "42";
		TestContext ctx = ctxbuild("dimmer.brightness.inst", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(4, brightness)
									.build();
		testValueChange(dimmerAppender, INSTANCE_FENRIS, DimmerCapability.ATTR_BRIGHTNESS, brightness, ctx);
	}

	@Test public void testDimmerBrightInstPers() {
		String brightness = "42";
		TestContext ctx = ctxbuild("dimmer.brightness.inst.pers", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(2, PERSON_NAME)
									.withValue(4, brightness)
									.build();
		testValueChange(dimmerAppender, INSTANCE_FENRIS, ACTOR_PERSON, DimmerCapability.ATTR_BRIGHTNESS, brightness, ctx);
	}

	@Test public void testDimmerBrightInstRule() {
		String brightness = "42";
		TestContext ctx = ctxbuild("dimmer.brightness.inst.rule", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(2, RULE_NAME)
									.withValue(4, brightness)
									.build();
		testValueChange(dimmerAppender, INSTANCE_FENRIS, ACTOR_RULE, DimmerCapability.ATTR_BRIGHTNESS, brightness, ctx);
	}

   @Test public void testSwitchFanShutoff() {
      TestContext ctx = ctxbuild("alarm.smoke.shutoff", true, false)
            .withValue(4, SwitchCapability.STATE_OFF)
            .withLogType(ALARM)
            .build();
      expectIncident();
      testValueChange(switchAppender, ACTOR_INCIDENT, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF, ctx);
   }
   
   @Test public void testSwitchFanShutoffNoActorAddress() {
      TestContext ctx = ctxbuild("device.switch.off", true, false)
            .build();
      
      testValueChange(switchAppender, (Address)null, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF, ctx);
   }

	@Test public void testSwitchOff() {
		TestContext ctx = ctxbuild("switch.off", true).build();
		testValueChange(switchAppender, ACTOR_NON_ALARM_SUBSYSTEM, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF, ctx);
	}

	@Test public void testSwitchOffInst() {
		TestContext ctx = ctxbuild("switch.off.inst", true)
									.withValue(1, INSTANCE_FENRIS)
									.build();
		testValueChange(switchAppender, INSTANCE_FENRIS, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF, ctx);
	}

	@Test public void testSwitchOffPers() {
		TestContext ctx = ctxbuild("switch.off.pers", true)
									.withValue(2, PERSON_NAME)
									.build();
		testValueChange(switchAppender, ACTOR_PERSON, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF, ctx);
	}

	@Test public void testSwitchOffRule() {
		TestContext ctx = ctxbuild("switch.off.rule", true)
				               .withValue(2, RULE_NAME)
									.build();
		testValueChange(switchAppender, ACTOR_RULE, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF, ctx);
	}

	@Test public void testSwitchOffPersInst() {
		TestContext ctx = ctxbuild("switch.off.inst.pers", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(2, PERSON_NAME)
									.build();
		testValueChange(switchAppender, INSTANCE_FENRIS, ACTOR_PERSON, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF, ctx);
	}

	@Test public void testSwitchOffRuleInst() {
		TestContext ctx = ctxbuild("switch.off.inst.rule", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(2, RULE_NAME)
									.build();
		testValueChange(switchAppender, INSTANCE_FENRIS, ACTOR_RULE, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_OFF, ctx);
	}

	@Test public void testSwitchOn() {
		TestContext ctx = ctxbuild("switch.on", true).build();
		testValueChange(switchAppender, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON, ctx);
	}

	@Test public void testSwitchOnInst() {
		TestContext ctx = ctxbuild("switch.on.inst", true)
									.withValue(1, INSTANCE_FENRIS)
									.build();
		testValueChange(switchAppender, INSTANCE_FENRIS, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON, ctx);
	}

	@Test public void testSwitchOnPers() {
		TestContext ctx = ctxbuild("switch.on.pers", true)
									.withValue(2, PERSON_NAME)
									.build();
		testValueChange(switchAppender, ACTOR_PERSON, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON, ctx);
	}

	@Test public void testSwitchOnRule() {
		TestContext ctx = ctxbuild("switch.on.rule", true)
									.withValue(2, RULE_NAME)
									.build();
		testValueChange(switchAppender, ACTOR_RULE, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON, ctx);
	}

	@Test public void testSwitchOnPersInst() {
		TestContext ctx = ctxbuild("switch.on.inst.pers", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(2, PERSON_NAME)
									.build();
		testValueChange(switchAppender, INSTANCE_FENRIS, ACTOR_PERSON, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON, ctx);
	}

	@Test public void testSwitchOnRuleInst() {
		TestContext ctx = ctxbuild("switch.on.inst.rule", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(2, RULE_NAME)
									.build();
		testValueChange(switchAppender, INSTANCE_FENRIS, ACTOR_RULE, SwitchCapability.ATTR_STATE, SwitchCapability.STATE_ON, ctx);
	}

	@Test public void testContactOpened() {
		TestContext context = ctxbuild("contact.opened", true).build();
		testValueChange(contactAppender, ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_OPENED, context);
	}

	@Test public void testContactClosed() {
		TestContext context = ctxbuild("contact.closed", true).build();
		testValueChange(contactAppender, ContactCapability.ATTR_CONTACT, ContactCapability.CONTACT_CLOSED, context);
	}

	@Test public void testPowerLine() {
		TestContext context = ctxbuild("power.line", true).build();
		testValueChange(powerAppender, DevicePowerCapability.ATTR_SOURCE, DevicePowerCapability.SOURCE_LINE, context);
	}

	@Test public void testPowerBackupBattery() {
		TestContext context = ctxbuild("power.backupbattery", true).build();
		testValueChange(powerAppender, DevicePowerCapability.ATTR_SOURCE, DevicePowerCapability.SOURCE_BACKUPBATTERY, context);
	}

	@Test public void testButtonPressed() {
		TestContext context = context()
				.withLogType(PLACE)
				.withLogType(CRITICAL)
				.withLogType(DEVICE)
				.template("device.button.pressed")
				.withValue(0, DEVICE_NAME)
				.withValue(1, "")
				.withValue(2, "")
				.withValue(3, "")
				.build();
		testValueChange(buttonAppender, ButtonCapability.ATTR_STATE, ButtonCapability.STATE_PRESSED, context);
	}

	@Test public void testButtonPressedInstance() {
		TestContext context = context()
				.withLogType(PLACE)
				.withLogType(CRITICAL)
				.withLogType(DEVICE)
				.template("device.button.pressed.inst")
				.withValue(0, DEVICE_NAME)
				.withValue(1, "")
				.withValue(2, "")
				.withValue(3, "")
				.withValue(1, INSTANCE_FENRIS)
				.build();
		testValueChange(buttonAppender, INSTANCE_FENRIS, ButtonCapability.ATTR_STATE, ButtonCapability.STATE_PRESSED, context);
	}

	@Test public void testCoolSetpoint() {
		String setpointF = "70";
		Double setpointC = UnitConversion.tempFtoC(70);
		TestContext ctx = ctxbuild("thermo.coolsetpoint", true).withValue(4, setpointF).build();
		testValueChange(thermostatAppender, ThermostatCapability.ATTR_COOLSETPOINT, setpointC, ctx);
	}

	@Test public void testCoolSetpointPers() {
		String setpointF = "70";
		Double setpointC = UnitConversion.tempFtoC(70);
		TestContext ctx = ctxbuild("thermo.coolsetpoint.pers", true)
									.withValue(2,  PERSON_NAME)
									.withValue(4, setpointF).build();
		testValueChange(thermostatAppender, ACTOR_PERSON, ThermostatCapability.ATTR_COOLSETPOINT, setpointC, ctx);
	}

	@Test public void testCoolSetpointRule() {
		String setpointF = "70";
		Double setpointC = UnitConversion.tempFtoC(70);
		TestContext ctx = ctxbuild("thermo.coolsetpoint.rule", true)
									.withValue(2, RULE_NAME)
									.withValue(4, setpointF)
									.build();
		testValueChange(thermostatAppender, ACTOR_RULE, ThermostatCapability.ATTR_COOLSETPOINT, setpointC, ctx);
	}

	@Test public void testCoolSetpointInst() {
		String setpointF = "70";
		Double setpointC = UnitConversion.tempFtoC(70);
		TestContext ctx = ctxbuild("thermo.coolsetpoint.inst", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(4, setpointF)
									.build();
		testValueChange(thermostatAppender, INSTANCE_FENRIS, ThermostatCapability.ATTR_COOLSETPOINT, setpointC, ctx);
	}

	@Test public void testCoolSetpointInstPers() {
		String setpointF = "70";
		Double setpointC = UnitConversion.tempFtoC(70);
		TestContext ctx = ctxbuild("thermo.coolsetpoint.inst.pers", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(2, PERSON_NAME)
									.withValue(4, setpointF)
									.build();
		testValueChange(thermostatAppender, INSTANCE_FENRIS, ACTOR_PERSON, ThermostatCapability.ATTR_COOLSETPOINT, setpointC, ctx);
	}

	@Test public void testCoolSetpointInstRule() {
		String setpointF = "70";
		Double setpointC = UnitConversion.tempFtoC(70);
		TestContext ctx = ctxbuild("thermo.coolsetpoint.inst.rule", true)
									.withValue(1, INSTANCE_FENRIS)
									.withValue(2, RULE_NAME)
									.withValue(4, setpointF)
									.build();
		testValueChange(thermostatAppender, INSTANCE_FENRIS, ACTOR_RULE, ThermostatCapability.ATTR_COOLSETPOINT, setpointC, ctx);
	}

	@Test public void testHeatSetpoint() {
		String setpointF = "70";
		Double setpointC = UnitConversion.tempFtoC(70);
		TestContext ctx = ctxbuild("thermo.heatsetpoint", true).withValue(4, setpointF).build();
		testValueChange(thermostatAppender, ThermostatCapability.ATTR_HEATSETPOINT, setpointC, ctx);
	}

	@Test public void testHeatSetpointPers() {
		String setpointF = "70";
		Double setpointC = UnitConversion.tempFtoC(70);
		TestContext ctx = ctxbuild("thermo.heatsetpoint.pers", true)
				.withValue(2, PERSON_NAME)
				.withValue(4, setpointF)
				.build();
		testValueChange(thermostatAppender, ACTOR_PERSON, ThermostatCapability.ATTR_HEATSETPOINT, setpointC, ctx);
	}

	@Test public void testHeatSetpointRule() {
		String setpointF = "70";
		Double setpointC = UnitConversion.tempFtoC(70);
		TestContext ctx = ctxbuild("thermo.heatsetpoint.rule", true)
				.withValue(2, RULE_NAME)
				.withValue(4, setpointF)
				.build();
		testValueChange(thermostatAppender, ACTOR_RULE, ThermostatCapability.ATTR_HEATSETPOINT, setpointC, ctx);
	}

	@Test public void testHeatSetpointInst() {
		String setpointF = "70";
		Double setpointC = UnitConversion.tempFtoC(70);
		TestContext ctx = ctxbuild("thermo.heatsetpoint.inst", true)
				.withValue(1, INSTANCE_FENRIS)
				.withValue(4, setpointF)
				.build();
		testValueChange(thermostatAppender, INSTANCE_FENRIS, ThermostatCapability.ATTR_HEATSETPOINT, setpointC, ctx);
	}

	@Test public void testHeatSetpointInstPers() {
		String setpointF = "70";
		Double setpointC = UnitConversion.tempFtoC(70);
		TestContext ctx = ctxbuild("thermo.heatsetpoint.inst.pers", true)
				.withValue(1, INSTANCE_FENRIS)
				.withValue(2, PERSON_NAME)
				.withValue(4, setpointF)
				.build();
		testValueChange(thermostatAppender, INSTANCE_FENRIS, ACTOR_PERSON, ThermostatCapability.ATTR_HEATSETPOINT, setpointC, ctx);
	}

	@Test public void testHeatSetpointInstRule() {
		String setpointF = "70";
		Double setpointC = UnitConversion.tempFtoC(70);
		TestContext ctx = ctxbuild("thermo.heatsetpoint.inst.rule", true)
				.withValue(1, INSTANCE_FENRIS)
				.withValue(2, RULE_NAME)
				.withValue(4, setpointF)
				.build();
		testValueChange(thermostatAppender, INSTANCE_FENRIS, ACTOR_RULE, ThermostatCapability.ATTR_HEATSETPOINT, setpointC, ctx);
	}

   @Test public void testThermoFanShutoff() {
      TestContext ctx = ctxbuild("alarm.smoke.shutoff", true, false)
            .withValue(4, ThermostatCapability.HVACMODE_OFF)
            .withLogType(ALARM)
            .build();
      expectIncident();
      testValueChange(thermostatAppender, ACTOR_INCIDENT, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF, ctx);
   }

	@Test public void testThermoModeOff() {
			TestContext ctx = ctxbuild("thermo.hvacmode.off", true)
					.withValue(4, ThermostatCapability.HVACMODE_OFF)
					.build();
			testValueChange(thermostatAppender, ACTOR_NON_ALARM_SUBSYSTEM, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF, ctx);
	}

   private void expectIncident()
   {
      AlarmIncident incident = AlarmIncident.builder()
         .withAlert(AlertType.SMOKE)
         .withId(INCIDENT_ID)
         .withPlaceId(PLACE_ID)
         .withMonitoringState(MonitoringState.PENDING)
         .build();

      EasyMock.expect(mockAlarmIncidentDao.findById(PLACE_ID, INCIDENT_ID)).andReturn(incident);
   }

	@Test public void testThermoModeOffPers() {
		TestContext ctx = ctxbuild("thermo.hvacmode.off.pers", true)
				.withValue(2,  PERSON_NAME)
				.withValue(4, ThermostatCapability.HVACMODE_OFF)
				.build();
		testValueChange(thermostatAppender, ACTOR_PERSON, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF, ctx);
	}

	@Test public void testThermoModeOffRule() {
		TestContext ctx = ctxbuild("thermo.hvacmode.off.rule", true)
				.withValue(2, RULE_NAME)
				.withValue(4, ThermostatCapability.HVACMODE_OFF)
				.build();
		testValueChange(thermostatAppender, ACTOR_RULE, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_OFF, ctx);
	}

	@Test public void testThermoModeAuto() {
		TestContext ctx = ctxbuild("thermo.hvacmode.auto", true)
				.withValue(4, ThermostatCapability.HVACMODE_AUTO)
				.build();
		testValueChange(thermostatAppender, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO, ctx);
	}

	@Test public void testThermoModeAutoPers() {
		TestContext ctx = ctxbuild("thermo.hvacmode.auto.pers", true)
				.withValue(2,  PERSON_NAME)
				.withValue(4, ThermostatCapability.HVACMODE_AUTO)
				.build();
		testValueChange(thermostatAppender, ACTOR_PERSON, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO, ctx);
	}

	@Test public void testThermoModeAutoRule() {
		TestContext ctx = ctxbuild("thermo.hvacmode.auto.rule", true)
				.withValue(2,  RULE_NAME)
				.withValue(4, ThermostatCapability.HVACMODE_AUTO)
				.build();
		testValueChange(thermostatAppender, ACTOR_RULE, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_AUTO, ctx);
	}

	@Test public void testThermoModeHeat() {
		TestContext ctx = ctxbuild("thermo.hvacmode.heat", true)
				.withValue(4, ThermostatCapability.HVACMODE_HEAT)
				.build();
		testValueChange(thermostatAppender, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT, ctx);
	}

	@Test public void testThermoModeHeatPers() {
		TestContext ctx = ctxbuild("thermo.hvacmode.heat.pers", true)
				.withValue(2,  PERSON_NAME)
				.withValue(4, ThermostatCapability.HVACMODE_HEAT)
				.build();
		testValueChange(thermostatAppender, ACTOR_PERSON, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT, ctx);
	}

	@Test public void testThermoModeHeatRule() {
		TestContext ctx = ctxbuild("thermo.hvacmode.heat.rule", true)
				.withValue(2,  RULE_NAME)
				.withValue(4, ThermostatCapability.HVACMODE_HEAT)
				.build();
		testValueChange(thermostatAppender, ACTOR_RULE, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_HEAT, ctx);
	}

	@Test public void testThermoModeCool() {
		TestContext ctx = ctxbuild("thermo.hvacmode.cool", true)
				.withValue(4, ThermostatCapability.HVACMODE_COOL)
				.build();
		testValueChange(thermostatAppender, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL, ctx);
	}

	@Test public void testThermoModeCoolPers() {
		TestContext ctx = ctxbuild("thermo.hvacmode.cool.pers", true)
				.withValue(2, PERSON_NAME)
				.withValue(4, ThermostatCapability.HVACMODE_COOL)
				.build();
		testValueChange(thermostatAppender, ACTOR_PERSON, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL, ctx);
	}

	@Test public void testThermoModeCoolRule() {
		TestContext ctx = ctxbuild("thermo.hvacmode.cool.rule", true)
				.withValue(2, RULE_NAME)
				.withValue(4, ThermostatCapability.HVACMODE_COOL)
				.build();
		testValueChange(thermostatAppender, ACTOR_RULE, ThermostatCapability.ATTR_HVACMODE, ThermostatCapability.HVACMODE_COOL, ctx);
	}
	
	@Test public void testPetDoorLocked() {
		TestContext ctx = ctxbuild("petdoor.locked", true)
									.build();
		testValueChange(petdoorAppender, PetDoorCapability.ATTR_LOCKSTATE, PetDoorCapability.LOCKSTATE_LOCKED, ctx);
	}
	
	@Test public void testPetDoorLockedPers() {
		TestContext ctx = ctxbuild("petdoor.locked.pers", true)
									.withValue(2, PERSON_NAME)
									.build();
		testValueChange(petdoorAppender, ACTOR_PERSON, PetDoorCapability.ATTR_LOCKSTATE, PetDoorCapability.LOCKSTATE_LOCKED, ctx);
	}
	
	@Test public void testPetDoorLockedRule() {
		TestContext ctx = ctxbuild("petdoor.locked.rule", true)
									.withValue(2,  RULE_NAME)
									.build();
		testValueChange(petdoorAppender, ACTOR_RULE, PetDoorCapability.ATTR_LOCKSTATE, PetDoorCapability.LOCKSTATE_LOCKED, ctx);
	}

	private TestContextBuilder ctxbuild(String template, boolean critical) {
	   return ctxbuild(template, critical, true);
	}

	private TestContextBuilder ctxbuild(String template, boolean critical, boolean devicePrefix) {
		boolean person = template.contains(".pers");
		boolean rule = template.contains(".rule");
		TestContextBuilder builder = context();
		builder.withLogType(PLACE);
		if (critical) {
			builder.withLogType(CRITICAL);
		}
		if (person) {
			builder.withLogType(PERSON);
		}
		if (rule) {
			builder.withLogType(RULE);
		}
		return builder.withLogType(DEVICE)
			.template(devicePrefix ? "device." + template : template)
			.withValue(0, DEVICE_NAME)
			.withValue(1, "")
			.withValue(2, "")
			.withValue(3, "");
	}

	private void testValueChange(HistoryAppender appender,
			String attr,
			Object value,
			TestContext cxt) {
		testValueChange(appender, null, null, attr, value, cxt);
	}

	private void testValueChange(HistoryAppender appender,
			String instance,
			String attr,
			Object value,
			TestContext cxt) {
		testValueChange(appender, instance, null, attr, value, cxt);
	}

	private void testValueChange(HistoryAppender appender,
			Address actor,
			String attr,
			Object value,
			TestContext cxt) {
		testValueChange(appender, null, actor, attr, value, cxt);
	}

	@SuppressWarnings("unchecked")
   private void testValueChange(HistoryAppender appender,
   									   String instance,
   									   Address actor,
											String attr,
											Object value,
											TestContext cxt) {
		String fullAttr = StringUtils.isEmpty(instance) ? attr : attr + ":" + instance;
		ValueChangeBuilder builder = valueChangeBuilder().fromDevice().withAttribute(fullAttr, value);
		if (actor != null) {
			builder.withActor(actor);
		}

		PlatformMessage msg = builder.build();
		expectFindPlaceName();
		expectFindDeviceName();
		if (actor != null && actor == ACTOR_PERSON) {
			expectFindPersonName();
		}
		else if (actor != null && actor == ACTOR_RULE) {
			expectFindRuleName();
		}
		Capture<HistoryLogEntry> event1 = expectAndCaptureAppend();
   	Capture<HistoryLogEntry> event2 = expectAndCaptureAppend();
   	Capture<HistoryLogEntry> event3 = expectAndCaptureAppend();
   	Capture<HistoryLogEntry> event4 = null;
   	if (actor != null && (actor == ACTOR_PERSON || actor == ACTOR_RULE || actor == ACTOR_ALARM_SUBSYSTEM || actor == ACTOR_INCIDENT)) {
			event4 = expectAndCaptureAppend();
		}   	

   	EasyMock.replay(mockAppenderDao, mockAlarmIncidentDao, mockNameCache);

		Assert.assertTrue(appender.append(msg));

		if (event4 != null) {
			verifyValues(cxt, event1, event2, event3, event4);
		}
		else {
			verifyValues(cxt, event1, event2, event3);
		}
	}
}

