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
package com.iris.platform.subsystem.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.common.subsystem.SubsystemExecutor;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.context.PlaceContext;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.Model;
import com.iris.messages.model.Place;
import com.iris.messages.type.Population;
import com.iris.platform.model.ModelDao;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.subsystem.SubsystemFactory;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

/**
 * 
 */
@Mocks({ SubsystemExecutor.class, SubsystemFactory.class, PlaceDAO.class, ModelDao.class, PlacePopulationCacheManager.class })
public class TestCachingSubsystemRegistry extends IrisMockTestCase {
   @Inject CachingSubsystemRegistry registry;
   
   @Inject SubsystemExecutor mockExecutor;
   @Inject SubsystemFactory mockFactory;
   @Inject PlaceDAO mockPlaceDao;
   @Inject ModelDao mockModelDao;
   @Inject protected PlacePopulationCacheManager mockPopulationCacheMgr;
   
   UUID placeId = UUID.randomUUID();
   Place place = Fixtures.createPlace();
   List<Model> models = new ArrayList<>();
   
   @Before
   public void setUp() throws Exception {
      super.setUp();
      
      place.setId(placeId);
      place.setCreated(new Date());
      place.setModified(new Date());
      place.setTzName("UTC");
      
      models.add(createDeviceModel());
      models.add(createDeviceModel());
      models.add(createDeviceModel());
      
      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }
   
   @Test
   public void testFindByPlaceId() {
      expectPlaceFound();
      expectListModels();
      Capture<PlaceContext> contextRef = expectCreateContext();
      expectExecutorStart();
      replay();
      
      SubsystemExecutor executor = registry.loadByPlace(placeId).get();
      assertEquals(mockExecutor, executor);
      
      PlaceContext context = contextRef.getValue();
      assertEquals(placeId, context.getPlaceId());
      assertEquals(place.getAccount(), context.getAccountId());
      assertEquals(models.size(), context.models().getModels().size());
      for(Model model: models) {
         Fixtures.assertMapEquals(model.toMap(), context.models().getModelByAddress(model.getAddress()).toMap());
      }
      assertNotNull(context.logger());
      assertNotNull(context.getLocalTime());
      
      verify();
   }
   
   @Test
   public void testFindByPlaceIdNotFound() {
      expectPlaceNotFound();
      replay();
      
      Optional<SubsystemExecutor> executor = registry.loadByPlace(placeId);
      assertFalse(executor.isPresent());
      
      verify();
   }
   
   @Test
   public void testFindByPlaceIdModelDaoThrowsException() {
      expectPlaceFound();
      expectListModelsAndThrow();
      replay();
      
      Optional<SubsystemExecutor> executor = registry.loadByPlace(placeId);
      assertFalse(executor.isPresent());
      
      verify();
   }
   
   @Test
   public void testFindByPlaceAndRemove() throws Exception {
      expectListModels();
      expectCreateContext();
      expectExecutorStart();
      expectExecutorStop();
      replay();
      
      SubsystemExecutor executor = registry.loadByPlace(place.getId(), place.getAccount()).get();
      assertEquals(mockExecutor, executor);
      
      registry.removeByPlace(placeId);
      
      verify();
   }
   
   @Test
   public void testFindByPlaceAndClear() throws Exception {
      expectListModels();
      expectCreateContext();
      expectExecutorStart();
      expectExecutorStop();
      replay();
      
      SubsystemExecutor executor = registry.loadByPlace(place.getId(), place.getAccount()).get();
      assertEquals(mockExecutor, executor);
      
      registry.clear();
      
      verify();
   }
   
   protected ModelEntity createDeviceModel() {
      Date timestamp = new Date();
      UUID id = UUID.randomUUID();
      Map<String, Object> attributes = new HashMap<>();
      attributes.put(Capability.ATTR_ID, id.toString());
      attributes.put(Capability.ATTR_TYPE, DeviceCapability.NAMESPACE);
      attributes.put(Capability.ATTR_ADDRESS, Address.platformDriverAddress(id).getRepresentation());
      attributes.put(Capability.ATTR_CAPS, ImmutableSet.of(Capability.NAMESPACE, DeviceCapability.NAMESPACE, DeviceAdvancedCapability.NAMESPACE));
      
      ModelEntity entity = new ModelEntity(attributes);
      entity.setCreated(timestamp);
      entity.setModified(timestamp);
      return entity;
   }

   protected void expectPlaceFound() {
      EasyMock.expect(mockPlaceDao.getAccountById(placeId)).andReturn(place.getAccount());
   }
   
   protected void expectPlaceNotFound() {
      EasyMock.expect(mockPlaceDao.getAccountById(placeId)).andReturn(null);
   }
   
   protected Capture<PlaceContext> expectCreateContext() {
      Capture<PlaceContext> contextRef = Capture.newInstance();
      EasyMock
         .expect(mockFactory.createExecutor(EasyMock.capture(contextRef)))
         .andReturn(mockExecutor)
         ;
      EasyMock
         .expect(mockExecutor.context())
         .andAnswer(() -> contextRef.getValue())
         .anyTimes()
         ;
      return contextRef;
   }
   
   protected void expectExecutorStart() {
      mockExecutor.start();
      EasyMock.expectLastCall();
   }
   
   protected void expectExecutorStop() {
      mockExecutor.stop();
      EasyMock.expectLastCall();
   }
   
   protected void expectListModels() {
      EasyMock
         .expect(mockModelDao.loadModelsByPlace(placeId, CachingSubsystemRegistry.TRACKED_TYPES))
         .andReturn(models);
   }
   
   protected void expectListModelsAndThrow() {
      EasyMock.expect(mockModelDao.loadModelsByPlace(placeId, CachingSubsystemRegistry.TRACKED_TYPES)).andThrow(new RuntimeException("Error loading models"));
   }
}

