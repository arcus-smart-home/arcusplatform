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
package com.iris.oauth;

import com.google.common.net.HttpHeaders;
import com.iris.bridge.server.auth.basic.BasicAuthCredentials;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Objects;
import com.iris.messages.errors.MissingParameterException;
import org.apache.commons.lang3.StringUtils;

public class OAuthUtil {

   public static final String AUTH_BEARER = "Bearer";

   public static final String ATTR_GRANT_TYPE = "grant_type";
   public static final String GRANT_AUTH_CODE = "authorization_code";
   public static final String GRANT_REFRESH = "refresh_token";

   public static final String ATTR_CODE = "code";
   public static final String ATTR_CLIENT_ID = "client_id";
   public static final String ATTR_CLIENT_SECRET = "client_secret";
   public static final String ATTR_REDIRECT_URI = "redirect_uri";

   public static final String ATTR_REFRESH = "refresh_token";
   public static final String ATTR_RESPONSETYPE = "response_type";
   public static final String ATTR_SCOPE = "scope";
   public static final String ATTR_STATE = "state";
   public static final String ATTR_ERROR = "error";
   public static final String ATTR_TOKEN = "token";
   public static final String ATTR_TOKEN_TYPE = "token_type_hint";

   public static final String ERR_INVALID_REQUEST = "invalid_request";
   public static final String ERR_INVALID_SCOPE = "invalid_scope";
   public static final String ERR_UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
   public static final String ERR_SERVER_ERROR = "server_error";

   public static final String DEFAULT_PLACE_ATTR = "defaultplace";

   private OAuthUtil() {
   }

   public static String extractFormParam(HttpPostRequestDecoder decoder, String name, boolean required) throws IOException {
      InterfaceHttpData data = decoder.getBodyHttpData(name);
      if(data != null && data.getHttpDataType() == HttpDataType.Attribute) {
         Attribute attribute = (Attribute) data;
         return attribute.getValue();
      }
      if(required) {
         throw new MissingParameterException(name);
      }

      return null;
   }

   public static BasicAuthCredentials extractApplicationCredentials(FullHttpRequest req) throws Exception {
      String authHeader = req.headers().get(HttpHeaders.AUTHORIZATION);
      if(!StringUtils.isBlank(authHeader)) {
         return BasicAuthCredentials.fromAuthHeaderString(authHeader);
      }
      HttpPostRequestDecoder decoder = null;
      try {
         decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), req);
         return extractApplicationCredentials(decoder, req);
      } finally {
         if(decoder != null) {
            decoder.destroy();
         }
      }
   }

   public static BasicAuthCredentials extractApplicationCredentials(HttpPostRequestDecoder decoder, FullHttpRequest req) throws Exception {
      String authHeader = req.headers().get(HttpHeaders.AUTHORIZATION);
      if(!StringUtils.isBlank(authHeader)) {
         return BasicAuthCredentials.fromAuthHeaderString(authHeader);
      }
      String appId = extractFormParam(decoder, OAuthUtil.ATTR_CLIENT_ID, true);
      String pass = extractFormParam(decoder, OAuthUtil.ATTR_CLIENT_SECRET, true);
      return new BasicAuthCredentials(appId, pass);
   }

   public static Optional<String> extractTokenFromBearer(FullHttpRequest req) {
      String header = req.headers().get(HttpHeaders.AUTHORIZATION);
      if(StringUtils.isBlank(header) || !StringUtils.containsIgnoreCase(header, OAuthUtil.AUTH_BEARER)) {
         return Optional.empty();
      }
      int idx = StringUtils.indexOfIgnoreCase(header, OAuthUtil.AUTH_BEARER);
      return Optional.of(header.substring(idx + OAuthUtil.AUTH_BEARER.length()).trim());
   }

   public static FullHttpResponse badRequest() {
      return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
   }

   public static FullHttpResponse ok() {
      return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
   }


   public static FullHttpResponse badRequest(String msg) {
      FullHttpResponse res = badRequest();
      res.content().writeBytes(msg.getBytes(StandardCharsets.UTF_8));
      return res;
   }

   public static FullHttpResponse unauthorized() {
      return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
   }

   public static Map<String, String> extractQueryArgs(FullHttpRequest req) {
      QueryStringDecoder decoder = new QueryStringDecoder(req.getUri());
      return decoder.parameters()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, (v) -> { return v.getValue().get(0); }));
   }

   public static boolean validateRedirect(String uri, String configuredUri) {
      try {
         URI incoming = new URI(uri);
         URI configured = new URI(configuredUri);
         return Objects.equal(incoming.getScheme(), configured.getScheme()) && Objects.equal(incoming.getHost(), configured.getHost());
      } catch(Exception e) {
         throw new RuntimeException(e);
      }
   }
}

