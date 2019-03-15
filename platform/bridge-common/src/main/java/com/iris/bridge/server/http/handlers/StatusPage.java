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

import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Date;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.platform.cluster.ClusterService;
import com.iris.platform.cluster.ClusterServiceRecord;

/**
 * 
 */
@Singleton
@HttpGet("/status")
public class StatusPage extends HttpResource {

   public static final String SERVER_NAME_PROP = "server.name";
      
   @Inject(optional=true)
   @Named(SERVER_NAME_PROP)
   private String serverName = "";
   
   @Inject(optional=true)
   @Named("application.version")
   private String version = "[unknown]";

   @Nullable
   private final ClusterService service;

   @Inject
   public StatusPage(
         AlwaysAllow alwaysAllow, 
         BridgeMetrics metrics,
         Optional<ClusterService> clusterServiceRef
   ) {
      super(alwaysAllow, new HttpSender(StatusPage.class, metrics));
      this.service = clusterServiceRef.orNull();
   }
   
   public String getServerName() {
      return serverName;
   }
   
   public void setServerName(String serverName) {
      this.serverName = serverName;
   }

   // TODO make this plug-able
   @Override
   public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      HttpResponseStatus status;
      String content;
      if(service != null) {
         ClusterServiceRecord record = service.getServiceRecord().orNull();
         if(record == null) {
            status = HttpResponseStatus.SERVICE_UNAVAILABLE;
            content = clusteredPendingId();
         }
         else {
            status = HttpResponseStatus.OK;
            content = clusteredHealthy(record);
         }
      }
      else {
         status = HttpResponseStatus.OK;
         content = healthy();
      }
      ByteBuf buffer = Unpooled.wrappedBuffer(content.getBytes(Charsets.UTF_8));
      FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, status, buffer);
      res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
      setContentLength(res, buffer.readableBytes());
      return res;
   }
   
   // TODO replace this with handlebars templates
   private String healthy() {
      return String.format(
            "<html><head><title>%s Server</title></head>" + NEWLINE +
            "<body>" + NEWLINE +
            "<h2>%s Server</h2>" + NEWLINE + 
            "<h3>Status: ONLINE</h3>" + NEWLINE +
            "<div>Version: %s</div>" + NEWLINE +
            "</body>" + NEWLINE + 
            "</html>" + NEWLINE,
            serverName,
            serverName,
            version
      );
   }
   
   private String clusteredHealthy(ClusterServiceRecord record) {
      return String.format(
            "<html><head><title>%s Server</title></head>" + NEWLINE +
            "<body>" + NEWLINE +
            "<h2>%s Server</h2>" + NEWLINE + 
            "<h3>Status: ONLINE</h3>" + NEWLINE +
            "<div>Host: %s</div>" + NEWLINE +
            "<div>Container: %s-%s</div>" + NEWLINE +
            "<div>Version: %s</div>" + NEWLINE +
            "<div>Registered: %s</div>" + NEWLINE +
            "</body>" + NEWLINE + 
            "</html>" + NEWLINE,
            serverName,
            serverName,
            record.getHost(),
            record.getService(),
            record.getMemberId(),
            version,
            new Date(record.getRegistered().toEpochMilli())
      );
   }
   
   private String clusteredPendingId() {
      return String.format(
            "<html><head><title>%s Server</title></head>" + NEWLINE +
            "<body>" + NEWLINE +
            "<h2>%s Server</h2>" + NEWLINE + 
            "<h3>Status: STANDBY</h3>" + NEWLINE +
            "<div>Version: %s</div>" + NEWLINE +
            "<div>Waiting for Cluster ID</div>" + NEWLINE +
            "</body>" + NEWLINE + 
            "</html>" + NEWLINE,
            serverName,
            serverName,
            version
      );
   }
}

