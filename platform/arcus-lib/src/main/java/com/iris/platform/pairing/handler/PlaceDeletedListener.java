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
package com.iris.platform.pairing.handler;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.platform.pairing.PairingDeviceDao;

@Singleton
public class PlaceDeletedListener {
	private static final Logger logger = LoggerFactory.getLogger(PlaceDeletedListener.class);
	
	private final PairingDeviceDao pairingDeviceDao;
	
	@Inject
	public PlaceDeletedListener(PairingDeviceDao pairingDeviceDao) {
		this.pairingDeviceDao = pairingDeviceDao;
	}
	
	@OnMessage(from="SERV:place:*", types=Capability.EVENT_DELETED)
	public void onPlaceDeleted(PlatformMessage message) {
		if(StringUtils.isEmpty(message.getPlaceId())) {
			logger.warn("Received hub [{}] deleted with no place id", message.getSource());
			return;
		}
		// this place is already dead, don't worry about emitting base:Deleted
		pairingDeviceDao.deleteByPlace((UUID) message.getSource().getId());
	}

}

