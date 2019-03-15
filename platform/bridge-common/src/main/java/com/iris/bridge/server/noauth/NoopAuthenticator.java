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
package com.iris.bridge.server.noauth;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.cookie.DefaultCookie;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.bridge.server.netty.Authenticator;

public class NoopAuthenticator implements Authenticator {
   private static final String DOMAIN_NAME_PROP = "domain.name";
   private static final String AUTH_COOKIE_NAME_PROP = "auth.cookie.name";

   @Inject(optional=true)
   @Named(DOMAIN_NAME_PROP)
   private String domainName = null;

   @Inject(optional=true)
   @Named(AUTH_COOKIE_NAME_PROP)
   protected String authCookieName = "AuthToken";

   @Override
   public FullHttpResponse authenticateRequest(Channel channel, FullHttpRequest req) {
      return null;
   }

   @Override
   public FullHttpResponse authenticateRequest(Channel channel, String username, String password, String isPublic, ByteBuf responseContentIfSuccess)
   {
      return null;
   }

   @Override
   public DefaultCookie createCookie(String value) {
      return createCookie(value, Long.MAX_VALUE);
   }

   @Override
   public DefaultCookie expireCookie() {
      return createCookie("", 1L);
   }

   private DefaultCookie createCookie(String value, Long maxAge) {
      DefaultCookie nettyCookie = new DefaultCookie(authCookieName, value);
      nettyCookie.setMaxAge(maxAge);
      nettyCookie.setHttpOnly(true);
      nettyCookie.setPath("/");
      nettyCookie.setDomain(domainName);
      return nettyCookie;
   }
   
}

