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
package com.iris.api.server;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.session.UnknownSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iris.bridge.bus.PlatformBusService;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.message.DeviceMessageHandler;
import com.iris.bridge.server.netty.BridgeMdcUtil;
import com.iris.bridge.server.netty.Constants;
import com.iris.bridge.server.session.Session;
import com.iris.capability.util.Addresses;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.ErrorEvent;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.SessionService;
import com.iris.netty.server.message.IrisNettyMessageUtil;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.apikey.ApiKeyPrincipal;
import com.iris.security.authz.Authorizer;
import com.iris.security.authz.AuthzUtil;
import com.iris.security.principal.Principal;
import com.iris.util.MdcContext.MdcContextReference;

/**
 * Message handler for the api-bridge. Similar to IrisNettyMessageHandler but
 * does not use ClientRequestDispatcher — session service messages (SetActivePlace,
 * preferences, etc.) are not supported for API key sessions.
 */
public class ApiMessageHandler implements DeviceMessageHandler<String> {
   private static final String SESSION_SERVICE_ADDRESS = Addresses.toServiceAddress(SessionService.NAMESPACE);
   private static final Logger logger = LoggerFactory.getLogger(ApiMessageHandler.class);

   private final PlatformBusService platformBusService;
   private final Authorizer authorizer;
   private final IrisNettyMessageUtil messageUtil;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public ApiMessageHandler(
         PlatformBusService platformBusService,
         Authorizer authorizer,
         IrisNettyMessageUtil messageUtil,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      this.platformBusService = platformBusService;
      this.authorizer = authorizer;
      this.messageUtil = messageUtil;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public String handleMessage(Session session, String message) {
      logger.debug("Received message from API client [{}]", message);
      Client client = session.getClient();
      if(client == null || !client.isAuthenticated()) {
         session.disconnect(Constants.SESSION_EXPIRED_STATUS);
         return null;
      }

      ClientMessage clientMsg = JSON.fromJson(message, ClientMessage.class);
      if(clientMsg.isRequest()) {
         session.getClient().requestReceived();
      }

      try (MdcContextReference context = BridgeMdcUtil.captureAndInitializeContext(session, clientMsg)) {
         // Session service messages (SetActivePlace, preferences, etc.) are not
         // supported for API key sessions — the place is fixed by the key.
         if(SESSION_SERVICE_ADDRESS.equals(clientMsg.getDestination())) {
            ErrorEvent err = Errors.unsupportedMessageType(clientMsg.getType());
            return createErrorResponse(session, clientMsg, err);
         }

         Principal principal = session.getAuthorizationContext().getPrincipal();
         String actorNamespace = (principal instanceof ApiKeyPrincipal)
               ? ApiKeyPrincipal.ACTOR_NAMESPACE
               : "person"; // fallback, shouldn't happen in api-bridge
         Address actor = Address.platformService(principal.getUserId(), actorNamespace);
         PlatformMessage platformMessage = null;

         try {
            platformMessage = messageUtil.convertClientToPlatform(clientMsg, session, actor, populationCacheMgr);
         } catch (Exception ex) {
            ErrorEvent err = Errors.fromException(ex);
            return createErrorResponse(session, clientMsg, err);
         }

         try {
            if (authorizer.isAuthorized(session.getAuthorizationContext(), session.getActivePlace(), platformMessage)) {
               logger.debug("Placing message on platform bus [{}] for place id [{}] and population [{}]",
                     platformMessage, platformMessage.getPlaceId(), platformMessage.getPopulation());
               platformBusService.placeMessageOnPlatformBus(platformMessage);
            } else {
               logger.debug("Placing unauthorized error message on platform bus");
               platformBusService.placeMessageOnPlatformBus(
                     PlatformMessage.createResponse(platformMessage, AuthzUtil.createUnauthorizedEvent()));
            }
         }
         catch(UnknownSessionException use) {
            session.disconnect(Constants.SESSION_EXPIRED_STATUS);
         }
         catch(Exception e) {
            ErrorEvent err = Errors.fromException(e);
            return createErrorResponse(session, clientMsg, err);
         }

         return null;
      }
   }

   private String createErrorResponse(Session session, ClientMessage clientMsg, ErrorEvent err) {
      Address dest = Address.fromString(messageUtil.buildId(session.getClientToken().getRepresentation()));
      ClientMessage.Builder builder = ClientMessage.builder()
              .withPayload(err)
              .withSource(SESSION_SERVICE_ADDRESS)
              .withDestination(dest.getRepresentation());

      if (!StringUtils.isBlank(clientMsg.getCorrelationId())) {
          builder.withCorrelationId(clientMsg.getCorrelationId());
      }

      return JSON.toJson(builder.create());
   }
}
