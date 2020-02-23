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
package com.iris.alexa.shs.handlers.v2;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.alexa.AlexaUtil;
import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Header;
import com.iris.alexa.message.v2.Payload;
import com.iris.alexa.message.v2.error.BridgeOfflineError;
import com.iris.alexa.message.v2.error.DriverInternalError;
import com.iris.alexa.message.v2.error.ErrorPayload;
import com.iris.alexa.message.v2.error.ErrorPayloadException;
import com.iris.alexa.message.v2.error.ExpiredAccessTokenError;
import com.iris.alexa.message.v2.error.InvalidAccessTokenError;
import com.iris.alexa.message.v2.error.NotSupportedInCurrentModeError;
import com.iris.alexa.message.v2.error.RateLimitExceededError;
import com.iris.alexa.message.v2.error.TargetFirmwareOutdatedError;
import com.iris.alexa.message.v2.error.TargetHardwareMalfunctionError;
import com.iris.alexa.message.v2.error.TargetOfflineError;
import com.iris.alexa.message.v2.error.UnableToGetValueError;
import com.iris.alexa.message.v2.error.UnableToSetValueError;
import com.iris.alexa.message.v2.error.UnsupportedOperationError;
import com.iris.alexa.message.v2.error.UnsupportedTargetError;
import com.iris.alexa.message.v2.error.UnsupportedTargetSettingError;
import com.iris.alexa.message.v2.error.UnwillingToSetValueError;
import com.iris.alexa.message.v2.error.ValueOutOfRangeError;
import com.iris.alexa.message.v2.request.HealthCheckRequest;
import com.iris.alexa.message.v2.request.RequestPayload;
import com.iris.alexa.message.v2.response.ResponsePayload;
import com.iris.alexa.shs.ShsConfig;
import com.iris.alexa.shs.ShsMetrics;
import com.iris.alexa.shs.handlers.SmartHomeSkillHandler;
import com.iris.core.platform.PlatformBusClient;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaTemperature;
import com.iris.messages.type.AlexaValidRange;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.voice.VoiceBridgeConfig;
import com.iris.voice.VoiceBridgeMetrics;

@Singleton
public class SmartHomeSkillV2Handler implements SmartHomeSkillHandler {

   private static final Logger logger = LoggerFactory.getLogger(SmartHomeSkillV2Handler.class);

   private static final String CODE_BUSY = "DEVICE_BUSY";
   private static final String CODE_LOW_BATTERY = "LOW_BATTERY";

   private final ShsConfig config;
   private final PlatformBusClient busClient;
   private final ExecutorService executor;
   private final HealthCheckDirectiveHandler healthCheckHandler;
   private final VoiceBridgeMetrics metrics;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public SmartHomeSkillV2Handler(
      ShsConfig config,
      PlatformMessageBus bus,
      @Named(VoiceBridgeConfig.NAME_EXECUTOR) ExecutorService executor,
      HealthCheckDirectiveHandler healthCheckHandler,
      VoiceBridgeMetrics metrics,
      PlacePopulationCacheManager populationCacheMgr
   ) {
      this.config = config;
      this.busClient = new PlatformBusClient(bus, executor, ImmutableSet.of(AddressMatchers.equals(AlexaUtil.ADDRESS_BRIDGE)));
      this.executor = executor;
      this.healthCheckHandler = healthCheckHandler;
      this.metrics = metrics;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public boolean supports(AlexaMessage message) {
      return message.getHeader().isV2();
   }

   @Override
   public ListenableFuture<AlexaMessage> handle(AlexaMessage message, UUID placeId) {

      if(message.getPayload() instanceof HealthCheckRequest) {
         ShsMetrics.incHealthCheck();
         return Futures.transform(
            healthCheckHandler.handle(),
            (Function<ResponsePayload, AlexaMessage>) input -> {
               Preconditions.checkNotNull(input, "input cannot be null");
               Header h = Header.v2(message.getHeader().getMessageId(), input.getName(), input.getNamespace());
               return new AlexaMessage(h, input);
            },
            MoreExecutors.directExecutor()
         );
      }

      if(message.getPayload() instanceof RequestPayload) {
         ShsMetrics.incShsRequest();
         long startTime = System.nanoTime();
         Txfm txfm = Txfm.transformerFor(message);
         PlatformMessage platformMessage = txfm.txfmRequest(message, placeId, populationCacheMgr.getPopulationByPlaceId(placeId), (int) config.getRequestTimeoutMs());
         logger.debug("[{}] transformed to platform message [{}]", message, platformMessage);
         return Futures.transformAsync(
            busClient.request(platformMessage),
            (AsyncFunction<PlatformMessage, AlexaMessage>) input -> {
               metrics.timeServiceSuccess(platformMessage.getMessageType(), startTime);
               return Futures.immediateFuture(txfm.transformResponse(message, input));
            },
            executor
         );
      } else {
         logger.warn("received non-directive request from Alexa {}", message);
         ShsMetrics.incNonDirective();
         return Futures.immediateFailedFuture(new AlexaException(AlexaErrors.unsupportedDirective(message.getHeader().getName())));
      }
   }

   @Override
   public AlexaMessage transformException(AlexaMessage message, Throwable e) {
      logger.warn("an exception occured handling {}", message, e);
      Payload p = new DriverInternalError();
      Throwable cause = e;
      if(e instanceof ExecutionException) {
         cause = e.getCause();
      }
      if(cause instanceof ErrorPayloadException) {
         p = ((ErrorPayloadException) cause).getPayload();
      }
      if(cause instanceof AlexaException) {
         p = transformAlexaError(message, ((AlexaException) cause).getErrorMessage());
      }

      String namespace = p.getNamespace();
      if(p instanceof BridgeOfflineError && Payload.QUERY_NAMESPACE.equals(message.getHeader().getNamespace())) {
         namespace = Payload.QUERY_NAMESPACE;
      }

      Header h = Header.v2(message.getHeader().getMessageId(), p.getName(), namespace);
      return new AlexaMessage(h, p);
   }

   @Override
   public Optional<String> extractOAuthToken(AlexaMessage message) {
      if(!(message.getPayload() instanceof  RequestPayload)) {
         return Optional.empty();
      }
      return Optional.ofNullable(((RequestPayload) message.getPayload()).getAccessToken());
   }


   @SuppressWarnings("unchecked")
   private ErrorPayload transformAlexaError(AlexaMessage message, MessageBody body) {
      String type = AlexaService.AlexaErrorEvent.getType(body);
      switch(type) {
         case AlexaErrors.TYPE_BRIDGE_UNREACHABLE:
            return new BridgeOfflineError();
         case AlexaErrors.TYPE_DUAL_SETPOINTS_UNSUPPORTED:
            return new UnsupportedTargetSettingError();
         case AlexaErrors.TYPE_ENDPOINT_BUSY:
            if(Payload.QUERY_NAMESPACE.equals(message.getHeader().getNamespace())) {
               return new UnableToGetValueError(CODE_BUSY, "Device is busy and cannot respond.");
            }
            return new UnableToSetValueError(CODE_BUSY, "Device is busy and cannot be controlled.");
         case AlexaErrors.TYPE_ENDPOINT_LOW_POWER:
            if(Payload.QUERY_NAMESPACE.equals(message.getHeader().getNamespace())) {
               return new UnableToGetValueError(CODE_LOW_BATTERY, "Device's battery is too low to respond.");
            }
            return new UnableToSetValueError(CODE_LOW_BATTERY, "Device's battery is too low and cannot be controlled.");
         case AlexaErrors.TYPE_ENDPOINT_UNREACHABLE:
            return new TargetOfflineError();
         case AlexaErrors.TYPE_EXPIRED_AUTHORIZATION_CREDENTIAL:
            return new ExpiredAccessTokenError();
         case AlexaErrors.TYPE_FIRMWARE_OUT_OF_DATE:
            return new TargetFirmwareOutdatedError();
         case AlexaErrors.TYPE_HARDWARE_MALFUNCTION:
            return new TargetHardwareMalfunctionError();
         case AlexaErrors.TYPE_INTERNAL_ERROR:
            return new DriverInternalError();
         case AlexaErrors.TYPE_INVALID_AUTHORIZATION_CREDENTIAL:
            return new InvalidAccessTokenError();
         case AlexaErrors.TYPE_INVALID_DIRECTIVE:
            return new UnsupportedOperationError();
         case AlexaErrors.TYPE_INVALID_VALUE:
            return new UnsupportedTargetSettingError();
         case AlexaErrors.TYPE_NO_SUCH_ENDPOINT:
            return new UnsupportedTargetError();
         case AlexaErrors.TYPE_NOT_SUPPORTED_IN_CURRENT_MODE:
            String mode = (String) AlexaService.AlexaErrorEvent.getPayload(body).getOrDefault("currentDeviceMode", "OTHER");
            return new NotSupportedInCurrentModeError(mode);
         case AlexaErrors.TYPE_RATE_LIMIT_EXCEEDED:
            return new RateLimitExceededError();
         case AlexaErrors.TYPE_REQUESTED_SETPOINTS_TOO_CLOSE:
            return new UnsupportedTargetSettingError();
         case AlexaErrors.TYPE_TEMPERATURE_VALUE_OUT_OF_RANGE: {
            Map<String, Object> payload = AlexaService.AlexaErrorEvent.getPayload(body);
            AlexaValidRange range = new AlexaValidRange((Map<String, Object>) payload.get(AlexaErrors.PROP_VALIDRANGE));
            AlexaTemperature min = new AlexaTemperature((Map<String, Object>) range.getMinimumValue());
            AlexaTemperature max = new AlexaTemperature((Map<String, Object>) range.getMaximumValue());
            return new ValueOutOfRangeError(min.getValue(), max.getValue());
         }
         case AlexaErrors.TYPE_THERMOSTAT_IS_OFF:
         case AlexaErrors.TYPE_UNWILLING_TO_SET_VALUE:
            return new UnwillingToSetValueError("ThermostatIsOff", "The thermostat is off and cannot be turned on to complete request.");
         case AlexaErrors.TYPE_TRIPLE_SETPOINTS_UNSUPPORTED:
            return new UnsupportedTargetSettingError();
         case AlexaErrors.TYPE_UNSUPPORTED_THERMOSTAT_MODE:
            return new UnsupportedTargetSettingError();
         case AlexaErrors.TYPE_VALUE_OUT_OF_RANGE:
            Map<String, Object> payload = AlexaService.AlexaErrorEvent.getPayload(body);
            AlexaValidRange range = new AlexaValidRange((Map<String, Object>) payload.get(AlexaErrors.PROP_VALIDRANGE));
            Double min = ((Number) range.getMinimumValue()).doubleValue();
            Double max = ((Number) range.getMaximumValue()).doubleValue();
            return new ValueOutOfRangeError(min, max);
         default:
            return new DriverInternalError();
      }
   }
}

