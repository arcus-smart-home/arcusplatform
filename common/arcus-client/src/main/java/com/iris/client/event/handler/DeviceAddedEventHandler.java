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

import com.iris.client.event.DeviceAddedEvent;
import com.iris.messages.ClientMessage;

// TODO:  can this be jettisoned with the new code generation stuffs?  if not we need to fix it
public abstract class DeviceAddedEventHandler extends BaseClientCachingEventHandler<DeviceAddedEvent> {

    @Override
    public void handleMessage(ClientMessage message) {
       logger.debug("Device added event received: {}", message);
//        com.iris.messages.device.DeviceAddedEvent platformEvent = (com.iris.messages.device.DeviceAddedEvent) message.getPayload();
//
//        ClientDeviceModel d = ClientDeviceModel.fromPlatformDevice(platformEvent.getDevice());
//        d.setLastUpdate(new Date().getTime());
//
//        if (cachingService != null) {
//        	cachingService.saveToCache(d);
//        } else {
//        	logger.warn("Cannot cache devices.  Caching service is null. Please register caching service by calling setCachingService()");
//        }
//
//        publishEvent(new DeviceAddedEvent(d));
    }
}

