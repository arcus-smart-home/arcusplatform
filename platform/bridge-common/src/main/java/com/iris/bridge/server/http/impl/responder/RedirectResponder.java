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
package com.iris.bridge.server.http.impl.responder;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.Responder;

public class RedirectResponder implements Responder {
   
   private final String redirectUri;
   private final HttpSender httpSender;
   
   public RedirectResponder(String redirectUri, HttpSender httpSender) {
      this.redirectUri = redirectUri;
      this.httpSender = httpSender;
   }

   @Override
   public void sendResponse(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      httpSender.sendRedirect(ctx, redirectUri, req);
   }

}

