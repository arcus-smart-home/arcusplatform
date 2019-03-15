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
package com.iris.client.server.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.service.ProductCatalogService;
import com.iris.messages.type.Population;
import com.iris.model.Version;
import com.iris.prodcat.Brand;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogManager;

@Singleton
@HttpPost("/" + ProductCatalogService.NAMESPACE + "/GetBrands")
public class GetBrandsRESTHandler extends ProductCatalogRESTHandler {

	@Inject
	public GetBrandsRESTHandler(AlwaysAllow alwaysAllow, BridgeMetrics metrics, ProductCatalogManager manager,
			PopulationDAO populationDao, PlaceDAO placeDao, HubDAO hubDao, @Named("UseChunkedSender") RESTHandlerConfig restHandlerConfig) {
		super(alwaysAllow, new HttpSender(GetBrandsRESTHandler.class, metrics), manager, populationDao, placeDao, hubDao, restHandlerConfig);
	}

	@Override
	protected MessageBody doHandle(ClientMessage request) throws Exception {
	   String placeAddressStr = ProductCatalogService.GetProductsRequest.getPlace(request.getPayload());
      Population population = determinePopulationFromRequest(placeAddressStr);
      Version hubFwVersion = determineHubFirmwareVersionFromRequest(placeAddressStr);
      ProductCatalog catalog = getCatalog(population);

		List<Brand> brands = catalog.getBrands();
		Map<String, Integer> counts = catalog.getProductCountByBrand(hubFwVersion);

		return ProductCatalogService.GetBrandsResponse.builder()
				.withBrands(brands.stream().map((b) -> brandToMap(b)).collect(Collectors.toList())).withCounts(counts)
				.build();
	}

	private Map<String, Object> brandToMap(Brand brand) {
		Map<String, Object> asMap = new HashMap<>();
		asMap.put("name", brand.getName());
		if (!StringUtils.isEmpty("image")) {
			asMap.put("image", brand.getImage());
		}
		if (!StringUtils.isEmpty("description")) {
			asMap.put("description", brand.getDescription());
		}
		return asMap;
	}
}

