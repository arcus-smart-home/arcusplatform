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
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Subsystem;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.service.SubsystemService;

/**
 *
 */
// TODO auto-generate these things
public class SubsystemServiceImpl implements SubsystemService {
   private static final String ADDRESS = Addresses.toServiceAddress(SubsystemService.NAMESPACE);
   private IrisClient client;

   /**
    *
    */
   public SubsystemServiceImpl(IrisClient client) {
      this.client = client;
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.Service#getName()
    */
   @Override
   public String getName() {
      return SubsystemService.NAME;
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.Service#getAddress()
    */
   @Override
   public String getAddress() {
      return ADDRESS;
   }


   /* (non-Javadoc)
    * @see com.iris.client.service.SubsystemService#listSubsystems(java.lang.String)
    */
   @Override
   public ClientFuture<ListSubsystemsResponse> listSubsystems(String placeId) {
      ListSubsystemsRequest request = new ListSubsystemsRequest();
      request.setAddress(getAddress());
      request.setPlaceId(placeId);
      request.setRestfulRequest(false);
      
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, ListSubsystemsResponse>() {
         @Override
         public ListSubsystemsResponse apply(ClientEvent input) {
            ListSubsystemsResponse response = new ListSubsystemsResponse(input);
            IrisClientFactory.getModelCache().retainAll(Subsystem.NAMESPACE, response.getSubsystems());
            return response;
         }

      });
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.SubsystemService#reload()
    */
   @Override
   public ClientFuture<ReloadResponse> reload() {
      ReloadRequest request = new ReloadRequest();
      request.setAddress(getAddress());
      request.setRestfulRequest(false);
      
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, ReloadResponse>() {
         @Override
         public ReloadResponse apply(ClientEvent input) {
            return new ReloadResponse(input);
         }

      });
   }

}

