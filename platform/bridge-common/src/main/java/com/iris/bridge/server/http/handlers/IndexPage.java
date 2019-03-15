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

import java.util.Date;

import com.google.common.base.Charsets;
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
@HttpGet("/index.html")
public class IndexPage extends HttpPageResource {

   private final String html;

   private final BridgeServerConfig serverConfig;

   @Inject
   public IndexPage(AlwaysAllow alwaysAllow, BridgeMetrics metrics, ClientFactory factory, BridgeServerConfig serverConfig) {
      super(alwaysAllow, new HttpSender(IndexPage.class, metrics), factory);
      this.serverConfig = serverConfig;
      html = load();
   }
   
   private String load() {
      return
            "<html><head><title>" + serverConfig.getServerName() + " Server</title></head>" + NEWLINE +
            "<body>" + NEWLINE +
            "<h2>" + serverConfig.getServerName() + " Server</h2>" + NEWLINE +
            "Version: " + serverConfig.getApplicationVersion() + NEWLINE +
            "<div>" + NEWLINE +
               "<b>User Name:</b> %s<br/>" + NEWLINE +
               "<b>Client Token:</b> %s<br/>"  + NEWLINE +
               "<b>Session Started:</b> %s<br/>" + NEWLINE +
               "<b>Session Expires:</b> %s<br/>" + NEWLINE +
               "%s" + NEWLINE +
            "</div>" + NEWLINE +
            "</body>" + NEWLINE +
            "</html>" + NEWLINE
            ;
   }

   @Override
   public ByteBuf getContent(Client client, String uri) {
      String user = client.getPrincipalName();
      String token = "[no token]";
      String startTime = "[unknown]";
      String expirationTime = "[never]";
      String link;
      
      Date loginTime = client.getLoginTime();
      if(loginTime != null) {
         startTime = loginTime.toString();
      }
      Date expirationDate = client.getExpirationTime();
      if(expirationDate != null) {
         expirationTime = expirationDate.toString();
      }
      
      if(client.isAuthenticated()) {
         link = "<a href='/logout'>Logout</a>";
      }
      else if (serverConfig.getLoginPageEnabled()) {
         link = "<a href='/login'>Login</a>";
      } else {
         link = "";
      }
      
      
      return Unpooled.copiedBuffer(
            String.format(html, user, token, startTime, expirationTime, link),
            Charsets.US_ASCII
      );
   }
   
}

