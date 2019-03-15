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

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import com.google.inject.Inject;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.capability.CareSubsystemCapability.BehaviorActionEvent;
import com.iris.messages.capability.CareSubsystemCapability.BehaviorAlertAcknowledgedEvent;
import com.iris.messages.capability.CareSubsystemCapability.BehaviorAlertClearedEvent;
import com.iris.messages.capability.CareSubsystemCapability.BehaviorAlertEvent;
import com.iris.platform.history.appender.subsys.CareSubsystemEventsAppender;
import com.iris.test.Modules;

@Modules({TestSubsystemAppenderModule.class})
public class TestCareSubsysEventsAppender extends EventAppenderTestCase {
	private static final Address SUBSYS_ADDR = Address.platformService(CareSubsystemCapability.NAMESPACE);
	private static final String BEHAVIOR_ID = "MyBehaviorId";
	private static final String BEHAVIOR_NAME = "MyBehaviorName";
	private static final String CARE_DEVICE_A = "DRIV:dev:e001f0c6-c05c-4e0d-8084-021dbf0ec585";
	private static final String CARE_DEVICE_B = "DRIV:dev:e002f0c6-c05c-4e0d-8084-021dbf0ec585";
	private static final String CARE_DEVICE_C = "DRIV:dev:e003f0c6-c05c-4e0d-8084-021dbf0ec585";
	private static final Address CARE_DEVICE_A_ADDR = Address.fromString(CARE_DEVICE_A);
	private static final Address CARE_DEVICE_B_ADDR = Address.fromString(CARE_DEVICE_B);
	private static final Address CARE_DEVICE_C_ADDR = Address.fromString(CARE_DEVICE_C);
	private static final String CARE_DEVICE_A_NAME = "Device Alfa";
	private static final String CARE_DEVICE_B_NAME = "Device Bravo";
	private static final String CARE_DEVICE_C_NAME = "Device Charlie";
	private static final String CARE_DEVICE_NAMES = CARE_DEVICE_A_NAME + "," + CARE_DEVICE_B_NAME + "," + CARE_DEVICE_C_NAME;
	private static final Set<String> CARE_DEVICES = new LinkedHashSet<String>(3);
	
	static {
		CARE_DEVICES.add(CARE_DEVICE_A);
		CARE_DEVICES.add(CARE_DEVICE_B);
		CARE_DEVICES.add(CARE_DEVICE_C);
	}
	

	@Inject CareSubsystemEventsAppender appender;
	
	/*
	 	#####################################################
		# Subsystem Care
		# no instance, rule, or method supported
		# {0} = device name     -- always empty
		# {1} = instance name   -- always empty
		# {2} = actor name      -- the name of the person that cleared the alert, raised the panic or acknowledged the alert.
		# {3} = method name     -- always empty
		# {4} = behavior name   -- the name of the behavior that triggered the alert
		# {5} = devices   		-- the names of the devices that triggered the alert
		# {6} = behavior action -- The CRUD action taken on the behavior (Added,Removed,Updated)
		#####################################################
	 */
	
	@Test
	public void testState() {
		TestContext ctx = ctxbuild("ready", true)
				.build();
		test()
			.context(ctx)
			.event(Capability.EVENT_VALUE_CHANGE)
			.withAttr(CareSubsystemCapability.ATTR_ALARMSTATE, CareSubsystemCapability.ALARMSTATE_READY)
			.reject()
			.go();		
	}
	
	@Test
	public void testModeAndState() {
		TestContext ctx = ctxbuild("ready", true)
				.build();
		test()
			.context(ctx)
			.event(Capability.EVENT_VALUE_CHANGE)
			.withAttr(CareSubsystemCapability.ATTR_ALARMMODE, CareSubsystemCapability.ALARMMODE_ON)
			.withAttr(CareSubsystemCapability.ATTR_ALARMSTATE, CareSubsystemCapability.ALARMSTATE_READY)
			.go();		
	}
	
	@Test
	public void testReady() {
		TestContext ctx = ctxbuild("ready", true)
				.build();
		test()
			.context(ctx)
			.event(Capability.EVENT_VALUE_CHANGE)
			.withAttr(CareSubsystemCapability.ATTR_ALARMMODE, CareSubsystemCapability.ALARMMODE_ON)
			.go();		
	}
	
	@Test
	public void testReadyPers() {
		TestContext ctx = ctxbuild("ready.pers", true)
				.withValue(2, PERSON_NAME)
				.build();
		test()
			.context(ctx)
			.withActor(ACTOR_PERSON)
			.event(Capability.EVENT_VALUE_CHANGE)
			.withAttr(CareSubsystemCapability.ATTR_ALARMMODE, CareSubsystemCapability.ALARMMODE_ON)
			.go();	
	}
	
	@Test
	public void testVisit() {
		TestContext ctx = ctxbuild("visit", true)
				.build();
		test()
			.context(ctx)
			.event(Capability.EVENT_VALUE_CHANGE)
			.withAttr(CareSubsystemCapability.ATTR_ALARMMODE, CareSubsystemCapability.ALARMMODE_VISIT)
			.go();
	}
	
	@Test
	public void testVisitPers() {
		TestContext ctx = ctxbuild("visit.pers", true)
				.withValue(2, PERSON_NAME)
				.build();
		test()
			.context(ctx)
			.withActor(ACTOR_PERSON)
			.event(Capability.EVENT_VALUE_CHANGE)
			.withAttr(CareSubsystemCapability.ATTR_ALARMMODE, CareSubsystemCapability.ALARMMODE_VISIT)
			.go();
	}
	
	@Test
	public void testAcknowledgedPers() {
		TestContext ctx = ctxbuild("acknowledged.pers", true)
				.withValue(2, PERSON_NAME)
				.build();
		test()
			.context(ctx)
			.withActor(ACTOR_PERSON)
			.event(BehaviorAlertAcknowledgedEvent.NAME)
			.go();
	}
	
	@Test
	public void testAlert() {
		TestContext ctx = ctxbuild("alert", true)
				.withValue(4, BEHAVIOR_NAME)
				.withValue(5, CARE_DEVICE_NAMES)
				.withValue(6, "")
				.build();
		test()
			.context(ctx)
			.withLookup(CARE_DEVICE_A_ADDR, CARE_DEVICE_A_NAME)
			.withLookup(CARE_DEVICE_B_ADDR, CARE_DEVICE_B_NAME)
			.withLookup(CARE_DEVICE_C_ADDR, CARE_DEVICE_C_NAME)
			.event(BehaviorAlertEvent.NAME)
			.withAttr(BehaviorAlertEvent.ATTR_BEHAVIORID, BEHAVIOR_ID)
			.withAttr(BehaviorAlertEvent.ATTR_BEHAVIORNAME, BEHAVIOR_NAME)
			.withAttr(BehaviorAlertEvent.ATTR_TRIGGEREDDEVICES, CARE_DEVICES)
			.go();	
	}
	
	@Test
	public void testClear() {
		TestContext ctx = ctxbuild("clear", true)
				.build();
		test()
			.context(ctx)
			.event(BehaviorAlertClearedEvent.NAME)
			.go();
	}
	
	@Test
	public void testClearPers() {
		TestContext ctx = ctxbuild("clear.pers", true)
				.withValue(2, PERSON_NAME)
				.build();
		test()
			.context(ctx)
			.withActor(ACTOR_PERSON)
			.event(BehaviorAlertClearedEvent.NAME)
			.go();
	}
	
	@Test
	public void testBehaviorActionPers() {
		TestContext ctx = ctxbuild("behaviorAction.pers", true)
				.withValue(2, PERSON_NAME)
				.withValue(4, BEHAVIOR_NAME)
				.withValue(5, "")
				.withValue(6, "modified")
				.build();
		test()
			.context(ctx)
			.withActor(ACTOR_PERSON)
			.event(BehaviorActionEvent.NAME)
			.withAttr(BehaviorActionEvent.ATTR_BEHAVIORID, BEHAVIOR_ID)
			.withAttr(BehaviorActionEvent.ATTR_BEHAVIORNAME, BEHAVIOR_NAME)
			.withAttr(BehaviorActionEvent.ATTR_BEHAVIORACTION, BehaviorActionEvent.BEHAVIORACTION_MODIFIED)
			.go();
	}
	
	private TestContextBuilder ctxbuild(String template, boolean critical) {
		boolean person = template.contains(".pers");
		
		TestContextBuilder builder = context();
		builder.withLogType(PLACE);
		if (critical) {
			builder.withLogType(CRITICAL);
		}
		if (person) {
			builder.withLogType(PERSON);
		}
		return builder
					.withLogType(SUBSYS)
					.template("subsys.care." + template)
					.withValue(0, "")
					.withValue(1, "")
					.withValue(2, "")
					.withValue(3, "");
	}
	
	private Tester test() {
		return new Tester(SUBSYS_ADDR, appender);
	}
}

