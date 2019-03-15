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
package com.iris.client.impl.netty;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

public class NettyHttpResponse {
   private final FullHttpResponse response;
   private Map<String, String> cookies = null;
   
   public NettyHttpResponse(FullHttpResponse response) {
      this.response = response;
   }
   
   @Nullable
   public String getLocation() {
      return response.headers().get(HttpHeaders.Names.LOCATION);
   }
   
   public int getStatusCode() {
      return response.getStatus().code();
   }
   
   public String getBodyAsText() {
      ByteBuf buf = response.content();
      return buf != null ? buf.toString(Charset.forName("UTF-8")) : "";
   }
   
   public String getCookieValue(String name) {
      if (cookies == null) {
         decodeCookies();
      }
      return cookies.get(name);
   }
   
   private void decodeCookies() {
      List<String> values = response.headers().getAll(HttpHeaders.Names.SET_COOKIE);
      cookies = new HashMap<>(values.size());
      for (String value : values) {
         Set<Cookie> setOfCookies = CookieDecoder.decode(value);
         for(Cookie cookie: setOfCookies) {
            cookies.put(cookie.getName(), cookie.getValue());
         }
      }
   }
}

