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
package com.iris.platform.rule;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.messages.type.Population;
import com.iris.platform.rule.catalog.RuleCatalog;
import com.iris.platform.rule.catalog.RuleCatalogManager;
import com.iris.platform.rule.catalog.RuleTemplate;
import com.iris.platform.rule.catalog.serializer.RuleCatalogDeserializer;
import com.iris.resource.Resource;
import com.iris.resource.Resources;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

/**
 * 
 */
@Modules( CapabilityRegistryModule.class )
public class TestRuleCatalogManagerWithActualFile extends IrisTestCase {
   @Inject RuleCatalogDeserializer deserializer;
   
   private RuleCatalogManager manager;
   
   @Before
   public void init() {
      Resource resource = Resources.getResource("classpath:/rule-catalog.xml");
      manager = new RuleCatalogManager(resource, deserializer);
      manager.init();
   }

   
   @Test
   public void testPopulationQa() throws Exception {
   	doTestFor(Population.NAME_QA);   	
   }
   
   @Test
   public void testPopulationBeta() throws Exception {
   	doTestFor(Population.NAME_BETA);   	
   }
   public void doTestFor(String population) throws Exception {
   	//For now, make sure rules defined in population general also appears in other populations
   	RuleCatalog generalCatalog = manager.getCatalog(Population.NAME_GENERAL);
   	assertNotNull(generalCatalog);
   	RuleCatalog catalog = manager.getCatalog(population);
   	assertNotNull(catalog);
   	AtomicInteger missingCount = new AtomicInteger(0);
   	
   	generalCatalog.getTemplates().forEach(t -> {
   		RuleTemplate curTemplate = catalog.getById(t.getId());
   		if(curTemplate == null) {
   			System.out.println(t.getId() + " is missing in Population "+population);
   			missingCount.incrementAndGet();
   		}
   		//assertNotNull("cur", curTemplate);
   	});
   	if(missingCount.get() > 0 ){
   		fail("There are "+missingCount.get() + " missing rules in population "+population);
   	}
   }

   
   
}

