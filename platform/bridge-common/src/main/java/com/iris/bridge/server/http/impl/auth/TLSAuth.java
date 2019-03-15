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
package com.iris.bridge.server.http.impl.auth;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.AttributeKey;

import com.iris.bridge.server.http.RequestAuthorizer;

public abstract class TLSAuth implements RequestAuthorizer {

   public enum TLSAuthType { SERVER, CLIENT, MUTUAL }
   public static AttributeKey<TLSAuthType> TLS_AUTH_TYPE_KEY = AttributeKey.<TLSAuthType>valueOf(TLSAuthType.class.getName());
   
   private final TLSAuthType authType;
   
   public TLSAuth(TLSAuthType authType) {
      this.authType = authType;
   }

   @Override
   public boolean isAuthorized(ChannelHandlerContext ctx, FullHttpRequest req) {
      TLSAuthType channelAuthType = ctx.channel().attr(TLS_AUTH_TYPE_KEY).get();
      return channelAuthType == authType;
   }

   @Override
   public boolean handleFailedAuth(ChannelHandlerContext ctx, FullHttpRequest req) {
      return false;
   }
   
}

