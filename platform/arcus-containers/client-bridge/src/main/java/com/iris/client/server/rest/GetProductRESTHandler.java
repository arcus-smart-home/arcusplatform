/*
 * Copyright 2020 Arcus Project
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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.ProductCatalogService;
import com.iris.messages.service.ProductCatalogService.GetProductRequest;
import com.iris.messages.type.Population;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;

@Singleton
@HttpPost("/" + ProductCatalogService.NAMESPACE + "/GetProduct")
public class GetProductRESTHandler extends ProductCatalogRESTHandler {

	private final BeanAttributesTransformer<ProductCatalogEntry> transformer;
	private static final int MAX_PRODUCT_ID_SIZE = 6;

	@Inject
	public GetProductRESTHandler(AlwaysAllow alwaysAllow, BridgeMetrics metrics, ProductCatalogManager manager,
			PopulationDAO populationDao, PlaceDAO placeDao, HubDAO hubDao, BeanAttributesTransformer<ProductCatalogEntry> transformer, @Named("UseChunkedSender") RESTHandlerConfig restHandlerConfig) {
		super(alwaysAllow, new HttpSender(GetProductRESTHandler.class, metrics), manager, populationDao, placeDao, hubDao, restHandlerConfig);
		this.transformer = transformer;
	}

	/**
	 * Builds a product catalog request for a given user-supplied product/place request.
	 *
	 * Security Implications: If an attacker can guess a product / place pairing, they may be able to gain knowledge of a non-public product.
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@Override
	protected MessageBody doHandle(ClientMessage request) throws ErrorEventException {
		MessageBody payload = request.getPayload(); // User supplied
		String placeAddressStr = ProductCatalogService.GetProductsRequest.getPlace(payload); // TODO: ensure this place is one the user has access to?
      Population population = determinePopulationFromRequest(placeAddressStr);
      ProductCatalog catalog = getCatalog(population);

		String id = ProductCatalogService.GetProductRequest.getId(payload);
		
		Errors.assertRequiredParam(id, GetProductRequest.ATTR_ID);
		Errors.assertValidRequest(id.length() != 0 && id.length() <= MAX_PRODUCT_ID_SIZE, "supplied parameter is invalid");

		ProductCatalogEntry product = catalog.getProductById(id);

		if (product == null) {
			return MessageBody.emptyMessage();
		}
		
		return ProductCatalogService.GetProductResponse.builder().withProduct(transformer.transform(product)).build();
	}
}

