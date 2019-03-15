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
package com.iris.population;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.messages.model.Hub;
import com.iris.messages.model.Place;
import com.iris.messages.type.Population;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({PlaceDAO.class, PopulationDAO.class})
public class TestHubPopulationResolver extends IrisMockTestCase {
   private static final String TEST_POPULATION = "Test Population";
   private static final String DEFAULT_POPULATION = Population.NAME_GENERAL;
   
   @Inject
   private PlaceDAO placeDaoMock;
   @Inject
   private PopulationDAO populationDaoMock;
   
   private PlacePopulationCacheManager populationCacheMgr;
   private HubPopulationResolver resolver;
   private Hub hub;
   private Place place;
   private Population population;
   private Population defaultPopulation;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      populationCacheMgr = new PlacePopulationCacheManager(placeDaoMock);
      resolver = new DaoHubPopulationResolver(populationCacheMgr);
      hub = new Hub();
      hub.setId("ABC-1234");
      hub.setPlace(UUID.randomUUID());
      place = new Place();
      population = new Population();
      population.setName(TEST_POPULATION);
      defaultPopulation = new Population();
      defaultPopulation.setName(Population.NAME_GENERAL);
   }
   
   @Test
   public void testResolvePopulationName() {
      place.setPopulation(population.getName());
      EasyMock.expect(placeDaoMock.getPopulationById(hub.getPlace())).andReturn(population.getName());
      EasyMock.expect(populationDaoMock.findByName(population.getName())).andReturn(population);
      replay();
      
      Assert.assertEquals(TEST_POPULATION, resolver.getPopulationNameForHub(hub));
   }
   
   @Test
   public void testResolvePopulationId() {
      place.setPopulation(population.getName());
      EasyMock.expect(placeDaoMock.getPopulationById(hub.getPlace())).andReturn(population.getName());
      replay();
      
      Assert.assertEquals(population.getName(), resolver.getPopulationNameForHub(hub));
   }
   
   @Test
   public void testPlaceHasNoPopulation() {
      EasyMock.expect(placeDaoMock.getPopulationById(hub.getPlace())).andReturn(null);
      EasyMock.expect(populationDaoMock.getDefaultPopulation()).andReturn(defaultPopulation);
      replay();
      
      Assert.assertEquals(DEFAULT_POPULATION, resolver.getPopulationNameForHub(hub));
   } 
}

