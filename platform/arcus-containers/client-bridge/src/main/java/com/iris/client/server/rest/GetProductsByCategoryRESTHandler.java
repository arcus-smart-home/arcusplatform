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

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.capability.attribute.transform.BeanListTransformer;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.ProductCatalogService;
import com.iris.messages.service.ProductCatalogService.GetProductsByCategoryRequest;
import com.iris.messages.type.Population;
import com.iris.model.Version;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;

@Singleton
@HttpPost("/" + ProductCatalogService.NAMESPACE + "/GetProductsByCategory")
public class GetProductsByCategoryRESTHandler extends ProductCatalogRESTHandler {

	private final BeanListTransformer<ProductCatalogEntry> listTransformer;

	@Inject
	public GetProductsByCategoryRESTHandler(AlwaysAllow alwaysAllow, BridgeMetrics metrics,
			ProductCatalogManager manager, PopulationDAO populationDao, PlaceDAO placeDao, HubDAO hubDao,
			BeanAttributesTransformer<ProductCatalogEntry> transformer, @Named("UseChunkedSender") RESTHandlerConfig restHandlerConfig) {
		super(alwaysAllow, new HttpSender(GetProductsByCategoryRESTHandler.class, metrics), manager, populationDao, placeDao, hubDao, restHandlerConfig);
		this.listTransformer = new BeanListTransformer<ProductCatalogEntry>(transformer);
	}

	@Override
	protected MessageBody doHandle(ClientMessage request) throws Exception {
		
		MessageBody payload = request.getPayload();
		String placeAddressStr = ProductCatalogService.GetProductsRequest.getPlace(payload);
      Population population = determinePopulationFromRequest(placeAddressStr);
      ProductCatalog catalog = getCatalog(population);
		String categoryName = ProductCatalogService.GetProductsByCategoryRequest.getCategory(payload);

		Errors.assertRequiredParam(categoryName, GetProductsByCategoryRequest.ATTR_CATEGORY);
		Version hubFwVersion = determineHubFirmwareVersionFromRequest(placeAddressStr); 
		List<ProductCatalogEntry> entries = catalog.getProductsByCategory(categoryName, hubFwVersion);
		List<Map<String, Object>> products = listTransformer.convertListToAttributes(entries);
		return ProductCatalogService.GetProductsByCategoryResponse.builder().withProducts(products).build();
	}

}

