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
package com.iris.protocol;

import java.util.UUID;

import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.capability.BridgeChildCapability;
import com.iris.messages.model.Device;

/**
 * A reference that allows a device to be removed even
 * if there isn't a full platform record yet.  This is
 * needed for early pairing use cases.
 * 
 * @author tweidlin
 */
public class RemoveProtocolRequest {
	private UUID accountId;
	private UUID placeId;
	private Address sourceAddress;
	private DeviceProtocolAddress protocolAddress;
	private boolean bridgeChild;
	private long timeoutMs;
	private boolean forceRemove;
	
	public RemoveProtocolRequest() {
		
	}
	
	public RemoveProtocolRequest(Device device) {
		this.accountId = device.getAccount();
		this.placeId = device.getPlace();
		this.sourceAddress = Address.fromString( device.getAddress() );
		this.protocolAddress = (DeviceProtocolAddress) Address.fromString( device.getProtocolAddress() );
		this.bridgeChild = device.getCaps() != null && device.getCaps().contains(BridgeChildCapability.NAMESPACE);
	}
	
	public UUID getAccountId() {
		return accountId;
	}
	
	public void setAccountId(UUID accountId) {
		this.accountId = accountId;
	}
	
	public UUID getPlaceId() {
		return placeId;
	}
	
	public void setPlaceId(UUID placeId) {
		this.placeId = placeId;
	}
	
	public Address getSourceAddress() {
		return sourceAddress;
	}
	
	public void setSourceAddress(Address sourceAddress) {
		this.sourceAddress = sourceAddress;
	}
	
	public DeviceProtocolAddress getProtocolAddress() {
		return protocolAddress;
	}
	
	public void setProtocolAddress(DeviceProtocolAddress protocolAddress) {
		this.protocolAddress = protocolAddress;
	}

	public long getTimeoutMs() {
		return timeoutMs;
	}

	public void setTimeoutMs(long timeoutMs) {
		this.timeoutMs = timeoutMs;
	}

	public boolean isBridgeChild() {
		return bridgeChild;
	}

	public void setBridgeChild(boolean bridgeChild) {
		this.bridgeChild = bridgeChild;
	}

	public boolean isForceRemove() {
		return forceRemove;
	}

	public void setForceRemove(boolean forceRemove) {
		this.forceRemove = forceRemove;
	}

}

