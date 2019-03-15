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
package com.iris.platform.subsystem.pairing.attribute;

import java.util.Date;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.util.AddressesAttributeBinder;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PairingSubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.PairingDeviceMockModel;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.platform.pairing.PairingDevice;
import com.iris.platform.pairing.PairingDeviceDao;
import com.iris.platform.pairing.PairingDeviceMock;
import com.iris.platform.subsystem.pairing.PairingProtocol;
import com.iris.platform.subsystem.pairing.PairingUtils;
import com.iris.platform.subsystem.pairing.ProductLoaderForPairing;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.protocol.zwave.ZWaveProtocol;

@Singleton
public class PairingDevicesAttributeBinder extends AddressesAttributeBinder<PairingSubsystemModel> {
	private final PairingDeviceDao pairingDeviceDao;
	private final ProductLoaderForPairing loader;
	
	@Inject
	public PairingDevicesAttributeBinder(PairingDeviceDao pairingDeviceDao, ProductLoaderForPairing loader) {
		super(PairingUtils.isPairingDevice(), PairingSubsystemCapability.ATTR_PAIRINGDEVICES);
		this.pairingDeviceDao = pairingDeviceDao;
		this.loader = loader;
	}

	@Override
	protected boolean addAddress(SubsystemContext<PairingSubsystemModel> context, Address address) {
		if(PairingSubsystemModel.isPairingModeIDLE(context.model())) {
			context.logger().debug("Ignoring pairing device [{}] because the pairing subsystem is idle", address);
			return false;
		}
		else {
			return super.addAddress(context, address);
		}
	}

	@Override
	protected void afterAdded(SubsystemContext<PairingSubsystemModel> context, Model model) {
		super.afterAdded(context, model);
		context.model().setSearchDeviceFound(true);
		context.model().setSearchIdle(false);
		context.model().setSearchIdleTimeout(new Date(0));
		
		// FIXME this load wouldn't be necessary if:
		//  - We added protocol address to the pairing device model
		//     -OR-
		//  - We trust hubzwave:healRecommended
		PairingDevice device = pairingDeviceDao.findByAddress(model.getAddress());
		if(device == null) {
			context.logger().warn("Unable to load pairing device [{}]; may not properly detect if a zwave rebuild is required", device);
			return;
		}
		
		String protocol = device.getProtocolAddress().getProtocolName();
		if(ZWaveProtocol.NAMESPACE.equals(protocol)) {
			PairingUtils.setZWaveRebuildRequired(context);
		}
		else if(device instanceof PairingDeviceMock) {
			if(isMockZWaveDevice(context, device)) {
				PairingUtils.setZWaveRebuildRequired(context);
			}
		}
	}

	private boolean isMockZWaveDevice(SubsystemContext<PairingSubsystemModel> context, PairingDevice model) {
		String productAddress = PairingDeviceMockModel.getTargetProductAddress(model, "");
		if(productAddress.isEmpty()) {
			return false;
		}
		Optional<ProductCatalogEntry> entryRef = loader.get(context, productAddress);
		if(!entryRef.isPresent()) {
			return false;
		}
		return PairingProtocol.forProduct(entryRef.get()) == PairingProtocol.ZWAV;
	}
	
	
}

