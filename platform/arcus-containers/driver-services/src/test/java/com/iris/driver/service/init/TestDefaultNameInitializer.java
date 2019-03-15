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
package com.iris.driver.service.init;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.driver.DeviceDriverContext;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Place;
import com.iris.messages.type.Population;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({ DeviceDriverContext.class, DeviceDAO.class, PlaceDAO.class, PopulationDAO.class, ProductCatalogManager.class })
public class TestDefaultNameInitializer extends IrisMockTestCase {
   private static final Logger logger = LoggerFactory.getLogger(TestDefaultNameInitializer.class);
   
   @Inject DeviceDAO mockDeviceDao;
   @Inject DeviceDriverContext context;
   @Inject PlaceDAO mockPlaceDao;
   @Inject PopulationDAO mockPopulationDao;
   @Inject ProductCatalogManager mockCatalog;
   
   @Inject DefaultNameInitializer initializer;

   UUID placeId = UUID.randomUUID();
   String productId = "e0e0e0";
   String population = Population.NAME_GENERAL;
   
   @Before
   public void setUp() throws Exception {
      super.setUp();
      EasyMock
         .expect(context.getPlaceId())
         .andReturn(placeId)
         .anyTimes();
      EasyMock
         .expect(context.getAttributeValue(DefaultNameInitializer.ATTR_PRODUCT_ID))
         .andReturn(productId)
         .anyTimes();
      EasyMock
         .expect(context.getLogger())
         .andReturn(logger)
         .anyTimes();
      
      Population genPop = new Population();
      genPop.setName(Population.NAME_GENERAL);
      EasyMock
         .expect(mockPopulationDao.findByName(Population.NAME_GENERAL))
         .andReturn(genPop)
         .anyTimes();
   }
   
   @Test
   public void testDefaultName() {
      expectFindPlaceAndReturn();
      expectGetProductAndReturnEmpty();
      expectListDevicesAndReturnNames();
      expectSetName("New Device 1");
      replay();
      
      initializer.intialize(context);
      
      verify();
   }

   @Test
   public void testDefaultNameIncrementsNumber() {
      expectFindPlaceAndReturn();
      expectGetProductAndReturnEmpty();
      expectListDevicesAndReturnNames("Some Random Name", "New Device", "New Device 2", "New Device 100");
      expectSetName("New Device 101");
      replay();
      
      initializer.intialize(context);
      
      verify();
   }

   @Test
   public void testProductName() {
      expectFindPlaceAndReturn();
      expectGetProductAndReturn("Vendor", "Magiks");
      expectListDevicesAndReturnNames("Some Random Name", "New Device", "New Device 2", "New Device 100");
      expectSetName("Vendor Magiks 1");
      replay();
      
      initializer.intialize(context);
      
      verify();
   }

   private void expectFindPlaceAndReturn() {
      Place place = new Place();
      place.setId(placeId);
      place.setPopulation(population);
      
      EasyMock
         .expect(mockPlaceDao.getPopulationById(placeId))
         .andReturn(population)
         .once();
   }

   private void expectGetProductAndReturnEmpty() {
      EasyMock
         .expect(mockCatalog.getCatalog(Population.NAME_GENERAL))
         .andReturn(new ProductCatalog());
   }
   
   private void expectGetProductAndReturn(String vendor, String name) {
      ProductCatalogEntry entry = new ProductCatalogEntry();
      entry.setId(productId);
      entry.setVendor(vendor);
      entry.setName(name);
      entry.setCanBrowse(false);
      entry.setCanSearch(false);
      ProductCatalog catalog = new ProductCatalog();
      List<ProductCatalogEntry> products = new ArrayList<>();
      products.add(entry);
      catalog.setData(ImmutableList.of(), ImmutableList.of(), products);
      EasyMock
         .expect(mockCatalog.getCatalog(Population.NAME_GENERAL))
         .andReturn(catalog);
   }
   
   private void expectSetName(String name) {
      EasyMock
         .expect(context.setAttributeValue(DefaultNameInitializer.ATTR_NAME, name))
         .andReturn(null)
         .once();
   }
   
   private void expectListDevicesAndReturnNames(String... names) {
      List<Device> devices = new ArrayList<Device>();
      for(String name: names) {
         Device device = Fixtures.createDevice();
         device.setName(name);
         devices.add(device);
      }
      
      EasyMock
         .expect(mockDeviceDao.listDevicesByPlaceId(placeId))
         .andReturn(devices)
         .once();
   }
   
}

