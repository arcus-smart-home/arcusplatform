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
package com.iris.video.streaming.server.http;

import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.video.VideoRecording;
import com.iris.video.streaming.server.VideoStreamingServerConfig;
import com.iris.video.streaming.server.dao.VideoStreamingDao;
import com.iris.video.streaming.server.dao.VideoStreamingSession;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;

@Singleton
@HttpGet("/dash-video/*")
public class DashVideoHandler extends AbstractStreamingHandler {
   public static final int UUID_START = "/dash-video/".length();
   public static final int UUID_END = UUID_START + 36;

   private final VideoStreamingServerConfig config;
   private final VideoStreamingDao dao;

   private final String dashBase;

   @Inject
   public DashVideoHandler(BridgeMetrics metrics, AlwaysAllow alwaysAllow, VideoStreamingServerConfig config, VideoStreamingDao dao) {
      super(alwaysAllow, new HttpSender(DashVideoHandler.class, metrics), config.getStreamingSecretAsSpec());
      this.config = config;
      this.dao = dao;

      this.dashBase = config.getVideoStreamingUrl() + ":" + config.getTcpPort() + "/dash/";
   }

   @Override
   public FullHttpResponse respond(@Nullable FullHttpRequest request, @Nullable ChannelHandlerContext ctx) throws Exception {
      if (request == null || ctx == null) {
         FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
         return errResponse;
      }

      QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
      UUID id = getRecordingId(decoder, UUID_START, UUID_END, "/playlist.m3u8");
      if (id == null || !validateRequest(request, id, decoder)) {
         FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
         return errResponse;
      }

      ByteBuf response = getResponse(id);
      if (response == null) {
         FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
         return errResponse;
      }

      return getNoCacheFullHttpResponse(response, "text/html");
   }

   @Nullable
   private ByteBuf getResponse(UUID id) throws Exception {
      VideoStreamingSession session;
      try {
         session = dao.session(id);
      } catch (Exception ex) {
         return null;
      }

      if(session == null) {
         return null;
      }

      VideoRecording recording = session.getRecording();

      int width = recording.width;
      int height = recording.height;
      String dash = dashBase + id.toString() + "/manifest.mpd";

      ByteBuf response = Unpooled.buffer();
      ByteBufUtil.writeUtf8(response,"<html><head>");
      ByteBufUtil.writeUtf8(response,"</head><body onLoad=\"Dash.createAll()\">");
      ByteBufUtil.writeUtf8(response,"<div><video class=\"dashjs-player\" autoplay preload=\"none\" controls=\"true\" width=\"" + width + "\" height=\"" + height + "\">");
      ByteBufUtil.writeUtf8(response,"<source src=\"" + dash + "\" type=\"application/dash+xml\"/></video></div>");
      ByteBufUtil.writeUtf8(response,"<script src=\"http://cdn.dashjs.org/latest/dash.all.js\"></script>");
      ByteBufUtil.writeUtf8(response,"</body></html>");

      return response;
   }
}

