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
package com.iris.client.server.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.noauth.NoAuthModule;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.EmptyResourceBundle;
import com.iris.core.dao.HubDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.dao.ResourceBundleDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Hub;
import com.iris.messages.model.Place;
import com.iris.messages.service.ProductCatalogService;
import com.iris.messages.service.ProductCatalogService.GetProductsRequest;
import com.iris.messages.service.ProductCatalogService.GetProductsResponse;
import com.iris.messages.services.PlatformConstants;
import com.iris.messages.type.Population;
import com.iris.model.Version;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.prodcat.Step;
import com.iris.resource.classpath.ClassPathResourceFactory;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.CharsetUtil;

@Mocks({ PlaceDAO.class, PopulationDAO.class, HubDAO.class, BridgeMetrics.class, FullHttpRequest.class, ChannelHandlerContext.class, PlatformMessageBus.class })
@Modules({NoAuthModule.class, AttributeMapTransformModule.class})
public class TestGetProductsRESTHandler extends IrisMockTestCase {

   @Inject
   private BeanAttributesTransformer<ProductCatalogEntry> transformer;     
   @Inject
   private PlaceDAO placeDAO;
   @Inject
   private HubDAO hubDAO;
   @Inject
   private PopulationDAO populationDAO;
   @Inject
   private BridgeMetrics metrics;
   @Inject
   private FullHttpRequest request;
   @Inject
   private ChannelHandlerContext ctx;  

   private GetProductsRESTHandler handler;
   private ProductCatalogManager prodCatManager;
   private Place curPlace;
   private Hub curHub;
   private Population defaultPopulation;
   private AlwaysAllow alwaysAllow;
   private RESTHandlerConfig restHandlerConfig;
   private Map<String, Population> populationMap;

   
   @Override
   protected void configure(Binder binder)
   {
      super.configure(binder);
      binder.bind(ResourceBundleDAO.class).to(EmptyResourceBundle.class);
   }

   @Before
   public void setUp() throws Exception {
      super.setUp();
      curPlace = Fixtures.createPlace();
      curPlace.setId(UUID.randomUUID());
      populationMap = new HashMap<String, Population>();
      defaultPopulation = createPopulation(Population.NAME_GENERAL);
      curPlace.setPopulation(defaultPopulation.getName());
      curHub = Fixtures.createHub();
      curHub.setPlace(curPlace.getId());
      curHub.setAccount(curPlace.getAccount());
      
      populationMap.put(defaultPopulation.getName(), defaultPopulation);
      
      ClassPathResourceFactory factory = new ClassPathResourceFactory();
      prodCatManager = new ProductCatalogManager(factory.create(new URI("classpath:/test_product_catalog.xml")));
      restHandlerConfig = new RESTHandlerConfig();
      alwaysAllow = new AlwaysAllow();
      handler = new GetProductsRESTHandler(alwaysAllow, metrics, prodCatManager, populationDAO, placeDAO, hubDAO, transformer, restHandlerConfig);
   }

   @Test
   public void testGetProductsSuccess() throws Exception {
      //default population   
      curHub.setOsver("2.0.0.031");
      FullHttpResponse response = callGetProducts(curPlace, curHub, GetProductsRequest.INCLUDE_BROWSEABLE);
      validateResponse(curHub.getOsver(), response, defaultPopulation);
   }
   
   @Test
   public void testGetProductsSuccess_beta() throws Exception {
      //beta population    
      Population betaPopulation = createPopulation("beta");      
      curPlace.setPopulation(betaPopulation.getName());
      curHub.setOsver(null);
      FullHttpResponse response = callGetProducts(curPlace, curHub, GetProductsRequest.INCLUDE_BROWSEABLE);
      
      validateResponse(curHub.getOsver(), response, betaPopulation);

   }
   
   private Population createPopulation(String name) {
      Population betaPopulation = Fixtures.createPopulation();
      betaPopulation.setName(name);
      populationMap.put(betaPopulation.getName(), betaPopulation);
      return betaPopulation;
   }
     

   private void validateResponse(String curHubFwVersion, FullHttpResponse response, Population betaPopulation)
   {
      MessageBody mb = toClientRequest(response);
      List<Map<String, Object>> products = GetProductsResponse.getProducts(mb);
      assertNotNull(products);
      assertFalse(products.isEmpty());
      List<ProductCatalogEntry> actual = products.stream().map(m -> transformer.transform(m)).collect(Collectors.toList());      
      List<ProductCatalogEntry> expected = prodCatManager.getCatalog(betaPopulation.getName()).getProducts(
                        StringUtils.isNotBlank(curHubFwVersion)?Version.fromRepresentation(curHubFwVersion):null);
      assertEquals(expected.size(), actual.size());
      ArrayList<ProductCatalogEntry> expected2 = new ArrayList<ProductCatalogEntry>(expected);      
      expected2.removeAll(actual);  
      if(!expected2.isEmpty()) {
      	//see if it's only the removal element that is different
      	for(ProductCatalogEntry curExpected : expected2) {
      		ProductCatalogEntry curActual = findProductFromList(curExpected.getId(), actual);
      		assertTrue(curExpected.getRemoval().size() > 1);
      		assertTrue(curActual.getRemoval().size() == 1);
      		assertEquals(Step.StepType.TEXT, curActual.getRemoval().get(0).getType());
      		//System.out.println(curActual.getRemoval().get(0).getText());
      		//If we remove the removal element from both object, the rest should be equal
      		curExpected.setRemoval(null);
      		curActual.setRemoval(null);
      		assertEquals(curActual, curExpected);
      		
      	}
      }      
   }
   
   private ProductCatalogEntry findProductFromList(String productId, List<ProductCatalogEntry> productList) {
   	for(ProductCatalogEntry cur : productList) {
   		if(cur.getId().equals(productId)) {
   			return cur;
   		}
   	}
   	return null;
   }

   private FullHttpResponse callGetProducts(Place place, Hub curHub2, String include) throws Exception {
      EasyMock.expect(populationDAO.getDefaultPopulation()).andReturn(defaultPopulation);
      EasyMock.expect(placeDAO.findById(place.getId())).andReturn(place);
      EasyMock.expect(populationDAO.findByName(place.getPopulation())).andReturn(populationMap.get(place.getPopulation()));
      EasyMock.expect(hubDAO.findHubForPlace(place.getId())).andReturn(curHub2);
      EasyMock.expect(request.content()).andReturn(Unpooled.copiedBuffer(generateClientMessage(generateRequest(place.getAddress(), include)).getBytes())); 
      replay();
            
      handler.init();  //postConstruct
      FullHttpResponse response = handler.respond(request, ctx);
      verify();
      return response;
   }
   
   private MessageBody toClientRequest(FullHttpResponse response) {
      String json = response.content().toString(CharsetUtil.UTF_8);
      ClientMessage clientMessage = JSON.fromJson(json, ClientMessage.class);
      return clientMessage.getPayload();
   }

   private MessageBody generateRequest(String place, String include) {
      return ProductCatalogService.GetProductsRequest.builder()
         .withPlace(place).withInclude(include).build();
   }

   private String generateClientMessage(MessageBody body) {
      ClientMessage.Builder messageBuilder = ClientMessage.builder().withCorrelationId("").withSource(Address.platformService(PlatformConstants.SERVICE_PEOPLE).getRepresentation()).withPayload(body);
      return JSON.toJson(messageBuilder.create());
   }

}

