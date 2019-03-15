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
package com.iris.video.recording;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.messaging.MessageListener;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.CameraCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.video.storage.PreviewStorage;

@Singleton
public class CameraDeletedListener implements MessageListener<PlatformMessage> {
	private static final Logger logger = LoggerFactory.getLogger(CameraDeletedListener.class);
	private final PreviewStorage previewStorage;
	
	@Inject
	public CameraDeletedListener(PreviewStorage previewStorage) {
		this.previewStorage = previewStorage;
	}
	@Override
	@OnMessage(types={Capability.EVENT_DELETED}, from="DRIV:dev:*")
	public void onMessage(PlatformMessage message) {
		if(isCamera(message)) {
			UUID cameraId = (UUID) message.getSource().getId();
			try{
				previewStorage.delete(cameraId.toString());
			}catch(Exception e) {
				logger.error("Error deleting preview for camera {}", cameraId, e);
			}
		}		
	}
	
	private boolean isCamera(PlatformMessage message) {
		Map<String, Object> attributes = message.getValue().getAttributes();
		if(attributes != null) {
			String deviceType = (String) attributes.get(DeviceCapability.ATTR_DEVTYPEHINT);
			if(deviceType != null && CameraCapability.NAME.equalsIgnoreCase(deviceType)) {
				return true;
			}
		}
		return false;
	}

}

