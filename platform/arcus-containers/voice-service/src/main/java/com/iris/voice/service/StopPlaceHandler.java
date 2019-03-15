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

import com.google.inject.name.Named;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.service.VoiceService.StopPlaceRequest;
import com.iris.voice.context.VoiceContext;
import com.iris.voice.context.VoiceContextExecutorRegistry;
import com.iris.voice.context.VoiceDAO;
import com.iris.voice.proactive.ProactiveCredsDAO;

class StopPlaceHandler {

   private final VoiceDAO voiceDao;
   private final ProactiveCredsDAO proactiveCredsDAO;
   private final VoiceContextExecutorRegistry registry;

   StopPlaceHandler(VoiceDAO voiceDao, ProactiveCredsDAO proactiveCredsDAO, VoiceContextExecutorRegistry registry) {
      this.voiceDao = voiceDao;
      this.proactiveCredsDAO = proactiveCredsDAO;
      this.registry = registry;
   }

   @Request(value = StopPlaceRequest.NAME, service = true)
   public void handleStopPlace(
      Optional<VoiceContext> context,
      @Named(StopPlaceRequest.ATTR_ASSISTANT) String assistant
   ) {
      long startTime = System.nanoTime();
      try {
         if(!context.isPresent()) {
            VoiceServiceMetrics.timeHandlerSuccess(StopPlaceRequest.NAME, startTime);
            return;
         }
         voiceDao.removeAssistant(context.get().getPlaceId(), assistant);
         proactiveCredsDAO.remove(context.get().getPlaceId(), assistant);
         context.get().removeAssistant(assistant);
         if(!context.get().hasAssistants()) {
            registry.remove(context.get().getPlaceId());
         }
         VoiceServiceMetrics.timeHandlerSuccess(StopPlaceRequest.NAME, startTime);
      } catch(RuntimeException e) {
         VoiceServiceMetrics.timeHandlerFailure(StopPlaceRequest.NAME, startTime);
         throw e;
      }
   }
}

