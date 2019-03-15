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
/**
 * 
 */
package com.iris.oculus.modules.product;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.swing.AbstractAction;
import javax.swing.Action;

import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Capability;
import com.iris.client.capability.Place;
import com.iris.client.capability.Product;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.ProductModel;
import com.iris.client.model.Store;
import com.iris.client.service.ProductCatalogService;
import com.iris.client.service.ProductCatalogService.GetProductsRequest;
import com.iris.client.service.ProductCatalogService.GetProductsResponse;
import com.iris.messages.address.Address;
import com.iris.oculus.Oculus;
import com.iris.oculus.util.DefaultSelectionModel;
import com.iris.oculus.util.SelectionModel;
import com.iris.oculus.view.SimpleViewModel;
import com.iris.oculus.view.ViewModel;

/**
 * 
 */
public class ProductController {

	private Action reloadProducts = new AbstractAction("Refresh") {
		@Override
		public void actionPerformed(ActionEvent e) {
			reloadProducts();
		}
	};

	private IrisClient client;
	private DefaultSelectionModel<ProductModel> productSelection = new DefaultSelectionModel<>();
	private Store<ProductModel> store;
	private SimpleViewModel<Map<String, Object>> brands = new SimpleViewModel<>();
	private SimpleViewModel<Map<String, Object>> categories = new SimpleViewModel<>();

	private GetProductsResponseSource productCatalogResponseSource;

	@Inject
	public ProductController(IrisClient client) {
		this.client = client;
		this.store = IrisClientFactory.getStore(ProductModel.class);
		this.productCatalogResponseSource = new GetProductsResponseSource();
	}

	public Store<ProductModel> getProductStore() {
		return store;
	}

	public SelectionModel<ProductModel> getProductSelection() {
		return productSelection;
	}
	
	public ClientFuture<List<String>> search(String term) {
		return
				IrisClientFactory
					.getService(ProductCatalogService.class)
					.findProducts(Address.platformService(client.getActivePlace(), Place.NAMESPACE).getRepresentation(), term)
					.transform((response) -> response.getProducts().stream().map((attributes) -> (String) attributes.get(Capability.ATTR_ADDRESS)).collect(Collectors.toList()))
					;
	}
	
	public ClientFuture<List<String>> listByBrand(String brand) {
		return
				IrisClientFactory
					.getService(ProductCatalogService.class)
					.getProductsByBrand(Address.platformService(client.getActivePlace(), Place.NAMESPACE).getRepresentation(), brand, null)
					.transform((response) -> response.getProducts().stream().map((attributes) -> (String) attributes.get(Capability.ATTR_ADDRESS)).collect(Collectors.toList()))
					;
	}
	
	public ClientFuture<List<String>> listByCategory(String category) {
		return
				IrisClientFactory
					.getService(ProductCatalogService.class)
					.getProductsByCategory(Address.platformService(client.getActivePlace(), Place.NAMESPACE).getRepresentation(), category)
					.transform((response) -> response.getProducts().stream().map((attributes) -> (String) attributes.get(Capability.ATTR_ADDRESS)).collect(Collectors.toList()))
					;
	}
	
	public ViewModel<Map<String, Object>> getBrands() {
		if(brands.size() == 0) {
			IrisClientFactory
				.getService(ProductCatalogService.class)
				.getBrands(Address.platformService(client.getActivePlace(), Place.NAMESPACE).getRepresentation())
				.onSuccess((response) -> brands.replaceAll(response.getBrands())); 
		}
		return brands;
	}

	public ViewModel<Map<String, Object>> getCategories() {
		if(categories.size() == 0) {
			IrisClientFactory
				.getService(ProductCatalogService.class)
				.getCategories(Address.platformService(client.getActivePlace(), Place.NAMESPACE).getRepresentation())
				.onSuccess((response) -> categories.replaceAll(response.getCategories())); 
		}
		return categories;
	}

	public ListenerRegistration addProductSelectedListener(Listener<ProductModel> l) {
		return productSelection.addNullableSelectionListener(l);
	}

	public Action actionReloadProducts() {
		return reloadProducts;
	}

	public void reloadProducts() {
		onStoreLoad();
	}

	private void onStoreLoad() {
		productCatalogResponseSource.refreshData().onSuccess((getProductsResponse) -> {
			List<Map<String, Object>> products = getProductsResponse.getProducts();
			if (products == null || products.isEmpty()) {
				store.clear();
				return;
			}
			IrisClientFactory.getModelCache().retainAll(Product.NAMESPACE, products);
		});
	}

	private class GetProductsResponseSource {
		ClientFuture<GetProductsResponse> getProductsResponse = null;

		public synchronized ClientFuture<GetProductsResponse> getData() {
			if (getProductsResponse == null) {
				return IrisClientFactory.getService(ProductCatalogService.class)
				   .getProducts(Address.platformService(client.getActivePlace(), Place.NAMESPACE).getRepresentation(), GetProductsRequest.INCLUDE_ALL, null).onFailure((e) -> {
							synchronized (GetProductsResponseSource.this) {
								getProductsResponse = null;
								Oculus.warn("Unable to load products", e);
							}
						});
			}
			return getProductsResponse;
		}

		public synchronized ClientFuture<GetProductsResponse> refreshData() {
			getProductsResponse = null;
			return getData();
		}

	}

}

