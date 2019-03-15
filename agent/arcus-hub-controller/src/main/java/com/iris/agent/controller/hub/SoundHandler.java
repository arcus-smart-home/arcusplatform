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
package com.iris.agent.controller.hub;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.hal.IrisHal;
import com.iris.agent.hal.SounderMode;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubChimeCapability;
import com.iris.messages.capability.HubSoundsCapability;
import com.iris.protocol.ProtocolMessage;

enum SoundHandler implements PortHandler {
   INSTANCE;

    private static final Logger log = LoggerFactory.getLogger(SoundHandler.class);

    
   //private static final HubAttributesService.Attribute<String> source = HubAttributesService.persisted(String.class, HubSoundsCapability.ATTR_SOURCE, "");

   void start(Port parent) {
      parent.delegate(
         this,
         HubChimeCapability.chimeRequest.NAME,
         HubSoundsCapability.PlayToneRequest.NAME,
         HubSoundsCapability.QuietRequest.NAME
      );
   }

   @Nullable
   @Override
   public Object recv(Port port, PlatformMessage message) throws Exception {
      String type = message.getMessageType();
      switch (type) {
      case HubChimeCapability.chimeRequest.NAME:
         return handleChimeRequest();

      case  HubSoundsCapability.PlayToneRequest.NAME:
         return handleHubSoundsPlayTone(message);

      case  HubSoundsCapability.QuietRequest.NAME:
         IrisHal.setSounderMode(SounderMode.NO_SOUND);
         return HubSoundsCapability.QuietResponse.instance();

      default:
         return null;
      }
   }

   @Override
   public void recv(Port port, ProtocolMessage message) {
   }

   @Override
   public void recv(Port port, Object message) {
   }

   private Object handleChimeRequest() {
      IrisHal.setSounderMode(SounderMode.CHIME);
      return HubChimeCapability.chimeResponse.instance();
   }
   
   private Object handleHubSoundsPlayTone(PlatformMessage message) {
      MessageBody body = message.getValue();
      String mode = HubSoundsCapability.PlayToneRequest.getTone(body);
      int duration = HubSoundsCapability.PlayToneRequest.getDurationSec(body);

      SounderMode sounderMode = SounderMode.NO_SOUND;
      try {
          sounderMode = SounderMode.valueOf(mode);
      } catch (Exception e) {
          log.warn("No sound of type {}", mode );
          throw e;
      }
      
      IrisHal.setSounderMode(sounderMode,duration);
      return HubSoundsCapability.PlayToneResponse.instance();
   }
}

