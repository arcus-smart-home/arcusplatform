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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.MoreExecutors;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Fixtures;
import com.iris.platform.rule.environment.PlaceEnvironmentExecutor;
import com.iris.platform.rule.environment.PlaceExecutorRegistry;

/**
 * Firing the same scene again within the rate limit is refused. Only fires that pass the place
 * check and reach a loaded place count towards it, so a request refused for another place can't
 * hold back that place's scenes.
 */
public class SceneFireRateLimitTest {
   private final UUID place = UUID.randomUUID();
   private long now = 1_000_000_000L;

   private InMemoryPlatformMessageBus bus;
   private PlaceExecutorRegistry registry;
   private PlaceEnvironmentExecutor executor;
   private SceneRequestHandler handler;
   private final List<PlatformMessage> dispatched = new ArrayList<>();

   @Before
   public void setUp() {
      bus = new InMemoryPlatformMessageBus();
      registry = EasyMock.createMock(PlaceExecutorRegistry.class);
      executor = EasyMock.createMock(PlaceEnvironmentExecutor.class);
      EasyMock.expect(registry.getExecutor(place)).andReturn(Optional.of(executor)).anyTimes();
      executor.handleRequest(EasyMock.anyObject(PlatformMessage.class));
      EasyMock.expectLastCall().andAnswer(() -> {
         dispatched.add((PlatformMessage) EasyMock.getCurrentArguments()[0]);
         return null;
      }).anyTimes();
      EasyMock.replay(registry, executor);
      handler = new SceneRequestHandler(bus, MoreExecutors.directExecutor(), registry, () -> now);
   }

   private void advanceMillis(long millis) {
      now += TimeUnit.MILLISECONDS.toNanos(millis);
   }

   @After
   public void tearDown() {
      EasyMock.verify(registry, executor);
   }

   private static PlatformMessage fire(UUID scenePlace, int scene, UUID placeHeader) {
      PlatformMessage.Builder builder = PlatformMessage
            .request(Address.platformService(scenePlace, SceneCapability.NAMESPACE, scene))
            .from(Fixtures.createClientAddress())
            .withPayload(SceneCapability.FireRequest.instance());
      if(placeHeader != null) {
         builder.withPlaceId(placeHeader);
      }
      return builder.create();
   }

   /** Error responses sent since the last call; a successful fire has nothing to answer. */
   private List<MessageBody> errors() {
      List<MessageBody> errors = new ArrayList<>();
      PlatformMessage message;
      while((message = bus.poll()) != null) {
         if(message.isError()) {
            errors.add(message.getValue());
         }
      }
      return errors;
   }

   private void assertRateLimited() {
      List<MessageBody> errors = errors();
      assertEquals(errors.toString(), 1, errors.size());
      assertEquals(Errors.CODE_INVALID_REQUEST, errors.get(0).getAttributes().get("code"));
   }

   @Test
   public void testFiringTheSameSceneAgainIsRateLimited() {
      handler.handleRequestAndSendResponse(fire(place, 1, place));
      assertTrue(errors().isEmpty());

      handler.handleRequestAndSendResponse(fire(place, 1, place));
      assertRateLimited();
      assertEquals(1, dispatched.size());
   }

   @Test
   public void testFiringAnotherSceneIsNotRateLimited() {
      handler.handleRequestAndSendResponse(fire(place, 1, place));
      handler.handleRequestAndSendResponse(fire(place, 2, place));
      assertTrue(errors().isEmpty());
      assertEquals(2, dispatched.size());
   }

   @Test
   public void testFireFromAnotherPlaceDoesNotHoldBackThePlacesScene() {
      handler.handleRequestAndSendResponse(fire(place, 1, UUID.randomUUID()));
      List<MessageBody> refused = errors();
      assertEquals(refused.toString(), 1, refused.size());
      assertEquals(Errors.CODE_NOT_FOUND, refused.get(0).getAttributes().get("code"));

      handler.handleRequestAndSendResponse(fire(place, 1, place));
      assertTrue("the place's own fire must not be rate limited", errors().isEmpty());
      assertEquals(1, dispatched.size());
   }

   @Test
   public void testFiringAgainAfterTheLimitIsAllowed() {
      handler.handleRequestAndSendResponse(fire(place, 1, place));
      advanceMillis(14_999);
      handler.handleRequestAndSendResponse(fire(place, 1, place));
      assertRateLimited();

      advanceMillis(1);
      handler.handleRequestAndSendResponse(fire(place, 1, place));
      assertTrue(errors().isEmpty());
      assertEquals(2, dispatched.size());
   }

   @Test
   public void testARateLimitedFireDoesNotExtendTheLimit() {
      handler.handleRequestAndSendResponse(fire(place, 1, place));
      advanceMillis(10_000);
      handler.handleRequestAndSendResponse(fire(place, 1, place));
      assertRateLimited();

      // 15 s after the fire that ran, not after the refused one
      advanceMillis(5_000);
      handler.handleRequestAndSendResponse(fire(place, 1, place));
      assertTrue(errors().isEmpty());
      assertEquals(2, dispatched.size());
   }

   @Test
   public void testOnlyTheLastSceneFiredIsRateLimited() {
      handler.handleRequestAndSendResponse(fire(place, 1, place));
      handler.handleRequestAndSendResponse(fire(place, 2, place));
      handler.handleRequestAndSendResponse(fire(place, 1, place));
      assertTrue(errors().isEmpty());
      assertEquals(3, dispatched.size());
   }

   @Test
   public void testFireAtAPlaceThatIsNotLoadedDoesNotCount() {
      UUID unloaded = UUID.randomUUID();
      EasyMock.reset(registry);
      EasyMock.expect(registry.getExecutor(unloaded)).andReturn(Optional.<PlaceEnvironmentExecutor>absent());
      EasyMock.expect(registry.getExecutor(unloaded)).andReturn(Optional.of(executor));
      EasyMock.replay(registry);

      handler.handleRequestAndSendResponse(fire(unloaded, 1, unloaded));
      List<MessageBody> notFound = errors();
      assertEquals(notFound.toString(), 1, notFound.size());
      assertEquals(Errors.CODE_NOT_FOUND, notFound.get(0).getAttributes().get("code"));

      handler.handleRequestAndSendResponse(fire(unloaded, 1, unloaded));
      assertTrue(errors().isEmpty());
      assertEquals(1, dispatched.size());
   }

   @Test
   public void testFireWithoutASceneNumberIsLeftToThePlace() {
      PlatformMessage noScene = PlatformMessage
            .request(Address.platformService(place, SceneCapability.NAMESPACE))
            .from(Fixtures.createClientAddress())
            .withPlaceId(place)
            .withPayload(SceneCapability.FireRequest.instance())
            .create();
      handler.handleRequestAndSendResponse(noScene);
      handler.handleRequestAndSendResponse(noScene);
      assertTrue(errors().isEmpty());
      assertEquals(2, dispatched.size());
   }

   @Test
   public void testInternalFireWithoutAPlaceHeaderCountsTowardsTheLimit() {
      handler.handleRequestAndSendResponse(fire(place, 1, null));
      handler.handleRequestAndSendResponse(fire(place, 1, place));
      assertRateLimited();
      assertEquals(1, dispatched.size());
   }
}
