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
package com.iris.client.impl;

import com.google.common.base.Function;
import com.iris.capability.util.Addresses;
import com.iris.client.ClientEvent;
import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Scene;
import com.iris.client.capability.SceneTemplate;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.service.SceneService;

public class SceneServiceImpl implements SceneService {
   private static final String ADDRESS = Addresses.toServiceAddress(SceneService.NAMESPACE);
   private final IrisClient client;

   public SceneServiceImpl(IrisClient client) {
      this.client = client;
   }

   @Override
   public String getName() {
      return SceneService.NAME;
   }

   @Override
   public String getAddress() {
      return ADDRESS;
   }

   @Override
   public ClientFuture<ListScenesResponse> listScenes(String placeId) {
      ListScenesRequest request = new ListScenesRequest();
      request.setAddress(ADDRESS);
      request.setPlaceId(placeId);
      request.setRestfulRequest(false);
      
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(
            result, 
            new Function<ClientEvent, ListScenesResponse>() {
               @Override
               public ListScenesResponse apply(ClientEvent input) {
                  ListScenesResponse response = new ListScenesResponse(input);
                  IrisClientFactory.getModelCache().retainAll(Scene.NAMESPACE, response.getScenes());
                  return response;
               }
         
            }
      );
   }

   @Override
   public ClientFuture<ListSceneTemplatesResponse> listSceneTemplates(String placeId) {
      ListSceneTemplatesRequest request = new ListSceneTemplatesRequest();
      request.setAddress(ADDRESS);
      request.setPlaceId(placeId);
      request.setRestfulRequest(false);
      
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(
            result, 
            new Function<ClientEvent, ListSceneTemplatesResponse>() {
               @Override
               public ListSceneTemplatesResponse apply(ClientEvent input) {
                  ListSceneTemplatesResponse response = new ListSceneTemplatesResponse(input);
                  IrisClientFactory.getModelCache().retainAll(SceneTemplate.NAMESPACE, response.getSceneTemplates());
                  return response;
               }
         
            }
      );
   }

}

