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
package com.iris.alexa.shs.handlers.v3;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.AlexaUtil;
import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Header;
import com.iris.alexa.shs.ShsConfig;
import com.iris.alexa.shs.handlers.SmartHomeSkillHandler;
import com.iris.core.platform.PlatformBusClient;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.service.AlexaService;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.voice.VoiceBridgeConfig;
import com.iris.voice.VoiceBridgeMetrics;

@Singleton
public class SmartHomeSkillV3Handler implements SmartHomeSkillHandler {

   private static final Logger logger = LoggerFactory.getLogger(SmartHomeSkillV3Handler.class);

   private final ShsConfig config;
   private final PlatformBusClient busClient;
   private final ExecutorService executor;
   private final VoiceBridgeMetrics metrics;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public SmartHomeSkillV3Handler(
      ShsConfig config,
      PlatformMessageBus bus,
      @Named(VoiceBridgeConfig.NAME_EXECUTOR) ExecutorService executor,
      VoiceBridgeMetrics metrics,
      PlacePopulationCacheManager populationCacheMgr
   ) {
      this.config = config;
      this.executor = executor;
      this.busClient = new PlatformBusClient(bus, executor, ImmutableSet.of(AddressMatchers.equals(AlexaUtil.ADDRESS_BRIDGE)));
      this.metrics = metrics;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public boolean supports(AlexaMessage message) {
      return message.getHeader().isV3();
   }

   @Override
   public ListenableFuture<AlexaMessage> handle(AlexaMessage message, UUID placeId) {
      long startTime = System.nanoTime();
      Txfm txfm = Txfm.transformerFor(message);
      PlatformMessage platformMessage = txfm.txfmRequest(message, placeId, populationCacheMgr.getPopulationByPlaceId(placeId), (int) config.getRequestTimeoutMs());
      logger.debug("[{}] transformed to platform message [{}]", message, platformMessage);
      return Futures.transformAsync(
         busClient.request(platformMessage),
         (AsyncFunction<PlatformMessage, AlexaMessage>) input -> {
            metrics.timeServiceSuccess(platformMessage.getMessageType(), startTime);
            return Futures.immediateFuture(txfm.transformResponse(input, message.getHeader().getCorrelationToken()));
         },
         executor
      );
   }

   @Override
   public AlexaMessage transformException(AlexaMessage m, Throwable e) {
      logger.warn("an exception occured handling {}", m, e);
      MessageBody err = AlexaErrors.INTERNAL_ERROR;
      Throwable cause = e;
      if(e instanceof ExecutionException) {
         cause = e.getCause();
      }
      if(cause instanceof AlexaException) {
         err = ((AlexaException) cause).getErrorMessage();
      }

      Map<String, Object> payload = AlexaService.AlexaErrorEvent.getPayload(err);

      String type = AlexaService.AlexaErrorEvent.getType(err);

      Header h = Header.v3(
         m.getHeader().getMessageId(),
         AlexaInterfaces.RESPONSE_ERROR,
         errNamespace(type),
         m.getHeader().getCorrelationToken()
      );

      ImmutableMap.Builder<String, Object> payloadBuilder = ImmutableMap.<String, Object>builder()
         .put("type", type)
         .put("message", AlexaService.AlexaErrorEvent.getMessage(err));

      if(payload != null) {
         payloadBuilder.putAll(payload);
      }

      return new AlexaMessage(h, payloadBuilder.build());
   }

   private String errNamespace(String type) {
      switch(type) {
         case AlexaErrors.TYPE_THERMOSTAT_IS_OFF:
         case AlexaErrors.TYPE_REQUESTED_SETPOINTS_TOO_CLOSE:
            return AlexaInterfaces.ThermostatController.NAMESPACE;
         default:
            return AlexaInterfaces.RESPONSE_NAMESPACE;
      }
   }

   @Override
   public Optional<String> extractOAuthToken(AlexaMessage message) {
      Txfm txfm = Txfm.transformerFor(message);
      return txfm.extractRequestOauthToken(message);
   }
}

