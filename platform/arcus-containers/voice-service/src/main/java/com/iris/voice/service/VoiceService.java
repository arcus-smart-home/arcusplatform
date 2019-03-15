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
package com.iris.voice.service;

import java.util.concurrent.ExecutorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.alexa.AlexaUtil;
import com.iris.core.messaging.MessageListener;
import com.iris.core.platform.AbstractPlatformService;
import com.iris.core.platform.PlatformDispatcherFactory;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.google.Constants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.voice.VoiceConfig;
import com.iris.voice.context.VoiceContextExecutorRegistry;
import com.iris.voice.context.VoiceContextExecutorResolver;
import com.iris.voice.context.VoiceContextResolver;
import com.iris.voice.context.VoiceDAO;
import com.iris.voice.proactive.ProactiveCredsDAO;

@Singleton
public class VoiceService extends AbstractPlatformService {

   private static final Address ADDRESS = Address.platformService(com.iris.messages.service.VoiceService.NAMESPACE);

   private final MessageListener<PlatformMessage> dispatcher;

   @Inject
   public VoiceService(
      PlatformMessageBus platformBus,
      @Named(VoiceConfig.NAME_EXECUTOR) ExecutorService executor,
      VoiceDAO voiceDao,
      ProactiveCredsDAO proactiveCredsDao,
      VoiceContextExecutorRegistry registry,
      VoiceContextExecutorResolver executorResolver,
      VoiceContextResolver contextResolver,
      PlatformDispatcherFactory factory
   ) {
      super(platformBus, ADDRESS, executor);
      this.dispatcher = factory
         .buildDispatcher()
         .addArgumentResolverFactory(executorResolver)
         .addArgumentResolverFactory(contextResolver)
         .addAnnotatedHandler(new StartPlaceHandler(voiceDao, registry))
         .addAnnotatedHandler(new StopPlaceHandler(voiceDao, proactiveCredsDao,registry))
         .addAnnotatedHandler(new EventHandler())
         .addAnnotatedHandler(new RequestHandler())
         .build();
   }

   @Override
   protected void doHandleMessage(PlatformMessage message) {
      dispatcher.onMessage(message);
   }

   @Override
   protected void onStart() {
      super.onStart();
      addListeners(
         AddressMatchers.equals(ADDRESS),
         AddressMatchers.equals(Constants.SERVICE_ADDRESS),
         AddressMatchers.equals(AlexaUtil.ADDRESS_SERVICE),
         AddressMatchers.BROADCAST_MESSAGE_MATCHER
      );
   }
}

