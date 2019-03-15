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
package com.iris.platform.services.productcatalog;

import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformService;
import com.iris.platform.services.productcatalog.handlers.FindProductsHandler;
import com.iris.platform.services.productcatalog.handlers.GetAllProductsHandler;
import com.iris.platform.services.productcatalog.handlers.GetBrandsHandler;
import com.iris.platform.services.productcatalog.handlers.GetCategoriesHandler;
import com.iris.platform.services.productcatalog.handlers.GetProductCatalogHandler;
import com.iris.platform.services.productcatalog.handlers.GetProductHandler;
import com.iris.platform.services.productcatalog.handlers.GetProductsByBrandHandler;
import com.iris.platform.services.productcatalog.handlers.GetProductsByCategoryHandler;
import com.iris.platform.services.productcatalog.handlers.GetProductsHandler;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogModule;
import com.netflix.governator.annotations.Modules;

@Modules(include = ProductCatalogModule.class)
public class ProductCatalogServiceModule extends AbstractIrisModule {
   
   @Override
   protected void configure() {
      Multibinder<ContextualRequestMessageHandler<ProductCatalog>> handlerBinder = bindSetOf(new TypeLiteral<ContextualRequestMessageHandler<ProductCatalog>>() {});
      handlerBinder.addBinding().to(FindProductsHandler.class);
      handlerBinder.addBinding().to(GetBrandsHandler.class);
      handlerBinder.addBinding().to(GetCategoriesHandler.class);
      handlerBinder.addBinding().to(GetProductCatalogHandler.class);
      handlerBinder.addBinding().to(GetProductHandler.class);
      handlerBinder.addBinding().to(GetProductsByBrandHandler.class);
      handlerBinder.addBinding().to(GetProductsByCategoryHandler.class);
      handlerBinder.addBinding().to(GetProductsHandler.class);
      handlerBinder.addBinding().to(GetAllProductsHandler.class);
      
      bindSetOf(PlatformService.class).addBinding().to(ProductCatalogService.class);
   }
   
}

