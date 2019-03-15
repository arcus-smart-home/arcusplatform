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

import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.iris.bridge.server.session.Session;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;

@Singleton
public class ClientRequestDispatcher
{
   private final ImmutableMap<String, ClientRequestHandler> handlers;

   @Inject
   public ClientRequestDispatcher(
      SetActivePlaceHandler setActivePlaceHandler,
      LogHandler logHandler,
      TagHandler tagHandler,
      ListAvailablePlacesHandler listAvailablePlacesHandler,
      GetPreferencesHandler getPreferencesHandler,
      SetPreferencesHandler setPreferencesHandler,
      ResetPreferenceHandler resetPreferenceHandler)
   {
      handlers = ImmutableMap.<String, ClientRequestHandler>builder()
         .put(setActivePlaceHandler.getRequestType(), setActivePlaceHandler)
         .put(logHandler.getRequestType(), logHandler)
         .put(tagHandler.getRequestType(), tagHandler)
         .put(listAvailablePlacesHandler.getRequestType(), listAvailablePlacesHandler)
         .put(getPreferencesHandler.getRequestType(), getPreferencesHandler)
         .put(setPreferencesHandler.getRequestType(), setPreferencesHandler)
         .put(resetPreferenceHandler.getRequestType(), resetPreferenceHandler)
         .build();
   }

   public void submit(ClientMessage request, Session session)
   {
      MessageBody requestBody = request.getPayload();

      ClientRequestHandler handler = handlers.get(requestBody.getMessageType());

      if (handler == null)
      {
         throw new ErrorEventException(Errors.unsupportedMessageType(requestBody.getMessageType()));
      }

      handler.submit(request, session);
   }
}

