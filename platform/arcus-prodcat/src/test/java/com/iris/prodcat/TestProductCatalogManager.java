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

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.inject.Inject;
import com.iris.resource.Resource;
import com.iris.resource.classpath.ClassPathResourceFactory;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Modules;

@RunWith(value = Parameterized.class)
@Modules({})
public class TestProductCatalogManager extends IrisMockTestCase {
	
	@Parameters(name="basePopulation[{0}],population[{1}]")
   public static List<Object []> files() {
      return Arrays.<Object[]>asList(
            new Object [] { "general","beta"},
            new Object [] { "general","qa"}            
      );
   }
	
	@Inject
	private ProductCatalogManager prodCatalogMgr;
	
	private final String population;	
	private final String basePopulation;
	
	public TestProductCatalogManager(String basePopulation, String population) {
		this.basePopulation = basePopulation;
		this.population = population;
	}
	
	@Test
	public void testAllProductsExist() throws Exception {
		ProductCatalog baseProductCat = prodCatalogMgr.getCatalog(basePopulation);
		assertNotNull(baseProductCat);
		
		ProductCatalog productCat = prodCatalogMgr.getCatalog(population);
		assertNotNull(productCat);
		AtomicInteger missingCount = new AtomicInteger(0);
		//Verify every product in basePopulation also exists in population
		baseProductCat.getAllProducts().forEach(curProduct -> {
			ProductCatalogEntry prod = productCat.getProductById(curProduct.getId());	
			if(prod == null) {
				missingCount.incrementAndGet();
				System.out.println("Product " + curProduct.getId() + " is missing in population " + population);				
			}else{
				assertEquals(curProduct, prod);
			}
		});
		if(missingCount.get() > 0) {
			fail("There are "+missingCount.get() + " missing product in population "+population);
		}
	}
	
   @Test
   public void testParseProductCatalog() throws Exception {
      URI uri = new URI("classpath:/test1.xml");
      assertNotNull(uri);
      ClassPathResourceFactory factory = new ClassPathResourceFactory(TestProductCatalogManager.class);
      Resource resource = factory.create(uri);
      ProductCatalogManager prodCatMan = new ProductCatalogManager(resource);
      Resource found = prodCatMan.getNewestResource(resource);
      assertEquals("classpath:/test1.xml", found.getRepresentation());
   }
   
   @Test
   public void testFindVersion() throws Exception {
      URI uri = new URI("classpath:/folder/");
      assertNotNull(uri);
      ClassPathResourceFactory factory = new ClassPathResourceFactory(TestProductCatalogManager.class);
      Resource resource = factory.create(uri);
      ProductCatalogManager prodCatMan = new ProductCatalogManager(resource);
      Resource found = prodCatMan.getNewestResource(resource);
      assertEquals("classpath:/folder/test100.xml", found.getRepresentation());
   }
   
   @Test
   public void testUseVersion() throws Exception {
      URI uri = new URI("classpath:/folder/");
      assertNotNull(uri);
      ClassPathResourceFactory factory = new ClassPathResourceFactory(TestProductCatalogManager.class);
      Resource resource = factory.create(uri);
      ProductCatalogManager prodCatMan = new ProductCatalogManager(resource);
      prodCatMan.loadCatalogs(9);
      Resource found = prodCatMan.getVersionedResource(resource, 9);
      assertEquals("classpath:/folder/test9.xml", found.getRepresentation());
   }
	
}

