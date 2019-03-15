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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PairingDeviceCapability;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Place;
import com.iris.messages.model.serv.PairingDeviceMockModel;
import com.iris.messages.model.serv.PairingDeviceModel;
import com.iris.messages.service.PairingDeviceService.CreateMockRequest;
import com.iris.messages.service.PairingDeviceService.CreateMockResponse;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;
import com.iris.platform.pairing.PairingDeviceMock;
import com.iris.platform.pairing.ProductLoader;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.protocol.mock.MockProtocol;
import com.iris.util.IrisUUID;

@Singleton
public class CreateMockRequestHandler {
	private final PairingDeviceDao pairingDao;
	private final PlatformMessageBus bus;
	private final ProductLoader loader;

	@Inject
	public CreateMockRequestHandler(
			PairingDeviceDao pairingDao, 
			PlatformMessageBus bus, 
			ProductLoader loader
	) {
		this.pairingDao = pairingDao;
		this.bus = bus;
		this.loader = loader;
	}

	@Request(value=CreateMockRequest.NAME, service=true)
	public MessageBody createMock(Place place, @Named(CreateMockRequest.ATTR_PRODUCTADDRESS) String productAddress) {
		ProductCatalogEntry entry = loader.get(place, Address.fromString(productAddress));
		assertMockableEntry(entry);
		
		PairingDevice device = new PairingDeviceMock();
		device.setPlaceId(place.getId());
		device.setProtocolAddress((DeviceProtocolAddress) Address.protocolAddress(MockProtocol.NAMESPACE, ProtocolDeviceId.hashDeviceId(IrisUUID.randomUUID().toString())));
		PairingDeviceModel.setCustomizations(device, ImmutableSet.of());
		PairingDeviceModel.setPairingPhase(device, PairingDeviceCapability.PAIRINGPHASE_JOIN);
		PairingDeviceModel.setPairingState(device, PairingDeviceCapability.PAIRINGSTATE_PAIRING);
		PairingDeviceMockModel.setTargetProductAddress(device, productAddress);
		device = pairingDao.save(device);
		
		Map<String, Object> attributes = device.toMap();
		PlatformMessage added =
			PlatformMessage
				.broadcast()
				.from(device.getAddress())
				.withPlaceId(device.getPlaceId())
				.withPopulation(device.getPopulation())
				.withPayload(MessageBody.buildMessage(Capability.EVENT_ADDED, attributes))
				.create();
		bus.send(added);
		return 
			CreateMockResponse
				.builder()
				.withDevice(attributes)
				.build();
	}

	private void assertMockableEntry(ProductCatalogEntry entry) {
		// TODO Auto-generated method stub
		
	}
}

