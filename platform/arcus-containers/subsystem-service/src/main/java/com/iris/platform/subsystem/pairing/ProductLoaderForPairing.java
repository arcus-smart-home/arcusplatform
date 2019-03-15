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
package com.iris.platform.subsystem.pairing;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.platform.pairing.ProductLoader;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;

@Singleton
public class ProductLoaderForPairing extends ProductLoader {
	
	@Inject
	public ProductLoaderForPairing(ProductCatalogManager manager) {
		super(manager);
	}
	
	public Optional<ProductCatalogEntry> getCurrent(SubsystemContext<PairingSubsystemModel> context) {
		return get(context, context.model().getSearchProductAddress());
	}
	
	public Optional<ProductCatalogEntry> get(SubsystemContext<?> context, String productAddress) {
		if(StringUtils.isEmpty(productAddress)) {
			return Optional.empty();
		}
		
		Model place = SubsystemUtils.getPlace(context);
		ProductCatalogEntry product = get(PlaceModel.getPopulation(place), Address.fromString(productAddress));
		if(product != null) {
			return Optional.of(product);
		}else{
			return Optional.empty();
		}		
		
	}
	
	public static final class ProductCacheInfo {
		private static final String VAR_PRODUCT_INFO = "currentProductInfo";
		
		@Nullable
		public static ProductCacheInfo get(SubsystemContext<?> context) {
			return context.getVariable(VAR_PRODUCT_INFO).as(ProductCacheInfo.class);
		}		
		
		@Nullable
		public static ProductCacheInfo saveOrClear(SubsystemContext<?> context, Optional<ProductCatalogEntry> product) {
			if(product !=null && product.isPresent()) {
				ProductCatalogEntry cur = product.get();
				ProductCacheInfo info = new ProductCacheInfo();
				info.pairingIdleTimeoutMs = cur.getPairingIdleTimeoutMs();
				info.pairingTimeoutMs = cur.getPairingTimeoutMs();	
				//Save it only if at least one value is not null to avoid saving ProductCacheInfo as Variable
				if(info.pairingIdleTimeoutMs != null || info.pairingTimeoutMs != null) {
					context.setVariable(VAR_PRODUCT_INFO, info);
					return info;
				}
			}
			context.setVariable(VAR_PRODUCT_INFO, null);
			return null;
		}
		
		public static void clear(SubsystemContext<?> context) {
			context.setVariable(VAR_PRODUCT_INFO, null);
		}
		
		
		private Integer pairingIdleTimeoutMs;
		private Integer pairingTimeoutMs; 
		public Integer getPairingIdleTimeoutMs() {
			return pairingIdleTimeoutMs;
		}

		public Integer getPairingTimeoutMs() {
			return pairingTimeoutMs;
		}
		
	}
}

