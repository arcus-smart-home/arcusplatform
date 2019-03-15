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
package com.iris.voice.oauth;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.platform.PlatformBusClient;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.service.VoiceService;
import com.iris.oauth.place.PlaceSelectionHandler;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.IrisUUID;
import com.iris.voice.VoiceBridgeConfig;
import com.iris.voice.VoiceBridgeMetrics;

@Singleton
public class VoicePlaceSelectionHandler implements PlaceSelectionHandler {

   private static final Logger logger = LoggerFactory.getLogger(VoicePlaceSelectionHandler.class);

   private final PlatformBusClient busClient;
   private final VoiceBridgeConfig config;
   private final Address bridgeAddress;
   private final VoiceBridgeMetrics metrics;
   private final String assistant;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public VoicePlaceSelectionHandler(
      PlatformMessageBus bus,
      @Named(VoiceBridgeConfig.NAME_EXECUTOR) ExecutorService executor,
      VoiceBridgeConfig config,
      @Named(VoiceBridgeConfig.NAME_BRIDGEADDRESS) Address bridgeAddress,
      VoiceBridgeMetrics metrics,
      @Named(VoiceBridgeConfig.NAME_BRIDGEASSISTANT) String assistant,
      PlacePopulationCacheManager populationCacheMgr
   ) {
      this.busClient = new PlatformBusClient(bus, executor, ImmutableSet.of(AddressMatchers.equals(bridgeAddress)));
      this.bridgeAddress = bridgeAddress;
      this.config = config;
      this.metrics = metrics;
      this.assistant = assistant;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public void placeAuthorized(UUID placeId) {
      long startTime = System.nanoTime();
      PlatformMessage msg = PlatformMessage.buildRequest(
         VoiceService.StartPlaceRequest.builder().withAssistant(assistant).build(),
         bridgeAddress,
         Address.platformService(VoiceService.NAMESPACE)
      )
      .withCorrelationId(IrisUUID.randomUUID().toString())
      .withPlaceId(placeId)
      .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
      .withTimeToLive((int) config.getRequestTimeoutMs())
      .create();
      try {
         busClient.request(msg).get(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
         metrics.timeServiceSuccess(msg.getMessageType(), startTime);
      } catch(Exception e) {
         logger.warn("failed to start place {}", placeId, e);
         metrics.timeServiceFailure(msg.getMessageType(), startTime);
      }
   }

   @Override
   public void placeDeauthorized(UUID placeId) {
      long startTime = System.nanoTime();
      PlatformMessage msg = PlatformMessage.buildRequest(
         VoiceService.StopPlaceRequest.builder().withAssistant(assistant).build(),
         bridgeAddress,
         Address.platformService(VoiceService.NAMESPACE)
      )
      .withCorrelationId(IrisUUID.randomUUID().toString())
      .withPlaceId(placeId)
      .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
      .withTimeToLive((int) config.getRequestTimeoutMs())
      .create();
      try {
         busClient.request(msg).get(config.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
         metrics.timeServiceSuccess(msg.getMessageType(), startTime);
      } catch(Exception e) {
         logger.warn("failed to stop place {}", placeId, e);
         metrics.timeServiceFailure(msg.getMessageType(), startTime);
      }
   }
}

