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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.google.Constants.Error;
import com.iris.google.Predicates;
import com.iris.google.Transformers;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.errors.Errors;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.service.GoogleService;
import com.iris.messages.service.VoiceService;
import com.iris.messages.type.GoogleCommand;
import com.iris.messages.type.GoogleCommandResult;
import com.iris.messages.type.GoogleDevice;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.voice.VoiceConfig;
import com.iris.voice.VoiceUtil;
import com.iris.voice.context.VoiceContext;
import com.iris.voice.google.homegraph.HomeGraphAPI;
import com.iris.voice.proactive.ProactiveCreds;
import com.iris.voice.proactive.ProactiveCredsDAO;

@Singleton
public class RequestHandler {

   private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

   private final PlatformMessageBus bus;
   private final ExecutorService executor;
   private final GoogleCommandExecutor commandExecutor;
   private final GoogleWhitelist whitelist;
   private final ProductCatalogManager prodCat;
   private final ProactiveCredsDAO proactiveCredsDAO;
   private final HomeGraphAPI homegraph;
   private final GoogleConfig config;

   @Inject
   public RequestHandler(
      PlatformMessageBus bus,
      @Named(VoiceConfig.NAME_EXECUTOR) ExecutorService executor,
      GoogleCommandExecutor commandExecutor,
      GoogleWhitelist whitelist,
      ProductCatalogManager prodCat,
      ProactiveCredsDAO proactiveCredsDAO,
      HomeGraphAPI homegraph,
      GoogleConfig config
   ) {
      this.bus = bus;
      this.executor = executor;
      this.commandExecutor = commandExecutor;
      this.whitelist = whitelist;
      this.prodCat = prodCat;
      this.proactiveCredsDAO = proactiveCredsDAO;
      this.homegraph = homegraph;
      this.config = config;
   }

   @Request(value = GoogleService.SyncRequest.NAME, service = true)
   public MessageBody handleSync(VoiceContext context) {
      long startTime = System.nanoTime();
      try {
         Optional<ProactiveCreds> optCreds = context.getProactiveCreds(VoiceService.StartPlaceRequest.ASSISTANT_GOOGLE);
         if(!optCreds.isPresent()) {
            ProactiveCreds creds = new ProactiveCreds(context.getPlaceId().toString());
            proactiveCredsDAO.upsert(context.getPlaceId(), VoiceService.StartPlaceRequest.ASSISTANT_GOOGLE, creds);
            context.updateProactiveCreds(VoiceService.StartPlaceRequest.ASSISTANT_GOOGLE, creds);
            optCreds = Optional.of(creds);
         }
         boolean whitelisted = whitelist.isWhitelisted(context.getPlaceId());
         MessageBody body = GoogleService.SyncResponse.builder()
            .withDevices(
               context.streamSupported(
                  model -> Predicates.isSupportedModel(model, whitelisted, VoiceUtil.getProduct(prodCat, model)),
                  model -> Transformers.modelToDevice(model, whitelisted, VoiceUtil.getProduct(prodCat, model), this.config.isReportStateEnabled())
               )
               .map(GoogleDevice::toMap)
               .collect(Collectors.toList())
            )
            .withUserAgentId(optCreds.get().getAccess())
            .build();
         GoogleMetrics.timeHandlerSuccess(GoogleService.SyncRequest.NAME, startTime);
         this.homegraph.sendDelayedReportState(context); // We're sending a SYNC response, so we are required to send a Report State post.  It must be delayed to give the SYNC a chance to succeed.
         return body;
      } catch(RuntimeException e) {
         GoogleMetrics.timeHandlerFailure(GoogleService.SyncRequest.NAME, startTime);
         throw e;
      }
   }

   @Request(value = GoogleService.QueryRequest.NAME, service = true)
   public MessageBody handleQuery(
      VoiceContext context,
      @Named(GoogleService.QueryRequest.ATTR_ADDRESSES) Set<String> devIds
   ) {
      long startTime = System.nanoTime();
      try {
         MessageBody body = GoogleService.QueryResponse.builder()
            .withDevices(
               context.query(
                  devIds,
                  (model, hubOffline) -> Transformers.modelToStateMap(model, hubOffline, whitelist.isWhitelisted(context.getPlaceId()), VoiceUtil.getProduct(prodCat, model))
               )
            )
            .build();
         GoogleMetrics.timeHandlerSuccess(GoogleService.QueryRequest.NAME, startTime);
         return body;
      } catch(RuntimeException e) {
         GoogleMetrics.timeHandlerFailure(GoogleService.QueryRequest.NAME, startTime);
         throw e;
      }
   }

   @Request(value = GoogleService.ExecuteRequest.NAME, service = true, response = false)
   public void handleExecute(
      VoiceContext context,
      PlatformMessage msg,
      @Named(GoogleService.ExecuteRequest.ATTR_COMMANDS) List<Map<String,Object>> commands
   ) {
      long startTime = System.nanoTime();
      if(commands == null || commands.isEmpty()) {
         bus.send(response(ImmutableList.of(), msg));
         GoogleMetrics.timeHandlerSuccess(GoogleService.ExecuteRequest.NAME, startTime);
         return;
      }

      Futures.addCallback(
            commandExecutor.execute(context, commands.stream().map(GoogleCommand::new).collect(Collectors.toList())),
            new FutureCallback<List<GoogleCommandResult>>() {
               @Override
               public void onSuccess(List<GoogleCommandResult> result) {
                  GoogleMetrics.timeHandlerSuccess(GoogleService.ExecuteRequest.NAME, startTime);
                  bus.send(response(result, msg));
               }

               @Override
               public void onFailure(@NonNull Throwable t) {
                  logger.warn("execution request [{}] failed", msg, t);
                  Throwable c = t.getCause();
                  if(c == null) {
                     c = t;
                  }
                  if(c instanceof TimeoutException) {
                     bus.send(error(Error.TIMEOUT, "execution timed out", msg));
                  } else {
                     bus.send(error(Error.UNKNOWN_ERROR, c.getMessage(), msg));
                  }
                  GoogleMetrics.timeHandlerFailure(GoogleService.ExecuteRequest.NAME, startTime);
               }
            },
            executor
      );
   }

   private PlatformMessage response(List<GoogleCommandResult> results, PlatformMessage req) {
      MessageBody body = GoogleService.ExecuteResponse.builder().withCommands(results.stream().map(GoogleCommandResult::toMap).collect(Collectors.toList())).build();
      return PlatformMessage.createResponse(req, body);
   }

   private PlatformMessage error(String code, String msg, PlatformMessage req) {
      MessageBody body = Errors.fromCode(code, msg);
      return PlatformMessage.createResponse(req, body);
   }
}

