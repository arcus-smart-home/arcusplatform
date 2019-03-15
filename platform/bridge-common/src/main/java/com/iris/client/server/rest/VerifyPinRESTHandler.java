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

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Person;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.authz.AuthorizationGrant;

@Singleton
@HttpPost("/person/VerifyPin")
public class VerifyPinRESTHandler extends BasePinRESTHandler {

   @Inject
   VerifyPinRESTHandler(
   		ClientFactory clientFactory,
   		PersonDAO personDao, 
   		AuthorizationGrantDAO authGrantDao, 
   		PlatformMessageBus bus, 
   		PlacePopulationCacheManager populationCacheMgr,
   		BridgeMetrics metrics, 
   		SessionAuth authorizer, 
   		RESTHandlerConfig restHandlerConfig
	) {
      super(clientFactory, personDao, authGrantDao, bus, populationCacheMgr, new HttpSender(VerifyPinRESTHandler.class, metrics), authorizer, restHandlerConfig, true);
   }

   @Override
   protected MessageBody handlePinOperation(Person person, String pin, String place, List<AuthorizationGrant> grants, ClientMessage clientMessage) {

      String currentPin = person.getPinAtPlace(place);

      boolean isMatching = !StringUtils.isBlank(pin) && pin.equals(currentPin);

      if (isMatching) {
         return buildResponseBody(true);
      } else {
         throw new ErrorEventException("MismatchedPins", "the pins do not match");
      }
   }

   @Override
   protected String extractPin(ClientMessage msg) {
      return PersonCapability.VerifyPinRequest.getPin(msg.getPayload());
   }

   @Override
   protected String extractPlaceId(ClientMessage msg, Person person) {
      return PersonCapability.VerifyPinRequest.getPlace(msg.getPayload());
   }

   @Override
   protected MessageBody buildResponseBody(boolean success) {
      return PersonCapability.VerifyPinResponse.builder().withSuccess(success).build();
   }

}

