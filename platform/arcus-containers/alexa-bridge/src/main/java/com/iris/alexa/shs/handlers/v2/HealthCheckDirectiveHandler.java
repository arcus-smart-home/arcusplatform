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

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.alexa.AlexaUtil;
import com.iris.alexa.bus.AlexaPlatformService;
import com.iris.alexa.message.v2.response.HealthCheckResponse;
import com.iris.alexa.shs.ShsConfig;
import com.iris.alexa.shs.ShsMetrics;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.services.PlatformConstants;
import com.iris.util.IrisUUID;

@Singleton
public class HealthCheckDirectiveHandler {

   private static final Logger logger = LoggerFactory.getLogger(HealthCheckDirectiveHandler.class);

   private static final Address STATUS_SERVICE = Address.platformService(PlatformConstants.SERVICE_STATUS);

   private final AlexaPlatformService platSvc;
   private final ShsConfig config;

   @Inject
   public HealthCheckDirectiveHandler(AlexaPlatformService platSvc, ShsConfig config) {
      this.platSvc = platSvc;
      this.config = config;
   }

   public ListenableFuture<HealthCheckResponse> handle() {
      ShsMetrics.incHealthCheck();
      SettableFuture<HealthCheckResponse> future = SettableFuture.create();
      try {
         Futures.addCallback(ping(), new FutureCallback<HealthCheckResponse>() {
            @Override
            public void onSuccess(HealthCheckResponse result) {
               future.set(result);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
               logger.error("alexa health check failed: ", t);
               future.set(badHealth());
            }
         }, MoreExecutors.directExecutor());
      } catch(Exception e) {
         logger.error("alexa health check failed: ", e);
         future.set(badHealth());
      }
      return future;
   }

   private ListenableFuture<HealthCheckResponse> ping() {
      PlatformMessage msg = PlatformMessage.buildRequest(MessageBody.ping(), AlexaUtil.ADDRESS_BRIDGE, STATUS_SERVICE)
            .withCorrelationId(IrisUUID.randomUUID().toString())
            .create();

      ListenableFuture<PlatformMessage> future = platSvc.request(
         msg,
         (pm) -> Objects.equals(msg.getCorrelationId(), pm.getCorrelationId()), config.getHealthCheckTimeoutSecs()
      );

      return Futures.transform(future, (Function<PlatformMessage, HealthCheckResponse>) input -> {
         HealthCheckResponse response = new HealthCheckResponse();
         response.setHealthy(true);
         response.setDescription("The system is currently healthy");
         return response;
      }, MoreExecutors.directExecutor());
   }

   private HealthCheckResponse badHealth() {
      HealthCheckResponse res = new HealthCheckResponse();
      res.setDescription("The system is currently not healthy");
      res.setHealthy(false);
      return res;
   }
}

