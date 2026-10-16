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

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.model.serv.PairingDeviceModel;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;

/**
 * This class depends on the driver state and the driver errors arriving
 * in the same ValueChange.  Driver services sends both whenever either
 * changes, and announces them again when a device that has had errors
 * loads.  A failed pairing (MISPAIRED or MISCONFIGURED) also recovers on
 * a change whose driver state isn't an active one, such as a lost device
 * whose error was cleared.
 * @author tweidlin
 *
 */
@Singleton
public class DeviceChangeListener extends BaseChangeListener {
	private final PairingDeviceDao pairingDeviceDao;
	
	@Inject
	public DeviceChangeListener(
			PlatformMessageBus messageBus,
			PairingDeviceDao pairingDeviceDao
	) {
		super(messageBus, pairingDeviceDao);
		this.pairingDeviceDao = pairingDeviceDao;
	}
	
	@OnMessage(from="DRIV:dev:*", types=Capability.EVENT_VALUE_CHANGE)
	public void onDeviceChanged(PlatformMessage message) {
		MessageBody changes = message.getValue();
		@Nullable String driverState = DeviceAdvancedCapability.getDriverstate(changes);
		boolean driverStateChanged =
				DeviceAdvancedCapability.DRIVERSTATE_PROVISIONING.equals(driverState) ||
				DeviceAdvancedCapability.DRIVERSTATE_ACTIVE.equals(driverState) ||
				DeviceAdvancedCapability.DRIVERSTATE_UNSUPPORTED.equals(driverState);
		if(!driverStateChanged && !mayClearPairingFailed(changes)) {
			// this is not the change you are looking for
			return;
		}
		
		UUID placeId = UUID.fromString(message.getPlaceId());
		String rep = message.getSource().getRepresentation();
		PairingDevice model = pairingDeviceDao.listByPlace(placeId).stream().filter((p) -> rep.equals(PairingDeviceModel.getDeviceAddress(p))).findAny().orElse(null);
		if(model == null) {
			// no pairing device, no worries
			return;
		}
		if(!driverStateChanged && !isFailed(model)) {
			// a change without an active driver state, e.g. on a lost device, only matters to a failed pairing
			return;
		}
		updatePairingStatus(model, message.getValue(), model.toMap());
	}

	// errors without pairing.failed may clear a failed pairing, e.g. a stale pairing.failed or pairing.misconfigured being cleared
	private static boolean mayClearPairingFailed(MessageBody changes) {
		if(!changes.getAttributes().containsKey(DeviceAdvancedCapability.ATTR_ERRORS)) {
			return false;
		}
		Map<String, String> errors = DeviceAdvancedCapability.getErrors(changes, ImmutableMap.of());
		return !errors.containsKey(DeviceAdvancedCapability.PairingFailedException.CODE_PAIRING_FAILED);
	}

}

