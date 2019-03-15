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
package com.iris.video.preview.server.handlers;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedStream;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.HttpException;
import com.iris.bridge.server.http.Responder;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.RequestHandlerImpl;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.video.storage.PreviewStorage;

import static com.iris.video.preview.server.VideoPreviewMetrics.*;

@Singleton
@HttpGet("/preview*")
public class PreviewHandler extends RequestHandlerImpl {
   private static final Logger logger = LoggerFactory.getLogger(PreviewHandler.class);

   @Inject
   public PreviewHandler(SessionAuth auth, PreviewStorage storage, ClientFactory factory) {
      super(auth, new PreviewResponder(storage, factory));
   }

   private static class PreviewResponder implements Responder {
      private final ClientFactory factory;
      private final PreviewStorage storage;
      private static final Pattern pathParamsPattern = Pattern.compile("^/preview/([\\d-abcdef]{36})/([\\d-abcdef]{36})/?(?:\\?.*)?$", Pattern.CASE_INSENSITIVE); // /preview/{place id}/{camera id}(? optional query string)

      private PreviewResponder(PreviewStorage storage, ClientFactory factory) {
         this.storage = storage;
         this.factory = factory;
      }

      @Override
      public void sendResponse(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
         logger.trace("Handling incoming request for {}", req.getUri());

         Matcher pathMatcher = pathParamsPattern.matcher(req.getUri());

         if(!pathMatcher.matches()) {
            PREVIEW_BAD.inc();
            throw new HttpException(HttpResponseStatus.BAD_REQUEST.code());
         }

         String place = pathMatcher.group(1);
         if(!isAuthorized(ctx, place)) {
            PREVIEW_UNAUTH.inc();

            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN);
            ChannelFuture future = ctx.writeAndFlush(response);
            future.addListener(ChannelFutureListener.CLOSE);
            return;
         }

         String cam = pathMatcher.group(2);

         byte[] content;
         try (Timer.Context context = PREVIEW_READ.time()) {
            content = storage.read(cam);
         }

         if(content == null) {
            PREVIEW_NOTFOUND.inc();
            throw new HttpException(HttpResponseStatus.NOT_FOUND.code());
         }

         PREVIEW_SIZE.update(content.length);

         HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
         setNoCacheHeaders(response);
         HttpHeaders.setTransferEncodingChunked(response);
         response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "image/jpeg");

         if(HttpHeaders.isKeepAlive(req)) {
            response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
         }

         ctx.write(response);
         ctx.write(new ChunkedStream(new ByteArrayInputStream(content)));
         ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
         if(!HttpHeaders.isKeepAlive(req)) {
            future.addListener(ChannelFutureListener.CLOSE);
         }
      }

      private boolean isAuthorized(ChannelHandlerContext ctx, String place) {
         Client client = factory.get(ctx.channel());
         UUID placeId = UUID.fromString(place);
         List<AuthorizationGrant> grants = client.getAuthorizationContext().getGrants();
         for(AuthorizationGrant grant : grants) {
            if(Objects.equal(grant.getPlaceId(), placeId)) {
               return true;
            }
         }
         return false;
      }
   }
}

