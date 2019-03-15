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

import static com.iris.messages.errors.Errors.serviceUnavailable;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.iris.bridge.server.session.Session;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.util.MdcContext;
import com.iris.util.MdcContext.MdcContextReference;

public abstract class BaseClientRequestHandler implements ClientRequestHandler
{
   private static final Logger logger = LoggerFactory.getLogger(BaseClientRequestHandler.class);

   private final Executor executor;

   protected BaseClientRequestHandler(Executor executor)
   {
      this.executor = executor;
   }

   @Override
   public void submit(ClientMessage request, Session session)
   {
      try
      {
         executor.execute(() ->
         {
            ClientMessage response = handle(request, session);
   
            sendResponse(response, session);
         });
      }
      catch (RejectedExecutionException e)
      {
         sendResponse(serviceUnavailable(), request, session);
      }
   }

   @Override
   public ClientMessage handle(ClientMessage request, Session session)
   {
      MessageBody responseBody;

      try (MdcContextReference context = captureAndInitializeContext(request, session))
      {
         if (session.getClient().getPrincipalId() == null)
         {
            throw new UnauthorizedRequestException(Address.fromString(request.getDestination()),
               "Received request from unauthorized session");
         }

         responseBody = doHandle(request, session);
      }
      catch (Exception e)
      {
         logger.warn("Error processing request [{}]", request, e);

         responseBody = Errors.fromException(e);
      }

      return buildResponse(responseBody, request, session);
   }

   private MdcContextReference captureAndInitializeContext(ClientMessage request, Session session)
   {
      MdcContextReference context = MdcContext.captureMdcContext();

      MDC.put(MdcContext.MDC_PLACE, session.getActivePlace());
      MDC.put(MdcContext.MDC_FROM, request.getSource());
      MDC.put(MdcContext.MDC_TO, request.getDestination());
      MDC.put(MdcContext.MDC_BY, session.getClient().getPrincipalName());
      MDC.put(MdcContext.MDC_ID, request.getCorrelationId());
      MDC.put(MdcContext.MDC_TYPE, request.getType());

      return context;
   }

   protected abstract MessageBody doHandle(ClientMessage request, Session session);

   protected abstract Address address();

   private void sendResponse(MessageBody responseBody, ClientMessage request, Session session)
   {
      ClientMessage response = buildResponse(responseBody, request, session);

      sendResponse(response, session);
   }

   private ClientMessage buildResponse(MessageBody responseBody, ClientMessage request, Session session)
   {
      return ClientMessage.builder()
         .withPayload(responseBody)
         .withCorrelationId(request.getCorrelationId())
         .withSource(address().getRepresentation())
         .create();
   }

   private void sendResponse(ClientMessage response, Session session)
   {
      session.sendMessage(JSON.toJson(response));
   }
}

