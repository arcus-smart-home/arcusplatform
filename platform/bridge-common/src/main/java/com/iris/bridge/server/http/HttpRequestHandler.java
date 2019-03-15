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
package com.iris.bridge.server.http;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.impl.HttpResource;

// FIXME should allow HttpRequest instead of FullHttpRequest
@Sharable
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
   private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);
   
   private final HttpSender sender;
   private final List<HttpResource> resources;
   
   @Inject
   public HttpRequestHandler(
         Set<HttpResource> resources,
         BridgeMetrics metrics
   ) {
      this.resources = new ArrayList<>(resources);
      this.sender = new HttpSender(HttpRequestHandler.class, metrics);
   }
   
   /* (non-Javadoc)
    * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.ChannelHandlerContext, java.lang.Object)
    */
   @Override
   protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
      try {
         for(HttpResource resource: resources) {
            if(resource.matches(req)) {
               resource.handleRequest(req, ctx);
               return;
            }
         }
         sender.sendError(ctx, HttpSender.STATUS_NOT_FOUND, req);
      }
      catch(Exception e) {
         logger.warn("Error handling request [{} {}]", req.getMethod(), req.getUri(), e);
         sender.sendError(ctx, HttpSender.STATUS_SERVER_ERROR, req);
      }
   }
}

