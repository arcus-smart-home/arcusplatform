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
package com.iris.client.server.rest.passkey;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.client.server.passkey.PasskeyService;
import com.iris.core.dao.PersonDAO;
import com.iris.messages.model.Person;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Starts adding a passkey for the logged-in person: returns options for
 * navigator.credentials.create() and the request id to send back to /passkey/Register.
 */
@Singleton
@HttpPost("/passkey/RegisterOptions")
public class PasskeyRegisterOptionsRESTHandler extends PasskeyResource {
   private final ClientFactory clientFactory;
   private final PersonDAO personDao;

   @Inject
   public PasskeyRegisterOptionsRESTHandler(SessionAuth auth, BridgeMetrics metrics, PasskeyService passkeyService, ClientFactory clientFactory, PersonDAO personDao) {
      super(auth, PasskeyRegisterOptionsRESTHandler.class, metrics, passkeyService);
      this.clientFactory = clientFactory;
      this.personDao = personDao;
   }

   @Override
   protected FullHttpResponse respond(JsonObject body, ChannelHandlerContext ctx) throws Exception {
      Person person = currentPerson(clientFactory, personDao, ctx);
      if (person == null) {
         return error(HttpResponseStatus.UNAUTHORIZED, "Not logged in");
      }
      return options(passkeyService.startRegistration(person));
   }
}
