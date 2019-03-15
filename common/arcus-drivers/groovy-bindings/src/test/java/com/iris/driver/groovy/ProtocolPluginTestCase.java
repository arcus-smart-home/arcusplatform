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
package com.iris.driver.groovy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.UUID;

import org.junit.Before;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubCapability.UnpairingRequestRequest;
import com.iris.messages.model.Device;
import com.iris.messages.service.BridgeService;
import com.iris.messages.service.DeviceService;

public abstract class ProtocolPluginTestCase {
	protected Device device;
	protected long timeoutMs = Math.round(Math.random() * 30000);
	protected String hubId = "ABC-1234";
	
	@Before
	public void createDevice() {
		device = new Device();
		device.setAccount(UUID.randomUUID());
		device.setPlace(UUID.randomUUID());
		
		device.setCaps(ImmutableSet.of(Capability.NAMESPACE, DeviceCapability.NAMESPACE));
		device.setId(UUID.randomUUID());
		device.setAddress(Address.platformDriverAddress(device.getId()).getRepresentation());

		DeviceProtocolAddress protocolAddress = (DeviceProtocolAddress) protocolAddress();
		device.setProtocol(protocolAddress.getProtocolName());
		device.setProtocolAddress(protocolAddress.getRepresentation());
		device.setProtocolid(protocolAddress.getProtocolDeviceId().getRepresentation());
		if(protocolAddress.getHubId() != null) {
			device.setHubId(protocolAddress.getHubId());
		}
	}

	protected abstract Address protocolAddress();
	
	protected void assertRequestToHub(PlatformMessage message) {
		assertEquals(device.getAddress(), message.getSource().getRepresentation());
		assertEquals(Address.hubService(hubId, HubCapability.NAMESPACE), message.getDestination());
		assertTrue(message.isRequest());
	}

	protected void assertRequestToBridge(String bridgeId, PlatformMessage message) {
		assertEquals(device.getAddress(), message.getSource().getRepresentation());
		assertEquals(Address.bridgeAddress(bridgeId), message.getDestination());
		assertTrue(message.isRequest());
	}

	protected void assertEventToDriverServices(PlatformMessage message) {
		assertEquals(device.getAddress(), message.getSource().getRepresentation());
		// oops DeviceService.ADDRESS is wrong
		assertEquals(Address.platformService(DeviceCapability.NAMESPACE), message.getDestination());
		assertFalse(message.isRequest());
	}

	protected void assertDeviceRemoved(String removalStyle, MessageBody message) {
		assertEquals(DeviceAdvancedCapability.RemovedDeviceEvent.NAME, message.getMessageType());
		assertEquals(device.getAccount().toString(), DeviceAdvancedCapability.RemovedDeviceEvent.getAccountId(message));
		assertEquals(device.getHubId(), DeviceAdvancedCapability.RemovedDeviceEvent.getHubId(message));
		assertEquals(device.getProtocol(), DeviceAdvancedCapability.RemovedDeviceEvent.getProtocol(message));
		assertEquals(device.getProtocolid(), DeviceAdvancedCapability.RemovedDeviceEvent.getProtocolId(message));
		assertEquals(removalStyle, DeviceAdvancedCapability.RemovedDeviceEvent.getStatus(message));
	}

	protected void assertBridgeDeviceRemove(String namespace, MessageBody value) {
		assertEquals(BridgeService.RemoveDeviceRequest.NAME, value.getMessageType());
		assertEquals(device.getProtocolAddress(), BridgeService.RemoveDeviceRequest.getId(value));
		assertEquals(device.getAccount().toString(), BridgeService.RemoveDeviceRequest.getAccountId(value));
		assertEquals(device.getPlace().toString(), BridgeService.RemoveDeviceRequest.getPlaceId(value));
	}

	protected void assertHubDeviceRemove(MessageBody message, boolean forceRemove) {
		assertEquals(UnpairingRequestRequest.NAME, message.getMessageType());
		assertEquals(UnpairingRequestRequest.ACTIONTYPE_START_UNPAIRING, UnpairingRequestRequest.getActionType(message));
		assertEquals(forceRemove, UnpairingRequestRequest.getForce(message));
		assertEquals(device.getProtocol(), UnpairingRequestRequest.getProtocol(message));
		assertEquals(device.getProtocolid(), UnpairingRequestRequest.getProtocolId(message));
		assertEquals((Long)timeoutMs, UnpairingRequestRequest.getTimeout(message));
	}

}

