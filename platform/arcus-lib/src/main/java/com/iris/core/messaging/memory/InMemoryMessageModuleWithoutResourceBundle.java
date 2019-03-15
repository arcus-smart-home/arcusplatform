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
package com.iris.core.messaging.memory;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.core.messaging.MessagesModule;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ProtocolMessageBus;
import com.netflix.governator.annotations.Modules;

/**
 * 
 */
@Modules(include = { MessagesModule.class, AttributeMapTransformModule.class })
public class InMemoryMessageModuleWithoutResourceBundle extends AbstractModule {

   @Override
   protected void configure() {
      bind(InMemoryPlatformMessageBus.class).in(Singleton.class);
      bind(InMemoryProtocolMessageBus.class).in(Singleton.class);
      bind(PlatformMessageBus.class).to(InMemoryPlatformMessageBus.class);
      bind(ProtocolMessageBus.class).to(InMemoryProtocolMessageBus.class);
   }
   
}

