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

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bridge.server.session.Session;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.service.SessionService;
import com.iris.messages.service.SessionService.LogRequest;
import com.iris.messages.service.SessionService.LogResponse;

@Singleton
public class LogHandler extends BaseClientRequestHandler
{
   private static final Logger sessionLogger = LoggerFactory.getLogger("session");

   // TODO move this onto the session object or somewhere else?
   public static void log(Session session, String category, String code, String message)
   {
      String placeId = session.getActivePlace();

      if (placeId == null)
      {
         placeId = "[notset]";
      }
      if (StringUtils.isEmpty(category))
      {
         category = "[notset]";
      }
      if (StringUtils.isEmpty(code))
      {
         code = "[notset]";
      }
      if (StringUtils.isEmpty(message))
      {
         message = "[none]";
      }

      sessionLogger.info("SessionLogMessage client:{} person:{} place:{} category:{} code:{} message:{}",
         session.getClientToken().getRepresentation(), session.getClient().getPrincipalId(), placeId, category, code,
         message);
   }

   public LogHandler()
   {
      super(directExecutor());
   }

   @Override
   public String getRequestType()
   {
      return LogRequest.NAME;
   }

   @Override
   protected MessageBody doHandle(ClientMessage request, Session session)
   {
      MessageBody requestBody = request.getPayload();

      String category = SessionService.LogRequest.getCategory(requestBody);
      String code = SessionService.LogRequest.getCode(requestBody);
      String message = SessionService.LogRequest.getMessage(requestBody);

      log(session, category, code, message);

      return LogResponse.instance();
   }

   @Override
   protected Address address()
   {
   	return SessionService.ADDRESS;
   }
}

