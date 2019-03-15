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
package com.iris.voice.exec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.platform.PlatformBusClient;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.service.VoiceService;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.IrisUUID;
import com.iris.voice.VoiceConfig;
import com.iris.voice.context.VoiceContext;

import io.netty.util.HashedWheelTimer;

@Singleton
public class CommandExecutor {

   private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);

   private final ExecutorService executor;
   private final VoiceConfig config;
   private final HashedWheelTimer timeoutTimer;
   private final PlatformBusClient busClient;
   private final ResponseCompleter responseCompleter;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public CommandExecutor(
      PlatformMessageBus bus,
      @Named(VoiceConfig.NAME_EXECUTOR) ExecutorService executor,
      VoiceConfig config,
      @Named(VoiceConfig.NAME_TIMEOUT_TIMER) HashedWheelTimer timeoutTimer,
      ResponseCompleter responseCompleter,
      PlacePopulationCacheManager populationCacheMgr
   ) {
      this.executor = executor;
      this.config = config;
      this.timeoutTimer = timeoutTimer;
      this.responseCompleter = responseCompleter;
      this.populationCacheMgr = populationCacheMgr;
      this.busClient = new PlatformBusClient(bus, executor, ImmutableSet.of(AddressMatchers.equals(Address.platformService(VoiceService.NAMESPACE))));
   }

   public ListenableFuture<Pair<Model, Optional<PlatformMessage>>> execute(
      Address addr,
      MessageBody body,
      VoiceContext context,
      boolean cheat,
      boolean updateModelOnCheat
   ) {
      PlatformMessage reqMsg = PlatformMessage.buildRequest(body, Address.platformService(VoiceService.NAMESPACE), addr)
         .withPlaceId(context.getPlaceId())
         .withPopulation(populationCacheMgr.getPopulationByPlaceId(context.getPlaceId()))
         .withCorrelationId(IrisUUID.randomUUID().toString())
         .withTimeToLive((int) config.getExecutionPerReqTimeoutMs())
         .create();
      return execute(reqMsg, context, cheat, updateModelOnCheat);
   }

   private ListenableFuture<Pair<Model, Optional<PlatformMessage>>> execute(PlatformMessage reqMsg, VoiceContext context, boolean cheat, boolean updateModelOnCheat) {
      SettableFuture<Pair<Model, Optional<PlatformMessage>>> future = SettableFuture.create();

      Model m = context.getModelByAddress(reqMsg.getDestination());
      final Model clone = new SimpleModel(m);

      Map<String,Object> expected = new HashMap<>(reqMsg.getValue().getAttributes());
      if(cheat) {
         if(updateModelOnCheat) {
            clone.update(expected);
         }
      } else if(!expected.isEmpty()){
         ResponseCorrelator correlator = new ResponseCorrelator(reqMsg.getCorrelationId(), reqMsg.getValue().getAttributes().keySet(), future);
         responseCompleter.completeResponse(m.getAddress(), correlator);
      }

      final ListenableFuture<PlatformMessage> reqFuture = busClient.request(reqMsg);
      Futures.addCallback(
         reqFuture,
         new RequestCallback(cheat, future, clone),
         executor
      );

      timeoutTimer.newTimeout((timer) -> {
         if(!future.isDone()) {
            future.setException(new TimeoutException());
         }
      }, config.getExecutionPerReqTimeoutMs(), TimeUnit.MILLISECONDS);

      return future;
   }

   private static class RequestCallback implements FutureCallback<PlatformMessage> {
      private final boolean cheat;
      private final SettableFuture<Pair<Model, Optional<PlatformMessage>>> future;
      private final Model clone;

      RequestCallback(boolean cheat, SettableFuture<Pair<Model, Optional<PlatformMessage>>> future, Model clone) {
         this.cheat = cheat;
         this.future = future;
         this.clone = clone;
      }

      @Override public void onSuccess(PlatformMessage result) {
         if(cheat && !future.isDone()) {
            logger.trace("completing via cheat");
            future.set(new ImmutablePair<>(clone, Optional.of(result)));
         }
      }

      @Override public void onFailure(@NonNull Throwable t) {
         if(!future.isDone()) {
            future.setException(t);
         }
      }
   }
}

