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
package com.iris.prodcat.search;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.resource.classpath.ClassPathResourceFactory;

public class TestProductIndex {

	private ProductIndex index;
	
	@Before
	public void setUp() throws Exception {
	   ClassPathResourceFactory factory = new ClassPathResourceFactory();
	   ProductCatalogManager manager = new ProductCatalogManager(factory.create(new URI("classpath:/test1.xml")));
		index = new ProductIndex(manager.getCatalog("beta"));
	}

	@Test
	public void testSimpleSearch() throws Exception {
		List<ProductCatalogEntry> res = index.search("GE"); 
		assertEquals(5, res.size());
		
		res = index.search("z-wave");
		assertEquals(8, res.size());	
	}
	
	@Test
	public void test() throws Exception {
		List<ProductCatalogEntry> res = index.search("lock"); 
		for (ProductCatalogEntry p : res) {
			System.out.println("[" + p.getId() + "] " + p.getName());
		}
	}

}

