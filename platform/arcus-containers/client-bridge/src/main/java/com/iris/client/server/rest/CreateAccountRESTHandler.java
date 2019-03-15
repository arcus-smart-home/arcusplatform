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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.core.platform.handlers.CreateAccountHandler;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.AccountService.CreateAccountRequest;
import com.iris.messages.services.PlatformConstants;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

@Singleton
@HttpPost("/account/CreateAccount")
public class CreateAccountRESTHandler extends HttpResource {
   private final CreateAccountHandler delegate;
   private final Authenticator authenticator;

   @Inject
   public CreateAccountRESTHandler(Authenticator authenticator, CreateAccountHandler delegate, BridgeMetrics metrics, AlwaysAllow alwaysAllow) {
      super(alwaysAllow, new HttpSender(CreateAccountRESTHandler.class, metrics));
      this.delegate = delegate;
      this.authenticator = authenticator;
   }

   @Override
   public FullHttpResponse respond(FullHttpRequest request, ChannelHandlerContext ctx) {
      String json = request.content().toString(CharsetUtil.UTF_8);
      ClientMessage clientMessage = JSON.fromJson(json, ClientMessage.class);
      ClientMessage.Builder responseBuilder = ClientMessage.builder()
            .withCorrelationId(clientMessage.getCorrelationId())
            .withSource(Address.platformService(PlatformConstants.SERVICE_ACCOUNTS).getRepresentation());

      PlatformMessage platMsg = PlatformMessage.buildMessage(
            clientMessage.getPayload(),
            // TODO: source is ignored for rest, so just hard coding
            Address.clientAddress("rest", "1"),
            Address.platformService(PlatformConstants.SERVICE_ACCOUNTS))
            .withCorrelationId(clientMessage.getCorrelationId())
            .create();

      MessageBody responseBody = null;
      HttpResponseStatus responseStatus = null;
      FullHttpResponse httpResponse = null;
      try {
         MessageBody requestBody = platMsg.getValue();
         responseBody = this.delegate.handleStaticRequest(platMsg);  
         if(!ErrorEvent.MESSAGE_TYPE.equals(responseBody.getMessageType())) {
            //create the session cookie and put cookie in response
            String email = CreateAccountRequest.getEmail(requestBody);
            String password = CreateAccountRequest.getPassword(requestBody);
            String isPublic = CreateAccountRequest.getIsPublic(requestBody);
            ClientMessage responseMessage = responseBuilder.withPayload(responseBody).create();
            ByteBuf responseContent = Unpooled.copiedBuffer(JSON.toJson(responseMessage), CharsetUtil.UTF_8);
            httpResponse = authenticator.authenticateRequest(ctx.channel(), email, password, isPublic, responseContent);
            responseStatus = HttpResponseStatus.OK;
            return httpResponse;
         }
         
      } catch(ErrorEventException e) {
         responseBody = Errors.fromException(e);
         responseStatus = HttpResponseStatus.BAD_REQUEST;
      }
      catch(IllegalArgumentException iae) {
         responseBody = Errors.fromException(iae);
         responseStatus = HttpResponseStatus.BAD_REQUEST;
      } catch(Exception e) {
         responseBody = Errors.fromException(e);
         responseStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR;
      }

      if(MessageConstants.MSG_ERROR.equals(responseBody.getMessageType())) {
         if(CreateAccountHandler.EMAIL_IN_USE_ERROR_CODE.equals(responseBody.getAttributes().get(ErrorEvent.CODE_ATTR))) {
            responseStatus = HttpResponseStatus.CONFLICT;
         } else if(CreateAccountHandler.ARGUMENT_ERROR.equals(responseBody.getAttributes().get(ErrorEvent.CODE_ATTR))) {
            responseStatus = HttpResponseStatus.BAD_REQUEST;
         }
      }

      ClientMessage responseMessage = responseBuilder.withPayload(responseBody).create();
      ByteBuf responseContent = Unpooled.copiedBuffer(JSON.toJson(responseMessage), CharsetUtil.UTF_8);
      if(httpResponse == null) {
         return new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            responseStatus,
            responseContent);
      }else{
         httpResponse.content().writeBytes(responseContent);
         return httpResponse;
      }
      
   }
}

