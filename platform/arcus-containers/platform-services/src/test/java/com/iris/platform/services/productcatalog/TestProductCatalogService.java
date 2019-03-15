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
package com.iris.platform.services.productcatalog;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.ProductCapability;
import com.iris.messages.capability.ProductCatalogCapability;
import com.iris.messages.capability.ProductCatalogCapability.FindProductsResponse;
import com.iris.messages.capability.ProductCatalogCapability.GetBrandsResponse;
import com.iris.messages.capability.ProductCatalogCapability.GetCategoriesResponse;
import com.iris.messages.capability.ProductCatalogCapability.GetProductCatalogResponse;
import com.iris.messages.capability.ProductCatalogCapability.GetProductResponse;
import com.iris.messages.capability.ProductCatalogCapability.GetProductsByBrandResponse;
import com.iris.messages.capability.ProductCatalogCapability.GetProductsByCategoryResponse;
import com.iris.messages.capability.ProductCatalogCapability.GetProductsResponse;
import com.iris.messages.model.Fixtures;
import com.iris.messages.services.PlatformConstants;
import com.iris.messages.type.Population;
import com.iris.prodcat.ProductCatalogReloadListener;
import com.iris.resource.config.ResourceModule;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;
import com.iris.test.util.TestUtils;

@Modules({ InMemoryMessageModule.class, AttributeMapTransformModule.class, ResourceModule.class, ProductCatalogServiceModule.class })
public class TestProductCatalogService extends IrisTestCase {
   @Inject
   private ProductCatalogService service;

   @Inject
   private InMemoryPlatformMessageBus bus;

   private PopulationDAO populationDao;
   private final Population generalPopulation = makePopulation(Population.NAME_GENERAL);
   private final Population betaPopulation = makePopulation("beta");
   private final Population alphaPopulation = makePopulation("alpha");


   /* (non-Javadoc)
    * @see com.iris.test.IrisTestCase#configs()
    */
   @Override
   protected Set<String> configs() {
      Set<String> configs = super.configs();
      configs.add("src/test/resources/test1.properties");
      return configs;
   }
   
   @Provides
   @Singleton
   @Named(ProductCatalogReloadListener.GENERIC_MESSAGE_BUS_ACTOR_ADDRESS)
   public Address provideMessageBusActorAddress() {
      return Address.fromString("SERV:prodcat:");
   }
   
   @Provides @Named(ProductCatalogService.PROP_THREADPOOL)
   public Executor executor() {
      return MoreExecutors.directExecutor();
   }

   @Provides
   public PopulationDAO populationDao() {
      populationDao = EasyMock.createMock(PopulationDAO.class);
      EasyMock.expect(populationDao.findByName(generalPopulation.getName())).andReturn(generalPopulation).anyTimes();
      EasyMock.expect(populationDao.findByName(betaPopulation.getName())).andReturn(betaPopulation).anyTimes();
      EasyMock.expect(populationDao.findByName(alphaPopulation.getName())).andReturn(alphaPopulation).anyTimes();
      EasyMock.expect(populationDao.getDefaultPopulation()).andReturn(generalPopulation).anyTimes();
      EasyMock.replay(populationDao);
      return populationDao;
   }

   @Test
   public void testGetProductWithGeneralPopulationSpecified() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductRequest.builder()
                              .withId("700faf")
                              .build();

      makeAndSendMessage(Population.NAME_GENERAL, request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetProductResponse.NAME, response.getMessageType());
      Map<String, Object> attrs = GetProductResponse.getProduct(response);

      Assert.assertEquals("700faf", attrs.get(ProductCapability.ATTR_ID));
      Assert.assertEquals("GE In-Wall Smart Outlet", attrs.get(ProductCapability.ATTR_NAME));
      Assert.assertEquals(Boolean.FALSE, attrs.get(ProductCapability.ATTR_BLACKLISTED));
      Assert.assertEquals("Smart Outlet", attrs.get(ProductCapability.ATTR_SHORTNAME));
   }
   
   @Test
   public void testGetProductWithBlacklistedFlag() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductRequest.builder()
                              .withId("798086")
                              .build();

      makeAndSendMessage(Population.NAME_GENERAL, request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetProductResponse.NAME, response.getMessageType());
      Map<String, Object> attrs = GetProductResponse.getProduct(response);

      Assert.assertEquals("798086", attrs.get(ProductCapability.ATTR_ID));
      Assert.assertEquals("First Alert Smoke and CO Detector", attrs.get(ProductCapability.ATTR_NAME));
      Assert.assertEquals(Boolean.TRUE, attrs.get(ProductCapability.ATTR_BLACKLISTED));
   }

   @Test
   public void testGetProductWithNullPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductRequest.builder()
                              .withId("700faf")
                              .build();

      makeAndSendMessage(null, request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetProductResponse.NAME, response.getMessageType());
      Map<String, Object> attrs = GetProductResponse.getProduct(response);

      Assert.assertEquals("700faf", attrs.get(ProductCapability.ATTR_ID));
      Assert.assertEquals("GE In-Wall Smart Outlet", attrs.get(ProductCapability.ATTR_NAME));
      Assert.assertEquals("Smart Outlet", attrs.get(ProductCapability.ATTR_SHORTNAME));
   }
   
   
   @Test
   public void testGetProductWithBetaPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductRequest.builder()
                              .withId("700faf")
                              .build();

      makeAndSendMessage(betaPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetProductResponse.NAME, response.getMessageType());
      Map<String, Object> attrs = GetProductResponse.getProduct(response);

      Assert.assertEquals("700faf", attrs.get(ProductCapability.ATTR_ID));
      Assert.assertEquals("GE In-Wall Smart Outlet", attrs.get(ProductCapability.ATTR_NAME));
   }

   @Test
   public void testGetProductWithAlphaPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductRequest.builder()
                              .withId("0c9a66")
                              .build();

      makeAndSendMessage(alphaPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetProductResponse.NAME, response.getMessageType());
      Map<String, Object> attrs = GetProductResponse.getProduct(response);

      Assert.assertEquals("0c9a66", attrs.get(ProductCapability.ATTR_ID));
      Assert.assertEquals("GE Plug-In Smart Switch", attrs.get(ProductCapability.ATTR_NAME));
   }

   @Test
   public void testGetAlphaProductWithGeneralPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductRequest.builder()
                              .withId("0c9a66")
                              .build();

      makeAndSendMessage(generalPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals("Error", response.getMessageType());
   }

   @Test
   public void testGetCategoriesWithGeneralPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetCategoriesRequest.instance();

      makeAndSendMessage(generalPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      List<Map<String,Object>> categories = GetCategoriesResponse.getCategories(response);
      Assert.assertEquals(2, categories.size());
      Assert.assertEquals("Home Safety", categories.get(0).get("name"));
      Assert.assertEquals("/o/categories/Home_Safety.png", categories.get(0).get("image"));
      Assert.assertEquals("Lights & Switches", categories.get(1).get("name"));
      Assert.assertEquals("/o/categories/Lights_&_Switches.png", categories.get(1).get("image"));

      Map<String,Integer> counts = GetCategoriesResponse.getCounts(response);
      Assert.assertEquals(Integer.valueOf(5), counts.get("Lights & Switches"));
      Assert.assertEquals(Integer.valueOf(2), counts.get("Home Safety"));
   }

   @Test
   public void testGetCategoriesWithBetaPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetCategoriesRequest.instance();

      makeAndSendMessage(betaPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      List<Map<String,Object>> categories = GetCategoriesResponse.getCategories(response);
      Assert.assertEquals(3, categories.size());
      Assert.assertEquals("Doors & Locks", categories.get(0).get("name"));
      Assert.assertEquals("/o/categories/Doors_&_Locks.png", categories.get(0).get("image"));
      Assert.assertEquals("Home Safety", categories.get(1).get("name"));
      Assert.assertEquals("/o/categories/Home_Safety.png", categories.get(1).get("image"));
      Assert.assertEquals("Lights & Switches", categories.get(2).get("name"));
      Assert.assertEquals("/o/categories/Lights_&_Switches.png", categories.get(2).get("image"));

      Map<String,Integer> counts = GetCategoriesResponse.getCounts(response);
      Assert.assertEquals(Integer.valueOf(5), counts.get("Lights & Switches"));
      Assert.assertEquals(Integer.valueOf(2), counts.get("Home Safety"));
      Assert.assertEquals(Integer.valueOf(1), counts.get("Doors & Locks"));
   }

   @Test
   public void testGetBrandNamesWithGeneralPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetBrandsRequest.instance();

      makeAndSendMessage(generalPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetBrandsResponse.NAME, response.getMessageType());

      List<Map<String,Object>> brands = GetBrandsResponse.getBrands(response);
      Assert.assertEquals(2, brands.size());
      Assert.assertEquals("First Alert", brands.get(0).get("name"));
      Assert.assertEquals("/o/brands/First_Alert.png", brands.get(0).get("image"));
      Assert.assertEquals("First Alert brand", brands.get(0).get("description"));
      Assert.assertEquals("GE", brands.get(1).get("name"));
      Assert.assertEquals("/o/brands/GE.png", brands.get(1).get("image"));
      Assert.assertEquals("GE brand", brands.get(1).get("description"));

      Map<String,Integer> counts = GetBrandsResponse.getCounts(response);
      Assert.assertEquals(Integer.valueOf(2), counts.get("First Alert"));
      Assert.assertEquals(Integer.valueOf(5), counts.get("GE"));
   }

   @Test
   public void testGetBrandNamesWithBetaPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetBrandsRequest.instance();

      makeAndSendMessage(betaPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetBrandsResponse.NAME, response.getMessageType());

      List<Map<String,Object>> brands = GetBrandsResponse.getBrands(response);
      Assert.assertEquals(3, brands.size());
      Assert.assertEquals("First Alert", brands.get(0).get("name"));
      Assert.assertEquals("/o/brands/First_Alert.png", brands.get(0).get("image"));
      Assert.assertEquals("First Alert brand", brands.get(0).get("description"));
      Assert.assertEquals("GE", brands.get(1).get("name"));
      Assert.assertEquals("/o/brands/GE.png", brands.get(1).get("image"));
      Assert.assertEquals("GE brand", brands.get(1).get("description"));
      Assert.assertEquals("Schlage", brands.get(2).get("name"));
      Assert.assertEquals("/o/brands/Schlage.png", brands.get(2).get("image"));
      Assert.assertEquals("Schlage brand", brands.get(2).get("description"));

      Map<String,Integer> counts = GetBrandsResponse.getCounts(response);
      Assert.assertEquals(Integer.valueOf(2), counts.get("First Alert"));
      Assert.assertEquals(Integer.valueOf(5), counts.get("GE"));
      Assert.assertEquals(Integer.valueOf(1), counts.get("Schlage"));
   }

   @Test
   public void testGetProductsByBrandWithGeneralPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductsByBrandRequest.builder()
            .withBrand("First Alert")
            .build();

      makeAndSendMessage(generalPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetProductsByBrandResponse.NAME, response.getMessageType());

      List<Map<String, Object>> products = GetProductsByBrandResponse.getProducts(response);
      Assert.assertEquals(2, products.size());

      Assert.assertEquals("bc45b5", products.get(0).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("First Alert Smoke Detector", products.get(0).get(ProductCapability.ATTR_NAME));
      //Assert.assertEquals("Push the button.", products.get(0).get(ProductCapability.ATTR_REMOVAL));
      Assert.assertEquals("798086", products.get(1).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("First Alert Smoke and CO Detector", products.get(1).get(ProductCapability.ATTR_NAME));
   }

   @Test
   public void testGetProductsByBrandWithBetaPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductsByBrandRequest.builder()
            .withBrand("Schlage")
            .build();

      makeAndSendMessage(betaPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetProductsByBrandResponse.NAME, response.getMessageType());

      List<Map<String, Object>> products = GetProductsByBrandResponse.getProducts(response);
      Assert.assertEquals(1, products.size());

      Assert.assertEquals("23af19", products.get(0).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("Schlage Camelot Lever Lock (Satin Nickel)", products.get(0).get(ProductCapability.ATTR_NAME));
   }

   @Test
   public void testGetProductsByCategoryWithGeneralPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductsByCategoryRequest.builder()
            .withCategory("Home Safety")
            .build();

      makeAndSendMessage(generalPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetProductsByCategoryResponse.NAME, response.getMessageType());

      List<Map<String, Object>> products = GetProductsByCategoryResponse.getProducts(response);
      Assert.assertEquals(2, products.size());

      Assert.assertEquals("bc45b5", products.get(0).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("First Alert Smoke Detector", products.get(0).get(ProductCapability.ATTR_NAME));
      Assert.assertEquals("798086", products.get(1).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("First Alert Smoke and CO Detector", products.get(1).get(ProductCapability.ATTR_NAME));
   }

   @Test
   public void testGetProductsByCategoryWithBetaPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductsByCategoryRequest.builder()
            .withCategory("Doors & Locks")
            .build();

      makeAndSendMessage(betaPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetProductsByCategoryResponse.NAME, response.getMessageType());

      List<Map<String, Object>> products = GetProductsByCategoryResponse.getProducts(response);
      Assert.assertEquals(1, products.size());

      Assert.assertEquals("23af19", products.get(0).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("Schlage Camelot Lever Lock (Satin Nickel)", products.get(0).get(ProductCapability.ATTR_NAME));
   }

   @Test
   public void testGetProductsWithGeneralPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductsRequest.instance();

      makeAndSendMessage(generalPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetProductsResponse.NAME, response.getMessageType());

      List<Map<String, Object>> products = GetProductsResponse.getProducts(response);
      Assert.assertEquals(7, products.size());
      
      
      Assert.assertEquals("359d72", products.get(6).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("GE Plug-In Outdoor Smart Switch", products.get(6).get(ProductCapability.ATTR_NAME));
      Assert.assertEquals("798086", products.get(1).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("First Alert Smoke and CO Detector", products.get(1).get(ProductCapability.ATTR_NAME));
   }
   
   private void printProductList(List<Map<String, Object>> products) {
   	products.forEach(p -> {
   		System.out.println("***"+p.get(ProductCapability.ATTR_ID)+"***"+p.get(ProductCapability.ATTR_NAME));
   	});
   }

   @Test
   public void testGetProductsWithBetaPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductsRequest.instance();

      makeAndSendMessage(betaPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetProductsResponse.NAME, response.getMessageType());

      List<Map<String, Object>> products = GetProductsResponse.getProducts(response);
      Assert.assertEquals(8, products.size());
      printProductList(products);
      Assert.assertEquals("359d72", products.get(6).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("GE Plug-In Outdoor Smart Switch", products.get(6).get(ProductCapability.ATTR_NAME));
      Assert.assertEquals("23af19", products.get(7).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("Schlage Camelot Lever Lock (Satin Nickel)", products.get(7).get(ProductCapability.ATTR_NAME));
   }

   @Test
   public void testGetProductsWithAlphaPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductsRequest.instance();

      makeAndSendMessage(alphaPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetProductsResponse.NAME, response.getMessageType());

      List<Map<String, Object>> products = GetProductsResponse.getProducts(response);
      Assert.assertEquals(8, products.size());
      printProductList(products);
      Assert.assertEquals("0c9a66", products.get(7).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("GE Plug-In Smart Switch", products.get(7).get(ProductCapability.ATTR_NAME));
      Assert.assertEquals("798086", products.get(1).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("First Alert Smoke and CO Detector", products.get(1).get(ProductCapability.ATTR_NAME));
   }

   @Test
   public void testFindProductsWithGeneralPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.FindProductsRequest.builder()
            .withSearch("GE")
            .build();

      makeAndSendMessage(generalPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(FindProductsResponse.NAME, response.getMessageType());

      List<Map<String, Object>> products = FindProductsResponse.getProducts(response);
      Assert.assertEquals(5, products.size());
      printProductList(products);
      Assert.assertEquals("359d72", products.get(4).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("GE Plug-In Outdoor Smart Switch", products.get(4).get(ProductCapability.ATTR_NAME));
      Assert.assertEquals("700faf", products.get(2).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("GE In-Wall Smart Outlet", products.get(2).get(ProductCapability.ATTR_NAME));
   }

   @Test
   public void testFindProductsWithNoResult() throws Exception {
      MessageBody request = ProductCatalogCapability.FindProductsRequest.builder()
            .withSearch("Bogus")
            .build();

      makeAndSendMessage(generalPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(FindProductsResponse.NAME, response.getMessageType());

      List<Map<String, Object>> products = FindProductsResponse.getProducts(response);
      Assert.assertEquals(0, products.size());
   }

   @Test
   public void testFindProductsWithBetaPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.FindProductsRequest.builder()
            .withSearch("Door")
            .build();

      makeAndSendMessage(betaPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(FindProductsResponse.NAME, response.getMessageType());

      List<Map<String, Object>> products = FindProductsResponse.getProducts(response);
      Assert.assertEquals(1, products.size());

      Assert.assertEquals("23af19", products.get(0).get(ProductCapability.ATTR_ID));
      Assert.assertEquals("Schlage Camelot Lever Lock (Satin Nickel)", products.get(0).get(ProductCapability.ATTR_NAME));
   }

   @Test
   public void testGetProductCatalogWithGeneralPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductCatalogRequest.instance();

      makeAndSendMessage(generalPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetProductCatalogResponse.NAME, response.getMessageType());

      Map<String, Object> cat = GetProductCatalogResponse.getCatalog(response);

      Assert.assertEquals(1, (int)ProductCatalogCapability.TYPE_FILENAMEVERSION.coerce(cat.get(ProductCatalogCapability.ATTR_FILENAMEVERSION)));
      Assert.assertTrue(TestUtils.verifyDate(2015, 4, 23, 18, 23, 9,
            (Date)ProductCatalogCapability.TYPE_VERSION.coerce(cat.get(ProductCatalogCapability.ATTR_VERSION))));
      Assert.assertEquals("Human", cat.get(ProductCatalogCapability.ATTR_PUBLISHER));
      Assert.assertEquals(2, (int)ProductCatalogCapability.TYPE_BRANDCOUNT.coerce(cat.get(ProductCatalogCapability.ATTR_BRANDCOUNT)));
      Assert.assertEquals(2, (int)ProductCatalogCapability.TYPE_CATEGORYCOUNT.coerce(cat.get(ProductCatalogCapability.ATTR_CATEGORYCOUNT)));
      Assert.assertEquals(7, (int)ProductCatalogCapability.TYPE_PRODUCTCOUNT.coerce(cat.get(ProductCatalogCapability.ATTR_PRODUCTCOUNT)));
   }

   @Test
   public void testGetProductCatalogWithBetaPopulation() throws Exception {
      MessageBody request = ProductCatalogCapability.GetProductCatalogRequest.instance();

      makeAndSendMessage(betaPopulation.getName(), request);

      PlatformMessage takeMessage = bus.take();
      Assert.assertNotNull(takeMessage);

      MessageBody response = takeMessage.getValue();
      Assert.assertEquals(GetProductCatalogResponse.NAME, response.getMessageType());

      Map<String, Object> cat = GetProductCatalogResponse.getCatalog(response);

      Assert.assertEquals(1, (int)ProductCatalogCapability.TYPE_FILENAMEVERSION.coerce(cat.get(ProductCatalogCapability.ATTR_FILENAMEVERSION)));
      Assert.assertTrue(TestUtils.verifyDate(2015, 4, 23, 18, 23, 9,
            (Date)ProductCatalogCapability.TYPE_VERSION.coerce(cat.get(ProductCatalogCapability.ATTR_VERSION))));
      Assert.assertEquals("Human", cat.get(ProductCatalogCapability.ATTR_PUBLISHER));      Assert.assertEquals(3, (int)ProductCatalogCapability.TYPE_BRANDCOUNT.coerce(cat.get(ProductCatalogCapability.ATTR_BRANDCOUNT)));
      Assert.assertEquals(3, (int)ProductCatalogCapability.TYPE_CATEGORYCOUNT.coerce(cat.get(ProductCatalogCapability.ATTR_CATEGORYCOUNT)));
      Assert.assertEquals(8, (int)ProductCatalogCapability.TYPE_PRODUCTCOUNT.coerce(cat.get(ProductCatalogCapability.ATTR_PRODUCTCOUNT)));
   }

   private void makeAndSendMessage(String populationName, MessageBody request) {
      PlatformMessage sendMessage = PlatformMessage.builder()
            .from(Fixtures.createClientAddress())
            //.to(Address.platformService(PlatformConstants.SERVICE_PRODUCTCATALOG))
            .to(populationName != null
                  ? Address.platformService(populationName, PlatformConstants.SERVICE_PRODUCTCATALOG)
                  : Address.platformService(PlatformConstants.SERVICE_PRODUCTCATALOG))
            .withPayload(request)
            .isRequestMessage(true)
            .create();

      service.handleMessage(sendMessage);
   }

   private Population makePopulation(String name) {
      Population population = new Population();
      population.setName(name);
      population.setDescription(name + " population.");
      return population;
   }
}

