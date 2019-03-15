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
package com.iris.driver.groovy.ipcd;

import org.junit.Test;

import com.iris.driver.groovy.ProtocolPluginTestCase;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.protocol.ipcd.IpcdProtocol;

public class TestIpcdProtocolPlugin extends ProtocolPluginTestCase {
	private IpcdProtocolPlugin plugin = new IpcdProtocolPlugin();
	
	@Override
	protected Address protocolAddress() {
		return Address.protocolAddress(IpcdProtocol.NAMESPACE, ProtocolDeviceId.hashDeviceId("test"));
	}

	@Test
	public void testRemove() {
		PlatformMessage message = plugin.handleRemove(device, timeoutMs, false);
		assertRequestToBridge(IpcdProtocol.NAMESPACE, message);
		assertBridgeDeviceRemove(IpcdProtocol.NAMESPACE, message.getValue());
	}

	@Test
	public void testForceRemove() {
		PlatformMessage message = plugin.handleRemove(device, timeoutMs, true);
		assertRequestToBridge(IpcdProtocol.NAMESPACE, message);
		assertBridgeDeviceRemove(IpcdProtocol.NAMESPACE, message.getValue());
	}

}

