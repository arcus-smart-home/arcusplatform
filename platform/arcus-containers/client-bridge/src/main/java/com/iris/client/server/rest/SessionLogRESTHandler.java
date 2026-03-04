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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.netty.BridgeHeaders;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.service.SessionService;
import com.iris.messages.service.SessionService.LogRequest;
import com.iris.messages.service.SessionService.LogResponse;

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
   private static final Logger sessionLogger = LoggerFactory.getLogger("session");

   @Inject
   public SessionLogRESTHandler(BridgeMetrics metrics, AlwaysAllow alwaysAllow) {
      super(alwaysAllow, new HttpSender(SessionLogRESTHandler.class, metrics));
   }

   @Override
   public FullHttpResponse respond(FullHttpRequest httpRequest, ChannelHandlerContext ctx) throws Exception {
      String requestJson = httpRequest.content().toString(CharsetUtil.UTF_8);
      ClientMessage requestMessage = JSON.fromJson(requestJson, ClientMessage.class);

      MessageBody requestBody = requestMessage.getPayload();
      String category = StringUtils.defaultIfEmpty(LogRequest.getCategory(requestBody), "[notset]");
      String code = StringUtils.defaultIfEmpty(LogRequest.getCode(requestBody), "[notset]");
      String message = StringUtils.defaultIfEmpty(LogRequest.getMessage(requestBody), "[none]");

      sessionLogger.info("SessionLogMessage client:rest person:[none] place:[notset] category:{} code:{} message:{}",
         category, code, message);

      ClientMessage responseMessage = ClientMessage.builder()
         .withPayload(LogResponse.instance())
         .withCorrelationId(requestMessage.getCorrelationId())
         .withSource(Address.platformService(SessionService.NAMESPACE).getRepresentation())
         .create();

      FullHttpResponse httpResponse = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(JSON.toJson(responseMessage), CharsetUtil.UTF_8)
      );
      httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, BridgeHeaders.CONTENT_TYPE_JSON_UTF8);
      return httpResponse;
   }
}
