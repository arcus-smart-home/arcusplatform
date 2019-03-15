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

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bridge.server.http.HttpException;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

public abstract class AsyncHttpResource extends BaseAsyncHttpResource {
	private static final Logger logger = LoggerFactory.getLogger(AsyncHttpResource.class);
   
   public AsyncHttpResource(RequestAuthorizer authorizer, HttpSender sender, Executor executor) {
      super(authorizer, sender, executor);
   }
  
   /**
    * Implementation point for handling the request.  At this point the
    * authorizer will have been applied to the message and it will
    * be executing in a background thread pool.
    * @param req
    * @param ctx
    * @return
    * @throws Exception
    */
   protected abstract FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception;

   @Override
   protected void doHandleRequest(FullHttpRequest req, ChannelHandlerContext ctx) {
   	try {
         FullHttpResponse res = respond(req, ctx);
   		sender.sendHttpResponse(ctx, req, res);
   	}
   	catch(Exception e) {
         logger.warn("Error handling request [{} {}]", req.getMethod(), req.getUri(), e);
         if(e instanceof HttpException) {
         	sender.sendError(ctx, ((HttpException) e).getStatusCode(), req);
         }
         else {
            sender.sendError(ctx, HttpSender.STATUS_SERVER_ERROR, req);
         }
   	}
   }

}

