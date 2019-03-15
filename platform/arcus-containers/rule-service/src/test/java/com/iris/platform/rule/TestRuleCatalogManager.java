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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
public class TestRuleCatalogManager extends IrisTestCase {
   @Inject RuleCatalogDeserializer deserializer;
   
   private RuleCatalogManager manager;
      
   private Map<String, List<String>> expectedRuleMap = ImmutableMap.<String, List<String>>of(
      Population.NAME_GENERAL, ImmutableList.<String>of("bd1116", "ed7648", "80d915"),
      Population.NAME_BETA, ImmutableList.<String>of("16536e", "3502e4"),
      Population.NAME_QA, ImmutableList.<String>of("f37345", "3502e4", "ed7648")
      );
   
   @Before
   public void init() {
      Resource resource = Resources.getResource("classpath:/test-rule-catalog2.xml");
      manager = new RuleCatalogManager(resource, deserializer);
      manager.init();
   }

   
   @Test
   public void testRuleCategory() throws Exception {
      for(Entry<String, List<String>> curExpected : expectedRuleMap.entrySet()) {
         RuleCatalog catalog = manager.getCatalog(curExpected.getKey());         	   
	      assertExpectRuleMathces(curExpected.getKey(), curExpected.getValue(), catalog);
	   }	   
   }

   private void assertExpectRuleMathces(String population, List<String> expectedRuleList, RuleCatalog actualCatalog)
   {
      assertNotNull("RuleCatalog for population ["+population + "] should not be null", actualCatalog);
      assertEquals("RuleCatalog for population ["+population + "] should be size "+expectedRuleList.size(), expectedRuleList.size(), actualCatalog.getTemplates().size());
      for(String curRuleId : expectedRuleList) {
         assertNotNull(actualCatalog.getById(curRuleId));
      }
   }
   
}

