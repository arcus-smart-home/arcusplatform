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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SceneTemplateCapability;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Fixtures;
import com.iris.messages.service.SceneService;
import com.iris.platform.rule.environment.ContextLoader;
import com.iris.platform.rule.environment.PlaceEnvironmentExecutor;
import com.iris.platform.rule.environment.PlaceExecutorRegistry;
import com.iris.platform.rule.service.handler.scene.ListSceneTemplateRequestHandler;
import com.iris.platform.rule.service.handler.scene.ListScenesRequestHandler;
import com.iris.platform.rule.service.handler.scenetmpl.CreateRequestHandler;
import com.iris.platform.rule.service.handler.scenetmpl.ResolveActionsRequestHandler;
import com.iris.platform.scene.SceneDao;
import com.iris.platform.scene.SceneDefinition;
import com.iris.platform.scene.SceneTemplateEntity;
import com.iris.platform.scene.SceneTemplateManager;
import com.iris.platform.scene.catalog.SceneTemplate;

/**
 * Scenes and scene templates are addressed by place, so each request has to be refused unless
 * that place is the client's active place. Every collaborator is a strict mock: a refused request
 * that still read or wrote anything fails the test.
 */
public class SceneScopeTest {
   /** Thrown by a collaborator to show a request got past the check, without building the rest of the request. */
   private static class PastTheCheck extends RuntimeException {
      private static final long serialVersionUID = 1L;
   }

   private final UUID activePlace = UUID.randomUUID();
   private final UUID otherPlace = UUID.randomUUID();

   private PlaceExecutorRegistry registry;
   private PlaceEnvironmentExecutor executor;
   private SceneDao sceneDao;
   private SceneTemplateManager templateManager;
   private PlaceDAO placeDao;
   private ContextLoader contextLoader;
   private PlatformMessageBus bus;
   private BeanAttributesTransformer<SceneDefinition> sceneTransformer;
   private BeanAttributesTransformer<SceneTemplateEntity> templateTransformer;

   private Object[] mocks;

   @SuppressWarnings("unchecked")
   @Before
   public void setUp() {
      registry = EasyMock.createMock(PlaceExecutorRegistry.class);
      executor = EasyMock.createMock(PlaceEnvironmentExecutor.class);
      sceneDao = EasyMock.createMock(SceneDao.class);
      templateManager = EasyMock.createMock(SceneTemplateManager.class);
      placeDao = EasyMock.createMock(PlaceDAO.class);
      contextLoader = EasyMock.createMock(ContextLoader.class);
      bus = EasyMock.createMock(PlatformMessageBus.class);
      sceneTransformer = EasyMock.createMock(BeanAttributesTransformer.class);
      templateTransformer = EasyMock.createMock(BeanAttributesTransformer.class);
      mocks = new Object[] { registry, executor, sceneDao, templateManager, placeDao, contextLoader, bus, sceneTransformer, templateTransformer };
   }

   @After
   public void tearDown() {
      EasyMock.verify(mocks);
   }

   private void replay() {
      EasyMock.replay(mocks);
   }

   private PlatformMessage request(Address destination, MessageBody body) {
      return PlatformMessage
            .request(destination)
            .from(Fixtures.createClientAddress())
            .withPlaceId(activePlace)
            .withPayload(body)
            .create();
   }

   private static void assertRefused(Callable<?> call) throws Exception {
      try {
         call.call();
         fail("expected a request for another place to be refused");
      }
      catch(UnauthorizedRequestException e) {
         // expected
      }
   }

   private static void assertPastTheCheck(Callable<?> call) throws Exception {
      try {
         call.call();
         fail("expected the request to reach the collaborator after the check");
      }
      catch(PastTheCheck e) {
         // expected
      }
   }

   // SceneRequestHandler: everything addressed to SERV:scene:<place>.<n>

   private SceneRequestHandler sceneHandler() {
      return new SceneRequestHandler(bus, MoreExecutors.directExecutor(), registry);
   }

   private static Address scene(UUID place) {
      return Address.platformService(place, SceneCapability.NAMESPACE, 1);
   }

   private static MessageBody rename() {
      return MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, ImmutableMap.of(SceneCapability.ATTR_NAME, "renamed"));
   }

   @Test
   public void testFireAtAnotherPlaceIsRefused() throws Exception {
      replay();
      SceneRequestHandler handler = sceneHandler();
      assertRefused(() -> handler.dispatch(request(scene(otherPlace), SceneCapability.FireRequest.instance())));
   }

   @Test
   public void testDeleteAtAnotherPlaceIsRefused() throws Exception {
      replay();
      SceneRequestHandler handler = sceneHandler();
      assertRefused(() -> handler.dispatch(request(scene(otherPlace), SceneCapability.DeleteRequest.instance())));
   }

   @Test
   public void testSetAttributesAtAnotherPlaceIsRefused() throws Exception {
      replay();
      SceneRequestHandler handler = sceneHandler();
      assertRefused(() -> handler.dispatch(request(scene(otherPlace), rename())));
   }

   @Test
   public void testFireAtTheActivePlaceReachesThePlace() throws Exception {
      PlatformMessage message = request(scene(activePlace), SceneCapability.FireRequest.instance());
      EasyMock.expect(registry.getExecutor(activePlace)).andReturn(Optional.of(executor));
      executor.handleRequest(message);
      EasyMock.expectLastCall();
      replay();

      assertNull(sceneHandler().dispatch(message));
   }

   @Test
   public void testInternalFireWithoutAPlaceHeaderReachesThePlace() throws Exception {
      PlatformMessage message = PlatformMessage
            .request(scene(otherPlace))
            .from(Address.platformService("scheduler"))
            .withPayload(SceneCapability.FireRequest.instance())
            .create();
      EasyMock.expect(registry.getExecutor(otherPlace)).andReturn(Optional.of(executor));
      executor.handleRequest(message);
      EasyMock.expectLastCall();
      replay();

      assertNull(sceneHandler().dispatch(message));
   }

   // service-level scene requests, which name the place in the body

   @Test
   public void testListScenesForAnotherPlaceIsRefused() throws Exception {
      replay();
      ListScenesRequestHandler handler = new ListScenesRequestHandler(sceneDao, sceneTransformer);
      MessageBody body = SceneService.ListScenesRequest.builder().withPlaceId(otherPlace.toString()).build();
      assertRefused(() -> handler.handleMessage(request(Address.platformService(SceneService.NAMESPACE), body)));
   }

   @Test
   public void testListScenesForTheActivePlace() throws Exception {
      EasyMock.expect(sceneDao.listByPlace(activePlace)).andReturn(Collections.<SceneDefinition>emptyList());
      replay();

      ListScenesRequestHandler handler = new ListScenesRequestHandler(sceneDao, sceneTransformer);
      MessageBody body = SceneService.ListScenesRequest.builder().withPlaceId(activePlace.toString()).build();
      MessageBody response = handler.handleMessage(request(Address.platformService(SceneService.NAMESPACE), body));
      assertEquals(SceneService.ListScenesResponse.NAME, response.getMessageType());
   }

   @Test
   public void testListSceneTemplatesForAnotherPlaceIsRefused() throws Exception {
      replay();
      ListSceneTemplateRequestHandler handler = new ListSceneTemplateRequestHandler(templateManager, templateTransformer);
      MessageBody body = SceneService.ListSceneTemplatesRequest.builder().withPlaceId(otherPlace.toString()).build();
      assertRefused(() -> handler.handleMessage(request(Address.platformService(SceneService.NAMESPACE), body)));
   }

   @Test
   public void testListSceneTemplatesForTheActivePlace() throws Exception {
      EasyMock.expect(templateManager.listByPlaceId(activePlace)).andReturn(Collections.<SceneTemplateEntity>emptyList());
      replay();

      ListSceneTemplateRequestHandler handler = new ListSceneTemplateRequestHandler(templateManager, templateTransformer);
      MessageBody body = SceneService.ListSceneTemplatesRequest.builder().withPlaceId(activePlace.toString()).build();
      MessageBody response = handler.handleMessage(request(Address.platformService(SceneService.NAMESPACE), body));
      assertEquals(SceneService.ListSceneTemplatesResponse.NAME, response.getMessageType());
   }

   // scene templates, which also name the place in the body

   private CreateRequestHandler createHandler() {
      return new CreateRequestHandler(contextLoader, bus, registry, placeDao, sceneDao, sceneTransformer);
   }

   private static SceneTemplateEntity availableTemplate() {
      SceneTemplateEntity template = new SceneTemplateEntity();
      template.setAvailable(true);
      return template;
   }

   private MessageBody create(UUID place) {
      return SceneTemplateCapability.CreateRequest.builder().withPlaceId(place.toString()).withName("scene").build();
   }

   @Test
   public void testCreateASceneAtAnotherPlaceIsRefused() throws Exception {
      replay();
      CreateRequestHandler handler = createHandler();
      PlatformMessage message = request(Address.platformService(activePlace, SceneTemplateCapability.NAMESPACE), create(otherPlace));
      assertRefused(() -> handler.handleRequest(availableTemplate(), message));
   }

   @Test
   public void testCreateASceneAtTheActivePlacePassesTheCheck() throws Exception {
      EasyMock.expect(placeDao.findById(activePlace)).andThrow(new PastTheCheck());
      replay();

      CreateRequestHandler handler = createHandler();
      PlatformMessage message = request(Address.platformService(activePlace, SceneTemplateCapability.NAMESPACE), create(activePlace));
      assertPastTheCheck(() -> handler.handleRequest(availableTemplate(), message));
   }

   private MessageBody resolve(UUID place) {
      return SceneTemplateCapability.ResolveActionsRequest.builder().withPlaceId(place.toString()).build();
   }

   @Test
   public void testResolveActionsAtAnotherPlaceIsRefused() throws Exception {
      replay();
      ResolveActionsRequestHandler handler = new ResolveActionsRequestHandler(contextLoader);
      PlatformMessage message = request(Address.platformService(activePlace, SceneTemplateCapability.NAMESPACE), resolve(otherPlace));
      assertRefused(() -> handler.handleRequest(new SceneTemplate(), message));
   }

   @Test
   public void testResolveActionsAtTheActivePlacePassesTheCheck() throws Exception {
      EasyMock.expect(contextLoader.load(activePlace)).andThrow(new PastTheCheck());
      replay();

      ResolveActionsRequestHandler handler = new ResolveActionsRequestHandler(contextLoader);
      PlatformMessage message = request(Address.platformService(activePlace, SceneTemplateCapability.NAMESPACE), resolve(activePlace));
      assertPastTheCheck(() -> handler.handleRequest(new SceneTemplate(), message));
   }
}
