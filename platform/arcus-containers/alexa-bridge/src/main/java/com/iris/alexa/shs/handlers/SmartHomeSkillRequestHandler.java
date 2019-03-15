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
package com.iris.alexa.shs.handlers;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.serder.SerDer;
import com.iris.alexa.shs.ShsConfig;
import com.iris.alexa.shs.ShsMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.Responder;
import com.iris.bridge.server.http.impl.RequestHandlerImpl;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.http.impl.matcher.WildcardMatcher;
import com.iris.oauth.OAuthUtil;
import com.iris.oauth.dao.OAuthDAO;
import com.iris.util.IrisUUID;
import com.iris.voice.VoiceBridgeConfig;
import com.iris.voice.VoiceBridgeMetrics;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

@Singleton
public class SmartHomeSkillRequestHandler extends RequestHandlerImpl {

   private static final Logger logger = LoggerFactory.getLogger(SmartHomeSkillRequestHandler.class);

   @Inject
   public SmartHomeSkillRequestHandler(
      AlwaysAllow authorizer,
      ShsConfig config,
      OAuthDAO oauthDao,
      @Named(VoiceBridgeConfig.NAME_EXECUTOR) ExecutorService executor,
      VoiceBridgeMetrics metrics,
      Set<SmartHomeSkillHandler> handlers
   ) {
      super(
         new WildcardMatcher("/alexa/shs", HttpMethod.POST),
         authorizer,
         new ResponderImpl(config, oauthDao, executor, metrics, handlers)
      );
   }

   private static class ResponderImpl implements Responder {

      private final ShsConfig config;
      private final OAuthDAO oauthDao;
      private final ExecutorService executor;
      private final HttpSender httpSender;
      private Set<SmartHomeSkillHandler> handlers;

      private ResponderImpl(
         ShsConfig config,
         OAuthDAO oauthDao,
         ExecutorService executor,
         VoiceBridgeMetrics metrics,
         Set<SmartHomeSkillHandler> handlers
      ) {
         this.config = config;
         this.oauthDao = oauthDao;
         this.executor = executor;
         this.httpSender = new HttpSender(SmartHomeSkillRequestHandler.class, metrics);
         this.handlers = handlers;
      }

      @Override
      public void sendResponse(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
         String s = req.content().toString(StandardCharsets.UTF_8);

         logger.trace("handling incoming alexa request: {}", s);

         AlexaMessage m = SerDer.deserialize(s);

         for(SmartHomeSkillHandler handler : handlers) {
            if(handler.supports(m)) {
               try {
                  String token = handler.extractOAuthToken(m).orElseThrow(() -> new AlexaException(AlexaErrors.INVALID_AUTHORIZATION_CREDENTIAL));
                  UUID placeId = getPlaceId(token);
                  Futures.addCallback(
                     handler.handle(m, placeId),
                     new FutureCallback<AlexaMessage>() {
                        @Override
                        public void onSuccess(AlexaMessage result) {
                           writeResult(ctx, req, result);
                        }

                        @Override
                        public void onFailure(@Nonnull Throwable t) {
                           AlexaMessage msg = handler.transformException(m, t);
                           writeResult(ctx, req, msg);
                        }
                     },
                     executor
                  );
               } catch(Exception e) {
                  logger.error("unexepected error handling {}", s, e);
                  ShsMetrics.incUncaughtException();
                  AlexaMessage msg = handler.transformException(m, e);
                  writeResult(ctx, req, msg);
               }
               return;
            }
         }
         logger.warn("no handler could be found for {}, message will timeout", s);
         ShsMetrics.incInvalidDirective();
         writeBadRequest(ctx, req);
      }

      private UUID getPlaceId(String token) {
         Pair<UUID,Integer> personWithTtl = oauthDao.getPersonWithAccess(config.getShsAppId(), token);
         if(personWithTtl == null) {
            ShsMetrics.incExpiredToken();
            throw new AlexaException(AlexaErrors.EXPIRED_AUTHORIZATION_CREDENTIAL);
         }
         Map<String,String> attrs = oauthDao.getAttrs(config.getShsAppId(), personWithTtl.getKey());
         if(attrs == null || !attrs.containsKey(OAuthUtil.DEFAULT_PLACE_ATTR)) {
            ShsMetrics.incNoPlace();
            throw new AlexaException(AlexaErrors.INTERNAL_ERROR);
         }
         String placeId = attrs.get(OAuthUtil.DEFAULT_PLACE_ATTR);
         if(StringUtils.isEmpty(placeId)) {
            ShsMetrics.incNoPlace();
            throw new AlexaException(AlexaErrors.INTERNAL_ERROR);
         }
         return IrisUUID.fromString(placeId);
      }

      private void writeResult(ChannelHandlerContext ctx, FullHttpRequest req, AlexaMessage responseMsg) {
         String json = SerDer.serialize(responseMsg);
         logger.debug("got response {}", json);
         FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
         ByteBufUtil.writeUtf8(response.content(), json);
         httpSender.sendHttpResponse(ctx, req, response);
      }

      private void writeBadRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
         FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
         httpSender.sendHttpResponse(ctx, req, response);
      }
   }

}

