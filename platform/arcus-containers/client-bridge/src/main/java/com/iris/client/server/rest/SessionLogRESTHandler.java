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
import com.iris.bridge.server.netty.BridgeHeaders;
import com.iris.bridge.server.session.Session;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageConstants;
import com.iris.messages.service.SessionService;
import com.iris.netty.server.message.LogHandler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

@Singleton
@HttpPost("/" + SessionService.NAMESPACE + "/Log")
public class SessionLogRESTHandler extends HttpResource {
   private Session session;
   private LogHandler handler;

   @Inject
   public SessionLogRESTHandler(BridgeMetrics metrics, AlwaysAllow alwaysAllow, LogHandler handler) {
      super(alwaysAllow, new HttpSender(SessionLogRESTHandler.class, metrics));
      this.handler = handler;
   }

   @Override
   public FullHttpResponse respond(FullHttpRequest httpRequest, ChannelHandlerContext ctx) throws Exception {
      String requestJson = httpRequest.content().toString(CharsetUtil.UTF_8);
      ClientMessage requestMessage = JSON.fromJson(requestJson, ClientMessage.class);
      ClientMessage responseMessage = handler.handle(requestMessage, session);
      FullHttpResponse httpResponse = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, 
            getStatus(responseMessage), 
            Unpooled.copiedBuffer(JSON.toJson(responseMessage), CharsetUtil.UTF_8)
      );
      httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, BridgeHeaders.CONTENT_TYPE_JSON_UTF8);
      return httpResponse;
   }

   private HttpResponseStatus getStatus(ClientMessage message) {
      return MessageConstants.MSG_ERROR.equals(message.getType()) ? HttpResponseStatus.INTERNAL_SERVER_ERROR : HttpResponseStatus.OK;
   }
}

