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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.iris.prodcat.ProductCatalogConfig;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.test.IrisMockTestCase;


public abstract class AbstractProductCatalogTestCase extends IrisMockTestCase {
	

	@Inject
	protected ProductCatalogConfig prodCatConfig;
	
	protected String productCatalogUriStr="classpath:/test1.xml";

   protected ProductCatalogManager productCatalogManager;
   
   protected static final List<String> expectedProductIdsForGeneral = Arrays.asList("359d72","700faf","6c56c8","671eee", "979695", "bc45b5", "798086", "3981d9", "7dfa41", "162918", "4ff66a");
   protected static final List<String> expectedProductIdsForAlphaOnly = Arrays.asList("0c9a66");
   protected static final List<String> expectedProductIdsForBetaOnly = Arrays.asList("23af19");
   
   protected static final List<String> expectedProductIdsForAlpha = Lists.newArrayList(expectedProductIdsForAlphaOnly);
   static {
      expectedProductIdsForAlpha.addAll(expectedProductIdsForGeneral);
   }
   
   protected static final List<String> expectedProductIdsForBeta = Lists.newArrayList(expectedProductIdsForBetaOnly);
   static {
      expectedProductIdsForBeta.addAll(expectedProductIdsForGeneral);
   }
   
   protected static final List<String> expectedProductIdsForIris = Arrays.asList("162918", "4ff66a");
   protected static final List<String> expectedProductIdsForGE = Arrays.asList("6c56c8","979695","700faf", "671eee", "359d72",  "0c9a66");
   protected static final List<String> expectedProductIdsForFirstAlert = Arrays.asList("bc45b5","798086");
   protected static final List<String> expectedProductIdsForSchlage = Arrays.asList("23af19");
   protected static final List<String> expectedProductIdsForWhirlPool = Arrays.asList("3981d9");
   protected static final Map<String, List<String>> expectedProductIdsForBrandsMap = ImmutableMap.<String, List<String>>of(
		 "Iris", expectedProductIdsForIris,
         "GE", expectedProductIdsForGE,
         "First Alert", expectedProductIdsForFirstAlert,
         "Schlage", expectedProductIdsForSchlage,
         "Whirlpool", expectedProductIdsForWhirlPool);
   
   protected static final List<String> expectedProductIdsForLights = Arrays.asList("0c9a66", "359d72", "700faf", "6c56c8","671eee","979695", "162918");
   protected static final List<String> expectedProductIdsForSafety = Arrays.asList("bc45b5","798086");
   protected static final List<String> expectedProductIdsForLocks = Arrays.asList("23af19", "4ff66a");
   protected static final List<String> expectedProductIdsForWater = Arrays.asList("3981d9");
   protected static final Map<String, List<String>> expectedProductIdsForCategoryMap = ImmutableMap.<String, List<String>>of(
      "Lights & Switches", expectedProductIdsForLights,
      "Home Safety", expectedProductIdsForSafety,
      "Doors & Locks", expectedProductIdsForLocks,
      "Water", expectedProductIdsForWater);
   
   @Override
	protected Set<String> configs() {
		Set<String> configs = super.configs();
		configs.add("src/test/resources/test-prodcat.properties");
		return configs;
	}

   
	@Before
	public void init() throws Exception {
		prodCatConfig.setProductCatalogPath(productCatalogUriStr);
		productCatalogManager = new ProductCatalogManager(prodCatConfig);
	}	
	
	
	protected void assertAllProductsFound(List<String> expectedProductids, List<ProductCatalogEntry> actualProducts)
   {
	   if(actualProducts != null) {
	      assertEquals(expectedProductids.size(), actualProducts.size());
	      List<String> actualProductIds = actualProducts.stream().map(p -> p.getId()).collect(Collectors.toList());
	      actualProductIds.removeAll(expectedProductids);
	      assertTrue(actualProductIds.isEmpty());
	   }else{
	      assertTrue(expectedProductids==null || expectedProductids.isEmpty());
	   }
       
   }
	
	@Nullable
	protected List<String> parseProductIdsToList(String productIdStr) {
	   if(StringUtils.isNotBlank(productIdStr)) {
	      return Arrays.asList(productIdStr.split("\\s*,\\s*"));
	   }else{
	      return null;
	   }
	}
	
	protected List<String> filter(List<String> originalList, List<String> filterList) {
      if(filterList == null || filterList.isEmpty()) {
         return Collections.unmodifiableList(originalList);
      }else{
         List<String> newList = new ArrayList<>(originalList);
         newList.removeAll(filterList);
         return newList;
      }
   }
	
}

