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
package com.iris.client.event.handler;

import java.util.Date;

import com.iris.client.event.DeviceUpdateEvent;
import com.iris.client.model.device.ClientDeviceModel;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;

public abstract class DeviceUpdateEventHandler extends BaseClientCachingEventHandler<DeviceUpdateEvent> {
	@Override
	public void handleMessage(ClientMessage message) {
		MessageBody event = message.getPayload();

		if(!event.getMessageType().equals(ClientDeviceModel.DeviceBase.EVENT_GET_ATTRIBUTES_RESPONSE)) {
        	handleDeviceUpdateEvent(event, message);
        }
	}

	public void handleDeviceUpdateEvent(MessageBody event, ClientMessage message) {
		ClientDeviceModel fromCache = null;

		if (cachingService != null) {
			fromCache = cachingService.loadItemFromCache(message.getSource(), ClientDeviceModel.class);

	        if(fromCache == null) {
	            logger.warn("No device with address {} found in the cache, only the updated data will be passed through the event", message.getSource());
	            fromCache = new ClientDeviceModel();
	            fromCache.setDriverAddress(message.getSource());
	        }
		} else {
			logger.warn("Cannot cache devices.  Caching service is null. Please register caching service by calling setCachingService()");
            fromCache = new ClientDeviceModel();
            fromCache.setDriverAddress(message.getSource());
		}

		fromCache.setLastUpdate(new Date().getTime());
        fromCache.putAttributes(event.getAttributes());

        publishEvent(new DeviceUpdateEvent(fromCache));
	}
}

