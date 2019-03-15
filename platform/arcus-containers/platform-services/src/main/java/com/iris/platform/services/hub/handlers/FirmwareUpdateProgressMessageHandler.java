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
package com.iris.platform.services.hub.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.model.Hub;

// TODO:  This should probably be an event not a message
@Singleton
public class FirmwareUpdateProgressMessageHandler implements ContextualRequestMessageHandler<Hub> {

   private static final Logger log = LoggerFactory.getLogger(FirmwareUpdateProgressMessageHandler.class);

   @Override
   public String getMessageType() {
      return HubAdvancedCapability.FirmwareUpgradeProcessEvent.NAME;
   }

   @Override
   public MessageBody handleRequest(Hub context, PlatformMessage msg) {
      log.debug("Updating:  [{}]", msg.getValue());
      return null;
   }
}

