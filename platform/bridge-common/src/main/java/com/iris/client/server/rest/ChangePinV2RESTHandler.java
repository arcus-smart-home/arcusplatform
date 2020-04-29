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
package com.iris.client.server.rest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.exception.PinNotUniqueAtPlaceException;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.model.Person;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.authz.AuthorizationGrant;

@Singleton
@HttpPost("/person/ChangePinV2")
public class ChangePinV2RESTHandler extends BasePinRESTHandler {
   private static final Logger logger = LoggerFactory.getLogger(ChangePinV2RESTHandler.class);

   @Inject
   public ChangePinV2RESTHandler(
         ClientFactory clientFactory,
         PersonDAO personDao,
         AuthorizationGrantDAO authGrantDao,
         PlatformMessageBus bus,
         PlacePopulationCacheManager populationCacheMgr,
         BridgeMetrics metrics,
         SessionAuth auth,
         RESTHandlerConfig restHandlerConfig,
         @Named("changepin.require.principal.check")
         boolean requirePrincipalCheck
   ) {
      super(clientFactory, personDao, authGrantDao, bus, populationCacheMgr, new HttpSender(ChangePinV2RESTHandler.class, metrics), auth, restHandlerConfig, requirePrincipalCheck);
   }

   @Override
   protected String extractPin(ClientMessage msg) {
      return PersonCapability.ChangePinV2Request.getPin(msg.getPayload());
   }

   @Override
   protected String extractPlaceId(ClientMessage msg, Person person) {
      return PersonCapability.ChangePinV2Request.getPlace(msg.getPayload());
   }

   @Override
   protected MessageBody buildResponseBody(boolean success) {
      return PersonCapability.ChangePinV2Response.builder().withSuccess(success).build();
   }
   
   @Override
   protected MessageBody handlePinOperation(Person person, String newPin, String place, List<AuthorizationGrant> grants,
      ClientMessage clientMessage) throws PinNotUniqueAtPlaceException {

      if (!StringUtils.equals(person.getPinAtPlace(place), newPin))
      {
         Set<String> previousPlacesWithPin = person.getPlacesWithPin();
         boolean hadPinAtPlace = person.hasPinAtPlace(place);

         person = personDao.updatePinAtPlace(person, UUID.fromString(place), newPin);

         broadcastValueChange(person, place, grants, hadPinAtPlace, previousPlacesWithPin);
         logger.info("person=[{}] changed their pin at place=[{}]", person.getId(), UUID.fromString(place)); // Audit event
         notify(person, place);
      }

      return buildResponseBody(true);
   }
}

