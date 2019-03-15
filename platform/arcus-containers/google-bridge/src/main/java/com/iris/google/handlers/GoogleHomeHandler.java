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
package com.iris.google.handlers;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.Responder;
import com.iris.bridge.server.http.impl.RequestHandlerImpl;
import com.iris.bridge.server.http.impl.matcher.WildcardMatcher;
import com.iris.core.platform.PlatformBusClient;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.google.Constants;
import com.iris.google.GoogleBridgeConfig;
import com.iris.google.Transformers;
import com.iris.google.model.Request;
import com.iris.google.model.Response;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.AddressMatchers;
import com.iris.messages.errors.ErrorEventException;
import com.iris.oauth.OAuthUtil;
import com.iris.oauth.dao.OAuthDAO;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.IrisUUID;
import com.iris.voice.VoiceBridgeConfig;
import com.iris.voice.VoiceBridgeMetrics;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

@Singleton
public class GoogleHomeHandler extends RequestHandlerImpl {

   public static final String BEARER_AUTH_NAME = "GoogleBridge#bearerAuth";

   private static final Logger logger = LoggerFactory.getLogger(GoogleHomeHandler.class);

   @Inject
   public GoogleHomeHandler(
      @Named(BEARER_AUTH_NAME) RequestAuthorizer authorizer,
      GoogleBridgeConfig config,
      OAuthDAO oauthDao,
      @Named(VoiceBridgeConfig.NAME_EXECUTOR) ExecutorService executor,
      PlatformMessageBus bus,
      VoiceBridgeMetrics metrics,
      PlacePopulationCacheManager populationCacheMgr
   ) {
      super(new WildcardMatcher("/ha", HttpMethod.POST), authorizer, new ResponderImpl(config, oauthDao, bus, executor, metrics, populationCacheMgr));
   }

   private static class ResponderImpl implements Responder {

      private final GoogleBridgeConfig config;
      private final OAuthDAO oauthDao;
      private final ExecutorService executor;
      private final PlatformBusClient busClient;
      private final HttpSender httpSender;
      private final VoiceBridgeMetrics metrics;
      private final PlacePopulationCacheManager populationCacheMgr;

      private ResponderImpl(GoogleBridgeConfig config, OAuthDAO oauthDao, PlatformMessageBus bus, ExecutorService executor, VoiceBridgeMetrics metrics, PlacePopulationCacheManager populationCacheMgr) {
         this.config = config;
         this.oauthDao = oauthDao;
         this.executor = executor;
         this.busClient = new PlatformBusClient(bus, executor, ImmutableSet.of(AddressMatchers.equals(Constants.BRIDGE_ADDRESS)));
         this.httpSender = new HttpSender(GoogleHomeHandler.class, metrics);
         this.metrics = metrics;
         this.populationCacheMgr = populationCacheMgr;
      }

      @Override
      public void sendResponse(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
         long startTime = System.nanoTime();
         String json = req.content().toString(StandardCharsets.UTF_8);
         logger.trace("incoming google home request:  [{}]", json);
         Request googleRequest = Transformers.GSON.fromJson(json, Request.class);

         try {
            String placeIdStr = getPlaceId(req);
            UUID placeId = IrisUUID.fromString(placeIdStr);
            PlatformMessage message = Transformers.requestToMessage(googleRequest, placeId, populationCacheMgr.getPopulationByPlaceId(placeId), (int) config.getRequestTimeoutMs());
            logger.trace("[{}] transformed to platform message [{}]", googleRequest, message);
            Futures.addCallback(
               busClient.request(message),
               new FutureCallback<PlatformMessage>() {
                  @Override
                  public void onSuccess(PlatformMessage result) {
                     try {
                        logger.trace("[{}] received result [{}]", googleRequest, result);
                        Response r = Transformers.messageToResponse(result);
                        writeResult(ctx, req, googleRequest, r);
                        metrics.timeServiceSuccess(message.getMessageType(), startTime);
                     } catch(Exception e) {
                        writeException(ctx, req, googleRequest, e);
                        metrics.timeServiceFailure(message.getMessageType(), startTime);
                     }
                  }

                  @Override public void onFailure(Throwable t) {
                     writeException(ctx, req, googleRequest, t);
                     metrics.timeServiceFailure(message.getMessageType(), startTime);
                  }
               },
               executor
            );
         } catch(Exception e) {
            writeException(ctx, req, googleRequest, e);
         }
      }

      private String getPlaceId(FullHttpRequest req) {
         Optional<String> token = OAuthUtil.extractTokenFromBearer(req);
         if(!token.isPresent()) {
            throw new ErrorEventException(Constants.Error.AUTH_FAILURE, "no bearer authorization header");
         }
         Pair<UUID,Integer> personWithTtl = oauthDao.getPersonWithAccess(config.getOauthAppId(), token.get());
         if(personWithTtl == null) {
            throw new ErrorEventException(Constants.Error.AUTH_EXPIRED, "authentication expired");
         }
         Map<String,String> attrs = oauthDao.getAttrs(config.getOauthAppId(), personWithTtl.getKey());
         if(attrs == null || !attrs.containsKey(OAuthUtil.DEFAULT_PLACE_ATTR)) {
            throw new ErrorEventException(Constants.Error.UNKNOWN_ERROR, "no default place could be found");
         }
         String placeId = attrs.get(OAuthUtil.DEFAULT_PLACE_ATTR);
         if(StringUtils.isEmpty(placeId)) {
            throw new ErrorEventException(Constants.Error.UNKNOWN_ERROR, "no default place could be found");
         }
         return placeId;
      }

      private void writeResult(ChannelHandlerContext ctx, FullHttpRequest req, Request googleRequest, Response res) {
         String json = Transformers.GSON.toJson(res);
         logger.trace("[{}] resulted in response [{}]", googleRequest, json);
         FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
         response.content().writeBytes(json.getBytes(StandardCharsets.UTF_8));
         httpSender.sendHttpResponse(ctx, req, response);
      }

      private void writeException(ChannelHandlerContext ctx, FullHttpRequest req, Request googleRequest, Throwable t) {
         logger.warn("failed to execute [{}]", googleRequest, t);
         Throwable c = t.getCause();
         if(c == null) {
            c = t;
         }

         String errCode = Constants.Error.UNKNOWN_ERROR;
         if(c instanceof ErrorEventException) {
            errCode = ((ErrorEventException) c).getCode();
         } else if(c instanceof TimeoutException) {
            errCode = Constants.Error.TIMEOUT;
         }
         Response r = new Response();
         r.setRequestId(googleRequest.getRequestId());
         r.setPayload(ImmutableMap.of(Constants.Response.ERROR_CODE, errCode));
         writeResult(ctx, req, googleRequest, r);
      }
   }

}

