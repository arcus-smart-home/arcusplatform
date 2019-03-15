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
package com.iris.platform.history.appender.video;

import java.util.Map;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.platform.history.HistoryAppenderDAO;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.HistoryLogEntryType;
import com.iris.platform.history.appender.EventAppenderTestCase;
import com.iris.platform.history.appender.ObjectNameCache;
import com.iris.test.Mocks;
import com.iris.util.IrisUUID;

@Mocks({ HistoryAppenderDAO.class, ObjectNameCache.class})
public class TestVideoAddedAppender extends EventAppenderTestCase {
	UUID placeId = UUID.randomUUID();
	UUID cameraId = UUID.randomUUID();
	Address cameraAddress = Address.platformDriverAddress(cameraId);
	String cameraName = "Test Cam";
	Address incidentAddress = Address.platformService(UUID.randomUUID(), AlarmIncidentCapability.NAMESPACE);
	Address videoAddress = Address.platformService(IrisUUID.timeUUID(), RecordingCapability.NAMESPACE);

	@Inject VideoAddedAppender appender;
	
	protected PlatformMessage videoAdded(@Nullable Address actor) {
		Map<String, Object> attributes =
				ModelFixtures
					.buildRecordingAttributes((UUID) videoAddress.getId())
					.put(RecordingCapability.ATTR_PLACEID, placeId.toString())
					.put(RecordingCapability.ATTR_CAMERAID, cameraId.toString())
					.create();
		MessageBody added = MessageBody.buildMessage(Capability.EVENT_ADDED, attributes);
		return 
				PlatformMessage
					.buildBroadcast(added, Address.fromString((String) attributes.get(Capability.ATTR_ADDRESS)))
					.withActor(actor)
					.withPlaceId(placeId)
					.create();
	}
	
	protected void assertHistoryEventMatches(HistoryLogEntry value, HistoryLogEntryType type, Object id) {
		assertEquals(type, value.getType());
		assertEquals(id, value.getId());
		assertEquals("alarm.security.recording", value.getMessageKey());
		assertEquals(videoAddress.getRepresentation(), value.getSubjectAddress());
		assertEquals(cameraName, value.getValues().get(0));
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
	public void testIncidentVideoAdded() {
		expectNameLookup(cameraAddress, cameraName);
		Capture<HistoryLogEntry> dashboard = expectAndCaptureAppend();
		Capture<HistoryLogEntry> details = expectAndCaptureAppend();
		Capture<HistoryLogEntry> incident = expectAndCaptureAppend();
		Capture<HistoryLogEntry> device = expectAndCaptureAppend();
		replay();
		
		PlatformMessage message = videoAdded(incidentAddress);
		assertTrue(appender.append(message));
		assertHistoryEventMatches(dashboard.getValue(), HistoryLogEntryType.CRITICAL_PLACE_LOG, placeId);
		assertHistoryEventMatches(details.getValue(),   HistoryLogEntryType.DETAILED_PLACE_LOG, placeId);
		assertHistoryEventMatches(incident.getValue(),  HistoryLogEntryType.DETAILED_ALARM_LOG, incidentAddress.getId());
		assertHistoryEventMatches(device.getValue(),    HistoryLogEntryType.DETAILED_DEVICE_LOG, cameraAddress.getId());
	}

	@Test
	public void testManualRecordingAdded() {
		replay();
		
		PlatformMessage message = videoAdded(Address.platformService(UUID.randomUUID(), PersonCapability.NAMESPACE));
		assertFalse(appender.append(message));
	}

	@Test
	public void testRuleRecordingAdded() {
		replay();
		
		PlatformMessage message = videoAdded(Address.platformService(UUID.randomUUID(), RuleCapability.NAMESPACE, 1));
		assertFalse(appender.append(message));
	}

	@Test
	public void testSceneRecordingAdded() {
		replay();
		
		PlatformMessage message = videoAdded(Address.platformService(UUID.randomUUID(), SceneCapability.NAMESPACE, 2));
		assertFalse(appender.append(message));
	}

	@Test
	public void testNoAttributionRecordingAdded() {
		replay();
		
		PlatformMessage message = videoAdded(null);
		assertFalse(appender.append(message));
	}

}

