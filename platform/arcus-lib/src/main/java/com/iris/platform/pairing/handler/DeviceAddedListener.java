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

import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ProductCapability;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.model.Device;
import com.iris.messages.model.serv.PairingDeviceModel;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;

@Singleton
public class DeviceAddedListener extends BaseChangeListener {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DeviceAddedListener.class);
	
	private final DeviceDAO deviceDao;
	private final PairingDeviceDao pairingDeviceDao;
	
	@Inject
	public DeviceAddedListener(
			PlatformMessageBus messageBus,
			DeviceDAO deviceDao,
			PairingDeviceDao pairingDeviceDao
	) {
		super(messageBus, pairingDeviceDao);
		this.deviceDao = deviceDao;
		this.pairingDeviceDao = pairingDeviceDao;
	}
	
	@OnMessage(from="DRIV:dev:*", types=Capability.EVENT_ADDED)
	public void onDeviceAdded(PlatformMessage message) {
		Device device = deviceDao.findById((UUID) message.getSource().getId());
		if(device == null) {
			logger.warn("Unable to load device with address [{}], can't create associated PairingDevice", message.getSource());
			return;
		}
		UUID placeId = device.getPlace();
		DeviceProtocolAddress protocolAddress = (DeviceProtocolAddress) Address.fromString(device.getProtocolAddress());
		Map<String, Object> original;
		PairingDevice model = pairingDeviceDao.findByProtocolAddress(placeId, protocolAddress);
		if(model == null) {
			model = new PairingDevice();
			model.setPlaceId(placeId);
			model.setProtocolAddress(protocolAddress);
			PairingDeviceModel.setCustomizations(model, ImmutableSet.of());
			original = null;
		}
		else {
			original = model.toMap();
		}
		PairingDeviceModel.setDeviceAddress(model, message.getSource().getRepresentation());
		if(!StringUtils.isEmpty(device.getProductId())) {
			PairingDeviceModel.setProductAddress(model, Address.platformService(device.getProductId(), ProductCapability.NAMESPACE).getRepresentation());
		}
		updatePairingStatus(model, message.getValue(), original);
	}

}

