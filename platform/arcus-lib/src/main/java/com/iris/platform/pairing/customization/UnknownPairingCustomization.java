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
package com.iris.platform.pairing.customization;

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bootstrap.ServiceLocator;
import com.iris.messages.address.Address;
import com.iris.messages.capability.ProductCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.Place;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.platform.pairing.ProductLoader;
import com.iris.prodcat.ProductCatalogEntry;

class UnknownPairingCustomization extends PairingCustomization {
	private static final Logger logger = LoggerFactory.getLogger(UnknownPairingCustomization.class);

	UnknownPairingCustomization(
			String action,
			String id,
			@Nullable String header,
			@Nullable String title,
			@Nullable String note,
			@Nullable List<String> description,
			@Nullable String linkText,
			@Nullable String linkUrl
	) {
		super(
				action,
				id,
				header,
				title,
				note,
				description,
				linkText,
				linkUrl
		);
	}

	/**
	 * Return true if there is no product id in the Device model, or the product id can not be 
	 * found in product catalog.
	 */
	@Override
	protected boolean apply(Place place, Model device) {
		String productId = DeviceModel.getProductId(device, "");
		if(productId.isEmpty()) {
			return true;
		}
		Address productAddress = Address.platformService(productId, ProductCapability.NAMESPACE);
		ProductCatalogEntry entry = 
			ServiceLocator
				.getInstance(ProductLoader.class)
				.get(place, productAddress);
		if(entry == null) {
			logger.warn("Missing product catalog entry [{}]", productAddress);
			return true;
		}
		return false;
	}
	
}

