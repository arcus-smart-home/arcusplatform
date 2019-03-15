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
/**
 * 
 */
package com.iris.platform.history.appender.incident;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.iris.common.alarm.AlertType;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.AlarmIncidentCapability.COAlertEvent;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.capability.SafetySubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.WaterSubsystemCapability;
import com.iris.messages.model.CompositeId;
import com.iris.messages.model.Fixtures;
import com.iris.messages.type.IncidentTrigger;
import com.iris.platform.alarm.incident.AlarmIncident;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.HistoryLogEntryType;
import com.iris.platform.history.appender.EventAppenderTestCase;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.test.Mocks;
import com.iris.util.IrisCollections;

@Mocks({ HistoryAppenderDAO.class, ObjectNameCache.class})
public class TestIncidentAppender extends EventAppenderTestCase {
	UUID placeId = UUID.randomUUID();
	Address incidentAddress = Address.platformService(UUID.randomUUID(), AlarmIncidentCapability.NAMESPACE);
   
	@Inject AlertEventAppender appender;
	
	protected PlatformMessage alert(AlertType type, String triggerEvent, Address address) {
		IncidentTrigger trigger = createIncidentTrigger(type, triggerEvent, address);
		return alert(type, ImmutableList.of( trigger ));
	}
	
	protected PlatformMessage alert(AlertType type, List<IncidentTrigger> triggers) {
		MessageBody event =
				MessageBody
					.messageBuilder(AlarmIncident.toEvent(type))
					.withAttribute(COAlertEvent.ATTR_TRIGGERS, IrisCollections.transform(triggers, IncidentTrigger::toMap))
					.create();
		
		return
				PlatformMessage
					.broadcast()
					.from(incidentAddress)
					.withPayload(event)
					.withPlaceId(placeId)
					.create();
	}
	
	protected IncidentTrigger createIncidentTrigger(AlertType alarm, String event) {
		return createIncidentTrigger(alarm, event, Fixtures.createDeviceAddress());
	}

	protected IncidentTrigger createIncidentTrigger(AlertType alarm, String event, Address source) {
		IncidentTrigger trigger = new IncidentTrigger();
		trigger.setAlarm(alarm.name());
		trigger.setEvent(event);
		trigger.setSource(source.getRepresentation());
		trigger.setTime(new Date());
		return trigger;
	}

	protected void assertHistoryEventMatches(HistoryLogEntry value, String alarmName, HistoryLogEntryType type) {
		assertEquals(type, value.getType());
		assertEquals("incident.triggered", value.getMessageKey());
		assertEquals(incidentAddress.getRepresentation(), value.getSubjectAddress());
		assertEquals(alarmName, value.getValues().get(0));
	}
	
	protected void assertHistoryEventMatches(HistoryLogEntry value, String alarmName, HistoryLogEntryType type, String name) {
		assertEquals(type, value.getType());
		assertEquals("incident.triggered.by", value.getMessageKey());
		assertEquals(incidentAddress.getRepresentation(), value.getSubjectAddress());
		assertEquals(alarmName, value.getValues().get(0));
		assertEquals(name, value.getValues().get(1));
	}
	
	@SuppressWarnings("unchecked")
	protected void assertHistoryEventMatches(HistoryLogEntry value, String alarmName, String subsystem, String name) {
		assertEquals(HistoryLogEntryType.DETAILED_SUBSYSTEM_LOG, value.getType());
		assertEquals(subsystem, ((CompositeId<UUID, String>) value.getId()).getSecondaryId());
		assertEquals("incident.triggered.by", value.getMessageKey());
		assertEquals(incidentAddress.getRepresentation(), value.getSubjectAddress());
		assertEquals(name, value.getValues().get(1));
	}
	
	@Before
	public void stagePlaceLookup() {
		EasyMock
			.expect(mockNameCache.getPlaceName(Address.platformService(placeId, PlaceCapability.NAMESPACE)))
			.andReturn("My House")
			.anyTimes();
	}
	
	@After
	public void verify() {
		super.verify();
	}
	
	@Test
	public void testCOAlarm() {
		String deviceName = "CO Detector";
		String alertName = "CO";
		
		Address deviceAddress = Fixtures.createDeviceAddress();
		expectNameLookup(deviceAddress, deviceName);
		Capture<HistoryLogEntry> dashboard = expectAndCaptureAppend();
		Capture<HistoryLogEntry> details = expectAndCaptureAppend();
		Capture<HistoryLogEntry> alarmSubsystem = expectAndCaptureAppend();
		Capture<HistoryLogEntry> subsystem = expectAndCaptureAppend();
		replay();
		
		PlatformMessage message = alert(AlertType.CO, IncidentTrigger.EVENT_CO, deviceAddress);
		assertTrue(appender.append(message));
		assertHistoryEventMatches(dashboard.getValue(), alertName, HistoryLogEntryType.CRITICAL_PLACE_LOG, deviceName);
		assertHistoryEventMatches(details.getValue(),   alertName, HistoryLogEntryType.DETAILED_PLACE_LOG, deviceName);
		assertHistoryEventMatches(alarmSubsystem.getValue(), alertName, AlarmSubsystemCapability.NAMESPACE, deviceName);
		assertHistoryEventMatches(subsystem.getValue(), alertName, SafetySubsystemCapability.NAMESPACE, deviceName);
	}

	@Test
	public void testSmokeAlarm() {
		String deviceName = "Smoke Detector";
		String alertName = "Smoke";
		
		Address deviceAddress = Fixtures.createDeviceAddress();
		expectNameLookup(deviceAddress, deviceName);
		Capture<HistoryLogEntry> dashboard = expectAndCaptureAppend();
		Capture<HistoryLogEntry> details = expectAndCaptureAppend();
		Capture<HistoryLogEntry> alarmSubsystem = expectAndCaptureAppend();
		Capture<HistoryLogEntry> subsystem = expectAndCaptureAppend();
		replay();
		
		PlatformMessage message = alert(AlertType.SMOKE, IncidentTrigger.EVENT_SMOKE, deviceAddress);
		assertTrue(appender.append(message));
		assertHistoryEventMatches(dashboard.getValue(), alertName, HistoryLogEntryType.CRITICAL_PLACE_LOG, deviceName);
		assertHistoryEventMatches(details.getValue(),   alertName, HistoryLogEntryType.DETAILED_PLACE_LOG, deviceName);
		assertHistoryEventMatches(alarmSubsystem.getValue(), alertName, AlarmSubsystemCapability.NAMESPACE, deviceName);
		assertHistoryEventMatches(subsystem.getValue(), alertName, SafetySubsystemCapability.NAMESPACE, deviceName);
	}

	@Test
	public void testPanicAlarmViaKeypad() {
		String deviceName = "Key Pad";
		String alertName = "Panic";
		
		Address deviceAddress = Fixtures.createDeviceAddress();
		expectNameLookup(deviceAddress, deviceName);
		Capture<HistoryLogEntry> dashboard = expectAndCaptureAppend();
		Capture<HistoryLogEntry> details = expectAndCaptureAppend();
		Capture<HistoryLogEntry> alarmSubsystem = expectAndCaptureAppend();
		Capture<HistoryLogEntry> subsystem = expectAndCaptureAppend();
		replay();
		
		PlatformMessage message = alert(AlertType.PANIC, IncidentTrigger.EVENT_KEYPAD, deviceAddress);
		assertTrue(appender.append(message));
		assertHistoryEventMatches(dashboard.getValue(), alertName, HistoryLogEntryType.CRITICAL_PLACE_LOG, deviceName);
		assertHistoryEventMatches(details.getValue(),   alertName, HistoryLogEntryType.DETAILED_PLACE_LOG, deviceName);
		assertHistoryEventMatches(alarmSubsystem.getValue(), alertName, AlarmSubsystemCapability.NAMESPACE, deviceName);
		assertHistoryEventMatches(subsystem.getValue(), alertName, SecuritySubsystemCapability.NAMESPACE, deviceName);
	}

	@Test
	public void testPanicAlarmViaRule() {
		Address rule = Address.platformService(UUID.randomUUID(), RuleCapability.NAMESPACE);
		expectNameLookup(rule, "My Rule");
		Capture<HistoryLogEntry> dashboard = expectAndCaptureAppend();
		Capture<HistoryLogEntry> details = expectAndCaptureAppend();
		Capture<HistoryLogEntry> alarmSubsystem = expectAndCaptureAppend();
		Capture<HistoryLogEntry> subsystem = expectAndCaptureAppend();
		replay();
		
		PlatformMessage message = alert(AlertType.PANIC, IncidentTrigger.EVENT_KEYPAD, rule);
		assertTrue(appender.append(message));
		assertHistoryEventMatches(dashboard.getValue(), "Panic", HistoryLogEntryType.CRITICAL_PLACE_LOG, "My Rule");
		assertHistoryEventMatches(details.getValue(),   "Panic", HistoryLogEntryType.DETAILED_PLACE_LOG, "My Rule");
		assertHistoryEventMatches(alarmSubsystem.getValue(), "Panic", AlarmSubsystemCapability.NAMESPACE, "My Rule");
		assertHistoryEventMatches(subsystem.getValue(), "Panic", SecuritySubsystemCapability.NAMESPACE, "My Rule");
	}

	@Test
	public void testWaterAlarm() {
		String deviceName = "Leak Detector";
		String alertName = "Water Leak";
		
		Address deviceAddress = Fixtures.createDeviceAddress();
		expectNameLookup(deviceAddress, deviceName);
		Capture<HistoryLogEntry> dashboard = expectAndCaptureAppend();
		Capture<HistoryLogEntry> details = expectAndCaptureAppend();
		Capture<HistoryLogEntry> alarmSubsystem = expectAndCaptureAppend();
		Capture<HistoryLogEntry> subsystem = expectAndCaptureAppend();
		replay();
		
		PlatformMessage message = alert(AlertType.WATER, IncidentTrigger.EVENT_LEAK, deviceAddress);
		assertTrue(appender.append(message));
		assertHistoryEventMatches(dashboard.getValue(), alertName, HistoryLogEntryType.CRITICAL_PLACE_LOG, deviceName);
		assertHistoryEventMatches(details.getValue(),   alertName, HistoryLogEntryType.DETAILED_PLACE_LOG, deviceName);
		assertHistoryEventMatches(alarmSubsystem.getValue(), alertName, AlarmSubsystemCapability.NAMESPACE, deviceName);
		assertHistoryEventMatches(subsystem.getValue(), alertName, WaterSubsystemCapability.NAMESPACE, deviceName);
	}

	@Test
	public void testSecurityAlarm() {
		String alertName = "Security";
		
		Capture<HistoryLogEntry> dashboard = expectAndCaptureAppend();
		Capture<HistoryLogEntry> details = expectAndCaptureAppend();
		Capture<HistoryLogEntry> alarmSubsystem = expectAndCaptureAppend();
		Capture<HistoryLogEntry> subsystem = expectAndCaptureAppend();
		replay();
		
		PlatformMessage message = alert(
				AlertType.SECURITY, 
				ImmutableList.of(
						createIncidentTrigger(AlertType.SECURITY, IncidentTrigger.EVENT_MOTION),
						createIncidentTrigger(AlertType.SECURITY, IncidentTrigger.EVENT_MOTION)
				)
		);
		assertTrue(appender.append(message));
		assertHistoryEventMatches(dashboard.getValue(), alertName, HistoryLogEntryType.CRITICAL_PLACE_LOG);
		assertHistoryEventMatches(details.getValue(),   alertName, HistoryLogEntryType.DETAILED_PLACE_LOG);
		assertHistoryEventMatches(alarmSubsystem.getValue(), alertName, HistoryLogEntryType.DETAILED_SUBSYSTEM_LOG);
		assertHistoryEventMatches(subsystem.getValue(), alertName, HistoryLogEntryType.DETAILED_SUBSYSTEM_LOG);
	}

}

