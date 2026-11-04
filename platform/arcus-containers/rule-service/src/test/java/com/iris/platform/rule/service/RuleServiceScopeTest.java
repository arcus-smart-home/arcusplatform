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

import java.util.Map;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
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
import com.iris.messages.capability.RuleCapability;
import com.iris.messages.capability.RuleTemplateCapability;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.services.PlatformConstants;
import com.iris.platform.model.ModelDao;
import com.iris.platform.partition.Partitioner;
import com.iris.platform.partition.simple.SimplePartitionModule;
import com.iris.platform.rule.LegacyRuleDefinition;
import com.iris.platform.rule.RuleDao;
import com.iris.platform.rule.RuleEnvironmentDao;
import com.iris.platform.rule.environment.PlaceExecutorRegistry;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

/**
 * A rule address names the place the rule belongs to, so a client request addressed
 * to a rule at another place has to be refused whatever the message type.
 */
@Mocks({
   RuleEnvironmentDao.class,
   PlaceExecutorRegistry.class,
   ModelDao.class,
   PlaceDAO.class,
   RuleDao.class,
   PlacePopulationCacheManager.class
})
@Modules({ InMemoryMessageModule.class, SimplePartitionModule.class })
public class RuleServiceScopeTest extends IrisMockTestCase {

   @Inject private RuleEnvironmentDao envDao;
   @Inject private PlaceExecutorRegistry registry;
   @Inject private RuleDao ruleDao;
   @Inject private ModelDao modelDao;
   @Inject private PlaceDAO placeDao;
   @Inject private InMemoryPlatformMessageBus bus;
   @Inject private Partitioner partitioner;
   @Inject private PlacePopulationCacheManager populationCacheMgr;

   private final UUID activePlace = UUID.randomUUID();
   private final UUID otherPlace = UUID.randomUUID();

   private RuleService ruleService;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      reset();
      RuleCatalogLoader ruleCatLoader = EasyMock.createNiceMock(RuleCatalogLoader.class);
      ruleService = new RuleService(MoreExecutors.directExecutor(), envDao, bus, partitioner, ruleDao, modelDao, placeDao, ruleCatLoader, registry, null, populationCacheMgr);
   }

   private Address ruleAddress(UUID placeId) {
      return Address.platformService(placeId, PlatformConstants.SERVICE_RULE, 1);
   }

   private PlatformMessage request(String type, Map<String, Object> attributes, Address destination, UUID placeHeader) {
      PlatformMessage.Builder builder = PlatformMessage.buildRequest(type, attributes, Address.clientAddress("android", "1"), destination);
      if(placeHeader != null) {
         builder.withPlaceId(placeHeader);
      }
      return builder.create();
   }

   private void assertRefused(String type, Map<String, Object> attributes) throws Exception {
      replay();
      try {
         ruleService.handleRequest(request(type, attributes, ruleAddress(otherPlace), activePlace));
         fail("expected " + type + " to a rule at another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
      verify();
   }

   @Test
   public void testDeleteAtAnotherPlaceIsRefused() throws Exception {
      assertRefused(RuleCapability.DeleteRequest.NAME, ImmutableMap.of());
   }

   @Test
   public void testUpdateContextAtAnotherPlaceIsRefused() throws Exception {
      assertRefused(RuleCapability.UpdateContextRequest.NAME, ImmutableMap.of());
   }

   @Test
   public void testSetAttributesAtAnotherPlaceIsRefused() throws Exception {
      assertRefused(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(RuleCapability.ATTR_NAME, "renamed"));
   }

   @Test
   public void testGetAttributesAtAnotherPlaceIsRefused() throws Exception {
      assertRefused(Capability.CMD_GET_ATTRIBUTES, ImmutableMap.of());
   }

   @Test
   public void testEnableAtAnotherPlaceIsRefused() throws Exception {
      assertRefused(RuleCapability.EnableRequest.NAME, ImmutableMap.of());
   }

   @Test
   public void testListHistoryEntriesAtAnotherPlaceIsRefused() throws Exception {
      assertRefused(RuleCapability.ListHistoryEntriesRequest.NAME, ImmutableMap.of());
   }

   @Test
   public void testGetAttributesAtTheActivePlace() throws Exception {
      LegacyRuleDefinition definition = new LegacyRuleDefinition();
      definition.setPlaceId(activePlace);
      definition.setSequenceId(1);
      definition.setName("a rule");

      EasyMock.expect(ruleDao.findById(activePlace, 1)).andReturn(definition);
      replay();

      MessageBody response = ruleService.handleRequest(request(Capability.CMD_GET_ATTRIBUTES, ImmutableMap.of(), ruleAddress(activePlace), activePlace));
      assertEquals(Capability.EVENT_GET_ATTRIBUTES_RESPONSE, response.getMessageType());
      assertEquals("a rule", response.getAttributes().get(RuleCapability.ATTR_NAME));
      verify();
   }

   @Test
   public void testListRulesForAnotherPlaceIsRefused() throws Exception {
      replay();
      try {
         ruleService.handleRequest(request(
               com.iris.messages.service.RuleService.ListRulesRequest.NAME,
               ImmutableMap.of(com.iris.messages.service.RuleService.ListRulesRequest.ATTR_PLACEID, otherPlace.toString()),
               Address.platformService(PlatformConstants.SERVICE_RULE),
               activePlace));
         fail("expected listing another place's rules to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
      verify();
   }

   @Test
   public void testListRuleTemplatesForAnotherPlaceIsRefused() throws Exception {
      replay();
      try {
         ruleService.handleRequest(request(
               com.iris.messages.service.RuleService.ListRuleTemplatesRequest.NAME,
               ImmutableMap.of(com.iris.messages.service.RuleService.ListRuleTemplatesRequest.ATTR_PLACEID, otherPlace.toString()),
               Address.platformService(PlatformConstants.SERVICE_RULE),
               activePlace));
         fail("expected listing rule templates for another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
      verify();
   }

   @Test
   public void testListRulesStillAnswersTheServiceAddress() throws Exception {
      EasyMock.expect(ruleDao.listByPlace(activePlace)).andReturn(ImmutableList.of());
      replay();

      MessageBody response = ruleService.handleRequest(request(
            com.iris.messages.service.RuleService.ListRulesRequest.NAME,
            ImmutableMap.of(com.iris.messages.service.RuleService.ListRulesRequest.ATTR_PLACEID, activePlace.toString()),
            Address.platformService(PlatformConstants.SERVICE_RULE),
            activePlace));
      assertEquals(com.iris.messages.service.RuleService.ListRulesResponse.NAME, response.getMessageType());
      verify();
   }

   private void assertServiceRequestRefused(String type, Map<String, Object> attributes, Address destination) throws Exception {
      replay();
      try {
         ruleService.handleRequest(request(type, attributes, destination, activePlace));
         fail("expected " + type + " for another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
      verify();
   }

   @Test
   public void testGetCategoriesForAnotherPlaceIsRefused() throws Exception {
      assertServiceRequestRefused(
            com.iris.messages.service.RuleService.GetCategoriesRequest.NAME,
            ImmutableMap.of(com.iris.messages.service.RuleService.GetCategoriesRequest.ATTR_PLACEID, otherPlace.toString()),
            Address.platformService(PlatformConstants.SERVICE_RULE));
   }

   @Test
   public void testGetRuleTemplatesByCategoryForAnotherPlaceIsRefused() throws Exception {
      assertServiceRequestRefused(
            com.iris.messages.service.RuleService.GetRuleTemplatesByCategoryRequest.NAME,
            ImmutableMap.of(
                  com.iris.messages.service.RuleService.GetRuleTemplatesByCategoryRequest.ATTR_PLACEID, otherPlace.toString(),
                  com.iris.messages.service.RuleService.GetRuleTemplatesByCategoryRequest.ATTR_CATEGORY, "Security"),
            Address.platformService(PlatformConstants.SERVICE_RULE));
   }

   @Test
   public void testResolveATemplateForAnotherPlaceIsRefused() throws Exception {
      assertServiceRequestRefused(
            RuleTemplateCapability.ResolveRequest.NAME,
            ImmutableMap.of(RuleTemplateCapability.ResolveRequest.ATTR_PLACEID, otherPlace.toString()),
            Address.platformService("a-template", RuleTemplateCapability.NAMESPACE));
   }

   @Test
   public void testCreateARuleAtAnotherPlaceIsRefused() throws Exception {
      assertServiceRequestRefused(
            RuleTemplateCapability.CreateRuleRequest.NAME,
            ImmutableMap.of(
                  RuleTemplateCapability.CreateRuleRequest.ATTR_PLACEID, otherPlace.toString(),
                  RuleTemplateCapability.CreateRuleRequest.ATTR_NAME, "a rule"),
            Address.platformService("a-template", RuleTemplateCapability.NAMESPACE));
   }
}
