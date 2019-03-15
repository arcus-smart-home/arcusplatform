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

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.iris.Utils;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.capability.attribute.transform.BeanListTransformer;
import com.iris.core.dao.PopulationDAO;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.ProductCatalogCapability;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;

public class GetAllProductsHandler extends AbstractProductCatalogHandler {
   private final BeanListTransformer<ProductCatalogEntry> listTransformer;

   @Inject
   public GetAllProductsHandler(BeanAttributesTransformer<ProductCatalogEntry> transformer,
                                PopulationDAO populationDao,
                                ProductCatalogManager manager) {
      super(populationDao, manager);
      listTransformer = new BeanListTransformer<ProductCatalogEntry>(transformer);
   }

   @Override
   public String getMessageType() {
      return ProductCatalogCapability.GetAllProductsRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(ProductCatalog context, PlatformMessage msg) {
      Utils.assertNotNull(context, "The product catalog is required");
      List<ProductCatalogEntry> entries = context.getAllProducts();
      List<Map<String, Object>> products = listTransformer.convertListToAttributes(entries);
      return ProductCatalogCapability.GetAllProductsResponse.builder()
               .withProducts(products)
               .build();
   }

}

