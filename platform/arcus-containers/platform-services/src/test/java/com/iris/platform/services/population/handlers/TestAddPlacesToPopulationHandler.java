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
package com.iris.platform.services.population.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Place;
import com.iris.messages.service.PopulationService;
import com.iris.messages.type.Population;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Modules({InMemoryMessageModule.class, AttributeMapTransformModule.class})
@Mocks({PlaceDAO.class, PopulationDAO.class})
public class TestAddPlacesToPopulationHandler extends IrisMockTestCase {

   @Inject
   private PlaceDAO mockPlaceDao;
   
   @Inject
   private PopulationDAO populationDao;
   
   // unit under test
   @Inject
   private AddPlacesToPopulationHandler handler;
   
   private Address populationServiceAddress = PlatformServiceAddress.platformService(PopulationService.NAMESPACE);
   private Address clientAddress = Fixtures.createClientAddress();

   private Population population;
   private Place place1;
   private Place place2;
   
   @Before
   public void createFixtures() {
      population = Fixtures.createPopulation();
      
      place1 = Fixtures.createPlace();
      place1.setId(UUID.randomUUID());
      place2 = Fixtures.createPlace();
      place2.setId(UUID.randomUUID());
   }

   
   
   @Test
   public void testAddPlacesSuccess() throws Exception {      
      
      setupMocksForSuccess(true);
      replay();
      
      MessageBody response = sendStaticRequest(population, place1, place2);
      
      assertEquals(MessageBody.emptyMessage(), response);
      assertEquals(population.getName(), place1.getPopulation());
      assertEquals(population.getName(), place2.getPopulation());
      verify();
   }
   
   @Test
   public void testAddPlacesSuccess2() throws Exception {      
      
      setupMocksForSuccess(false);
      replay();
      //no population in the request, should use the context
      PlatformMessage request = request(createMessageBody(null, place1, place2));
      MessageBody response = handler.handleRequest(population, request);
      
      assertEquals(MessageBody.emptyMessage(), response);
      assertEquals(population.getName(), place1.getPopulation());
      assertEquals(population.getName(), place2.getPopulation());
      verify();
   }
   
   @Test
   public void testAddPlacesSuccess3() throws Exception {      
      
      setupMocksForSuccess(true);
      replay();
      //population set in the request, and the population context is set also, but they match. So ok 
      MessageBody response = sendRequest(population, place1, place2);
      
      assertEquals(MessageBody.emptyMessage(), response);
      assertEquals(population.getName(), place1.getPopulation());
      assertEquals(population.getName(), place2.getPopulation());
      verify();
   }
   
   @Test
   public void testAddPlacesPopulationContextDoesNotMatch() throws Exception {      
          
      //population set in the request, and the population context is set also, but they match. So ok 
      PlatformMessage request = request(createMessageBody(population, place1, place2));
      Population population2 = Fixtures.createPopulation();
      try{
         handler.handleRequest(population2, request);
      }catch(ErrorEventException e) {
         assertEquals(Errors.CODE_INVALID_PARAM, e.getCode());
      }
 
   }

   @Test
   public void testAddPlacesInvalidPopulation() throws Exception {      
      
      EasyMock.expect(populationDao.findByName(population.getName())).andReturn(null);  //not found      
      replay();
      
      sendRequestAndAssertFail(Errors.CODE_INVALID_PARAM, population, place1, place2);   
      
      verify();
   }
   
   @Test
   public void testAddPlacesMissingPopulation() throws Exception {
      replay();
      
      sendRequestAndAssertFail(Errors.CODE_MISSING_PARAM, null, place1, place2);   
      
      verify();
      
   }
   
   @Test
   public void testAddPlacesMissingPlaces() throws Exception {
      EasyMock.expect(populationDao.findByName(population.getName())).andReturn(population);
      replay();
      
      sendRequestAndAssertFail(Errors.CODE_MISSING_PARAM, population);   
      
      verify();
   }
   
   private void sendRequestAndAssertFail(String errorCode, Population population, Place... places) {
      try{
         sendStaticRequest(population, places);
         fail("should have failed");
      }catch(ErrorEventException e) {
         assertEquals(errorCode, e.getCode());
      }
   }
   
   private MessageBody createMessageBody(Population population, Place... places) {
      List<String> placeAddresses = new ArrayList<String>();
      if(places != null && places.length > 0) {
         for(Place cur : places) {
            placeAddresses.add(cur.getAddress());
         }
      }
      return PopulationService.AddPlacesRequest.builder()
         .withPopulation(population!=null?population.getName():null)
         .withPlaces(placeAddresses)
         .build();
   }
   
   private MessageBody sendStaticRequest(Population population, Place... places) {
      MessageBody request = createMessageBody(population, places);
      return handler.handleStaticRequest(request(request));
   }
   
   private MessageBody sendRequest(Population population, Place... places) {
      MessageBody request = createMessageBody(population, places);
      return handler.handleRequest(population, request(request));
   }
   
   
   private PlatformMessage request(MessageBody request) {
      return 
            PlatformMessage
               .buildRequest(request, clientAddress, populationServiceAddress)               
               .create();
   }
   
   private void setupMocksForSuccess(boolean needLookupPopulation) {
      if(needLookupPopulation) {
         EasyMock.expect(populationDao.findByName(population.getName())).andReturn(population);
      }
      EasyMock
         .expect(mockPlaceDao.findByPlaceIDIn(ImmutableSet.<UUID>of(place1.getId(), place2.getId())))
         .andReturn(ImmutableList.<Place>of(place1, place2));
      
      EasyMock.expect(mockPlaceDao.save(place1)).andReturn(place1);
      EasyMock.expect(mockPlaceDao.save(place2)).andReturn(place2);
   }
}

