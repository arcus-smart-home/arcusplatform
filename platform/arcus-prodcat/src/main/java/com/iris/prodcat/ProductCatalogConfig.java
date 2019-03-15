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
package com.iris.prodcat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

@Singleton
public class ProductCatalogConfig  {
   @Inject(optional = true)
   @Named(value = "product.catalog.path")
   private String productCatalogPath = "classpath:/product_catalog.xml";
   
   @Inject
   private RedirectBaseUrlHelper urlHelper;

   public String getProductCatalogPath() {
      return productCatalogPath;
   }

   public void setProductCatalogPath(String productCatalogPath) {
      this.productCatalogPath = productCatalogPath;
   }

   public Resource getProductCatalogResource() {
      return Resources.getResource(productCatalogPath);
   }

	public RedirectBaseUrlHelper getRedirectBaseUrlHelper() {
		return urlHelper;
	}

	public void setRedirectBaseUrlHelper(RedirectBaseUrlHelper urlHelper) {
		this.urlHelper = urlHelper;
	}
   
   
}

