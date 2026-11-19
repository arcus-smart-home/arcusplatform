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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

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
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.errors.Errors;
import com.iris.platform.rule.environment.PlaceEnvironmentExecutor;
import com.iris.platform.rule.environment.PlaceExecutorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SceneRequestHandler extends AbstractPlatformMessageListener {
   private static final Logger logger = LoggerFactory.getLogger(SceneRequestHandler.class);

   public static final String PROP_THREADPOOL = "service.scenehandler.threadpool";

   private final PlaceExecutorRegistry registry;
   // the last scene fired at each place, for the rate limit; one entry per place that fired a scene
   private final ConcurrentMap<UUID, PlaceSceneRecord> placeSceneRecordMap;
   // monotonic, so a wall clock step can't stretch or skip the rate limit
   private final LongSupplier clock;

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
      this(platformBus, executor, registry, System::nanoTime);
   }

   // tests pass their own clock
   SceneRequestHandler(
         PlatformMessageBus platformBus,
         Executor executor,
         PlaceExecutorRegistry registry,
         LongSupplier clock
   ) {
      super(platformBus, executor);
      this.registry = registry;
      this.placeSceneRecordMap = new ConcurrentHashMap<>();
      this.clock = clock;
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
      // counted only once the request passed the place check and reached a loaded place, so a
      // request for another place can't hold back this place's scenes, and only real places get
      // a record
      if(SceneCapability.FireRequest.NAME.equals(message.getMessageType())) {
         Integer scene = ((PlatformServiceAddress) message.getDestination()).getContextQualifier();
         if(scene != null && !tryRecordFire(placeId, scene)) {
            logger.info("Rate limited scene: [{}.{}]", placeId, scene);
            return Errors.invalidRequest("Scene has been run very recently. Please try again in a few seconds");
         }
      }
      executor.handleRequest(message);
      return null;
   }

   /**
    * Records a fire of the scene and returns true, unless the place fired the same scene within
    * the rate limit, which returns false and leaves the record alone. The check and the update are
    * one atomic step, so two fires at once can't both get through.
    */
   private boolean tryRecordFire(UUID placeId, int scene) {
      PlaceSceneRecord fire = new PlaceSceneRecord(scene, clock.getAsLong());
      PlaceSceneRecord recorded = placeSceneRecordMap.compute(placeId, (id, last) ->
         last != null && last.scene == scene && fire.firedAtNanos - last.firedAtNanos < TimeUnit.MILLISECONDS.toNanos(rateLimitTime)
            ? last
            : fire
      );
      return recorded == fire;
   }

   private static final class PlaceSceneRecord {
      private final int scene;
      private final long firedAtNanos;

      private PlaceSceneRecord(int scene, long firedAtNanos) {
         this.scene = scene;
         this.firedAtNanos = firedAtNanos;
      }
   }
}

