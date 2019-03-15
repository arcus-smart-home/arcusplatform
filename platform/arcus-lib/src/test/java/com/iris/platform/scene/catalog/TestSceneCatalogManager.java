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
package com.iris.platform.scene.catalog;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.core.dao.PopulationDAO;
import com.iris.messages.model.Fixtures;
import com.iris.messages.type.Population;
import com.iris.platform.scene.SceneConfig;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({PopulationDAO.class})
@Modules({CapabilityRegistryModule.class})
public class TestSceneCatalogManager extends IrisMockTestCase
{
   @Inject
   private CapabilityRegistry registry;
   @Inject
   private PopulationDAO populationDao;
   
   private List<Population> expectedPopulationList; 
   private Map<String, List<String>> expectedSceneMap = ImmutableMap.<String, List<String>>of(
         "general", ImmutableList.<String>of("away", "night", "custom"),
         "beta", ImmutableList.<String>of("away", "vacation", "morning"),
         "qa", ImmutableList.<String>of("home", "custom")
      );
   
   
   @Override
   protected Set<String> configs() {
      Set<String> configs = super.configs();
      configs.add("src/test/resources/test.properties");
      return configs;
   }

   private SceneCatalogManager manager;
   private SceneConfig config;

   @Before
   public void setUp() throws Exception
   {
      super.setUp();
      config = new SceneConfig();
      config.setCatalogPath("classpath:/test-scene-catalog.xml");
      Population popGeneral = Fixtures.createPopulation();
      popGeneral.setName(Population.NAME_GENERAL);
      Population popBeta = Fixtures.createPopulation();
      popBeta.setName(Population.NAME_BETA);
      Population popAlpha = Fixtures.createPopulation();
      popAlpha.setName(Population.NAME_QA);
      expectedPopulationList = ImmutableList.<Population>of(popGeneral, popBeta, popAlpha);
      
   }

   @After
   public void tearDown() throws Exception
   {
      super.tearDown();
   }
   
   @Test
   public void testInit() {
      EasyMock.expect(populationDao.listPopulations()).andReturn(expectedPopulationList);
      replay();      
      
      manager = new SceneCatalogManager(config, registry, populationDao);
      manager.init();
      
      verify();
   }

   @Test
   public void testGetCatalog()
   {
      EasyMock.expect(populationDao.listPopulations()).andReturn(expectedPopulationList);
      replay();      
      
      manager = new SceneCatalogManager(config, registry, populationDao);
      manager.init();
      
      expectedSceneMap.forEach((curPopulation, expectedTemplateIdList) -> {
         SceneCatalog catalog = manager.getCatalog(curPopulation);
         assertNotNull(catalog);
         assertEquals(expectedTemplateIdList.size(), catalog.getTemplates().size());
         expectedTemplateIdList.forEach(id -> {
            assertNotNull(catalog.getById(id));
         });
      });
      
      
      verify();
   }

}

