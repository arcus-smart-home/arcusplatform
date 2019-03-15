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
package com.iris.firmware;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.messages.model.Hub;
import com.iris.messages.model.Place;
import com.iris.messages.type.Population;
import com.iris.model.Version;

public class TestHubMinimumFirmwareVersionResolver {

   PlaceDAO mockPlaceDao;
   PopulationDAO mockPopulationDao;
   MinimumFirmwareVersionResolver<Hub> resolver;
   private Population defaultPopulation;
   private Population alphaPopulation;

   @Before
   public void setUp() {
      mockPlaceDao = createMock(PlaceDAO.class);
      mockPopulationDao = createMock(PopulationDAO.class);

      defaultPopulation = new Population();
      defaultPopulation.setName(Population.NAME_GENERAL);
      defaultPopulation.setMinHubV2Version("2.0.0.017");
      defaultPopulation.setMinHubV3Version("3.0.0.006");

      alphaPopulation = new Population();
      alphaPopulation.setName("alpha_centari");
      alphaPopulation.setMinHubV2Version("2.0.1.0");
      alphaPopulation.setMinHubV3Version("3.0.0.0");

      resolver = new HubMinimumFirmwareVersionResolver(mockPlaceDao, mockPopulationDao);
   }

   @After
   public void tearDown() {
      verify(mockPlaceDao);
      verify(mockPopulationDao);
   }

   @Test
   public void testNoPlaceReturnsDefault() {
      replay(mockPlaceDao);
      expect(mockPopulationDao.getDefaultPopulation()).andReturn(defaultPopulation);
      replay(mockPopulationDao);
      Hub hub = new Hub();
      hub.setModel("IH200");
      assertEquals(Version.fromRepresentation(defaultPopulation.getMinHubV2Version()), resolver.resolveMinimumVersion(hub));
   }

   @Test
   public void testPlaceWithNoPopulationReturnsDefault() {
      replay(mockPlaceDao);
      expect(mockPopulationDao.getDefaultPopulation()).andReturn(defaultPopulation);
      replay(mockPopulationDao);
      Hub hub = new Hub();
      hub.setModel("IH200");
      assertEquals(Version.fromRepresentation(defaultPopulation.getMinHubV2Version()), resolver.resolveMinimumVersion(hub));
   }

   @Test
   public void testPlaceWithPopulationReturnsVersion2() {
      UUID placeId = UUID.randomUUID();
      Hub hub = new Hub();
      hub.setPlace(placeId);
      hub.setModel("IH200");
      Place place = new Place();
      place.setPopulation(alphaPopulation.getName());
      expect(mockPlaceDao.getPopulationById(placeId)).andReturn(alphaPopulation.getName());
      replay(mockPlaceDao);
      expect(mockPopulationDao.findByName(alphaPopulation.getName())).andReturn(alphaPopulation);
      replay(mockPopulationDao);
      assertEquals(Version.fromRepresentation(alphaPopulation.getMinHubV2Version()), resolver.resolveMinimumVersion(hub));
   }
   
   @Test
   public void testPlaceWithPopulationReturnsVersion3() {
      UUID placeId = UUID.randomUUID();
      Hub hub = new Hub();
      hub.setPlace(placeId);
      hub.setModel("IH300");
      Place place = new Place();
      place.setPopulation(alphaPopulation.getName());
      expect(mockPlaceDao.getPopulationById(placeId)).andReturn(place.getPopulation());
      replay(mockPlaceDao);
      expect(mockPopulationDao.findByName(alphaPopulation.getName())).andReturn(alphaPopulation);
      replay(mockPopulationDao);
      assertEquals(Version.fromRepresentation(alphaPopulation.getMinHubV3Version()), resolver.resolveMinimumVersion(hub));
   }

   @Test
   public void testBadHubModel() {
   	replay(mockPlaceDao);
      expect(mockPopulationDao.getDefaultPopulation()).andReturn(defaultPopulation);
      replay(mockPopulationDao);
      Hub hub = new Hub();
      assertNull(resolver.resolveMinimumVersion(hub));
   }
   
}

