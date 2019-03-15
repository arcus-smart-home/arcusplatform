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

import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;

public class BaseDeletedListener {
	private final PlatformMessageBus messageBus;
	private final PairingDeviceDao pairingDeviceDao;

	protected BaseDeletedListener(
			PlatformMessageBus messageBus,
			PairingDeviceDao pairingDeviceDao
	) {
		this.messageBus = messageBus;
		this.pairingDeviceDao = pairingDeviceDao;
	}

	protected void delete(PairingDevice device) {
		pairingDeviceDao.delete(device);
		PlatformMessage deleted =
				PlatformMessage
					.broadcast()
					.from(device.getAddress())
					.withPlaceId(device.getPlaceId())
					.withPopulation(device.getPopulation())
					.withPayload(Capability.EVENT_DELETED)
					.create();
		messageBus.send(deleted);
	}
	
}

