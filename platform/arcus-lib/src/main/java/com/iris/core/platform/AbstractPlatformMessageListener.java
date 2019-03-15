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
package com.iris.core.platform;

import java.util.concurrent.Executor;

import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;

public abstract class AbstractPlatformMessageListener extends AbstractMessageListener<PlatformMessage> {
   protected AbstractPlatformMessageListener(RequestResponseMessageBus<PlatformMessage> bus) {
      super(bus);
   }
   
   protected AbstractPlatformMessageListener(RequestResponseMessageBus<PlatformMessage> bus, String name, int maxThreads, long threadTimeoutMs) {
      super(bus, name, maxThreads, threadTimeoutMs);
   }
   
   protected AbstractPlatformMessageListener(RequestResponseMessageBus<PlatformMessage> bus, Executor executor) {
      super(bus, executor);
   }

   @Override
   protected boolean isError(PlatformMessage message) {
      return message.isError();
   }

   @Override
   protected boolean isRequest(PlatformMessage message) {
      return message.isRequest();
   }

   @Override
   protected MessageBody getMessageBody(PlatformMessage message) {
      return message.getValue();
   }

   protected void sendPlatformMessage(PlatformMessage message) {
      sendMessage(message);
   }
}

