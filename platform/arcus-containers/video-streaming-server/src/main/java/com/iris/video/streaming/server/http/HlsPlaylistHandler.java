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

import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_PLAYLIST_DOESNT_EXIST;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_PLAYLIST_DOES_EXIST;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_PLAYLIST_FAIL;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_PLAYLIST_FINAL_SEGMENT_DURATION;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_PLAYLIST_FINAL_SEGMENT_SIZE;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_PLAYLIST_FINISHED;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_PLAYLIST_INPROGRESS;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_PLAYLIST_NOID;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_PLAYLIST_NULL;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_PLAYLIST_SEGMENT_DURATION;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_PLAYLIST_SEGMENT_NUM;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_PLAYLIST_SEGMENT_SIZE;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_PLAYLIST_SUCCESS;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_PLAYLIST_VALIDATION;

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
@HttpGet("/hls-playlist/*")
public class HlsPlaylistHandler extends AbstractStreamingHandler {
   private static final Logger logger = LoggerFactory.getLogger(HlsPlaylistHandler.class);
   public static final int UUID_START = "/hls-playlist/".length();
   public static final int UUID_END = UUID_START + 36;

   private final VideoStreamingServerConfig config;
   private final VideoStreamingDao dao;

   @Inject
   public HlsPlaylistHandler(BridgeMetrics metrics, AlwaysAllow alwaysAllow, VideoStreamingServerConfig config, VideoStreamingDao dao) {
      super(alwaysAllow, new HttpSender(HlsPlaylistHandler.class, metrics), config.getStreamingSecretAsSpec());
      this.config = config;
      this.dao = dao;
   }

   @Override
   public FullHttpResponse respond(@Nullable FullHttpRequest request, @Nullable ChannelHandlerContext ctx) throws Exception {
      long startTime = System.nanoTime();
      try {
         if (request == null || ctx == null) {
            HLS_PLAYLIST_NULL.inc();
            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            return errResponse;
         }

         QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
         UUID id = getRecordingId(decoder, UUID_START, UUID_END, "/playlist.m3u8");
         if (id == null || !validateRequest(request, id, decoder)) {
            if (id == null) {
               HLS_PLAYLIST_NOID.inc();
               logger.debug("returning bad request, recording id is null");
            } else {
               HLS_PLAYLIST_VALIDATION.inc();
               logger.debug("returning bad request, request failed validation");
            }

            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            return errResponse;
         }

         Date ts = VideoUtil.getExpirationTs(decoder);
         ByteBuf response = getResponse(id, ts);
         if (response == null) {
            logger.debug("returning 404, not found response was null");
            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
            return errResponse;
         }

         FullHttpResponse httpResponse = getMpegFullHttpResponse(response);

         HLS_PLAYLIST_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         return httpResponse;
      } catch (Exception ex) {
         HLS_PLAYLIST_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

   @Nullable
   private ByteBuf getResponse(UUID id, Date ts) throws Exception {
      VideoStreamingSession session = null;
      try {
         session = dao.session(id);
         HLS_PLAYLIST_DOES_EXIST.inc();
      } catch(Exception e) {
         if(e.getMessage().contains("does not exist")) {
            HLS_PLAYLIST_DOESNT_EXIST.inc();
            return null;
         }

         throw e;
      }

      if(session == null) {
         HLS_PLAYLIST_DOESNT_EXIST.inc();
         return null;
      }

      VideoRecording recording = session.getRecording();
      boolean finished = recording.isRecordingFinished();

      URI videoUri = dao.getUri(recording.storage, ts);
      String videoPath = videoUri.toString();

      ByteBuf response = Unpooled.buffer();
      ByteBufUtil.writeUtf8(response, "#EXTM3U\n");
      if (finished) {
         HLS_PLAYLIST_FINISHED.inc();
         ByteBufUtil.writeUtf8(response, "#EXT-X-PLAYLIST-TYPE:VOD\n");
      } else {
         HLS_PLAYLIST_INPROGRESS.inc();
         ByteBufUtil.writeUtf8(response, "#EXT-X-PLAYLIST-TYPE:EVENT\n");
      }

      ByteBufUtil.writeUtf8(response, "#EXT-X-TARGETDURATION:" + config.getHlsTargetLength() + "\n");
      ByteBufUtil.writeUtf8(response, "#EXT-X-MEDIA-SEQUENCE:0\n");
      ByteBufUtil.writeUtf8(response, "#EXT-X-VERSION:4\n");

      int num = 0;
      VideoIFrame last = null;
      for (VideoIFrame iframe : recording.iframes) {
         if (last == null) {
            last = iframe;
            continue;
         }

         double hlsSegmentLength = config.getHlsSegmentLength();
         double length = iframe.timestamp - last.timestamp;
         boolean write = (num == 0 && length >= hlsSegmentLength) ||
                         (num == 1 && length >= hlsSegmentLength) ||
                         (num == 2 && length >= hlsSegmentLength) ||
                         (length >= hlsSegmentLength);

         if (write) {
            long size = iframe.byteOffset - last.byteOffset;

            HLS_PLAYLIST_SEGMENT_SIZE.update(size);
            HLS_PLAYLIST_SEGMENT_DURATION.update((long)(length * 1000000000), TimeUnit.NANOSECONDS);

            ByteBufUtil.writeUtf8(response,"#EXTINF:" + length + ",\n");
            ByteBufUtil.writeUtf8(response,"#EXT-X-BYTERANGE:" + size + "@" + last.byteOffset + "\n");
            ByteBufUtil.writeUtf8(response,videoPath + "\n");

            num++;
            last = iframe;
         }
      }

      if (finished) {
         double lastTs = (last != null) ? last.timestamp : 0.0;
         long lastBo = (last != null) ? last.byteOffset : 0L;

         double length = recording.duration - lastTs;
         long size = recording.size - lastBo;

         if (length > 0.0 && size > 0) {
            num++;
            HLS_PLAYLIST_FINAL_SEGMENT_SIZE.update(size);
            HLS_PLAYLIST_FINAL_SEGMENT_DURATION.update((long)(length * 1000000000), TimeUnit.NANOSECONDS);

            ByteBufUtil.writeUtf8(response,"#EXTINF:" + length + ",\n");
            ByteBufUtil.writeUtf8(response,"#EXT-X-BYTERANGE:" + size + "@" + lastBo + "\n");
            ByteBufUtil.writeUtf8(response,videoPath + "\n");
         }

         ByteBufUtil.writeUtf8(response,"#EXT-X-ENDLIST\n");
      }

      HLS_PLAYLIST_SEGMENT_NUM.update(num);
      return response;
   }
}

