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
package com.iris.common.subsystem.cameras;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.CameraStatusCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.CameraStatusModel;
import com.iris.util.IrisUUID;

public class TestCamerasSubsystem_CameraStatus extends CamerasSubsystemTestCase {

	private Model addCamera() {
		return addModel(CamerasFixtures.buildCamera().online().create());
	}
	
	private Model addOfflineCamera() {
		return addModel(CamerasFixtures.buildCamera().offline().create());
	}
	
	private MessageReceivedEvent videoAdded(Model camera, String type) {
		Map<String, Object> attributes = 
				CamerasFixtures
					.buildRecording()
					.put(RecordingCapability.ATTR_TYPE, type)
					.put(RecordingCapability.ATTR_PLACEID, placeId)
					.put(RecordingCapability.ATTR_CAMERAID, camera.getAddress().getId())
					.create();
		return event(MessageBody.buildMessage(Capability.EVENT_ADDED, attributes), Address.fromString((String) attributes.get(Capability.ATTR_ADDRESS)), UUID.randomUUID().toString());
	}
	
	private MessageReceivedEvent videoCompleted(Address video) {
		Map<String, Object> attributes = 
				ImmutableMap.<String, Object>of(
						RecordingCapability.ATTR_DURATION, 52.1,
						RecordingCapability.ATTR_SIZE, 1278
				);
		return event(MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, attributes), video);
	}
	
	private MessageReceivedEvent videoDeleted(Address video) {
		Map<String, Object> attributes = 
				ImmutableMap.<String, Object>of(
						RecordingCapability.ATTR_DELETED, true,
						RecordingCapability.ATTR_DELETETIME, new Date()
				);
		return event(MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, attributes), video);
	}
	
	private MessageReceivedEvent videoPurged(Address video) {
		return event(MessageBody.buildMessage(Capability.EVENT_DELETED), video);
	}
	
	private ScheduledEvent videoTimeout(Model camera) {
		return timeout(CameraStatusAdapter.RECORDING_TIMEOUT_PREFIX + camera.getId());
	}
	
	private void assertIdleNoLastRecording(Address camera) {
		assertCamera(camera, CameraStatusCapability.STATE_IDLE, null, null, null);
		assertTimeoutCleared(CameraStatusAdapter.RECORDING_TIMEOUT_PREFIX + camera.getId().toString());
	}
	
	private void assertIdleLastRecording(Address camera, Address recordingAddress) {
		assertCamera(camera, CameraStatusCapability.STATE_IDLE, null, recordingAddress, new Date(IrisUUID.timeof((UUID) recordingAddress.getId())));
		assertTimeoutCleared(CameraStatusAdapter.RECORDING_TIMEOUT_PREFIX + camera.getId().toString());
	}
	
	private void assertIdleLastRecordingTime(Address camera, Date recordingTime) {
		assertCamera(camera, CameraStatusCapability.STATE_IDLE, null, null, recordingTime);
		assertTimeoutCleared(CameraStatusAdapter.RECORDING_TIMEOUT_PREFIX + camera.getId().toString());
	}
	
	private void assertStreamingNoLastRecording(Address camera, Address stream) {
		assertCamera(camera, CameraStatusCapability.STATE_STREAMING, stream, null, null);
		assertTimeoutSet(CameraStatusAdapter.RECORDING_TIMEOUT_PREFIX + camera.getId().toString());
	}
	
	private void assertRecordingNoLastRecording(Address camera, Address activeRecording) {
		assertCamera(camera, CameraStatusCapability.STATE_RECORDING, activeRecording, null, null);
		assertTimeoutSet(CameraStatusAdapter.RECORDING_TIMEOUT_PREFIX + camera.getId().toString());
	}
	
	private void assertOfflineNoLastRecording(Address camera) {
		assertCamera(camera, CameraStatusCapability.STATE_OFFLINE, null, null, null);
		assertTimeoutCleared(CameraStatusAdapter.RECORDING_TIMEOUT_PREFIX + camera.getId().toString());
	}
	
	private void assertCamera(Address camera, String state, Address activeRecording, Address lastRecording, Date lastRecordingTime) {
		assertEquals(activeRecording != null ? activeRecording.getRepresentation() : "", CameraStatusModel.getActiveRecording(camera.getId().toString(), model));
		assertEquals(camera.getRepresentation(), CameraStatusModel.getCamera(camera.getId().toString(), model));
		assertEquals(lastRecording != null ? lastRecording.getRepresentation() : "", CameraStatusModel.getLastRecording(camera.getId().toString(), model));
		assertEquals(lastRecordingTime, CameraStatusModel.getLastRecordingTime(camera.getId().toString(), model));
		assertEquals(state, CameraStatusModel.getState(camera.getId().toString(), model));
	}
	
	private void assertCameraDeleted(Address camera) {
		assertNull(CameraStatusModel.getActiveRecording(camera.getId().toString(), model));
		assertNull(CameraStatusModel.getCamera(camera.getId().toString(), model));
		assertNull(CameraStatusModel.getLastRecording(camera.getId().toString(), model));
		assertNull(CameraStatusModel.getLastRecordingTime(camera.getId().toString(), model));
		assertNull(CameraStatusModel.getState(camera.getId().toString(), model));
	}
	
	@Test
	public void testAddOnlineCamera() {
		start();
		
		assertEquals(ImmutableMap.of(), context.model().getInstances());
		
		Model camera = addCamera();
		assertEquals(ImmutableMap.of(camera.getId(), ImmutableSet.of(CameraStatusCapability.NAMESPACE)), context.model().getInstances());
		assertIdleNoLastRecording(camera.getAddress());
	}

	@Test
	public void testAddOfflineCamera() {
		start();
		
		assertEquals(ImmutableMap.of(), context.model().getInstances());
		
		Model camera = addOfflineCamera();
		assertEquals(ImmutableMap.of(camera.getId(), ImmutableSet.of(CameraStatusCapability.NAMESPACE)), context.model().getInstances());
		assertOfflineNoLastRecording(camera.getAddress());
	}

	@Test
	public void testDiscoverOnlineCamera() {
		Model camera = addCamera();
		
		start();
		
		assertEquals(ImmutableMap.of(camera.getId(), ImmutableSet.of(CameraStatusCapability.NAMESPACE)), context.model().getInstances());
		assertIdleNoLastRecording(camera.getAddress());
	}

	@Test
	public void testDiscoverOfflineCamera() {
		Model camera = addOfflineCamera();
		
		start();
		
		assertEquals(ImmutableMap.of(camera.getId(), ImmutableSet.of(CameraStatusCapability.NAMESPACE)), context.model().getInstances());
		assertOfflineNoLastRecording(camera.getAddress());
	}

	@Test
	public void testRemoveCamera() {
		Model camera = addCamera();
		start();
		
		assertEquals(ImmutableMap.of(camera.getId(), ImmutableSet.of(CameraStatusCapability.NAMESPACE)), context.model().getInstances());

		removeModel(camera);
		
		assertEquals(ImmutableMap.of(), context.model().getInstances());
		assertCameraDeleted(camera.getAddress());
	}

	@Test
	public void testStream() {
		Model camera = addCamera();
		
		start();
		
		MessageReceivedEvent mre = videoAdded(camera, RecordingCapability.TYPE_STREAM);
		subsystem.onEvent(mre, context);
		assertStreamingNoLastRecording(camera.getAddress(), mre.getAddress());
		
		mre = videoDeleted(mre.getAddress());
		subsystem.onEvent(mre, context);
		assertIdleNoLastRecording(camera.getAddress());
	}

	@Test
	public void testActiveStreamPurged() {
		Model camera = addCamera();
		
		start();
		
		MessageReceivedEvent mre = videoAdded(camera, RecordingCapability.TYPE_STREAM);
		subsystem.onEvent(mre, context);
		assertStreamingNoLastRecording(camera.getAddress(), mre.getAddress());
		
		mre = videoPurged(mre.getAddress());
		subsystem.onEvent(mre, context);
		assertIdleNoLastRecording(camera.getAddress());
	}

	@Test
	public void testActiveStreamTimeout() {
		Model camera = addCamera();
		
		start();
		
		MessageReceivedEvent mre = videoAdded(camera, RecordingCapability.TYPE_STREAM);
		subsystem.onEvent(mre, context);
		assertStreamingNoLastRecording(camera.getAddress(), mre.getAddress());
		
		ScheduledEvent to = videoTimeout(camera);
		subsystem.onEvent(to, context);
		assertIdleNoLastRecording(camera.getAddress());
	}

	@Test
	public void testRecording() {
		Model camera = addCamera();
		
		start();
		
		MessageReceivedEvent mre = videoAdded(camera, RecordingCapability.TYPE_RECORDING);
		subsystem.onEvent(mre, context);
		assertRecordingNoLastRecording(camera.getAddress(), mre.getAddress());

		mre = videoCompleted(mre.getAddress());
		subsystem.onEvent(mre, context);
		assertIdleLastRecording(camera.getAddress(), mre.getAddress());

		mre = videoDeleted(mre.getAddress());
		subsystem.onEvent(mre, context);
		assertIdleLastRecording(camera.getAddress(), mre.getAddress());

		mre = videoPurged(mre.getAddress());
		subsystem.onEvent(mre, context);
		assertIdleLastRecordingTime(camera.getAddress(), new Date(IrisUUID.timeof((UUID) mre.getAddress().getId())));
	}
	
	@Test
   public void testStreaming() {
      Model camera = addCamera();
      
      start();
      
      MessageReceivedEvent mre = videoAdded(camera, RecordingCapability.TYPE_STREAM);
      subsystem.onEvent(mre, context);
      assertStreamingNoLastRecording(camera.getAddress(), mre.getAddress());
      
      mre = videoCompleted(mre.getAddress());
      subsystem.onEvent(mre, context);
      assertIdleNoLastRecording(camera.getAddress());

      mre = videoDeleted(mre.getAddress());
      subsystem.onEvent(mre, context);
      assertIdleNoLastRecording(camera.getAddress());

      mre = videoPurged(mre.getAddress());
      subsystem.onEvent(mre, context);
      assertIdleLastRecordingTime(camera.getAddress(), null);
   }

	@Test
	public void testActiveRecordingDeleted() {
		Model camera = addCamera();
		
		start();
		
		MessageReceivedEvent mre = videoAdded(camera, RecordingCapability.TYPE_RECORDING);
		subsystem.onEvent(mre, context);
		assertRecordingNoLastRecording(camera.getAddress(), mre.getAddress());
		
		mre = videoDeleted(mre.getAddress());
		subsystem.onEvent(mre, context);
		assertIdleNoLastRecording(camera.getAddress());

		mre = videoPurged(mre.getAddress());
		subsystem.onEvent(mre, context);
		assertIdleNoLastRecording(camera.getAddress());
	}

	@Test
	public void testActiveRecordingPurged() {
		Model camera = addCamera();
		
		start();
		
		MessageReceivedEvent mre = videoAdded(camera, RecordingCapability.TYPE_RECORDING);
		subsystem.onEvent(mre, context);
		assertRecordingNoLastRecording(camera.getAddress(), mre.getAddress());
		
		mre = videoPurged(mre.getAddress());
		subsystem.onEvent(mre, context);
		assertIdleNoLastRecording(camera.getAddress());
	}

	@Test
	public void testActiveRecordingTimeout() {
		Model camera = addCamera();
		
		start();
		
		MessageReceivedEvent mre = videoAdded(camera, RecordingCapability.TYPE_RECORDING);
		subsystem.onEvent(mre, context);
		assertRecordingNoLastRecording(camera.getAddress(), mre.getAddress());
		
		ScheduledEvent to = videoTimeout(camera);
		subsystem.onEvent(to, context);
		assertIdleNoLastRecording(camera.getAddress());
	}

}

