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

import com.iris.client.event.ListDevicesEvent;
import com.iris.messages.ClientMessage;

// TODO:  jettison due to code generation?  if not fix
public abstract class ListDevicesHandler extends BaseClientCachingEventHandler<ListDevicesEvent> {

	@Override
	public void handleMessage(ClientMessage message) {
	   logger.debug("Got list devices response");
//		if (!(message.getPayload() instanceof ListDevicesResponse)) {
//			logger.warn("Unexpected response type {}, when expecting {}.  Returning no devices!", message.getPayload().getClass(), ListDevicesResponse.class);
//			return;
//		}
//
//		Collection<Device> devices = ((ListDevicesResponse) message.getPayload()).getDevices();
//
//		ListDevicesEvent event = new ListDevicesEvent(devices);
//		if (cachingService != null) {
//			for (ClientDeviceModel modelDevice : event.getDevices()) {
//				cachingService.saveToCache(modelDevice);
//			}
//		} else {
//			logger.warn("Cannot cache devices.  Caching service is null. Please register caching service by calling setCachingService()");
//		}
//
//		publishEvent(event);
	}
}

