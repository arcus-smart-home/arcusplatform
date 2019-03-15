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
package com.iris.driver.groovy.mock;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.model.Device;
import com.iris.messages.service.DeviceService;
import com.iris.protocol.mock.MockProtocol;

import static org.junit.Assert.*;

public class TestMockProtocolPlugin{

	private MockProtocolPlugin plugin = null;
	private Device device = new Device();
	
	@Before
	public void setUp() throws Exception {
		plugin=new MockProtocolPlugin();
		device.setProtocolid("testid");
		device.setAccount(UUID.randomUUID());
		device.setHubId(UUID.randomUUID().toString());
		device.setAddress("SERV:dev:"+UUID.randomUUID());
		device.setId(UUID.randomUUID());
		device.setProtocolAddress(Address.protocolAddress(MockProtocol.NAMESPACE, ProtocolDeviceId.hashDeviceId("testid")).getRepresentation());
		
		
	}

	@Test
	public void testHandleRemovePlatformMessage() {
		PlatformMessage pm = plugin.handleRemove(device, 0, false);
		assertFalse("This message should not be a request",pm.isRequest());
		assertEquals(DeviceAdvancedCapability.RemovedDeviceEvent.NAME,pm.getValue().getMessageType());
		assertEquals(device.getAddress(), pm.getSource().getRepresentation());
		assertEquals(DeviceService.ADDRESS, pm.getDestination());
		assertEquals(device.getPlace(), pm.getPlaceId());
 	}

}

