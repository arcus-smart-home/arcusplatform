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
package com.iris.platform.services.productcatalog.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.iris.Utils;
import com.iris.core.dao.PopulationDAO;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.ProductCatalogCapability;
import com.iris.prodcat.Brand;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogManager;

public class GetBrandsHandler extends AbstractProductCatalogHandler {

   @Inject
   public GetBrandsHandler(PopulationDAO populationDao, ProductCatalogManager manager) {
      super(populationDao, manager);
   }

	@Override
	public String getMessageType() {
		return ProductCatalogCapability.GetBrandsRequest.NAME;
	}

	@Override
	public MessageBody handleRequest(ProductCatalog context, PlatformMessage msg) {
		Utils.assertNotNull(context, "The product catalog is required");
		List<Brand> brands = context.getBrands();
		Map<String,Integer> counts = context.getProductCountByBrand();

		return ProductCatalogCapability.GetBrandsResponse.builder()
		      .withBrands(brands.stream().map((b) -> brandToMap(b)).collect(Collectors.toList()))
		      .withCounts(counts)
		      .build();
	}

	private Map<String,Object> brandToMap(Brand brand) {
	   Map<String,Object> asMap = new HashMap<>();
	   asMap.put("name", brand.getName());
	   if(!StringUtils.isEmpty("image")) {
	      asMap.put("image", brand.getImage());
	   }
	   if(!StringUtils.isEmpty("description")) {
	      asMap.put("description", brand.getDescription());
	   }
	   return asMap;
	}
}

