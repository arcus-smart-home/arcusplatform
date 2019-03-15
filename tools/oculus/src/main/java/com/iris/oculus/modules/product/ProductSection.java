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

import java.awt.Component;

import javax.inject.Inject;

import com.iris.client.capability.Product;
import com.iris.oculus.OculusSection;
import com.iris.oculus.modules.capability.ux.ModelStoreViewBuilder;
import com.iris.oculus.util.BaseComponentWrapper;

/**
 * 
 */
public class ProductSection extends BaseComponentWrapper<Component> implements OculusSection {
   private ProductController controller;
   
   @Inject
   public ProductSection(ProductController controller) {
      this.controller = controller;
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.OculusSection#getName()
    */
   @Override
   public String getName() {
      return "Products";
   }


   @Override
   protected Component createComponent() {
      return 
            ModelStoreViewBuilder
               .builder(controller.getProductStore())
               .withTypeName("Product")
               .withModelSelector(
                     Product.ATTR_NAME,
                     controller.getProductSelection(),
                     controller.actionReloadProducts()
               )
               .withListView(new ProductListView(controller).getComponent())
               .addShowListener((e) -> controller.reloadProducts())
               .build();
   }

}

