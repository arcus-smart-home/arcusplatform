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
package com.iris.voice.service;

import java.util.Optional;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.iris.messages.model.SimpleModelStore;
import com.iris.messages.service.VoiceService;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.util.IrisUUID;
import com.iris.voice.context.VoiceContext;
import com.iris.voice.context.VoiceContextExecutorRegistry;
import com.iris.voice.context.VoiceDAO;
import com.iris.voice.proactive.ProactiveCredsDAO;

@Mocks({VoiceDAO.class, ProactiveCredsDAO.class, VoiceContextExecutorRegistry.class})
public class TestStopPlaceHandler extends IrisMockTestCase {

   private static final UUID placeId = IrisUUID.randomUUID();

   @Inject
   private VoiceDAO mockVoiceDao;

   @Inject
   private ProactiveCredsDAO mockProactiveCredsDao;

   @Inject
   private VoiceContextExecutorRegistry mockContextRegistry;

   private VoiceContext context;

   private StopPlaceHandler handler;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      context = new VoiceContext(
         placeId,
         new SimpleModelStore(),
         ImmutableSet.of(VoiceService.StartPlaceRequest.ASSISTANT_ALEXA, VoiceService.StartPlaceRequest.ASSISTANT_GOOGLE),
         ImmutableMap.of()
      );
      handler = new StopPlaceHandler(mockVoiceDao, mockProactiveCredsDao, mockContextRegistry);
   }

   @Test
   public void testStopDoesntFlushCache() {
      mockVoiceDao.removeAssistant(placeId, VoiceService.StartPlaceRequest.ASSISTANT_ALEXA);
      EasyMock.expectLastCall();
      mockProactiveCredsDao.remove(placeId, VoiceService.StartPlaceRequest.ASSISTANT_ALEXA);
      EasyMock.expectLastCall();

      replay();

      handler.handleStopPlace(Optional.of(context), VoiceService.StartPlaceRequest.ASSISTANT_ALEXA);

      assertTrue(context.getAssistants().noneMatch(VoiceService.StartPlaceRequest.ASSISTANT_ALEXA::equals));
      assertTrue(context.getAssistants().anyMatch(VoiceService.StartPlaceRequest.ASSISTANT_GOOGLE::equals));

      verify();

   }

   @Test
   public void testStopLastFlushesCache() {
      context.removeAssistant(VoiceService.StartPlaceRequest.ASSISTANT_ALEXA);

      mockVoiceDao.removeAssistant(placeId, VoiceService.StartPlaceRequest.ASSISTANT_GOOGLE);
      EasyMock.expectLastCall();
      mockProactiveCredsDao.remove(placeId, VoiceService.StartPlaceRequest.ASSISTANT_GOOGLE);
      EasyMock.expectLastCall();

      mockContextRegistry.remove(placeId);
      EasyMock.expectLastCall();

      replay();

      handler.handleStopPlace(Optional.of(context), VoiceService.StartPlaceRequest.ASSISTANT_GOOGLE);

      assertEquals(0, context.getAssistants().count());

      verify();

   }

}

