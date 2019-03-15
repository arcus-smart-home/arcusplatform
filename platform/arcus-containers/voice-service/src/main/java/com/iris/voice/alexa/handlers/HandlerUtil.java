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
package com.iris.voice.alexa.handlers;

import com.iris.messages.service.VoiceService;
import com.iris.voice.context.VoiceContext;
import com.iris.voice.context.VoiceDAO;

enum HandlerUtil {
   ;

   // helper to make sure that alexa gets added as a supported assistant if it isn't already.  V2 customers from
   // before voice-service won't be marked initially so this ensures they will be if a request is handled.
   static void markAssistantIfNecessary(VoiceContext context, VoiceDAO voiceDao) {
      if(context.addAssistant(VoiceService.StartPlaceRequest.ASSISTANT_ALEXA)) {
         voiceDao.recordAssistant(context.getPlaceId(), VoiceService.StartPlaceRequest.ASSISTANT_ALEXA);
      }
   }
}

