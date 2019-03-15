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
package com.iris.voice.google;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.google.Commands;
import com.iris.google.Constants;
import com.iris.google.Transformers;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.type.GoogleCommand;
import com.iris.messages.type.GoogleCommandResult;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.voice.VoiceConfig;
import com.iris.voice.VoiceUtil;
import com.iris.voice.context.VoiceContext;
import com.iris.voice.context.VoiceContextExecutorRegistry;
import com.iris.voice.exec.CommandExecutor;

import io.netty.util.HashedWheelTimer;

@Singleton
public class GoogleCommandExecutor {

   private static final Logger logger = LoggerFactory.getLogger(GoogleCommandExecutor.class);

   private final ExecutorService executor;
   private final HashedWheelTimer timeoutTimer;
   private final GoogleConfig config;
   private final CommandExecutor commandExecutor;
   private final GoogleWhitelist whitelist;
   private final ProductCatalogManager prodCat;
   private final Provider<VoiceContextExecutorRegistry> registry;

   @Inject
   public GoogleCommandExecutor(
      @Named(VoiceConfig.NAME_EXECUTOR) ExecutorService executor,
      @Named(VoiceConfig.NAME_TIMEOUT_TIMER) HashedWheelTimer timeoutTimer,
      GoogleConfig config,
      CommandExecutor commandExecutor,
      GoogleWhitelist whitelist,
      ProductCatalogManager prodCat,
      Provider<VoiceContextExecutorRegistry> registry
   ) {
      this.executor = executor;
      this.timeoutTimer = timeoutTimer;
      this.config = config;
      this.commandExecutor = commandExecutor;
      this.whitelist = whitelist;
      this.prodCat = prodCat;
      this.registry = registry;
   }

   public ListenableFuture<List<GoogleCommandResult>> execute(VoiceContext context, List<GoogleCommand> commands) {
      Map<Address, Map<String, GoogleCommand>> collapsed = collapse(commands);
      int ops = collapsed.values().stream().mapToInt(Map::size).sum();
      ExecutionResult result = new ExecutionResult(ops, context.getPlaceId());
      timeoutTimer.newTimeout((timer) -> {
         if(!result.isDone()) {
            result.setException(new TimeoutException());
         }
      }, config.getExecutionTimeoutMs(), TimeUnit.MILLISECONDS);

      collapsed.forEach((a, m) -> m.values().forEach((c) -> executeCommand(result, context, a, c.getCommand(), c.getParams())));

      return result;
   }

   private Map<Address, Map<String, GoogleCommand>> collapse(List<GoogleCommand> commands) {
      Map<Address, Map<String, GoogleCommand>> collapsed = new HashMap<>();
      for(GoogleCommand command : commands) {
         GoogleMetrics.incCommand(command.getCommand());
         GoogleCommand remappedCommand = new GoogleCommand();
         remappedCommand.setCommand(remapName(command.getCommand()));
         remappedCommand.setParams(new HashMap<>(command.getParams()));
         for(String a : command.getAddresses()) {
            Map<String, GoogleCommand> devCommands = collapsed.computeIfAbsent(Address.fromString(a), (k) -> new HashMap<>());
            devCommands.compute(remappedCommand.getCommand(), (s, c) -> merge(c, remappedCommand));
         }
      }
      return collapsed;
   }

   private GoogleCommand merge(GoogleCommand existing, GoogleCommand incoming) {
      if(existing == null) {
         return incoming;
      }
      Map<String, Object> params = existing.getParams() == null ? new HashMap<>() : new HashMap<>(existing.getParams());
      Map<String, Object> incomingParams = incoming.getParams() == null ? ImmutableMap.of() : incoming.getParams();
      switch(existing.getCommand()) {
         case Commands.SetThermostat.name:
            // make sure to clear out set points from previous commands if they exist
            if(incomingParams.containsKey(Commands.TemperatureSetPoint.arg_temperature)) {
               params.remove(Commands.TemperatureSetRange.arg_temperature_high);
               params.remove(Commands.TemperatureSetRange.arg_temperature_low);
            } else if(incomingParams.containsKey(Commands.TemperatureSetRange.arg_temperature_high)) {
               params.remove(Commands.TemperatureSetPoint.arg_temperature);
            }
            // intentional fall through
         default:
            params.putAll(incomingParams);
            existing.setParams(params);
            break;
      }

      return existing;
   }

   private String remapName(String commandName) {
      switch(commandName) {
         case Commands.TemperatureSetPoint.name:
         case Commands.TemperatureSetRange.name:
         case Commands.SetMode.name:
            return Commands.SetThermostat.name;
         default:
            return commandName;
      }
   }

   private void executeCommand(
         ExecutionResult result,
         VoiceContext context,
         Address addr,
         String command,
         Map<String,Object> params
   ) {

      Model m = context.getModelByAddress(addr);
      boolean hubOffline = context.isHubOffline();
      try {
         Optional<MessageBody> body = Transformers.commandToMessageBody(m, hubOffline, command, params, whitelist.isWhitelisted(context.getPlaceId()), VoiceUtil.getProduct(prodCat, m));
         if(body.isPresent()) {
            logger.trace("command {}:[{}] transformed to message body: [{}]", command, params, body);
            Futures.addCallback(
               commandExecutor.execute(addr, body.get(), context, config.isSuccessCheatEnabled(), true),
               new FutureCallback<Pair<Model, Optional<PlatformMessage>>>() {

                  @Override public void onSuccess(Pair<Model, Optional<PlatformMessage>> r) {
                     result.onSuccess(addr, r.getLeft());
                  }

                  @Override
                  public void onFailure(@NonNull Throwable t) {
                     logger.warn("command execution [{}] failed", body.get(), t);
                     Throwable c = t.getCause();
                     if (c == null) {
                        c = t;
                     }
                     if (c instanceof TimeoutException) {
                        result.onError(addr, Constants.Error.TIMEOUT);
                     } else {
                        result.onError(addr, Constants.Error.UNKNOWN_ERROR);
                     }
                  }
               },
               executor
            );
         } else {
            result.onSuccess(addr, new SimpleModel(context.getModelByAddress(addr)));
         }
      } catch(ErrorEventException eee) {
         logger.warn("failed to execute command {} [{}] against {}", command, params, addr, eee);
         switch(eee.getCode()) {
            case Constants.Error.DEVICE_OFFLINE: result.onOffline(addr); break;
            default: result.onError(addr, eee.getCode());
         }
      }
   }

   private class ExecutionResult implements ListenableFuture<List<GoogleCommandResult>> {

      private final AtomicInteger pendingOps;
      private final UUID placeId;
      private final ConcurrentHashMap<String, Set<Address>> errors = new ConcurrentHashMap<>();
      private final ConcurrentMap<Address, Model> successes = new ConcurrentHashMap<>();
      private final Set<Address> pending = ConcurrentHashMap.newKeySet();
      private final Set<Address> offline = ConcurrentHashMap.newKeySet();
      private final SettableFuture<List<GoogleCommandResult>> delegate = SettableFuture.create();

      ExecutionResult(int pendingOps, UUID placeId) {
         this.placeId = placeId;
         this.pendingOps = new AtomicInteger(pendingOps);
      }

      void onSuccess(Address devAddr, Model m) {
         successes.put(devAddr, m);
         completeIfNecessary();
      }

      void onOffline(Address devAddr) {
         offline.add(devAddr);
         completeIfNecessary();
      }

      void onError(Address devAddr, String errorCode) {
         errors.computeIfAbsent(errorCode, k -> new HashSet<>()).add(devAddr);
         completeIfNecessary();
      }

      void setException(Throwable t) {
         delegate.setException(t);
      }

      private void completeIfNecessary() {
         if (delegate.isDone()) {
            return;
         }
         int remaining = pendingOps.decrementAndGet();
         if (remaining == 0) {
            complete();
         }
      }

      private void complete() {
         ImmutableList.Builder<GoogleCommandResult> results = ImmutableList.builder();
         // complete would be called via a callback, so get the context from the registry just in case the cache entry
         // was flushed
         VoiceContext context = registry.get().get(placeId).context();
         if (!successes.isEmpty()) {
            Map<GoogleCommandResult, Set<String>> successMap = new HashMap<>();
            successes.forEach((a, b) -> {
               GoogleCommandResult res = GoogleCommandResultExt.success(Transformers.modelToStateMap(b, context.isHubOffline(), whitelist.isWhitelisted(placeId), VoiceUtil.getProduct(prodCat, b)));
               successMap.computeIfAbsent(res, k -> new HashSet<>()).add(a.getRepresentation());
            });
            successMap.forEach((r, a) -> {
               r.setIds(a);
               results.add(r);
            });
         }
         addAddressSet(results, pending, GoogleCommandResultExt.pending());
         addAddressSet(results, offline, GoogleCommandResultExt.offline());
         if (!errors.isEmpty()) {
            errors.forEach((k, v) -> addAddressSet(results, v, GoogleCommandResultExt.error(k)));
         }

         delegate.set(results.build());
      }

      private void addAddressSet(ImmutableList.Builder<GoogleCommandResult> results, Set<Address> addrs, GoogleCommandResult res) {
         if (!addrs.isEmpty()) {
            GoogleCommandResult result = GoogleCommandResultExt.copy(res);
            result.setIds(addrs.stream().map(Address::getRepresentation).collect(Collectors.toSet()));
            results.add(result);
         }
      }

      @Override
      public void addListener(@NonNull Runnable listener, @NonNull Executor executor) {
         delegate.addListener(listener, executor);
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
         return delegate.cancel(mayInterruptIfRunning);
      }

      @Override
      public boolean isCancelled() {
         return delegate.isCancelled();
      }

      @Override
      public boolean isDone() {
         return delegate.isDone();
      }

      @Override
      public List<GoogleCommandResult> get() throws InterruptedException, ExecutionException {
         return delegate.get();
      }

      @Override
      public List<GoogleCommandResult> get(long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
         return delegate.get(timeout, unit);
      }
   }

   private static class GoogleCommandResultExt extends GoogleCommandResult {

      private static GoogleCommandResult create(String status) {
         GoogleCommandResultExt result = new GoogleCommandResultExt();
         result.setStatus(status);
         return result;
      }

      static GoogleCommandResult success(Map<String,Object> states) {
         GoogleCommandResult result = create(Constants.Status.SUCCESS);
         result.setStates(states);
         return result;
      }

      static GoogleCommandResult error(String errorCode) {
         GoogleCommandResult result = create(Constants.Status.ERROR);
         result.setErrorCode(errorCode);
         return result;
      }

      private static final GoogleCommandResult OFFLINE = error(Constants.Error.DEVICE_OFFLINE);
      private static final GoogleCommandResult PENDING = create(Constants.Status.PENDING);

      static GoogleCommandResult offline() {
         return OFFLINE;
      }

      static GoogleCommandResult pending() {
         return PENDING;
      }

      static GoogleCommandResult copy(GoogleCommandResult result) {
         GoogleCommandResultExt copy = new GoogleCommandResultExt();
         copy.setErrorCode(result.getErrorCode());
         copy.setStatus(result.getStatus());

         if(result.getStates() != null) {
            copy.setStates(ImmutableMap.copyOf(result.getStates()));
         }

         if(result.getIds() != null) {
            copy.setIds(ImmutableSet.copyOf(result.getIds()));
         }

         return copy;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         GoogleCommandResultExt result = (GoogleCommandResultExt) o;

         if (getIds() != null ? !getIds().equals(result.getIds()) : result.getIds() != null) return false;
         if (getStatus() != null ? !getStatus().equals(result.getStatus()) : result.getStatus() != null) return false;
         if (getStates() != null ? !getStates().equals(result.getStates()) : result.getStates() != null) return false;
         return getErrorCode() != null ? getErrorCode().equals(result.getErrorCode()) : result.getErrorCode() == null;
      }

      @Override
      public int hashCode() {
         int result = getIds() != null ? getIds().hashCode() : 0;
         result = 31 * result + (getStatus() != null ? getStatus().hashCode() : 0);
         result = 31 * result + (getStates() != null ? getStates().hashCode() : 0);
         result = 31 * result + (getErrorCode() != null ? getErrorCode().hashCode() : 0);
         return result;
      }

   }

}

