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
package com.iris.oauth.handlers;

import com.iris.oauth.OAuthMetrics;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.io.json.JSON;
import com.iris.messages.type.PlaceAccessDescriptor;

@Singleton
@HttpGet("/oauth/places")
public class ListPlacesRESTHandler extends HttpResource {
   private final ClientFactory clientFactory;
   private final PersonPlaceAssocDAO personPlaceDao;

   @Inject
   public ListPlacesRESTHandler(ClientFactory clientFactory, SessionAuth auth, BridgeMetrics metrics, PersonPlaceAssocDAO personPlaceDao) {
      super(auth, new HttpSender(ListPlacesRESTHandler.class, metrics));
      this.personPlaceDao = personPlaceDao;
      this.clientFactory = clientFactory;
   }

   @Override
   public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      OAuthMetrics.incListPlacesRequests();
      Client client = clientFactory.get(ctx.channel());
      UUID loggedInPerson = client.getPrincipalId();
      List<PlaceAccessDescriptor> places = personPlaceDao.listPlaceAccessForPerson(loggedInPerson);
      DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
      String json = JSON.toJson(places);
      response.content().writeBytes(json.getBytes(StandardCharsets.UTF_8));
      return response;
   }
}

