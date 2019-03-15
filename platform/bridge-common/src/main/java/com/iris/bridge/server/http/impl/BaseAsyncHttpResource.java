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
import io.netty.handler.codec.http.HttpResponseStatus;

public abstract class BaseAsyncHttpResource extends RequestHandlerImpl {
	private static final Logger logger = LoggerFactory.getLogger(BaseAsyncHttpResource.class);
   
   protected final HttpSender sender;
   private final Executor executor;
   
   public BaseAsyncHttpResource(RequestAuthorizer authorizer, HttpSender sender, Executor executor) {
      super(authorizer, null);
      this.sender = sender;
      this.executor = executor;
   }
  
   @Override
   public void handleRequest(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      if (!authorizer.isAuthorized(ctx, req)) {
      	logger.warn("Authorization failed for request {}", req);
         if (!authorizer.handleFailedAuth(ctx, req))  {
         	logger.error("Failed to handle authorization failure for request {}", req);
            throw new HttpException(HttpResponseStatus.FORBIDDEN.code());
         }
         return;
      }

      logger.debug("Handling request for [{} {}]", req.getMethod(), req.getUri());

      /*
       * This call prevents an "io.netty.util.IllegalReferenceCountException: refCnt: 0" when we try to read the content
       * ByteBuf in the Executor thread.  Without it, the FullHttpRequest's content ByteBuf would get deallocated by
       * Netty's SimpleChannelInboundHandler.channelRead(), which calls ReferenceCountUtil.release(msg) in its finally
       * block.
       * 
       * Netty does manual reference-counting and de-allocating of certain significant resources like request bodies for
       * high performance: http://netty.io/wiki/reference-counted-objects.html
       * 
       * More info:
       * https://stackoverflow.com/questions/28647048/why-do-we-need-to-manually-handle-reference-counting-for-netty-bytebuf-if-jvm-gc
       */
      req.retain();

      executor.execute(() ->
      {
         try
         {
            doHandleRequest(req, ctx);
         }
         finally
         {
            /*
             * This call prevents a Netty ByteBuf pool leak.  Without it, the content ByteBuf would be GC'ed without the
             * Netty ByteBuf pool's knowledge, which would result in a Netty ByteBuf pool leak, and eventually a memory
             * leak as the Netty ByteBuf pool grows in size without realizing that some of its buffers have been GC'ed
             * and should no longer be in service.
             */
            req.release();
         }
      });
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
   protected abstract void doHandleRequest(FullHttpRequest req, ChannelHandlerContext ctx);

}

