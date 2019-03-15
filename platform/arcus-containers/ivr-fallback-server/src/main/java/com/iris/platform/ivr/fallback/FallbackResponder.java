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
package com.iris.platform.ivr.fallback;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.codahale.metrics.Counter;
import com.google.common.net.MediaType;
import com.iris.bridge.server.http.Responder;

public class FallbackResponder implements Responder {

   private final String resource;
   private final Counter counter;

   public FallbackResponder(String resource, Counter counter) {
      this.resource = resource;
      this.counter = counter;
   }

   @Override
   public void sendResponse(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      counter.inc();
      byte[] content = null;

      try(InputStream is = FallbackResponder.class.getClassLoader().getResourceAsStream(resource)) {
         content = IOUtils.toByteArray(is);
      }

      FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
      HttpHeaders.setContentLength(response, content.length);
      response.headers().set(HttpHeaders.Names.CONTENT_TYPE, MediaType.APPLICATION_XML_UTF_8.toString());
      response.content().writeBytes(content);
      ctx.write(response);
      ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
      future.addListener(ChannelFutureListener.CLOSE);
   }
}

