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
package com.iris.platform.rule.service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.platform.AbstractPlatformMessageListener;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.errors.Errors;
import com.iris.platform.rule.environment.PlaceEnvironmentExecutor;
import com.iris.platform.rule.environment.PlaceExecutorRegistry;

@Singleton
public class SceneRequestHandler extends AbstractPlatformMessageListener {
   public static final String PROP_THREADPOOL = "service.scenehandler.threadpool";
   
   private final PlaceExecutorRegistry registry;
   
   @Inject
   public SceneRequestHandler(
         PlatformMessageBus platformBus,
         @Named(PROP_THREADPOOL) Executor executor,
         PlaceExecutorRegistry registry
   ) {
      super(platformBus, executor);
      this.registry = registry;
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#onStart()
    */
   @Override
   protected void onStart() {
      super.onStart();
      // add listeners
      addListeners(AddressMatchers.platformService(MessageConstants.SERVICE, SceneCapability.NAMESPACE));
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.AbstractPlatformMessageListener#handleRequestAndSendResponse(com.iris.messages.PlatformMessage)
    */
   @Override
   protected void handleRequestAndSendResponse(PlatformMessage message) {
      if(Address.ZERO_UUID.equals(message.getDestination().getId())) {
         return;
      }
      getMessageBus().invokeAndSendIfNotNull(message, () -> Optional.ofNullable(dispatch(message)));
   }
   
   protected MessageBody dispatch(PlatformMessage message) {
      UUID placeId = (UUID) message.getDestination().getId();
      Errors.assertPlaceMatches(message, placeId);
      PlaceEnvironmentExecutor executor =
         registry
            .getExecutor(placeId)
            .orNull();
      if(executor == null) {
         return Errors.notFound(message.getDestination());
      }
      executor.handleRequest(message);
      return null;
   }
   
}

