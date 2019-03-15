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

import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_VIDEO_DOESNT_EXIST;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_VIDEO_DOES_EXIST;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_VIDEO_FAIL;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_VIDEO_NOID;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_VIDEO_NULL;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_VIDEO_SUCCESS;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_VIDEO_VALIDATION;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
@HttpGet("/hls-video/*")
public class HlsVideoHandler extends AbstractStreamingHandler {
   public static final int UUID_START = "/hls-video/".length();
   public static final int UUID_END = UUID_START + 36;

   private final VideoStreamingDao dao;

   private final String hlsBase;

   @Inject
   public HlsVideoHandler(BridgeMetrics metrics, AlwaysAllow alwaysAllow, VideoStreamingServerConfig config, VideoStreamingDao dao) {
      super(alwaysAllow, new HttpSender(HlsVideoHandler.class, metrics), config.getStreamingSecretAsSpec());
      this.dao = dao;
      this.hlsBase = config.getVideoStreamingUrl() + ":" + config.getTcpPort() + "/hls/";
   }

   @Override
   public FullHttpResponse respond(@Nullable FullHttpRequest request, @Nullable ChannelHandlerContext ctx) throws Exception {
      long startTime = System.nanoTime();
      try {
         if (request == null || ctx == null) {
            HLS_VIDEO_NULL.inc();
            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            return errResponse;
         }

         QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
         UUID id = getRecordingId(decoder, UUID_START, UUID_END, "/playlist.m3u8");
         if (id == null || !validateRequest(request, id, decoder)) {
            if (id == null) {
               HLS_VIDEO_NOID.inc();
            } else {
               HLS_VIDEO_VALIDATION.inc();
            }

            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            return errResponse;
         }

         ByteBuf response = getResponse(id);
         if (response == null) {
            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            return errResponse;
         }

         FullHttpResponse httpResponse = getNoCacheFullHttpResponse(response, "text/html");

         HLS_VIDEO_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         return httpResponse;
      } catch (Exception ex) {
         HLS_VIDEO_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

   @Nullable
   private ByteBuf getResponse(UUID id) throws Exception {
      VideoStreamingSession session;
      try {
         session = dao.session(id);
         HLS_VIDEO_DOES_EXIST.inc();
      } catch (Exception e) {
         if(e.getMessage().contains("does not exist")) {
            HLS_VIDEO_DOESNT_EXIST.inc();
            return null;
         }

         throw e;
      }

      if(session == null) {
         HLS_VIDEO_DOESNT_EXIST.inc();
         return null;
      }

      VideoRecording recording = session.getRecording();

      int width = recording.width;
      int height = recording.height;

      String hls = hlsBase + id.toString() + "/playlist.m3u8";

      ByteBuf response = Unpooled.buffer();
      ByteBufUtil.writeUtf8(response,"<html><body><video src=\"");
      ByteBufUtil.writeUtf8(response,hls);
      ByteBufUtil.writeUtf8(response,"\" width=\"");
      ByteBufUtil.writeUtf8(response,String.valueOf(width));
      ByteBufUtil.writeUtf8(response,"\" height=\"");
      ByteBufUtil.writeUtf8(response,String.valueOf(height));
      ByteBufUtil.writeUtf8(response,"\"></video></body></html>");

      return response;
   }
}

