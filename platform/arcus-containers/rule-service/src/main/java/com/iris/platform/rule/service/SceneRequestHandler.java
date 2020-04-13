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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

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
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.services.PlatformConstants;
import com.iris.platform.rule.environment.PlaceEnvironmentExecutor;
import com.iris.platform.rule.environment.PlaceExecutorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SceneRequestHandler extends AbstractPlatformMessageListener {
   private static final Logger logger = LoggerFactory.getLogger(SceneRequestHandler.class);

   public static final String PROP_THREADPOOL = "service.scenehandler.threadpool";

   private final PlaceExecutorRegistry registry;
   private final Map<String, SceneRequestHandler.PlaceSceneRecord> placeSceneRecordMap;

   // TODO: move to some config object?
   @Inject(optional = true)
   @Named("service.scene.ratelimit.time.ms")
   private long rateLimitTime = TimeUnit.SECONDS.toMillis(15);

   @Inject
   public SceneRequestHandler(
         PlatformMessageBus platformBus,
         @Named(PROP_THREADPOOL) Executor executor,
         PlaceExecutorRegistry registry
   ) {
      super(platformBus, executor);
      this.registry = registry;
      this.placeSceneRecordMap = new HashMap<>();
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

      if (message.getMessageType().equals(SceneCapability.FireRequest.NAME)) {
         PlatformServiceAddress m = (PlatformServiceAddress) message.getDestination();

         // the timeframe for what is acceptable
         Date past = new Date(System.currentTimeMillis() - rateLimitTime);

         SceneRequestHandler.PlaceSceneRecord psr = placeSceneRecordMap.get(m.getId().toString());
         if (psr == null) {
            psr = new SceneRequestHandler.PlaceSceneRecord (0, 0);
         }

         logger.trace("size of placeSceneMap=[{}]", placeSceneRecordMap.size());
         logger.trace("last record of place scene: scene=[{}] , ts=[{}]", psr.scene, psr.lastFireAtTs);

         if (psr.scene == m.getContextQualifier() && psr.lastFireAtTs >= past.getTime()) {
            logger.info("Rate limited scene: [{}.{}]", m.getId(), m.getContextQualifier());
            getMessageBus().sendResponse(message, Errors.invalidRequest("Scene has been run very recently. Please try again in a few seconds"));
            return; // Rate limited.
         }

         psr.lastFireAtTs = new Date().getTime();
         psr.scene = m.getContextQualifier();
         placeSceneRecordMap.put(m.getId().toString(), psr);
      }

      getMessageBus().invokeAndSendIfNotNull(message, () -> Optional.ofNullable(dispatch(message)));
   }

   @Override
   protected void handleEvent(PlatformMessage message) {
      if (Capability.EVENT_DELETED.equals(message.getMessageType())) {
         Address source = message.getSource();
         if (source instanceof PlatformServiceAddress && PlatformConstants.SERVICE_PLACES.equals(source.getGroup())) {
            onPlaceDeleted((UUID) source.getId());
            return;
         }
      }
   }

   private void onPlaceDeleted(UUID placeId) {
      // remove records of recently fired scene for rate-limiting.
      placeSceneRecordMap.remove(placeId);
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

   private class PlaceSceneRecord {
      private int scene;
      private long lastFireAtTs;

      private PlaceSceneRecord(int scene, long lastFireAtTs) {
         this.scene = scene;
         this.lastFireAtTs = lastFireAtTs;
      }
   }
}

