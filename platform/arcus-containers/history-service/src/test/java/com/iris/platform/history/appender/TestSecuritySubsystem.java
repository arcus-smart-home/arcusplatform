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

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.messages.address.Address;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability.AlertEvent;
import com.iris.messages.capability.SecuritySubsystemCapability.ArmedEvent;
import com.iris.messages.capability.SecuritySubsystemCapability.DisarmedEvent;
import com.iris.platform.history.appender.subsys.SecuritySubsystemEventsAppender;
import com.iris.test.Modules;
import com.iris.util.IrisCollections;

@Modules({TestSubsystemAppenderModule.class})
public class TestSecuritySubsystem extends EventAppenderTestCase {
	private static final Address SUBSYS_ADDR = Address.platformService(SecuritySubsystemCapability.NAMESPACE);
	private static final String BYPASSED_DEVICE_A = "DRIV:dev:e001f0c6-c05c-4e0d-8084-021dbf0ec585";
	private static final String BYPASSED_DEVICE_B = "DRIV:dev:e002f0c6-c05c-4e0d-8084-021dbf0ec585";
	private static final String BYPASSED_DEVICE_C = "DRIV:dev:e003f0c6-c05c-4e0d-8084-021dbf0ec585";
	private static final String BYPASSED_DEVICE_D = "DRIV:dev:e004f0c6-c05c-4e0d-8084-021dbf0ec585";
	private static final String PARTICIPATING_DEVICE_X = "DRIV:dev:e011f0c6-c05c-4e0d-8084-021dbf0ec585";
	private static final String PARTICIPATING_DEVICE_Y = "DRIV:dev:e012f0c6-c05c-4e0d-8084-021dbf0ec585";
	private static final String ARMING_DEVICE = deviceAddress.getRepresentation();
	private static final String ARMING_PERSON = personAddress.getRepresentation();
	private static final String ARMING_RULE = ruleAddress.getRepresentation();
	private static final Set<String> BYPASSED_DEVICES_2 = IrisCollections.setOf(BYPASSED_DEVICE_A, BYPASSED_DEVICE_B);
	private static final Set<String> BYPASSED_DEVICES_4 = IrisCollections.setOf(BYPASSED_DEVICE_A, BYPASSED_DEVICE_B, BYPASSED_DEVICE_C, BYPASSED_DEVICE_D);
	private static final Set<String> NO_BYPASSED_DEVICES = Collections.emptySet();
	private static final Set<String> PARTICIPATING_DEVICES = IrisCollections.setOf(PARTICIPATING_DEVICE_X, PARTICIPATING_DEVICE_Y);
	private static final String CAUSE_ALARM = "ALARM";
	private static final String CAUSE_PANIC = "PANIC";
	
	private static final Date TIME_FIRST = new Date(20000L);
	private static final Date TIME_SECOND = new Date(30000L);
	private static final Date TIME_THIRD = new Date(40000L);
	
	private static final Map<String, Date> TRIGGERS = ImmutableMap.of(PARTICIPATING_DEVICE_X, TIME_SECOND, ARMING_DEVICE, TIME_THIRD, PARTICIPATING_DEVICE_Y, TIME_FIRST);
	
	@Inject SecuritySubsystemEventsAppender appender;
	
	/*
	   #####################################################
		# Subsystem Security
		# no device name, instance, rule, or method supported
		# {0} = device name      -- always empty
		# {1} = instance name    -- always empty
		# {2} = actor name       -- populated
		# {3} = method name
		# {4} = arming mode when arming event
		# {4} = triggering device name when alert event
		# {5} = number of bypassed devices
		# {6} = participating devices
		# {7} = armed method
		# {8} = armed by
		# {9} = armed actor address
		#####################################################
	 */

	@Test
	public void testSecurityArmedOn() {
		TestContext ctx = ctxbuild("armed", true)
				.withValue( 4, ArmedEvent.ALARMMODE_ON)
				.withValue( 5, "0")
				.withValue( 6, StringUtils.join(PARTICIPATING_DEVICES, ','))
				.withValue( 7, ArmedEvent.METHOD_DEVICE)
				.withValue( 8, ARMING_DEVICE)
				.withValue( 9, "")
				.build();
		test()
		   .context(ctx)
			.event(ArmedEvent.NAME)
			.withAttr(ArmedEvent.ATTR_ALARMMODE, ArmedEvent.ALARMMODE_ON)
			.withAttr(ArmedEvent.ATTR_BY, ARMING_DEVICE)
			.withAttr(ArmedEvent.ATTR_METHOD, ArmedEvent.METHOD_DEVICE)
			.withAttr(ArmedEvent.ATTR_BYPASSEDDEVICES, NO_BYPASSED_DEVICES)
		   .withAttr(ArmedEvent.ATTR_PARTICIPATINGDEVICES, PARTICIPATING_DEVICES)
		   .go();
	}
	
	@Test
	public void testSecurityArmedPartial() {
		TestContext ctx = ctxbuild("armed", true)
				.withValue( 4, ArmedEvent.ALARMMODE_PARTIAL)
				.withValue( 5, "0")
				.withValue( 6, StringUtils.join(PARTICIPATING_DEVICES, ','))
				.withValue( 7, ArmedEvent.METHOD_DEVICE)
				.withValue( 8, ARMING_DEVICE)
				.withValue( 9, "")
				.build();
		test()
		   .context(ctx)
			.event(ArmedEvent.NAME)
			.withAttr(ArmedEvent.ATTR_ALARMMODE, ArmedEvent.ALARMMODE_PARTIAL)
			.withAttr(ArmedEvent.ATTR_BY, ARMING_DEVICE)
			.withAttr(ArmedEvent.ATTR_METHOD, ArmedEvent.METHOD_DEVICE)
			.withAttr(ArmedEvent.ATTR_BYPASSEDDEVICES, NO_BYPASSED_DEVICES)
		   .withAttr(ArmedEvent.ATTR_PARTICIPATINGDEVICES, PARTICIPATING_DEVICES)
		   .go();							
	}
	
	@Test
	public void testSecurityArmedPers() {
		TestContext ctx = ctxbuild("armed.pers", true)
				.withValue( 2, PERSON_NAME)
				.withValue( 4, ArmedEvent.ALARMMODE_PARTIAL)
				.withValue( 5, "0")
				.withValue( 6, StringUtils.join(PARTICIPATING_DEVICES, ','))
				.withValue( 7, ArmedEvent.METHOD_DEVICE)
				.withValue( 8, ARMING_DEVICE)
				.withValue( 9, ARMING_PERSON)
				.build();
		test()
		   .context(ctx)
			.event(ArmedEvent.NAME)
			.withActor(ACTOR_PERSON)
			.withAttr(ArmedEvent.ATTR_ALARMMODE, ArmedEvent.ALARMMODE_PARTIAL)
			.withAttr(ArmedEvent.ATTR_BY, ARMING_DEVICE)
			.withAttr(ArmedEvent.ATTR_METHOD, ArmedEvent.METHOD_DEVICE)
			.withAttr(ArmedEvent.ATTR_BYPASSEDDEVICES, NO_BYPASSED_DEVICES)
		   .withAttr(ArmedEvent.ATTR_PARTICIPATINGDEVICES, PARTICIPATING_DEVICES)
		   .go();	
	}
	
   @Test
   public void testSecurityArmedOnFromKeyPad() {
      TestContext ctx = ctxbuild("armed", true)
            .withValue( 4, ArmedEvent.ALARMMODE_ON)
            .withValue( 5, "0")
            .withValue( 6, StringUtils.join(PARTICIPATING_DEVICES, ','))
            .withValue( 7, ArmedEvent.METHOD_DEVICE)
            .withValue( 8, "")
            .withValue( 9, "")
            .build();
      test()
         .context(ctx)
         .event(ArmedEvent.NAME)
         .withAttr(ArmedEvent.ATTR_ALARMMODE, ArmedEvent.ALARMMODE_ON)
         .withAttr(ArmedEvent.ATTR_BY, null)
         .withAttr(ArmedEvent.ATTR_METHOD, ArmedEvent.METHOD_DEVICE)
         .withAttr(ArmedEvent.ATTR_BYPASSEDDEVICES, NO_BYPASSED_DEVICES)
         .withAttr(ArmedEvent.ATTR_PARTICIPATINGDEVICES, PARTICIPATING_DEVICES)
         .go();
   }
   
	/*
	 * TODO: Enable test when method is supported.
	 *
	@Test
	public void testSecurityArmedPersMeth() {
		
	}
	*/
	
	@Test
	public void testSecurityArmedRule() {
		TestContext ctx = ctxbuild("armed.rule", true)
				.withValue( 2, RULE_NAME)
				.withValue( 4, ArmedEvent.ALARMMODE_PARTIAL)
				.withValue( 5, "0")
				.withValue( 6, StringUtils.join(PARTICIPATING_DEVICES, ','))
				.withValue( 7, ArmedEvent.METHOD_DEVICE)
				.withValue( 8, ARMING_DEVICE)
				.withValue( 9, ARMING_RULE)
				.build();
		test()
		   .context(ctx)
			.event(ArmedEvent.NAME)
			.withActor(ACTOR_RULE)
			.withAttr(ArmedEvent.ATTR_ALARMMODE, ArmedEvent.ALARMMODE_PARTIAL)
			.withAttr(ArmedEvent.ATTR_BY, ARMING_DEVICE)
			.withAttr(ArmedEvent.ATTR_METHOD, ArmedEvent.METHOD_DEVICE)
			.withAttr(ArmedEvent.ATTR_BYPASSEDDEVICES, NO_BYPASSED_DEVICES)
		   .withAttr(ArmedEvent.ATTR_PARTICIPATINGDEVICES, PARTICIPATING_DEVICES)
		   .go();
	}
	
	@Test
	public void testSecurityArmedBypassed() {
		TestContext ctx = ctxbuild("armedbypassed", true)
				.withValue( 4, ArmedEvent.ALARMMODE_ON)
				.withValue( 5, "2")
				.withValue( 6, StringUtils.join(PARTICIPATING_DEVICES, ','))
				.withValue( 7, ArmedEvent.METHOD_DEVICE)
				.withValue( 8, ARMING_DEVICE)
				.withValue( 9, "")
				.build();
		test()
		   .context(ctx)
			.event(ArmedEvent.NAME)
			.withAttr(ArmedEvent.ATTR_ALARMMODE, ArmedEvent.ALARMMODE_ON)
			.withAttr(ArmedEvent.ATTR_BY, ARMING_DEVICE)
			.withAttr(ArmedEvent.ATTR_METHOD, ArmedEvent.METHOD_DEVICE)
			.withAttr(ArmedEvent.ATTR_BYPASSEDDEVICES, BYPASSED_DEVICES_2)
		   .withAttr(ArmedEvent.ATTR_PARTICIPATINGDEVICES, PARTICIPATING_DEVICES)
		   .go();
	}
	
	@Test
	public void testSecurityArmedBypassedPers() {
		TestContext ctx = ctxbuild("armedbypassed.pers", true)
				.withValue( 2, PERSON_NAME)
				.withValue( 4, ArmedEvent.ALARMMODE_ON)
				.withValue( 5, "2")
				.withValue( 6, StringUtils.join(PARTICIPATING_DEVICES, ','))
				.withValue( 7, ArmedEvent.METHOD_DEVICE)
				.withValue( 8, ARMING_DEVICE)
				.withValue( 9, ARMING_PERSON)
				.build();
		test()
		   .context(ctx)
			.event(ArmedEvent.NAME)
			.withActor(ACTOR_PERSON)
			.withAttr(ArmedEvent.ATTR_ALARMMODE, ArmedEvent.ALARMMODE_ON)
			.withAttr(ArmedEvent.ATTR_BY, ARMING_DEVICE)
			.withAttr(ArmedEvent.ATTR_METHOD, ArmedEvent.METHOD_DEVICE)
			.withAttr(ArmedEvent.ATTR_BYPASSEDDEVICES, BYPASSED_DEVICES_2)
		   .withAttr(ArmedEvent.ATTR_PARTICIPATINGDEVICES, PARTICIPATING_DEVICES)
		   .go();
	}
	
	/*
	 * TODO: Enable test with method is supported.
	@Test
	public void testSecurityArmedBypassedPersMeth() {
		
	}
	*/
	
	@Test
	public void testSecurityArmedBypassedRule() {
		TestContext ctx = ctxbuild("armedbypassed.rule", true)
				.withValue( 2, RULE_NAME)
				.withValue( 4, ArmedEvent.ALARMMODE_ON)
				.withValue( 5, "4")
				.withValue( 6, StringUtils.join(PARTICIPATING_DEVICES, ','))
				.withValue( 7, ArmedEvent.METHOD_DEVICE)
				.withValue( 8, ARMING_DEVICE)
				.withValue( 9, ARMING_RULE)
				.build();
		test()
		   .context(ctx)
			.event(ArmedEvent.NAME)
			.withActor(ACTOR_RULE)
			.withAttr(ArmedEvent.ATTR_ALARMMODE, ArmedEvent.ALARMMODE_ON)
			.withAttr(ArmedEvent.ATTR_BY, ARMING_DEVICE)
			.withAttr(ArmedEvent.ATTR_METHOD, ArmedEvent.METHOD_DEVICE)
			.withAttr(ArmedEvent.ATTR_BYPASSEDDEVICES, BYPASSED_DEVICES_4)
		   .withAttr(ArmedEvent.ATTR_PARTICIPATINGDEVICES, PARTICIPATING_DEVICES)
		   .go();
	}
	
	@Test
	public void testSecurityAlert() {
		TestContext ctx = ctxbuild("alert", true)
				.withValue(4, DEVICE_NAME)
				.build();
				
		test()
			.context(ctx)
			.event(AlertEvent.NAME)
			.withLookup(deviceAddress, DEVICE_NAME)
			.withAttr(AlertEvent.ATTR_CAUSE, CAUSE_ALARM)
			.withAttr(AlertEvent.ATTR_TRIGGERS, TRIGGERS)
			.withAttr(AlertEvent.ATTR_METHOD, AlertEvent.METHOD_DEVICE)
			.withAttr(AlertEvent.ATTR_BY, ARMING_DEVICE)
			.go();
	}
	
   @Test
   public void testSecurityAlertPers() {
      TestContext ctx = ctxbuild("alert.pers", true)
            .withValue( 2, PERSON_NAME)
            .withValue(4, DEVICE_NAME)
            .build();
      test()
         .context(ctx)
         .event(AlertEvent.NAME)
         .withLookup(deviceAddress, DEVICE_NAME)
         .withActor(ACTOR_PERSON)
         .withAttr(AlertEvent.ATTR_CAUSE, CAUSE_PANIC)
         .withAttr(AlertEvent.ATTR_TRIGGERS, TRIGGERS)
         .withAttr(AlertEvent.ATTR_METHOD, AlertEvent.METHOD_DEVICE)
         .withAttr(AlertEvent.ATTR_BY, ARMING_DEVICE)
         .go();
   }
   
   @Test
   public void testSecurityAlertRule() {
      TestContext ctx = ctxbuild("alert.rule", true)
            .withValue( 2, RULE_NAME)
            .withValue(4, DEVICE_NAME)
            .build();
      test()
         .context(ctx)
         .event(AlertEvent.NAME)
         .withActor(ACTOR_RULE)
         .withLookup(deviceAddress, DEVICE_NAME)
         .withAttr(AlertEvent.ATTR_CAUSE, CAUSE_PANIC)
         .withAttr(AlertEvent.ATTR_TRIGGERS, TRIGGERS)
         .withAttr(AlertEvent.ATTR_METHOD, AlertEvent.METHOD_DEVICE)
         .withAttr(AlertEvent.ATTR_BY, ARMING_DEVICE)
         .go();
   }   
   
	@Test
	public void testSecurityDisarmed() {
		TestContext ctx = ctxbuild("disarmed", true)
				.withValue(4, DisarmedEvent.METHOD_DEVICE)
				.withValue(5, ARMING_DEVICE)
				.withValue(6, "")
				.build();
		test()
			.context(ctx)
			.event(DisarmedEvent.NAME)
			.withAttr(DisarmedEvent.ATTR_METHOD, DisarmedEvent.METHOD_DEVICE)
			.withAttr(DisarmedEvent.ATTR_BY, ARMING_DEVICE)
			.go();
	}
	
	@Test
	public void testSecurityDisarmedPers() {
		TestContext ctx = ctxbuild("disarmed.pers", true)
				.withValue(2, PERSON_NAME)
				.withValue(4, DisarmedEvent.METHOD_DEVICE)
				.withValue(5, ARMING_DEVICE)
				.withValue(6, ARMING_PERSON)
				.build();
		test()
			.context(ctx)
			.withActor(ACTOR_PERSON)
			.event(DisarmedEvent.NAME)
			.withAttr(DisarmedEvent.ATTR_METHOD, DisarmedEvent.METHOD_DEVICE)
			.withAttr(DisarmedEvent.ATTR_BY, ARMING_DEVICE)
			.go();
	}
	
	/*
	 * TODO: Enable test when method is supported.
	 * 
	@Test
	public void testSecurityDisarmedPersMeth() {
		
	}
	*/
	
	@Test
	public void testSecurityDisarmedRule() {
		TestContext ctx = ctxbuild("disarmed.rule", true)
				.withValue(2, RULE_NAME)
				.withValue(4, DisarmedEvent.METHOD_DEVICE)
				.withValue(5, ARMING_DEVICE)
				.withValue(6, ARMING_RULE)
				.build();
		test()
			.context(ctx)
			.withActor(ACTOR_RULE)
			.event(DisarmedEvent.NAME)
			.withAttr(DisarmedEvent.ATTR_METHOD, DisarmedEvent.METHOD_DEVICE)
			.withAttr(DisarmedEvent.ATTR_BY, ARMING_DEVICE)
			.go();
	}
	
	@Test
	public void testSecurityPanic() {
		TestContext ctx = ctxbuild("panic", true)
				.build();
		test()
			.context(ctx)
			.event(AlertEvent.NAME)
			.withLookup(deviceAddress, DEVICE_NAME)
			.withAttr(AlertEvent.ATTR_CAUSE, CAUSE_PANIC)
			.withAttr(AlertEvent.ATTR_TRIGGERS, TRIGGERS)
			.withAttr(AlertEvent.ATTR_METHOD, AlertEvent.METHOD_PANIC)
			.withAttr(AlertEvent.ATTR_BY, ARMING_DEVICE)
			.go();
	}
	
	@Test
	public void testSecurityPanicPers() {
		TestContext ctx = ctxbuild("panic.pers", true)
				.withValue(2, PERSON_NAME)
				.build();
		test()
			.context(ctx)
			.event(AlertEvent.NAME)
			.withLookup(deviceAddress, DEVICE_NAME)
			.withActor(ACTOR_PERSON)
			.withAttr(AlertEvent.ATTR_CAUSE, CAUSE_PANIC)
			.withAttr(AlertEvent.ATTR_TRIGGERS, TRIGGERS)
			.withAttr(AlertEvent.ATTR_METHOD, AlertEvent.METHOD_PANIC)
			.withAttr(AlertEvent.ATTR_BY, ARMING_DEVICE)
			.go();
	}
	
	/*
	 * TODO: Enable test when method is supported.
	 *
	@Test
	public void testSecurityPanicPersMeth() {
		
	}
	*/
	
	@Test
	public void testSecurityPanicRule() {
		TestContext ctx = ctxbuild("panic.rule", true)
				.withValue(2, RULE_NAME)
				.build();
		test()
			.context(ctx)
			.event(AlertEvent.NAME)
			.withLookup(deviceAddress, DEVICE_NAME)
			.withActor(ACTOR_RULE)
			.withAttr(AlertEvent.ATTR_CAUSE, CAUSE_PANIC)
			.withAttr(AlertEvent.ATTR_TRIGGERS, TRIGGERS)
			.withAttr(AlertEvent.ATTR_METHOD, AlertEvent.METHOD_PANIC)
			.withAttr(AlertEvent.ATTR_BY, ARMING_DEVICE)
			.go();
	}
	
	private TestContextBuilder ctxbuild(String template, boolean critical) {
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
		return builder
					.withLogType(SUBSYS)
					.template("subsys.security." + template)
					.withValue(0, "")
					.withValue(1, "")
					.withValue(2, "")
					.withValue(3, "");
	}
	
	private Tester test() {
		return new Tester(SUBSYS_ADDR, appender);
	}
}

