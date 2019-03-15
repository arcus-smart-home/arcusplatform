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

import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_REQUEST_DOESNT_EXIST;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_REQUEST_DOES_EXIST;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_REQUEST_FAIL;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_REQUEST_NOBW;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_REQUEST_NOFR;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_REQUEST_NOID;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_REQUEST_NOIFRAME;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_REQUEST_NORES;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_REQUEST_NOTENOUGH_SEGMENTS;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_REQUEST_NULL;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_REQUEST_SUCCESS;
import static com.iris.video.streaming.server.VideoStreamingMetrics.HLS_REQUEST_VALIDATION;

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
import com.iris.video.AudioCodec;
import com.iris.video.VideoCodec;
import com.iris.video.VideoIFrame;
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
import io.netty.handler.codec.http.QueryStringEncoder;

@Singleton
@HttpGet("/hls/*")
public class HlsHandler extends AbstractStreamingHandler {
   private static final Logger logger = LoggerFactory.getLogger(HlsHandler.class);

   public static final int UUID_START = "/hls/".length();
   public static final int UUID_END = UUID_START + 36;

   private final VideoStreamingServerConfig config;
   private final VideoStreamingDao dao;

   private final String playlistBase;
   private final String iframeBase;

   @Inject
   public HlsHandler(BridgeMetrics metrics, AlwaysAllow alwaysAllow, VideoStreamingServerConfig config, VideoStreamingDao dao) {
      super(alwaysAllow, new HttpSender(HlsHandler.class, metrics), config.getStreamingSecretAsSpec());
      this.config = config;
      this.dao = dao;

      this.playlistBase = config.getVideoStreamingUrl() + "/hls-playlist/";
      this.iframeBase = config.getVideoStreamingUrl()  + "/hls-iframe/";
   }

   @Override
   public FullHttpResponse respond(@Nullable FullHttpRequest request, @Nullable ChannelHandlerContext ctx) throws Exception {
      long startTime = System.nanoTime();
      try {
         if (request == null || ctx == null) {
            HLS_REQUEST_NULL.inc();

            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            return errResponse;
         }

         logger.debug("handling request {}", request.getUri());

         QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
         UUID id = getRecordingId(decoder, UUID_START, UUID_END, "/playlist.m3u8");
         if (id == null || !validateRequest(request, id, decoder)) {
            if (id == null) {
               HLS_REQUEST_NOID.inc();
               logger.debug("returning bad request, recording id is null");
            } else {
               HLS_REQUEST_VALIDATION.inc();
               logger.debug("returning bad request, request failed validation");
            }

            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            return errResponse;
         }

         ByteBuf response = getResponse(id, decoder);
         if (response == null) {
            logger.debug("returning 404 not found, getReponse returned null");
            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
            return errResponse;
         }

         FullHttpResponse httpResponse = getMpegFullHttpResponse(response);

         HLS_REQUEST_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         return httpResponse;
      } catch (Exception ex) {
         HLS_REQUEST_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

   @Nullable
   private ByteBuf getResponse(UUID id, QueryStringDecoder decoder) throws Exception {
      VideoStreamingSession session = null;
      try {
         session = dao.session(id);
         HLS_REQUEST_DOES_EXIST.inc();
      } catch(Exception e) {
         if(e.getMessage().contains("does not exist")) {
            HLS_REQUEST_DOESNT_EXIST.inc();
            return null;
         }

         throw e;
      }

      if(session == null) {
         HLS_REQUEST_DOESNT_EXIST.inc();
         return null;
      }

      VideoRecording recording = session.getRecording();
      if(recording.iframes.size() == 0) {
         HLS_REQUEST_NOIFRAME.inc();
         return null;
      }

      VideoIFrame iframe = recording.iframes.get(recording.iframes.size() - 1);
      if(iframe.timestamp < config.getHlsSegmentLength() * config.getHlsSegmentsRequired()) {
         HLS_REQUEST_NOTENOUGH_SEGMENTS.inc();
         return null;
      }

      int width = recording.width;
      int height = recording.height;
      String res = ",RESOLUTION=" + width + "x" + height;
      if (width <= 0 || height <= 0) {
         HLS_REQUEST_NORES.inc();
         res = "";
      }

      int bandwidth = 2*recording.bandwidth;
      if (bandwidth <= 0) {
         HLS_REQUEST_NOBW.inc();
         bandwidth = 2048000;
      }

      double framerate = recording.framerate;
      if (framerate <= 0) {
         HLS_REQUEST_NOFR.inc();
         framerate = 5.0;
      }

      String exp = decoder.parameters().get("exp").get(0);
      String sig = decoder.parameters().get("sig").get(0);

      QueryStringEncoder playlistEnc = new QueryStringEncoder(playlistBase + id.toString() + "/playlist.m3u8");
      playlistEnc.addParam("exp", exp);
      playlistEnc.addParam("sig", sig);

      QueryStringEncoder iframesEnc = new QueryStringEncoder(iframeBase + id.toString() + "/playlist.m3u8");
      iframesEnc.addParam("exp", exp);
      iframesEnc.addParam("sig", sig);

      String playlist = playlistEnc.toString();
      String iframes = iframesEnc.toString();

      ByteBuf response = Unpooled.buffer();
      ByteBufUtil.writeUtf8(response,"#EXTM3U\n");
      ByteBufUtil.writeUtf8(response,"#EXT-X-VERSION:4\n");
      ByteBufUtil.writeUtf8(response,"#EXT-X-STREAM-INF:BANDWIDTH="+bandwidth+res+",CODECS=\"" + formatCodecs(recording.videoCodec, recording.audioCodec) + "\",CLOSED-CAPTIONS=NONE\n");
      ByteBufUtil.writeUtf8(response,playlist + "\n");
      ByteBufUtil.writeUtf8(response,"#EXT-X-I-FRAME-STREAM-INF:BANDWIDTH="+bandwidth+res+",CODECS=\"" + formatCodecs(recording.videoCodec, recording.audioCodec) + "\",URI=\"" + iframes + "\"\n");

      return response;
   }

   private String formatCodecs(VideoCodec videoCodec, AudioCodec audioCodec) {
      if(videoCodec == null) {
         videoCodec = VideoCodec.H264_BASELINE_3_1;
      }
      String codec = videoCodec.playlistCodec();
      if(audioCodec != null && audioCodec != AudioCodec.NONE) {
         codec = codec + ',' + audioCodec.playlistCodec();
      }
      return codec;
   }
}

