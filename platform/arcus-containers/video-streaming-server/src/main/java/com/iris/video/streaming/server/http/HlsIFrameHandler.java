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

import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_IFRAME_DOESNT_EXIST;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_IFRAME_DOES_EXIST;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_IFRAME_DURATION;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_IFRAME_FAIL;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_IFRAME_FINISHED;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_IFRAME_INPROGRESS;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_IFRAME_NOID;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_IFRAME_NULL;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_IFRAME_NUM;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_IFRAME_SIZE;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_IFRAME_SUCCESS;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_IFRAME_VALIDATION;

import java.net.URI;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.media.MpegTsH264;
import com.iris.video.VideoIFrame;
import com.iris.video.VideoRecording;
import com.iris.video.VideoUtil;
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
@HttpGet("/hls-iframe/*")
public class HlsIFrameHandler extends AbstractStreamingHandler {

   private static final Logger logger = LoggerFactory.getLogger(HlsIFrameHandler.class);

   public static final int UUID_START = "/hls-iframe/".length();
   public static final int UUID_END = UUID_START + 36;

   private final VideoStreamingDao dao;
   private final VideoStreamingServerConfig config;

   @Inject
   public HlsIFrameHandler(BridgeMetrics metrics, AlwaysAllow alwaysAllow, VideoStreamingServerConfig config, VideoStreamingDao dao) {
      super(alwaysAllow, new HttpSender(HlsIFrameHandler.class, metrics), config.getStreamingSecretAsSpec());
      this.dao = dao;
      this.config = config;
   }

   @Override
   public FullHttpResponse respond(@Nullable FullHttpRequest request, @Nullable ChannelHandlerContext ctx) throws Exception {
      long startTime = System.nanoTime();
      try {
         if (request == null || ctx == null) {
            HLS_IFRAME_NULL.inc();
            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            return errResponse;
         }

         QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
         UUID id = getRecordingId(decoder, UUID_START, UUID_END, "/playlist.m3u8");
         if (id == null || !validateRequest(request, id, decoder)) {
            if (id == null) {
               HLS_IFRAME_NOID.inc();
               logger.debug("returning bad request, recording id is null");
            } else {
               HLS_IFRAME_VALIDATION.inc();
               logger.debug("returning bad request, request failed validation");
            }

            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            return errResponse;
         }

         Date ts = VideoUtil.getExpirationTs(decoder);
         ByteBuf response = getResponse(id, ts);
         if (response == null) {
            logger.debug("returning 404 not found, response was null");
            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
            return errResponse;
         }

         FullHttpResponse httpResponse = getMpegFullHttpResponse(response);

         HLS_IFRAME_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         return httpResponse;
      } catch (Exception ex) {
         HLS_IFRAME_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

   @Nullable
   private ByteBuf getResponse(UUID id, Date ts) throws Exception {
      VideoStreamingSession session = null;
      try {
         session = dao.session(id);
         HLS_IFRAME_DOES_EXIST.inc();
      } catch(Exception e) {
         if(e.getMessage().contains("does not exist")) {
            HLS_IFRAME_DOESNT_EXIST.inc();
            return null;
         }

         throw e;
      }

      if(session == null) {
         HLS_IFRAME_DOESNT_EXIST.inc();
         return null;
      }

      VideoRecording recording = session.getRecording();
      ByteBuf response = Unpooled.buffer();
      ByteBufUtil.writeUtf8(response, "#EXTM3U\n");

      boolean finished = recording.isRecordingFinished();
      URI videoUri = dao.getUri(recording.storage, ts);
      String videoPath = videoUri.toString();

      if (finished) {
         HLS_IFRAME_FINISHED.inc();
         ByteBufUtil.writeUtf8(response, "#EXT-X-PLAYLIST-TYPE:VOD\n");
      } else {
         HLS_IFRAME_INPROGRESS.inc();
         ByteBufUtil.writeUtf8(response, "#EXT-X-PLAYLIST-TYPE:EVENT\n");
      }

      ByteBufUtil.writeUtf8(response, "#EXT-X-TARGETDURATION:" + config.getHlsTargetLength() + "\n");
      ByteBufUtil.writeUtf8(response, "#EXT-X-MEDIA-SEQUENCE:0\n");
      ByteBufUtil.writeUtf8(response, "#EXT-X-VERSION:4\n");
      ByteBufUtil.writeUtf8(response, "#EXT-X-I-FRAMES-ONLY\n");

      HLS_IFRAME_NUM.update(recording.iframes.size());

      VideoIFrame last = null;
      for (VideoIFrame iframe : recording.iframes) {
         if (last == null) {
            last = iframe;
            continue;
         }

         long offset = last.byteOffset + MpegTsH264.PAT_LENGTH + MpegTsH264.PMT_LENGTH;
         long size = last.byteLength - MpegTsH264.PAT_LENGTH - MpegTsH264.PMT_LENGTH;
         double length = iframe.timestamp - last.timestamp;
         //long size = iframe.byteOffset - last.byteOffset;

         HLS_IFRAME_SIZE.update(size);
         HLS_IFRAME_DURATION.update((long)(length * 1000000000), TimeUnit.NANOSECONDS);

         ByteBufUtil.writeUtf8(response,"#EXTINF:" + length + ",\n");
         ByteBufUtil.writeUtf8(response,"#EXT-X-BYTERANGE:" + size + "@" + offset + "\n");
         ByteBufUtil.writeUtf8(response,videoPath + "\n");

         last = iframe;
      }

      if (finished) {
         ByteBufUtil.writeUtf8(response,"#EXT-X-ENDLIST\n");
      }

      return response;
   }
}

