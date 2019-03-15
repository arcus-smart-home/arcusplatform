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
import com.iris.prodcat.Category;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogManager;

public class GetCategoriesHandler extends AbstractProductCatalogHandler {

   @Inject
   public GetCategoriesHandler(PopulationDAO populationDao, ProductCatalogManager manager) {
      super(populationDao, manager);
   }

	@Override
	public String getMessageType() {
		return ProductCatalogCapability.GetCategoriesRequest.NAME;
	}

	@Override
	public MessageBody handleRequest(ProductCatalog context, PlatformMessage msg) {
		Utils.assertNotNull(context, "The product catalog is required");
		List<Category> categories = context.getCategories();
		Map<String,Integer> counts = context.getProductCountByCategory();

		return ProductCatalogCapability.GetCategoriesResponse.builder()
            .withCategories(categories.stream().map((c) -> categoryToMap(c)).collect(Collectors.toList()))
            .withCounts(counts)
            .build();
	}

	private Map<String,Object> categoryToMap(Category category) {
	   Map<String,Object> asMap = new HashMap<>();
	   asMap.put("name", category.getName());
	   if(!StringUtils.isBlank(category.getImage())) {
	      asMap.put("image", category.getImage());
	   }
	   return asMap;
   }

}

