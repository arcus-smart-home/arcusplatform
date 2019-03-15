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
package com.iris.common.subsystem.alarm;

import static com.iris.common.subsystem.alarm.AlarmSubsystemFixture.assertTriggersMatch;
import static com.iris.common.subsystem.alarm.AlarmSubsystemFixture.createTrigger;

import java.util.Date;
import java.util.List;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.capability.key.NamespacedKey;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.alarm.generic.AlarmState.TriggerEvent;
import com.iris.common.subsystem.alarm.incident.AlarmIncidentService;
import com.iris.common.subsystem.alarm.security.SecurityAlarm;
import com.iris.common.subsystem.alarm.security.SecurityAlarmTestCase;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmCapability;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.SecurityAlarmModeCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.type.IncidentTrigger;

public class TestAlarmSubsystemSecurity extends SecurityAlarmTestCase {

	private AlarmIncidentService incidentService;
	private AlarmSubsystem subsystem;
	private Model contact;
	
	@Before
	public void startSubsystem() throws Exception {
		this.incidentService = EasyMock.createMock(AlarmIncidentService.class);
		this.subsystem = new AlarmSubsystem(incidentService, true);
		this.contact = addContactSensor(true, false);
		updateModel(
				Address.platformService(placeId, SecuritySubsystemCapability.NAMESPACE), 
				ImmutableMap.<String, Object>of(
						NamespacedKey.representation(SecurityAlarmModeCapability.ATTR_DEVICES, AlarmSubsystemCapability.SECURITYMODE_ON), 
						ImmutableSet.<String>of(contact.getAddress().getRepresentation())
				)
		);
		
		init(subsystem);
	}
	
	protected void replay() {
		EasyMock.replay(incidentService);
	}
	
	protected void verify() {
		EasyMock.verify(incidentService);
	}
	
	protected Capture<List<IncidentTrigger>> expectAddPreAlert() {
		Capture<List<IncidentTrigger>> triggerCapture = EasyMock.newCapture();
		EasyMock
			.expect(
					incidentService.addPreAlert(EasyMock.eq(context), EasyMock.eq(SecurityAlarm.NAME), EasyMock.isA(Date.class), EasyMock.capture(triggerCapture))
			)
			.andReturn(null);
		return triggerCapture;
		
	}

	protected Capture<List<IncidentTrigger>> expectAddAlert() {
		Capture<List<IncidentTrigger>> triggerCapture = EasyMock.newCapture();
		EasyMock
			.expect(
					incidentService.addAlert(EasyMock.eq(context), EasyMock.eq(SecurityAlarm.NAME), EasyMock.capture(triggerCapture))
			)
			.andReturn(null);
		return triggerCapture;
		
	}

	protected Capture<List<IncidentTrigger>> expectUpdateIncident() {
		Capture<List<IncidentTrigger>> triggerCapture = EasyMock.newCapture();
		incidentService.updateIncident(EasyMock.eq(context), EasyMock.capture(triggerCapture));
		EasyMock.expectLastCall();
		return triggerCapture;
	}

	@Test
	public void testArmPrealertAlert() {
		Capture<List<IncidentTrigger>> prealert = expectAddPreAlert();
		Capture<List<IncidentTrigger>> alert = expectAddAlert();
		expectUpdateIncident();
		replay();
		
		// arming
		{
			Date now = new Date();
			arm(AlarmSubsystemCapability.SECURITYMODE_ON);
			commit();
			
			assertEquals(AlarmSubsystemCapability.ALARMSTATE_READY, context.model().getAlarmState());
			assertEquals(AlarmSubsystemCapability.SECURITYMODE_ON, context.model().getSecurityMode());
			assertTrue(context.model().getSecurityArmTime().after(now));
		}
		
		// armed
		{
			subsystem.onEvent(new ScheduledEvent(context.model().getAddress(), context.model().getSecurityArmTime().getTime()), context);
			commit();
			
			assertEquals(AlarmSubsystemCapability.ALARMSTATE_READY, context.model().getAlarmState());
			assertEquals(AlarmSubsystemCapability.SECURITYMODE_ON, context.model().getSecurityMode());
		}
		
		// pre-alert
		{
			trigger(contact);
			commit();
			
			assertEquals(AlarmSubsystemCapability.ALARMSTATE_PREALERT, context.model().getAlarmState());
			assertTriggersMatch(prealert, createTrigger(contact, TriggerEvent.CONTACT));
			assertFalse(alert.hasCaptured());
		}
		
		// alert
		{
			Date alertAt = SubsystemUtils.getTimeout(context, SecurityAlarm.NAME + ":" + AlarmCapability.ALERTSTATE_PREALERT).get();
			subsystem.onEvent(new ScheduledEvent(context.model().getAddress(), alertAt.getTime()), context);
			commit();
			
			assertEquals(AlarmSubsystemCapability.ALARMSTATE_ALERTING, context.model().getAlarmState());
			assertTriggersMatch(alert, createTrigger(contact, TriggerEvent.CONTACT));
		}
		
		verify();
	}
	
}

