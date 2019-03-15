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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@RunWith(value = Parameterized.class)
@Mocks({DeviceDAO.class, PairingDeviceDao.class})
@Modules({ InMemoryMessageModule.class} )
public class TestDeviceAddedListener extends IrisMockTestCase {
	
	@Inject
	private InMemoryPlatformMessageBus platformBus;
	@Inject
	private DeviceDAO deviceDao;
	@Inject
	private PairingDeviceDao pairingDeviceDao;
	@Inject
	private DeviceAddedListener theHandler;
	
	@Parameters(name="pairingDeviceExist[{0}],protocolAddress[{1}],expectedRemoveMode[{2}]")
   public static List<Object []> files() {
      return Arrays.<Object[]>asList(
            new Object [] { false, Address.hubProtocolAddress("LWW-1250", "ZIGB", ProtocolDeviceId.fromBytes(new byte[] {(byte)0x04})), PairingDeviceCapability.REMOVEMODE_HUB_AUTOMATIC},
            new Object [] { false, Address.hubProtocolAddress("LWW-1250", "ZWAV", ProtocolDeviceId.fromBytes(new byte[] {(byte)0x03})), PairingDeviceCapability.REMOVEMODE_HUB_MANUAL},
            new Object [] { false, Address.protocolAddress(IpcdProtocol.NAMESPACE, ProtocolDeviceId.hashDeviceId("BlackBox:ms2:1234")), PairingDeviceCapability.REMOVEMODE_CLOUD},
            new Object [] { false, Address.protocolAddress("MOCK", new byte[] {(byte)0x04}), PairingDeviceCapability.REMOVEMODE_CLOUD},
            new Object [] { true, Address.hubProtocolAddress("LWW-1250", "ZIGB", ProtocolDeviceId.fromBytes(new byte[] {(byte)0x04})), PairingDeviceCapability.REMOVEMODE_HUB_AUTOMATIC},
            new Object [] { true, Address.hubProtocolAddress("LWW-1250", "ZWAV", ProtocolDeviceId.fromBytes(new byte[] {(byte)0x03})), PairingDeviceCapability.REMOVEMODE_HUB_MANUAL},
            new Object [] { true, Address.protocolAddress(IpcdProtocol.NAMESPACE, ProtocolDeviceId.hashDeviceId("BlackBox:ms2:1234")), PairingDeviceCapability.REMOVEMODE_CLOUD},
            new Object [] { true, Address.protocolAddress("MOCK", new byte[] {(byte)0x04}), PairingDeviceCapability.REMOVEMODE_CLOUD}

      );
   }
   
   private final boolean pairingDeviceExist;
   private final DeviceProtocolAddress protocolAddress;
   private final String expectedRemoveMode;
   public TestDeviceAddedListener(boolean pairingDeviceExist, DeviceProtocolAddress protocolAddress, String expectedRemoveMode) {
   	this.protocolAddress = protocolAddress;
   	this.expectedRemoveMode = expectedRemoveMode;
   	this.pairingDeviceExist = pairingDeviceExist;
   }
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	@Test
	public void testOnDeviceAdded_NoExistingPairingDevice() throws InterruptedException, TimeoutException {
		Device device = Fixtures.createDevice();
		device.setPlace(UUID.randomUUID());
		device.setProtocolAddress(protocolAddress.toString());
		MessageBody msgbody = MessageBody.buildMessage(Capability.EVENT_ADDED);
		PlatformMessage message = PlatformMessage.builder().from(device.getAddress()).withPayload(msgbody).create();
		
		EasyMock.expect(deviceDao.findById(device.getId())).andReturn(device);
		
		if(pairingDeviceExist) {
			PairingDevice pairingDevice = new PairingDevice();
			pairingDevice.setPlaceId(device.getPlace());
			pairingDevice.setId(device.getPlace(), 1);
			pairingDevice.setProtocolAddress(protocolAddress);
			EasyMock.expect(pairingDeviceDao.findByProtocolAddress(device.getPlace(), Address.fromString(device.getProtocolAddress()))).andReturn(pairingDevice);			
		}else{
			EasyMock.expect(pairingDeviceDao.findByProtocolAddress(device.getPlace(), Address.fromString(device.getProtocolAddress()))).andReturn(null);
		}
		Capture<PairingDevice> pairingDeviceCapture = EasyMock.newCapture(CaptureType.ALL);
		EasyMock.expect(pairingDeviceDao.save(EasyMock.capture(pairingDeviceCapture))).andAnswer(new IAnswer<PairingDevice>() {
			@Override
			public PairingDevice answer() throws Throwable {
				PairingDevice val = pairingDeviceCapture.getValue();
				if(val.getId() == null) {
					val.setId(device.getPlace(), 1);
					val.setCreated(new Date());
					val.setModified(new Date());
				}else{
					val.setModified(new Date());
				}
				return val;
			}
		});
		replay();
		
		theHandler.onDeviceAdded(message);
		
		PlatformMessage msg = platformBus.take();
		PairingDevice savedPairingDevice = pairingDeviceCapture.getValue();
		assertEquals(savedPairingDevice.getAddress(), msg.getSource());
		assertEquals(device.getPlace(), savedPairingDevice.getPlaceId());
		assertEquals(device.getProtocolAddress(), savedPairingDevice.getProtocolAddress().toString());
		assertEquals(expectedRemoveMode, savedPairingDevice.getRemoveMode());
	}

}

