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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.platform.ContextualEventMessageHandler;
import com.iris.core.platform.ContextualPlatformMessageDispatcher;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.platform.PlatformService;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.ProductCatalogCapability;
import com.iris.messages.type.Population;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogManager;

/**
 * @deprecated  As of release 1.11, replaced by {@link com.iris.client.service.ProductCatalogService}
 * The new service is a restful API that completely replaces the functionality.
 * 
 * Specific RESTHandlers are available:
 * 
 *  {@see com.iris.client.server.rest.GetBrandsRESTHandler}
 *  {@see com.iris.client.server.rest.GetCategoriesRESTHandler}
 *  {@see com.iris.client.server.rest.GetProductCatalogRESTHandler}
 *  {@see com.iris.client.server.rest.GetProductRESTHandler}
 *  {@see com.iris.client.server.rest.GetProductsByBrandRESTHandler}
 *  {@see com.iris.client.server.rest.GetProductsByCategoryRESTHandler}
 *  {@see com.iris.client.server.rest.GetProductsRESTHandler}
 *  {@see com.iris.client.server.rest.GetBrandsRESTHandler}
 */
@Deprecated
@Singleton
public class ProductCatalogService extends ContextualPlatformMessageDispatcher<ProductCatalog> implements PlatformService {
   public static final String PROP_THREADPOOL = "platform.service.prodcat.threadpool";
   
   private static final Address address = Address.platformService(ProductCatalogCapability.NAMESPACE);

   private final ProductCatalogManager manager;
   private final PopulationDAO populationDao;

   @Inject
   public ProductCatalogService(
         PlatformMessageBus platformBus,
         @Named(PROP_THREADPOOL) Executor executor,
         Set<ContextualRequestMessageHandler<ProductCatalog>> handlers,
         PopulationDAO populationDao,
         ProductCatalogManager manager
   ) {
      super(platformBus, executor, handlers, Collections.<ContextualEventMessageHandler<ProductCatalog>>emptySet());
      this.populationDao = populationDao;
      this.manager = manager;
   }

   @Override
   public Address getAddress() {
      return address;
   }

   @Override
   public void handleMessage(PlatformMessage message) {
      super.handleMessage(message);
   }

   @Override
   protected ProductCatalog loadContext(Object contextId, Integer qualifier) {
      if (!(contextId instanceof String)) {
         throw new IllegalArgumentException("The context ID must be a String");
      }
      Population population = populationDao.findByName((String)contextId);
      if (population == null) {
         population = populationDao.getDefaultPopulation();
      }
      return manager.getCatalog(population.getName());
   }
}

