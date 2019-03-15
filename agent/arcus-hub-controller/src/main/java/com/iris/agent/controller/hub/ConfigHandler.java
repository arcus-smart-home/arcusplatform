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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.agent.config.ConfigService;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubCapability;
import com.iris.protocol.ProtocolMessage;

enum ConfigHandler implements PortHandler {
   INSTANCE;

   void start(Port parent) {
      parent.delegate(this, HubCapability.GetConfigRequest.NAME, HubCapability.SetConfigRequest.NAME);
   }

   @Nullable
   @Override
   public Object recv(Port port, PlatformMessage message) throws Exception {
      String type = message.getMessageType();
      switch (type) {
      case HubCapability.GetConfigRequest.NAME:
         return handleGetConfig(port, message);

      case HubCapability.SetConfigRequest.NAME:
         return handleSetConfig(port, message);

      default:
         throw new Exception("hub cannot handle logging message: " + type);
      }
  }

   @Override
   public void recv(Port port, ProtocolMessage message) {
   }

   @Override
   public void recv(Port port, Object message) {
   }

   @Nullable
   private Object handleGetConfig(Port port, PlatformMessage message) {
      MessageBody body = message.getValue();

      String matching = HubCapability.GetConfigRequest.getMatching(body);
      Boolean withDefaults = HubCapability.GetConfigRequest.getDefaults(body);

      if (withDefaults == null) {
         withDefaults = Boolean.FALSE;
      }

      Map<String,String> config = ConfigService.all(withDefaults);
      if (matching != null) {
         Map<String,String> matches = new HashMap<>();

         Pattern p = Pattern.compile(matching);
         for (Map.Entry<String,String> entry : config.entrySet()) {
            String key = entry.getKey();
            Matcher m = p.matcher(key);
            if (m.matches()) {
               matches.put(key, entry.getValue());
            }
         }

         config = matches;
      }

      return HubCapability.GetConfigResponse.builder()
         .withConfig(config)
         .build();
   }

   @Nullable
   private Object handleSetConfig(Port port, PlatformMessage message) {
      MessageBody body = message.getValue();
      Map<String,String> configs = HubCapability.SetConfigRequest.getConfig(body);

      Map<String,String> failed = new HashMap<>();
      for (Map.Entry<String,String> entry : configs.entrySet()) {
         try {
            ConfigService.put(entry.getKey(), entry.getValue());
         } catch (Throwable th) {
            failed.put(entry.getKey(), entry.getValue());
         }
      }

      return HubCapability.SetConfigResponse.builder()
         .withFailed(failed)
         .build();
   }
}

