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
package com.iris.prodcat.parser;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.iris.messages.type.Population;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogEntry;

@RunWith(value = Parameterized.class)
public class TestGetProductsByHubRequired extends AbstractProductCatalogTestCase
{
   @Parameters(name="population[{0}], hubRequired[{1}], brand[{2}], expectedProducts[{3}], expectedFilteredProducts[{4}]")
   public static List<Object []> files() {
      return Arrays.<Object[]>asList(
            new Object [] { Population.NAME_GENERAL, true, "GE", "6c56c8, 979695, 700faf, 671eee, 359d72, bc45b5, 798086, 4ff66a", "4ff66a, bc45b5, 798086, 0c9a66"},
            new Object [] { Population.NAME_BETA, true, "Schlage", "23af19, 6c56c8, 979695, 700faf, 671eee, 359d72, bc45b5, 798086, 4ff66a", "6c56c8, 979695, 700faf, 671eee, 359d72, bc45b5, 798086, 4ff66a"},
            new Object [] { Population.NAME_GENERAL, false, "Whirlpool", "3981d9, 7dfa41, 162918", "7dfa41, 162918"},
            new Object [] { Population.NAME_BETA, false, "Iris", "3981d9, 7dfa41, 162918", "3981d9, 7dfa41, 4ff66a"}
      );
   }
   
   private final String population;
   private boolean hubRequired;
   private String brand;
   private final List<String> expectedProducts;
   private final List<String> expectedFilteredProducts;
   
   public TestGetProductsByHubRequired(String population, boolean hubRequired, String brand, String expectedProducts, String expectedFilteredProducts) {
      this.population = population;
      this.hubRequired = hubRequired;
      this.brand = brand;
      this.expectedProducts = parseProductIdsToList(expectedProducts);
      this.expectedFilteredProducts = parseProductIdsToList(expectedFilteredProducts);
   }
   
   @Test
   public void testGetProductsByHubRequired() throws Exception {
      ProductCatalog prodcat = productCatalogManager.getCatalog(population);
      List<ProductCatalogEntry> productList = prodcat.getProducts(null, hubRequired);
      assertAllProductsFound(expectedProducts, productList);
   }
   
   @Test
   public void testGetProductsByBrandAndHubRequired() throws Exception {
      ProductCatalog prodcat = productCatalogManager.getCatalog(population);
	  List<ProductCatalogEntry> productList = prodcat.getProductsByBrand(brand, null, hubRequired);
	  assertAllProductsFound(filter(expectedProductIdsForBrandsMap.get(brand), expectedFilteredProducts), productList);
   }
   
}

