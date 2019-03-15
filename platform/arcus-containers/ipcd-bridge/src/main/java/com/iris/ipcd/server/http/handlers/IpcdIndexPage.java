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
package com.iris.ipcd.server.http.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.HttpPageResource;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;

@Singleton
@HttpGet("/index.html")
public class IpcdIndexPage extends HttpPageResource {
   
   @Inject(optional=true)
   @Named("server.name")
   private String serverName = "Ipcd Bridge";
   
   @Inject(optional=true)
   @Named("application.version")
   private String version = "[unknown]";
      
   private String pageHeader;

   @Inject
   public IpcdIndexPage(AlwaysAllow alwaysAllow, BridgeMetrics metrics, ClientFactory clientFactory) {
      super(alwaysAllow, new HttpSender(IpcdIndexPage.class, metrics), clientFactory);
   }
   
   @Override
   protected void init() {
      super.init();
      pageHeader = loadHeader(serverName, version);
   }
   
   @Override
   public ByteBuf getContent(Client context, String uri) {
      StringBuilder sb = new StringBuilder(pageHeader);
      sb.append(FOOTER);
      return Unpooled.copiedBuffer(sb.toString(), Charsets.US_ASCII);
   }

   private static String loadHeader(String serverName, String version) {
      return  "<html><head>" + NEWLINE + 
            "<title>" + serverName + " Server</title>" + NEWLINE + 
            "<style>" + NEWLINE +
               "th { border-bottom: 2px solid black; }" + NEWLINE +
               "td { border: 1px solid black; }" + NEWLINE +
            "</style>" + NEWLINE +
            "</head>" + NEWLINE +
            "<body>" + NEWLINE +
            "<h2>" + serverName + " Server</h2>" + NEWLINE + 
            "<p>Version: " + version + "</p>" + NEWLINE;
   }
   
   private final static String FOOTER =  
         "</body>" + NEWLINE + 
         "</html>" + NEWLINE;
}

