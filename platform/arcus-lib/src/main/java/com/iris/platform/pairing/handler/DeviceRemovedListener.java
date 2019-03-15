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
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.DeviceAdvancedCapability.RemovedDeviceEvent;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;

/**
 * This handles the case where a pairing device is removed before there
 * is an entry in the device database.
 * 
 * This can also cause the pairing entry to be deleted when before we receive
 * the base:Deleted for the device when there is a driver.  However the outcome
 * is the same in both cases -- the pairing entry is deleted.
 * @author tweidlin
 *
 */
@Singleton
public class DeviceRemovedListener extends BaseDeletedListener {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DeviceRemovedListener.class);
	
	private final PairingDeviceDao pairingDeviceDao;
	
	@Inject
	public DeviceRemovedListener(
			PlatformMessageBus messageBus,
			PairingDeviceDao pairingDeviceDao
	) {
		super(messageBus, pairingDeviceDao);
		this.pairingDeviceDao = pairingDeviceDao;
	}
	
	@OnMessage(types=RemovedDeviceEvent.NAME)
	public void onDeviceRemoved(PlatformMessage message) {
		if(StringUtils.isEmpty(message.getPlaceId())) {
			logger.warn("Received device [{}] removed with no place id", message.getSource());
			return;
		}
		
		MessageBody removed = message.getValue();
		UUID placeId = UUID.fromString(message.getPlaceId());
		String hubId = RemovedDeviceEvent.getHubId(removed);
		String protocol = RemovedDeviceEvent.getProtocol(removed);
		ProtocolDeviceId protocolId = ProtocolDeviceId.fromRepresentation( RemovedDeviceEvent.getProtocolId(removed) );
		Address protocolAddress;
		if(StringUtils.isEmpty(hubId)) {
			protocolAddress = Address.protocolAddress(protocol, protocolId);
		}
		else {
			protocolAddress = Address.hubProtocolAddress(hubId, protocol, protocolId);
		}
		PairingDevice device = pairingDeviceDao.findByProtocolAddress(placeId, protocolAddress);
		if(device != null) {
			logger.debug("Received {} for device {}, removing pairing entry", RemovedDeviceEvent.NAME, protocolAddress);
			delete(device);
		}
	}

}

