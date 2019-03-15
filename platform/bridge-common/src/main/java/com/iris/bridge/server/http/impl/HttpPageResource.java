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
package com.iris.bridge.server.http.impl;

import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.HttpException;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;

public abstract class HttpPageResource extends HttpResource {
   protected final ClientFactory factory;
   
   public HttpPageResource(RequestAuthorizer authorizer, HttpSender httpSender, ClientFactory factory) {
      super(authorizer, httpSender);
      this.factory = factory;
   }
   
   public abstract ByteBuf getContent(Client context, String uri);

   @Override
   public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      String uri = req.getUri();
      ByteBuf content = getContent(factory.get(ctx.channel()), uri);
      if(content != null) {
         FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
         res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
         setContentLength(res, content.readableBytes());
         return res;
      }
      else {
         throw new HttpException(HttpSender.STATUS_NOT_FOUND);
      }
   }
}

