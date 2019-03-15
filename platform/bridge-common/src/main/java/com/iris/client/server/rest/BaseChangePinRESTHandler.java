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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.Permission;

import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.handlers.RESTHandler;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.exception.PinNotUniqueAtPlaceException;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PersonCapability.PinChangedEventEvent;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Person;
import com.iris.messages.services.PlatformConstants;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthorizationGrant;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.CharsetUtil;

abstract class BasePinRESTHandler extends RESTHandler {

   private final ClientFactory clientFactory;
   private final AuthorizationGrantDAO authGrantDao;
   private final PlatformMessageBus bus;
   private final boolean checkPrincipal;
   
   protected final PersonDAO personDao;
   protected final PlacePopulationCacheManager populationCacheMgr;

   // KEEP - NEEDED BY ALL PIN HANDLERS
   protected abstract String extractPin(ClientMessage msg);
   protected abstract String extractPlaceId(ClientMessage msg, Person person);
   protected abstract MessageBody buildResponseBody(boolean success);
   protected abstract MessageBody handlePinOperation(Person person, String pin, String place, List<AuthorizationGrant> grants, ClientMessage clientMessage) throws PinNotUniqueAtPlaceException;
   
   BasePinRESTHandler(
   		ClientFactory clientFactory, 
   		PersonDAO personDao, 
   		AuthorizationGrantDAO authGrantDao, 
   		PlatformMessageBus bus, 
   		PlacePopulationCacheManager populationCacheMgr, 
   		HttpSender httpSender, 
   		RequestAuthorizer auth, 
   		RESTHandlerConfig restHandlerConfig,
   		boolean checkPrincipal
	) {
      super(auth, httpSender, restHandlerConfig);
      this.clientFactory = clientFactory;
      this.personDao = personDao;
      this.authGrantDao = authGrantDao;
      this.bus = bus;
      this.checkPrincipal = checkPrincipal;
      this.populationCacheMgr = populationCacheMgr;
   }
   
   @Override
   public void assertValidRequest(FullHttpRequest req, ChannelHandlerContext ctx) {
      String json = req.content().toString(CharsetUtil.UTF_8);
      ClientMessage clientMessage = JSON.fromJson(json, ClientMessage.class);
      
      String destination = clientMessage.getDestination();
      // FIXME really annoying that we have to load person twice, but would require a pretty major API shift in RESTHandler to fix this
      Person person = loadPerson(destination);
      PinErrors.assertPersonFound(person);
      
      String pin = extractPin(clientMessage);
      String place = extractPlaceId(clientMessage, person);

      // Validate the request
      Errors.assertRequiredParam(pin, PersonCapability.VerifyPinRequest.ATTR_PIN);
      Errors.assertRequiredParam(place, PersonCapability.VerifyPinRequest.ATTR_PLACE);
      Errors.assertValidRequest(pin.length() == 4 && StringUtils.isNumeric(pin), "pin must be 4 numerical digits");
      
      Client client = clientFactory.get(ctx.channel());
      
      /* Validate the creds and auth */
      UUID placeID = UUID.fromString(place);

      Address dst = Address.fromString(destination);
      
      List<AuthorizationGrant> grants = authGrantDao.findForEntity(person.getId());
      
      if(checkPrincipal) {
         AuthorizationContext authCtx = client.getAuthorizationContext();
         if(permissionsEmpty(authCtx.getInstancePermissions(placeID)) && permissionsEmpty(authCtx.getNonInstancePermissions(placeID))) {
            throw new UnauthorizedRequestException(dst, "Unauthorized");
         }

         UUID loggedInPerson = client.getPrincipalId();
         if(!loggedInPerson.equals(person.getId()) && person.getHasLogin()) {
            throw new UnauthorizedRequestException(dst, "Unauthorized request, requester must be logged in as the person that the pin is being verified for");
         }
      }

      if(!grants.stream().anyMatch((a) -> { return a.getPlaceId().equals(placeID); })) {
         throw new UnauthorizedRequestException(dst, "Unauthorized");
      }
   }
   
   @Override
   protected MessageBody doHandle(ClientMessage request) throws Exception {
      Person person = loadPerson(request.getDestination());
      String pin = extractPin(request);
      String place = extractPlaceId(request, person);

      List<AuthorizationGrant> grants = authGrantDao.findForEntity(person.getId());

      try {
         return handlePinOperation(person, pin, place, grants, request);
      }
      catch (PinNotUniqueAtPlaceException e) {
         throw new ErrorEventException(PinErrors.PIN_NOT_UNIQUE_AT_PLACE_CODE, PinErrors.PIN_NOT_UNIQUE_AT_PLACE_MSG);
      }
   }

   private Person loadPerson(String destination) {
      Address addr = Address.fromString(destination);
      if(addr == null) {
         return null;
      }
      return personDao.findById((UUID) addr.getId());
   }

   private boolean permissionsEmpty(Collection<? extends Permission> permissions) {
      return permissions == null || permissions.isEmpty();
   }

   protected void broadcastValueChange(Person person, String placeId, List<AuthorizationGrant> grants, boolean hadPin, Set<String> previousPlaces) {
      Map<String,Object> attrs = new HashMap<>();
      if(person.hasPinAtPlace(placeId) && !hadPin) {
         attrs.put(PersonCapability.ATTR_HASPIN, true);
      }
      if(!Objects.equals(person.getPlacesWithPin(), previousPlaces)) {
         attrs.put(PersonCapability.ATTR_PLACESWITHPIN, person.getPlacesWithPin());
      }

      if(attrs.isEmpty()) {
         return;
      }

      grants.forEach((g) -> {
         PlatformMessage broadcast = PlatformMessage.buildBroadcast(
               MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, attrs),
               Address.fromString(person.getAddress()))
               .withPlaceId(g.getPlaceId())
               .withPopulation(populationCacheMgr.getPopulationByPlaceId(g.getPlaceId()))
               .create();
         bus.send(broadcast);
      });
   }

   // KEEP
   protected void notify(Person person, String placeId) {
   	String population = populationCacheMgr.getPopulationByPlaceId(placeId);
      PlatformMessage broadcast = PlatformMessage.buildBroadcast(
            PinChangedEventEvent.instance(),
            Address.fromString(person.getAddress()))
            .withPlaceId(placeId)
            .withPopulation(population)
            .create();
      bus.send(broadcast);

      PlatformMessage msg = Notifications.builder()
            .withSource(Address.platformService(PlatformConstants.SERVICE_PEOPLE))
            .withPersonId(person.getId())
            .withPlaceId(UUID.fromString(placeId))
            .withPopulation(population)
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .withMsgKey(Notifications.PinChanged.KEY)
            .create();
      bus.send(msg);
   }
}

