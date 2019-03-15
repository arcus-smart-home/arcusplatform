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
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.model.SimpleModelStore;
import com.iris.messages.service.VoiceService;
import com.iris.messages.type.Population;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.util.IrisUUID;
import com.iris.voice.context.VoiceContext;
import com.iris.voice.context.VoiceContextExecutorRegistry;
import com.iris.voice.context.VoiceDAO;

@Mocks({VoiceDAO.class, VoiceContextExecutorRegistry.class})
public class TestStartPlaceHandler extends IrisMockTestCase {

   private static final UUID placeId = IrisUUID.randomUUID();

   @Inject
   private VoiceDAO mockVoiceDao;

   @Inject
   private VoiceContextExecutorRegistry mockContextRegistry;

   private VoiceContext context;

   private StartPlaceHandler handler;

   @Override
   public void setUp() throws Exception {
      super.setUp();
      context = new VoiceContext(placeId, new SimpleModelStore(), ImmutableSet.of(), ImmutableMap.of());
      handler = new StartPlaceHandler(mockVoiceDao, mockContextRegistry);
   }

   @Test
   public void testContextNotPresent() {
      mockVoiceDao.recordAssistant(placeId, VoiceService.StartPlaceRequest.ASSISTANT_ALEXA);
      EasyMock.expectLastCall();

      mockContextRegistry.add(placeId);
      EasyMock.expectLastCall();

      replay();

      handler.handleStartPlace(createMessage(), Optional.empty(), VoiceService.StartPlaceRequest.ASSISTANT_ALEXA);

      verify();
   }

   @Test
   public void testContextPresent() {
      mockVoiceDao.recordAssistant(placeId, VoiceService.StartPlaceRequest.ASSISTANT_ALEXA);
      EasyMock.expectLastCall();

      replay();

      handler.handleStartPlace(createMessage(), Optional.of(context), VoiceService.StartPlaceRequest.ASSISTANT_ALEXA);

      assertTrue(context.getAssistants().anyMatch(VoiceService.StartPlaceRequest.ASSISTANT_ALEXA::equals));

      verify();
   }

   private PlatformMessage createMessage() {
      return PlatformMessage.buildRequest(
         VoiceService.StartPlaceRequest.builder().withAssistant(VoiceService.StartPlaceRequest.ASSISTANT_ALEXA).build(),
         Address.bridgeAddress("ALXA"),
         Address.platformService(VoiceService.NAMESPACE)
      )
      .withPlaceId(placeId)
      .withPopulation(Population.NAME_GENERAL)
      .create();
   }

}

