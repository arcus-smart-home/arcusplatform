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
package com.iris.bridge.server.netty;

import org.apache.commons.lang3.StringUtils;

import com.iris.bridge.server.http.impl.HttpRequestParameters;
import com.iris.bridge.server.session.Session;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;

public class BridgeHeaders {
   public static final String HEADER_CLIENT_VERSION = "X-Client-Version";
   public static final String PARAM_CLIENT_VERSION = "v";

   public static final String CONTENT_TYPE_JSON_UTF8 = HttpHeaderValues.APPLICATION_JSON + "; charset=utf-8";

   /**
    * Gets the type of client (app).
    * @param request
    * @return
    */
   public static String getClientType(HttpRequest request) {
      String userAgent = request.headers().get(HttpHeaderNames.USER_AGENT);
      if(StringUtils.isEmpty(userAgent)) {
         return null;
      }

      userAgent = userAgent.toLowerCase();
      if(userAgent.startsWith("ip")) { // iPhone / iPad
         return Session.TYPE_IOS;
      }
      else if(userAgent.startsWith("android")) {
         return Session.TYPE_ANDROID;
      }
      else if(userAgent.startsWith("oculus")) {
         return Session.TYPE_OCULUS;
      }
      else {
         return Session.TYPE_BROWSER;
      }
   }

   public static String getClientVersion(FullHttpRequest request) {
      String clientVersion = request.headers().get(HEADER_CLIENT_VERSION);

      if (StringUtils.isEmpty(clientVersion)) {
         HttpRequestParameters parameters = new HttpRequestParameters(request);
         clientVersion = parameters.getParameter(PARAM_CLIENT_VERSION, null);
      }

      return clientVersion;
   }

   public static String getContentType(FullHttpRequest req) {
      String contentType = req.headers().get(HttpHeaderNames.CONTENT_TYPE);
      if(contentType == null) {
         return "";
      }
      int semi = contentType.indexOf(';');
      if(semi > 0) {
         return contentType.substring(0, semi);
      }
      else {
         return contentType;
      }
   }

   public static OsType getOsType(HttpRequest request) {
      String userAgent = request.headers().get(HttpHeaderNames.USER_AGENT);
      if(StringUtils.isEmpty(userAgent)) {
         return OsType.OTHER;
      }

      userAgent = userAgent.toLowerCase();
      if(userAgent.contains("android")) {
         return OsType.ANDROID;
      }
      else if(userAgent.contains("ios") || userAgent.contains("iphone") || userAgent.contains("ipad")) {
         return OsType.IOS;
      }
      else {
         return OsType.OTHER;
      }
   }

   public enum OsType {
      IOS,
      ANDROID,
      OTHER // could get more specific here but don't care for the moment
   }

}

