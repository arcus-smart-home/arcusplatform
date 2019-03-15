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
package com.iris.client.impl;

import com.google.common.base.Function;
import com.iris.capability.util.Addresses;
import com.iris.client.ClientEvent;
import com.iris.client.IrisClient;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.service.PlaceService;

import java.util.Map;

/**
 *
 */
public class PlaceServiceImpl implements PlaceService {
   private static final String ADDRESS = Addresses.toServiceAddress(PlaceService.NAMESPACE);
   private IrisClient client;

   /**
    *
    */
   public PlaceServiceImpl(IrisClient client) {
      this.client = client;
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.Service#getName()
    */
   @Override
   public String getName() {
      return PlaceService.NAME;
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.Service#getAddress()
    */
   @Override
   public String getAddress() {
      return ADDRESS;
   }


   /* (non-Javadoc)
    * @see com.iris.client.service.PlaceService#listTimezones()
    */
   @Override
   public ClientFuture<ListTimezonesResponse> listTimezones() {
      ListTimezonesRequest request = new ListTimezonesRequest();
      request.setAddress(getAddress());
      request.setRestfulRequest(true);

      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, ListTimezonesResponse>() {
         @Override
         public ListTimezonesResponse apply(ClientEvent input) {
            ListTimezonesResponse response = new ListTimezonesResponse(input);
            return response;
         }
      });
   }

   @Override
   public ClientFuture<ValidateAddressResponse> validateAddress(String placeId, Map<String, Object> streetAddress) {
      ValidateAddressRequest request = new ValidateAddressRequest();
      request.setAddress(getAddress());
      request.setPlaceId(placeId);
      request.setStreetAddress(streetAddress);

      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, ValidateAddressResponse>() {
         @Override
         public ValidateAddressResponse apply(ClientEvent input) {
            return new ValidateAddressResponse(input);
         }
      });
   }
}

