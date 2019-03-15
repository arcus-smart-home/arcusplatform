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
package com.iris.core.platform;

import java.util.Optional;

import com.iris.messages.PlatformMessage;

/**
 * Base class that adds common handler patterns for different types of
 * platform messages and exposes send.
 * @deprecated Use AbstractPlatformMessageListener directly instead
 */
@Deprecated
public class AbstractPlatformMessageHandler extends AbstractPlatformMessageListener {
   protected AbstractPlatformMessageHandler(PlatformMessageBus bus) {
      super(bus);
   }
   
   protected boolean isAsync() {
      return false;
   }

   protected void handleRequestAndSendResponse(PlatformMessage message) {
      if(isAsync()) {
         getMessageBus().invokeAndSendIfNotNull(message, () -> Optional.ofNullable(handleRequest(message)));
      }
      else {
         getMessageBus().invokeAndSendResponse(message, () -> handleRequest(message));
      }
   }

}

