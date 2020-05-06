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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;

import com.iris.bridge.server.http.HttpException;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.RequestHandler;
import com.iris.bridge.server.http.RequestMatcher;
import com.iris.bridge.server.http.Responder;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.matcher.MultiMatcher;
import com.iris.bridge.server.http.impl.matcher.NeverMatcher;
import com.iris.bridge.server.http.impl.matcher.WildcardMatcher;

/**
 * Extend this class to provide a Http request handler.
 * 
 * Use @HttpGet(url) or @HttpPost(url) to specify a wildcard matcher for the resource. Multiple annotations may be used.
 *
 */
public class RequestHandlerImpl implements RequestHandler {
	private static Logger logger = LoggerFactory.getLogger(RequestHandlerImpl.class);
   protected RequestMatcher matcher;
   protected RequestAuthorizer authorizer;
   protected Responder responder;
   
   public RequestHandlerImpl(RequestMatcher matcher, RequestAuthorizer authorizer, Responder responder) {
      this.matcher = matcher;
      this.authorizer = authorizer;
      this.responder = responder;
   }
   
   public RequestHandlerImpl(RequestAuthorizer authorizer, Responder responder) {
      this(null, authorizer, responder);
   }
   
   @PostConstruct
   protected void init() {
      if (matcher == null) {
         matcher = createMatcher();
      }
   }
   
   @Override
   public boolean matches(FullHttpRequest req) {
      return matcher.matches(req);
   }
   
   @Override
   public void handleRequest(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      if (!authorizer.isAuthorized(ctx, req)) {
      	logger.warn("Authorization failed for request {}", req.uri());
         if (!authorizer.handleFailedAuth(ctx, req))  {
         	logger.error("Failed to handle authorization failure for request {}", req);
            throw new HttpException(HttpResponseStatus.FORBIDDEN.code());
         }
         return;
      }
      logger.debug("Handling request for [{} {}]", req.method(), req.uri());
      responder.sendResponse(req, ctx);
   }

   @Override
   public String toString() {
      return "RequestHandlerImpl [matcher=" + matcher + ", authorizer="
            + authorizer + ", responder=" + responder + "]";
   }

   public static void setNoCacheHeaders(HttpResponse response) {
      response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
      response.headers().set(HttpHeaderNames.PRAGMA, "no-cache");
      response.headers().set(HttpHeaderNames.EXPIRES, "0");
   }
   
   private RequestMatcher createMatcher() {
      HttpGet[] gets = this.getClass().getAnnotationsByType(HttpGet.class);
      HttpPost[] posts = this.getClass().getAnnotationsByType(HttpPost.class);
      
      if ((gets.length + posts.length) > 0) {
         List<RequestMatcher> matchers = new ArrayList<>(gets.length + posts.length);
         for (HttpGet get : gets) {
            matchers.add(new WildcardMatcher(get.value(), HttpMethod.GET));
         }
         for (HttpPost post : posts) {
            matchers.add(new WildcardMatcher(post.value(), HttpMethod.POST));
         }
         return new MultiMatcher(matchers);
      }
      else {
         return new NeverMatcher();
      }
   }
}

