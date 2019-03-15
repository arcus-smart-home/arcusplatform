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
package com.iris.security.authz.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.PlatformMessage;

@Singleton
public class MessageFilterRegistry {

   private final Map<String,MessageFilter> filters = new HashMap<>();
   private final MessageFilter fallbackFilter = new DefaultMessageFilter();

   @Inject
   public MessageFilterRegistry(Set<MessageFilter> filters) {
      filters.forEach((mf) -> { mf.getSupportedMessageTypes().forEach((s) -> { this.filters.put(s, mf); }); });
   }

   public MessageFilter getMessageFilter(PlatformMessage message) {
      MessageFilter filter = filters.get(message.getMessageType());
      return filter == null ? fallbackFilter : filter;
   }
}

