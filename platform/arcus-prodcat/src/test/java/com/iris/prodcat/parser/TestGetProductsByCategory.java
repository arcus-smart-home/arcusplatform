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

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.iris.messages.type.Population;
import com.iris.model.Version;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogEntry;

@RunWith(value = Parameterized.class)
public class TestGetProductsByCategory extends AbstractProductCatalogTestCase
{
   @Parameters(name="population[{0}],category[{1}],hubFirmware[{2}], success[{3}],expectedProductsToFilter[{4}]")
   public static List<Object []> files() {
      return Arrays.<Object[]>asList(
            new Object [] { Population.NAME_GENERAL, "Lights & Switches", (String)null, true,  (String)"0c9a66,bc45b5,"},  //0c9a66 belongs to alpha population
            new Object [] { "alpha", "Lights & Switches", (String)null, true,  (String)null},
            new Object [] { "alpha", "Lights & Switches", "2.0.0.030", true,  "359d72,0c9a66"},
            new Object [] { "alpha", "Lights & Switches", "2.0.0.031", true,  "0c9a66"},
            new Object [] { "alpha", "Lights & Switches", "2.0.0.048", true,  (String)null},
            new Object [] { "alpha", "Lights & Switches", "2.0.0.049", true,  (String)null},
            new Object [] { Population.NAME_GENERAL, "Lights & Switches", "2.0.0.030", true,  "359d72,0c9a66"},
            new Object [] { Population.NAME_GENERAL, "Lights & Switches", "2.0.0.031", true,  "0c9a66"},
            new Object [] { Population.NAME_GENERAL, "Lights & Switches", "2.0.0.048", true,  "0c9a66"},
            new Object [] { Population.NAME_GENERAL, "Lights & Switches", "2.0.0.049", true,  "0c9a66"},
            new Object [] { "alpha", "Home Safety", "2.0.0.030", true,  (String)null},
            new Object [] { Population.NAME_GENERAL, "Home Safety", "2.0.0.030", true,  (String)null},
            new Object [] { "beta", "Doors & Locks", "2.0.0.030", true,  null},  //23af19 belongs to beta population
            new Object [] { "alpha", "Doors & Locks", "2.0.0.030", true,  "23af19"},
            new Object [] { Population.NAME_GENERAL, "Doors & Locks", "2.0.0.030", true,  "23af19"},
            new Object [] { "alpha", "Water", "2.0.0.030", true,  (String)null},
            new Object [] { Population.NAME_GENERAL, "Water", "2.0.0.030", true,  (String)null}
      );
   }
   
   
  
   
   private final String population;
   private final String category;
   private final boolean isSuccess;
   private final List<String> expectedProductsToFilter;
   private final String fwVersion;
   
   public TestGetProductsByCategory(String population, String category, String fwVersion, boolean isSuccess, String expectedProductsToFilter) {
      this.population = population;
      this.category = category;
      this.isSuccess = isSuccess;
      this.expectedProductsToFilter = parseProductIdsToList(expectedProductsToFilter);
      this.fwVersion = fwVersion;
   }
   
   @Test
   public void testGetProductsByCategory() throws Exception {
      ProductCatalog prodcat = productCatalogManager.getCatalog(population);
      Version hubFwVersion = null;
      if(StringUtils.isNotBlank(fwVersion)) {
         hubFwVersion = Version.fromRepresentation(fwVersion);
      }
      List<ProductCatalogEntry> productList = prodcat.getProductsByCategory(category, hubFwVersion);
      assertAllProductsFound(filter(expectedProductIdsForCategoryMap.get(category), expectedProductsToFilter), productList);
   }
   
}

