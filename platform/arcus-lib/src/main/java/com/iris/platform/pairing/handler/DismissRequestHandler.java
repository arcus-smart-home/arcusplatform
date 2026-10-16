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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.DeviceDAO;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PairingDeviceCapability.DismissResponse;
import com.iris.messages.capability.PairingDeviceCapability.DismissRequest;
import com.iris.messages.errors.Errors;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.serv.PairingDeviceModel;
import com.iris.platform.model.PersistentModelWrapper;
import com.iris.platform.pairing.MispairedPairingDevices;
import com.iris.platform.pairing.PairingDevice;

@Singleton
public class DismissRequestHandler {
	private final DeviceDAO deviceDao;

	@Inject
	public DismissRequestHandler(DeviceDAO deviceDao) {
		this.deviceDao = deviceDao;
	}

	@Request(DismissRequest.NAME)
	public MessageBody dismiss(PersistentModelWrapper<PairingDevice> device) {
		Errors.assertValidRequest(
				PairingDeviceModel.isPairingStatePAIRED(device.model()) || isStaleMispaired(device.model()),
				"Can only dismiss fully PAIRED devices, or MISPAIRED devices whose device has since recovered"
		);
		device.delete();
		return DismissResponse.instance();
	}

	// checked against the stored device, so a client can't dismiss a device that is still mispaired
	private boolean isStaleMispaired(PairingDevice model) {
		if(!MispairedPairingDevices.isMispaired(model)) {
			return false;
		}
		String deviceAddress = PairingDeviceModel.getDeviceAddress(model);
		if(StringUtils.isEmpty(deviceAddress)) {
			return false;
		}
		Object deviceId = Address.fromString(deviceAddress).getId();
		if(!(deviceId instanceof UUID)) {
			return false;
		}
		return MispairedPairingDevices.hasRecovered(deviceDao.modelById((UUID) deviceId));
	}
}
