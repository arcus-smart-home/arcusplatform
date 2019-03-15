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
/**
 * 
 */
package com.iris.bridge.server.http.handlers;

import com.iris.bridge.server.config.BridgeServerConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.HttpPageResource;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;

/**
 * 
 */
@Singleton
@HttpGet("/login")
public class LoginPage extends HttpPageResource {

   private final byte [] contents;

   private final BridgeServerConfig serverConfig;

   @Inject
   public LoginPage(AlwaysAllow alwaysAllow, BridgeMetrics metrics, ClientFactory factory, BridgeServerConfig serverConfig) {
      super(alwaysAllow, new HttpSender(LoginPage.class, metrics), factory);
      this.serverConfig = serverConfig;
      contents = load();
   }
   
   private byte[] load() {
      String html = 
               "<html><head><title>" + serverConfig.getServerName() + " Server</title></head>" + NEWLINE +
               "<body>" + NEWLINE +
               "<h2>" + serverConfig.getServerName() + " Server</h2>" + NEWLINE +
               "<form action='/login' method='POST'>" + NEWLINE +
                  "Username: <input name='user' /><br/>" + NEWLINE +
                  "Password: <input type='password' name='password' /><br/>" + NEWLINE +
                  "<input type='checkbox' name='public' value='true' checked/>Public Computer<br/>" + NEWLINE +
                  "<input type='submit' value='login'/>" + NEWLINE +
               "</form>" + NEWLINE +
               "</body>" + NEWLINE + 
               "</html>" + NEWLINE;
      return html.getBytes(CharsetUtil.US_ASCII);
   }

   @Override
   public ByteBuf getContent(Client client, String uri) {
      if (!serverConfig.getLoginPageEnabled()) {
        return null;
      }

      return Unpooled.copiedBuffer(contents);
   }
   
}

