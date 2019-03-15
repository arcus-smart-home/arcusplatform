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
package com.iris.netty.server.message;

import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.session.UnknownSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
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
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.SessionService;
import com.iris.messages.service.VideoService;
import com.iris.metrics.IrisMetrics;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.authz.Authorizer;
import com.iris.security.authz.AuthzUtil;
import com.iris.util.MdcContext.MdcContextReference;

public class IrisNettyMessageHandler implements DeviceMessageHandler<String> {
   private static final String SESSION_SERVICE_ADDRESS = Addresses.toServiceAddress(SessionService.NAMESPACE);
   private static final Counter NUM_VIDEO_CACHE_RESPONSE_HIT = IrisMetrics.metrics("client.bridge").counter("video.cached.response.hit");
   private static final Counter NUM_VIDEO_CACHE_RESPONSE_MISS = IrisMetrics.metrics("client.bridge").counter("video.cached.response.miss");
   private static final MessageBody EMPTY_LIST_RECORDINGS_RESPONSE = VideoService.ListRecordingsResponse.builder()
      .withRecordings(Collections.emptyList())
      .build();

   private static final Logger logger = LoggerFactory.getLogger(IrisNettyMessageHandler.class);
   private final PlatformBusService platformBusService;
   private final Authorizer authorizer;
   private final IrisNettyMessageUtil messageUtil;
   private final ClientRequestDispatcher clientRequestDispatcher;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public IrisNettyMessageHandler(
         PlatformBusService platformBusService,
         Authorizer authorizer,
         IrisNettyMessageUtil messageUtil,
         ClientRequestDispatcher clientRequestDispatcher,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      this.platformBusService = platformBusService;
      this.authorizer = authorizer;
      this.messageUtil = messageUtil;
      this.clientRequestDispatcher = clientRequestDispatcher;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public String handleMessage(Session session, String message) {
      logger.debug("Received message from client [{}]", message);
      Client client = session.getClient();
      if(client == null || !client.isAuthenticated()) {
         session.disconnect(Constants.SESSION_EXPIRED_STATUS);
         return null;
      }

      ClientMessage clientMsg = JSON.fromJson(message, ClientMessage.class);
      if(clientMsg.isRequest()) {
         session.getClient().requestReceived();
      }
      
      /* 
       * try(...) - Uses Java 1.7+ Automatic Resource Management eliminating need for finally block to  
       * manage closeable resources. All resources that implement AutoCloseable will be automatically 
       * closed as necessary without a finally block. e.g. MdcContextReference
       */
      try (MdcContextReference context = BridgeMdcUtil.captureAndInitializeContext(session,clientMsg)) {
         if(SESSION_SERVICE_ADDRESS.equals(clientMsg.getDestination())) {
            clientRequestDispatcher.submit(clientMsg, session);
            return null;
         }  

         Address actor = Address.platformService(session.getAuthorizationContext().getPrincipal().getUserId(), PersonCapability.NAMESPACE);
         PlatformMessage platformMessage = null;
         
         try {
           platformMessage = messageUtil.convertClientToPlatform(clientMsg, session, actor, populationCacheMgr);
         } catch (Exception ex) {
            ErrorEvent err = Errors.fromException(ex);
            // Return error response instead of disconnecting socket which would cause a reconnect
            return createResponseFromErrorEvent(session, clientMsg, err);
        	 
         }

         if (VideoService.ListRecordingsRequest.NAME.equals(clientMsg.getType())) {
            MessageBody result = EMPTY_LIST_RECORDINGS_RESPONSE;
            NUM_VIDEO_CACHE_RESPONSE_HIT.inc();

            PlatformMessage cachedResponse = PlatformMessage.createResponse(platformMessage, result);
            return JSON.toJson(messageUtil.convertPlatformToClient(cachedResponse));
         }

         try {
            if (authorizer.isAuthorized(session.getAuthorizationContext(), session.getActivePlace(), platformMessage)) {
               logger.debug("Placing message on platform bus [{}] for place id [{}] and population [{}]", platformMessage, platformMessage.getPlaceId(), platformMessage.getPopulation());
               platformBusService.placeMessageOnPlatformBus(platformMessage);
            } else {
               logger.debug("Placing unauthorized error message on platform bus");
               platformBusService.placeMessageOnPlatformBus(PlatformMessage.createResponse(platformMessage, AuthzUtil.createUnauthorizedEvent()));
            }
         }
         catch(UnknownSessionException use) {
            session.disconnect(Constants.SESSION_EXPIRED_STATUS);
         }catch(Exception e) {
            ErrorEvent err = Errors.fromException(e);            
            return createResponseFromErrorEvent(session, clientMsg, err);
         }        

         return null;
      }
   }
   
   
   private String createResponseFromErrorEvent(Session session, ClientMessage clientMsg, ErrorEvent err)
   {
      Address dest = Address.fromString(messageUtil.buildId(session.getClientToken().getRepresentation()));
      ClientMessage.Builder builder = ClientMessage.builder()
              .withPayload(err)
              .withSource(SESSION_SERVICE_ADDRESS)
              .withDestination(dest.getRepresentation());
      
      if (!StringUtils.isBlank(clientMsg.getCorrelationId())) {
          builder.withCorrelationId(clientMsg.getCorrelationId());
       }
      
      ClientMessage response  = builder.create();
      
      return JSON.toJson(response);
   }

   
}

