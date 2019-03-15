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

import java.util.Map;

import com.google.common.util.concurrent.ListenableFuture;
import com.iris.core.messaging.MessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;

/**
 * The {@link MessageBus} interface for {@link PlatformMessage}s. Used as a
 * injection point.
 */
public interface PlatformMessageBus extends RequestResponseMessageBus<PlatformMessage> {
   @Override
   default PlatformMessage getResponse(PlatformMessage request, MessageBody response) {
      return PlatformMessage.respondTo(request)
         .withPayload(response)
         .create();
   }

   default ListenableFuture<Void> emitValueChange(PlatformMessage request, Map<String, Object> attributes) {
      PlatformMessage message = PlatformMessage.buildEvent(request, Capability.EVENT_VALUE_CHANGE, attributes).create();
      return send(message);
   }
}

