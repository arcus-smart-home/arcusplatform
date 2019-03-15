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
/**
 *
 */
package com.iris.driver;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.messages.Message;
import com.iris.messages.PlatformMessage;
import com.iris.protocol.ProtocolMessage;

/**
 *
 */
public class DeviceDriverEventHandler implements ContextualEventHandler<Message> {
   private final ContextualEventHandler<PlatformMessage> platformHandler;
   private final ContextualEventHandler<ProtocolMessage> protocolHandler;

   DeviceDriverEventHandler(
      ContextualEventHandler<PlatformMessage> platformMessageHandler,
      ContextualEventHandler<ProtocolMessage> protocolMessageHandler
   ) {
      this.platformHandler = platformMessageHandler;
      this.protocolHandler = protocolMessageHandler;
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, Message event) throws Exception {
      if(event == null) {
         return false;
      }
      if(event instanceof PlatformMessage) {
         return platformHandler.handleEvent(context, (PlatformMessage) event);
      }
      if(event instanceof ProtocolMessage) {
         return protocolHandler.handleEvent(context, (ProtocolMessage) event);
      }
      return false;
   }

   @Override
   public String toString() {
      return "DeviceDriverEventHandler [platformHandler="
            + platformHandler + ", protocolHandler="
            + protocolHandler + "]";
   }

}

