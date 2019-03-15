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
package com.iris.platform.rule.service;

import java.util.UUID;
import java.util.stream.Stream;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Place;
import com.iris.messages.type.Population;
import com.iris.platform.model.ModelDao;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.simple.SimplePartitionModule;
import com.iris.platform.rule.LegacyRuleDefinition;
import com.iris.platform.rule.RuleDao;
import com.iris.platform.rule.RuleDefinition;
import com.iris.platform.rule.RuleEnvironmentDao;
import com.iris.platform.rule.environment.PlaceEnvironmentExecutor;
import com.iris.platform.rule.environment.PlaceExecutorRegistry;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({
   RuleEnvironmentDao.class,
   PlaceExecutorRegistry.class,
   ModelDao.class,
   PlaceDAO.class,
   RuleDao.class,
   PlacePopulationCacheManager.class
})
@Modules({ InMemoryMessageModule.class, SimplePartitionModule.class })
public class TestRuleService_CascadeOnPlaceDelete extends IrisMockTestCase {

   
   @Inject private RuleEnvironmentDao envDao;
   @Inject private PlaceExecutorRegistry registry;
   @Inject private RuleDao ruleDao;
   @Inject private ModelDao modelDao;
   @Inject private PlaceDAO placeDao;
   @Inject private InMemoryPlatformMessageBus bus;
   @Inject private Partitioner partitioner;
   @Inject private PlacePopulationCacheManager mockPopulationCacheMgr;
   private RuleCatalogLoader ruleCatLoader;
   private Place place;
   private Population pop;
   private RuleDefinition ruleDef;
   private RuleService ruleService;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      reset();
      
      // If you inject RuleCatalogManager, then the @WarmUp method init() gets called and messes up the mock.
      ruleCatLoader = EasyMock.createMock(RuleCatalogLoader.class);
            
      place = new Place();
      place.setId(UUID.randomUUID());
      
      pop = new Population();
      pop.setName(Population.NAME_GENERAL);
      
      ruleDef = new LegacyRuleDefinition();
      ruleDef.setSequenceId(1);
      ruleDef.setPlaceId(place.getId());

      ruleService = new RuleService(MoreExecutors.directExecutor(), envDao, bus, partitioner, ruleDao, modelDao, placeDao, ruleCatLoader, registry, null, mockPopulationCacheMgr);
   }

   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

   @Test
   public void testOnPlaceDelete() throws Exception {
      PlaceEnvironmentExecutor executor = setupExecutor();
      
      EasyMock.expect(registry.stop(place.getId())).andReturn(true);
      executor.stop();
      EasyMock.expectLastCall();
      envDao.deleteByPlace(place.getId());
      EasyMock.expectLastCall();
      replay();

      ruleService.start();

      ruleService.handleEvent(createDeleted());
   }

   private PlatformMessage createDeleted() {
      MessageBody body = MessageBody.buildMessage(Capability.EVENT_DELETED, ImmutableMap.of());
      return PlatformMessage.buildBroadcast(body, Address.fromString(place.getAddress()))
            .withPlaceId(place.getAddress())
            .create();
   }

   private PlaceEnvironmentExecutor setupExecutor() {
      PlaceEnvironmentExecutor executor = EasyMock.createMock(PlaceEnvironmentExecutor.class);

      EasyMock.expect(ruleCatLoader.getCatalogForPlace(place.getId())).andReturn(null);
      EasyMock.expect(registry.getExecutor(place.getId())).andReturn(Optional.of(executor)).anyTimes();
      executor.start();
      EasyMock.expectLastCall();
      registry.clear();
      EasyMock.expectLastCall();
      executor.stop();
      EasyMock.expectLastCall();
      
      EasyMock.expect(placeDao.streamByPartitionId(EasyMock.anyInt())).andReturn(Stream.empty()).anyTimes();
      
      return executor;
   }

}

