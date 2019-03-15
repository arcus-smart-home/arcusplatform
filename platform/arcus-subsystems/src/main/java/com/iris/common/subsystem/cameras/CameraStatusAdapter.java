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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.CameraStatusCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceConnectionModel;
import com.iris.messages.model.serv.CameraStatusModel;
import com.iris.messages.model.subs.CamerasSubsystemModel;
import com.iris.util.IrisUUID;

public class CameraStatusAdapter {
	public static long BUFFER_MS = TimeUnit.MINUTES.toMillis(1);
	public static long MAX_VIDEO_TIME_MS = TimeUnit.MINUTES.toMillis(20) + BUFFER_MS;
	static final String RECORDING_TIMEOUT_PREFIX = "recordingtimeout:";
	
	public static CameraStatusAdapter get(SubsystemContext<CamerasSubsystemModel> context, Address cameraAddress) {
		return new CameraStatusAdapter(context, cameraAddress);
	}
	
	public static void onVideoAdded(SubsystemContext<CamerasSubsystemModel> context, PlatformMessage message) {
		MessageBody video = message.getValue();
		Address cameraAddress = CamerasSubsystem.cameraIdToAddress(RecordingCapability.getCameraid(video));
		CameraStatusAdapter
			.get(context, cameraAddress)
			.onVideoAdded(message.getSource(), video.getAttributes());
	}

	public static void onVideoValueChange(SubsystemContext<CamerasSubsystemModel> context, PlatformMessage message) {
		// FIXME we could optimize this flow if we add camera as the actor for recordings
		String videoAddress = message.getSource().getRepresentation();
		for(String cameraId: context.model().getInstances().keySet()) {
			if(CameraStatusModel.getActiveRecording(cameraId, context.model(), "").equals(videoAddress)) {
				CameraStatusAdapter
					.get(context, CamerasSubsystem.cameraIdToAddress(cameraId))
					.onVideoChanged(message.getSource(), message.getValue().getAttributes());
				break;
			}
		}
	}

	public static void onVideoDeleted(SubsystemContext<CamerasSubsystemModel> context, PlatformMessage message) {
		// FIXME we could optimize this flow if we add camera as the actor for recordings
		String videoAddress = message.getSource().getRepresentation();
		for(String cameraId: context.model().getInstances().keySet()) {
			if(
					CameraStatusModel.getActiveRecording(cameraId, context.model(), "").equals(videoAddress) ||
					CameraStatusModel.getLastRecording(cameraId, context.model(), "").equals(videoAddress)
			) {
				CameraStatusAdapter
					.get(context, CamerasSubsystem.cameraIdToAddress(cameraId))
					.onVideoDeleted(message.getSource());
				break;
			}
		}
	}
	
	public static void onTimeout(SubsystemContext<CamerasSubsystemModel> context, ScheduledEvent event) {
		for(String cameraId: context.model().getInstances().keySet()) {
			if(SubsystemUtils.isMatchingTimeout(event, context, RECORDING_TIMEOUT_PREFIX + cameraId)) {
				get(context, CamerasSubsystem.cameraIdToAddress(cameraId)).onTimeout();
			}
		}
	}
	
	private final SubsystemContext<CamerasSubsystemModel> context;
	private final Address cameraAddress;
	private final String id;
	
	private CameraStatusAdapter(SubsystemContext<CamerasSubsystemModel> context, Address cameraAddress) {
		this.context = context;
		this.cameraAddress = cameraAddress;
		this.id = cameraAddress.getId().toString();
	}
	
	
	public void onSubsystemStarted() {
		Model m = context.models().getModelByAddress(cameraAddress);
		if(m == null) {
			context.logger().warn("Unable to load camera [{}]", cameraAddress);
			return;
		}
		
		if(!context.model().getInstances().containsKey(id)) {
			onCameraAdded(m);
		}
		else if(DeviceConnectionModel.isStateOFFLINE(m)) {
			// if its online don't muck with idle, streaming, recording...
			CameraStatusModel.setState(id, context.model(), CameraStatusCapability.STATE_OFFLINE);
		}
		SubsystemUtils.restoreTimeout(context, timeoutName());
	}
	
	public void onCameraAdded(Model camera) {
		Map<String, Set<String>> instances = SubsystemUtils.getEditableMap(context.model().getInstances());
		instances.put(id, ImmutableSet.of(CameraStatusCapability.NAMESPACE));
		context.model().setAttribute(Capability.ATTR_INSTANCES, instances);
		
		CameraStatusModel.setCamera(id, context.model(), camera.getAddress().getRepresentation());
		CameraStatusModel.setActiveRecording(id, context.model(), "");
		CameraStatusModel.setLastRecording(id, context.model(), "");
		CameraStatusModel.setState(id, context.model(), DeviceConnectionModel.isStateONLINE(camera) ? CameraStatusCapability.STATE_IDLE : CameraStatusCapability.STATE_OFFLINE);
	}
	
	public void onConnected() {
		markOnline();
	}
	
	public void onDisconnected() {
		markOffline();
	}
	
	public void onCameraChanged(ModelChangedEvent event) {
		switch(event.getAttributeName()) {
		case DeviceConnectionCapability.ATTR_STATE:
			if(DeviceConnectionCapability.STATE_ONLINE.equals(event.getAttributeValue())) {
				markOnline();
			}
			else {
				markOffline();
			}
			break;
		}
	}
	
	public void onCameraRemoved() {
		Map<String, Set<String>> instances = SubsystemUtils.getEditableMap(context.model().getInstances());
		instances.remove(id);
		context.model().setAttribute(Capability.ATTR_INSTANCES, instances);
		
		CameraStatusModel.setCamera(id, context.model(), null);
		CameraStatusModel.setActiveRecording(id, context.model(), null);
		CameraStatusModel.setLastRecording(id, context.model(), null);
		CameraStatusModel.setLastRecordingTime(id, context.model(), null);
		CameraStatusModel.setState(id, context.model(), null);
	}

	public void onVideoAdded(Address source, Map<String, Object> video) {
	   String recordingType = (String) video.get(RecordingCapability.ATTR_TYPE);
		context.logger().debug("New {} started for camera {}", recordingType, cameraAddress);
		CameraStatusModel.setActiveRecording(id, context.model(), source.getRepresentation());
		CameraStatusModel.setState(id, context.model(), RecordingCapability.TYPE_RECORDING.equals(recordingType) ? CameraStatusCapability.STATE_RECORDING : CameraStatusCapability.STATE_STREAMING);
		SubsystemUtils.setTimeout(MAX_VIDEO_TIME_MS, context, timeoutName());
	}
	
	public void onVideoChanged(Address source, Map<String, Object> changes) {
		if(!Objects.equals(CameraStatusModel.getActiveRecording(id, context.model()), source.getRepresentation())) {
			return;
		}
		
		if(changes.containsKey(RecordingCapability.ATTR_SIZE)) {		   
         if(CameraStatusCapability.STATE_RECORDING.equals(CameraStatusModel.getState(id, context.model()))) {         
            CameraStatusModel.setLastRecording(id, context.model(), source.getRepresentation());
            // FIXME should this be set when the recording starts?
            CameraStatusModel.setLastRecordingTime(id, context.model(), new Date(IrisUUID.timeof((UUID) source.getId())));
         }
         // completed recording
         markIdle();
		}
		
		
		if(Boolean.TRUE.equals(changes.get(RecordingCapability.ATTR_DELETED))) {
			// deleted in-progress recording... weird
			markIdle();
		}
	}

	public void onVideoDeleted(Address source) {
		String recording = source.getRepresentation();
		if(CameraStatusModel.getActiveRecording(id, context.model(), "").equals(recording)) {
			// purged in-progress recording... this has to be a stale recording
			markIdle();
		}
		if(CameraStatusModel.getLastRecording(id, context.model(), "").equals(recording)) {
			CameraStatusModel.setLastRecording(id, context.model(), "");
		}
	}
	
	public void onTimeout() {
		markIdle();
	}
	
	private void markIdle() {
		CameraStatusModel.setActiveRecording(id, context.model(), "");
		if(CameraStatusModel.isStateOFFLINE(id, context.model())) {
			context.logger().warn("Video completed or deleted while camera is offline");
		}
		else {
			CameraStatusModel.setState(id, context.model(), CameraStatusCapability.STATE_IDLE);
		}
		SubsystemUtils.clearTimeout(context, timeoutName());
	}
	
	private void markOnline() {
		String activeRecording = CameraStatusModel.getActiveRecording(id, context.model(), "");
		if(activeRecording.isEmpty()) {
			// expected
			CameraStatusModel.setState(id, context.model(), CameraStatusCapability.STATE_IDLE);
		}
		else {
			context.logger().warn("Camera flapped mid-recording, marking state as STREAMING but could be RECORDING");
			CameraStatusModel.setState(id, context.model(), CameraStatusCapability.STATE_STREAMING);
		}
	}
	
	private void markOffline() {
		CameraStatusModel.setState(id, context.model(), CameraStatusCapability.STATE_OFFLINE);
	}
	
	private String timeoutName() {
		return RECORDING_TIMEOUT_PREFIX + id;
	}

}

