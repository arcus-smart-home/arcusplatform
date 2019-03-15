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
package com.iris.client.impl;

import com.google.common.base.Function;
import com.iris.capability.util.Addresses;
import com.iris.client.ClientEvent;
import com.iris.client.IrisClient;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.service.ProductCatalogService;

@Deprecated
public class ProductCatalogServiceImpl implements ProductCatalogService {
	private static final String ADDRESS = Addresses.toServiceAddress(ProductCatalogService.NAMESPACE);
	private IrisClient client;

	public ProductCatalogServiceImpl(IrisClient client) {
		this.client = client;
	}

	@Override
	public String getName() {
		return ProductCatalogService.NAME;
	}

	@Override
	public String getAddress() {
		return ADDRESS;
	}

	@Override
	public ClientFuture<GetProductCatalogResponse> getProductCatalog(String place) {
		GetProductCatalogRequest request = new GetProductCatalogRequest();
		request.setAddress(getAddress());
		request.setRestfulRequest(true);
		request.setPlace(place);

		ClientFuture<ClientEvent> result = client.request(request);

		return Futures.transform(result, new Function<ClientEvent, GetProductCatalogResponse>() {
			@Override
			public GetProductCatalogResponse apply(ClientEvent input) {
				GetProductCatalogResponse response = new GetProductCatalogResponse(input);
				return response;
			}
		});
	}

	@Override
	public ClientFuture<GetCategoriesResponse> getCategories(String place) {
		GetCategoriesRequest request = new GetCategoriesRequest();
		request.setAddress(getAddress());
		request.setRestfulRequest(true);
		request.setPlace(place);

		ClientFuture<ClientEvent> result = client.request(request);

		return Futures.transform(result, new Function<ClientEvent, GetCategoriesResponse>() {
			@Override
			public GetCategoriesResponse apply(ClientEvent input) {
				GetCategoriesResponse response = new GetCategoriesResponse(input);
				return response;
			}
		});
	}

	@Override
	public ClientFuture<GetBrandsResponse> getBrands(String place) {
		GetBrandsRequest request = new GetBrandsRequest();
		request.setAddress(getAddress());
		request.setRestfulRequest(true);
		request.setPlace(place);

		ClientFuture<ClientEvent> result = client.request(request);

		return Futures.transform(result, new Function<ClientEvent, GetBrandsResponse>() {
			@Override
			public GetBrandsResponse apply(ClientEvent input) {
				GetBrandsResponse response = new GetBrandsResponse(input);
				return response;
			}
		});
	}

	@Override
	public ClientFuture<GetProductsByBrandResponse> getProductsByBrand(String place, String brand, Boolean hubRequired) {
		GetProductsByBrandRequest request = new GetProductsByBrandRequest();
		request.setAddress(getAddress());
		request.setRestfulRequest(true);
		request.setBrand(brand);
		request.setHubrequired(hubRequired);
		request.setPlace(place);

		ClientFuture<ClientEvent> result = client.request(request);

		return Futures.transform(result, new Function<ClientEvent, GetProductsByBrandResponse>() {
			@Override
			public GetProductsByBrandResponse apply(ClientEvent input) {
				GetProductsByBrandResponse response = new GetProductsByBrandResponse(input);
				return response;
			}
		});
	}
	
	@Override
	public ClientFuture<GetProductsByCategoryResponse> getProductsByCategory(String place, String category) {
		GetProductsByCategoryRequest request = new GetProductsByCategoryRequest();
		request.setAddress(getAddress());
		request.setRestfulRequest(true);
		request.setCategory(category);
		request.setPlace(place);

		ClientFuture<ClientEvent> result = client.request(request);

		return Futures.transform(result, new Function<ClientEvent, GetProductsByCategoryResponse>() {
			@Override
			public GetProductsByCategoryResponse apply(ClientEvent input) {
				GetProductsByCategoryResponse response = new GetProductsByCategoryResponse(input);
				return response;
			}
		});
	}

	@Override
	public ClientFuture<GetProductsResponse> getProducts(String place, String include, Boolean hubRequired) {
		GetProductsRequest request = new GetProductsRequest();
		request.setAddress(getAddress());
		request.setRestfulRequest(true);
		request.setInclude(include);
		request.setHubRequired(hubRequired);
		request.setPlace(place);

		ClientFuture<ClientEvent> result = client.request(request);

		return Futures.transform(result, new Function<ClientEvent, GetProductsResponse>() {
			@Override
			public GetProductsResponse apply(ClientEvent input) {
				GetProductsResponse response = new GetProductsResponse(input);
				return response;
			}
		});
	}

	@Override
	public ClientFuture<FindProductsResponse> findProducts(String place, String search) {
		FindProductsRequest request = new FindProductsRequest();
		request.setAddress(getAddress());
		request.setRestfulRequest(true);
		request.setSearch(search);
		request.setPlace(place);

		ClientFuture<ClientEvent> result = client.request(request);

		return Futures.transform(result, new Function<ClientEvent, FindProductsResponse>() {
			@Override
			public FindProductsResponse apply(ClientEvent input) {
				FindProductsResponse response = new FindProductsResponse(input);
				return response;
			}
		});
	}

	@Override
	public ClientFuture<GetProductResponse> getProduct(String place, String id) {
		GetProductRequest request = new GetProductRequest();
		request.setAddress(getAddress());
		request.setRestfulRequest(true);
		request.setId(id);
		request.setPlace(place);

		ClientFuture<ClientEvent> result = client.request(request);

		return Futures.transform(result, new Function<ClientEvent, GetProductResponse>() {
			@Override
			public GetProductResponse apply(ClientEvent input) {
				GetProductResponse response = new GetProductResponse(input);
				return response;
			}
		});
	}

}

