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
/**
 * 
 */
package com.iris.platform.services.place.handlers;

import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.AuthorizationGrantDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.model.Place;
import com.iris.messages.service.PlaceService;
import com.iris.platform.address.StreetAddress;
import com.iris.platform.address.validation.AddressValidationResult;
import com.iris.platform.address.validation.AddressValidator;
import com.iris.platform.address.validation.AddressValidatorFactory;
import com.iris.security.authz.AuthzUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 
 */
@Singleton
public class ValidateAddressHandler implements ContextualRequestMessageHandler<Place> {

   private final PlaceDAO placeDao;
   private final AuthorizationGrantDAO grantDao;
   private final AddressValidatorFactory validatorFactory;

   @Inject
   public ValidateAddressHandler(PlaceDAO placeDao, AuthorizationGrantDAO grantDao, AddressValidatorFactory validatorFactory) {
      this.placeDao = placeDao;
      this.grantDao = grantDao;
      this.validatorFactory = validatorFactory;
   }

   @Override
   public String getMessageType() {
      return PlaceService.ValidateAddressRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Place context, PlatformMessage msg) {
      return handleRequest(null, validatorFactory.validatorFor(context), msg.getValue());
   }

   @Override
   public MessageBody handleStaticRequest(PlatformMessage msg) {
      if(!authorized(msg)) {
         return AuthzUtil.createUnauthorizedEvent();
      }

      MessageBody body = msg.getValue();
      String id = PlaceService.ValidateAddressRequest.getPlaceId(body);
      if (!StringUtils.isBlank(id)) {
         UUID placeId = UUID.fromString(id);
         Place p = placeDao.findById(placeId);
         return handleRequest(p, validatorFactory.validatorFor(p), body);
      }
      return handleRequest(null, validatorFactory.defaultValidator(), body);
   }

   private MessageBody handleRequest(Place place, AddressValidator validator, MessageBody body) {
	   AddressValidationResult result = null;
	   if(place != null) {
		   result = validator.validate(place, StreetAddress.fromMap(PlaceService.ValidateAddressRequest.getStreetAddress(body)));
	   }else{
		   result = validator.validate(StreetAddress.fromMap(PlaceService.ValidateAddressRequest.getStreetAddress(body)));
	   }
      
	   return PlaceService.ValidateAddressResponse.builder()
            .withSuggestions(result.getSuggestions().stream().map(StreetAddress::toTypeAttributes).collect(Collectors.toList()))
            .withValid(result.isValid())
            .build();
   }

   private boolean authorized(PlatformMessage msg) {
      String id = PlaceService.ValidateAddressRequest.getPlaceId(msg.getValue());
      if(StringUtils.isBlank(id)) {
         return true;
      }

      Address actor = msg.getActor();
      if(actor == null) {
         return false;
      }

      UUID placeId = UUID.fromString(id);
      return grantDao.findForPlace(placeId).stream().anyMatch((g) -> Objects.equal(Address.platformService(g.getEntityId(), PersonCapability.NAMESPACE), msg.getActor()));
   }
}

