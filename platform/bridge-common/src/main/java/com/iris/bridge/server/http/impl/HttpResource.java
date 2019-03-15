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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.Responder;

public abstract class HttpResource extends RequestHandlerImpl {
   protected static final String NEWLINE = "\r\n";
   
   private final HttpSender httpSender;
   
   public HttpResource(RequestAuthorizer authorizer, HttpSender httpSender) {
      super(authorizer, null);
      this.httpSender = httpSender;
   }
  
   @Override
   protected void init() {
      super.init();
      responder = new ResponderImpl();
   }
   
   public abstract FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception;

   private class ResponderImpl implements Responder {

      @Override
      public void sendResponse(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
         FullHttpResponse res = respond(req, ctx);
         httpSender.sendHttpResponse(ctx, req, res);
      }
      
   }
}

