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

import static com.iris.video.streaming.server.VideoStreamingMetrics.JPG_CACHE_HIT;
import static com.iris.video.streaming.server.VideoStreamingMetrics.JPG_CACHE_MISS;
import static com.iris.video.streaming.server.VideoStreamingMetrics.JPG_CACHE_WRITE_FAIL;
import static com.iris.video.streaming.server.VideoStreamingMetrics.JPG_CACHE_WRITE_SUCCESS;
import static com.iris.video.streaming.server.VideoStreamingMetrics.JPG_REQUEST_DOESNT_EXIST;
import static com.iris.video.streaming.server.VideoStreamingMetrics.JPG_REQUEST_DOES_EXIST;
import static com.iris.video.streaming.server.VideoStreamingMetrics.JPG_REQUEST_FAIL;
import static com.iris.video.streaming.server.VideoStreamingMetrics.JPG_REQUEST_NOID;
import static com.iris.video.streaming.server.VideoStreamingMetrics.JPG_REQUEST_NOIFRAME;
import static com.iris.video.streaming.server.VideoStreamingMetrics.JPG_REQUEST_NULL;
import static com.iris.video.streaming.server.VideoStreamingMetrics.JPG_REQUEST_SUCCESS;
import static com.iris.video.streaming.server.VideoStreamingMetrics.JPG_REQUEST_TRANSCODE_ERROR;
import static com.iris.video.streaming.server.VideoStreamingMetrics.JPG_REQUEST_TRANSCODE_RESPONSE;
import static com.iris.video.streaming.server.VideoStreamingMetrics.JPG_REQUEST_VALIDATION;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpGet;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.video.VideoIFrame;
import com.iris.video.VideoRecording;
import com.iris.video.storage.PreviewStorage;
import com.iris.video.storage.VideoStorage;
import com.iris.video.storage.VideoStorageSession;
import com.iris.video.streaming.server.VideoStreamingServerConfig;
import com.iris.video.streaming.server.dao.VideoStreamingDao;
import com.iris.video.streaming.server.dao.VideoStreamingSession;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;

@Singleton
@HttpGet("/jpg/*")
public class JPGHandler extends AbstractStreamingHandler {
   private static final Logger log = LoggerFactory.getLogger(JPGHandler.class);
   public static final int UUID_START = "/jpg/".length();
   public static final int UUID_END = UUID_START + 36;
   private static final int SNAPSHOT_TIMEOUT = 5000;

   private final VideoStreamingDao dao;
   private final VideoStorage videoStorage;
   private final PreviewStorage previewStorage;

   @Named("video.snapshost.server.url")
   @Inject
   private String snaphostUrl;

   @Inject
   public JPGHandler(BridgeMetrics metrics, AlwaysAllow alwaysAllow, VideoStreamingServerConfig config, VideoStreamingDao dao, VideoStorage videoStorage, PreviewStorage previewStorage) {
      super(alwaysAllow, new HttpSender(JPGHandler.class, metrics), config.getStreamingSecretAsSpec());
      this.dao = dao;
      this.videoStorage = videoStorage;
      this.previewStorage = previewStorage;
   }

   @Override
   public FullHttpResponse respond(@Nullable FullHttpRequest request, @Nullable ChannelHandlerContext ctx) throws Exception {
      long startTime = System.nanoTime();
      try {
         if (request == null || ctx == null) {
            JPG_REQUEST_NULL.inc();

            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            return errResponse;
         }

         log.debug("handling preview request {}", request.getUri());
         QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
         UUID id = getRecordingId(decoder, UUID_START, UUID_END, "/preview.jpg");
         if (id == null || !validateRequest(request, id, decoder)) {
            if (id == null) {
               JPG_REQUEST_NOID.inc();
               log.debug("returning back failure because the recording id was null");
            } else {
               JPG_REQUEST_VALIDATION.inc();
               log.debug("returning back failure because the request could not be validated");
            }

            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
            return errResponse;
         }

         byte[] response = getResponse(id);
         if (response == null) {
            FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
            return errResponse;
         }

         FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
         HttpHeaders.setContentLength(httpResponse, response.length);
         httpResponse.headers().set(HttpHeaders.Names.CONTENT_TYPE, "image/jpeg");
         httpResponse.content().writeBytes(response);

         JPG_REQUEST_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         return httpResponse;
      } catch (Exception ex) {
         JPG_REQUEST_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

   @Nullable
   public byte[] getResponse(UUID recordingId) throws Exception {
      long startTime = System.nanoTime();
      String id = recordingId.toString();

      byte[] result = previewStorage.read(id);
      if (result != null) {
         JPG_CACHE_HIT.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         return result;
      }

      result = getResponseFromStorage(recordingId);
      if (result != null) {
         long writeStartTime = System.nanoTime();
         try {
            previewStorage.write(id, result);
            JPG_CACHE_WRITE_SUCCESS.update(System.nanoTime() - writeStartTime, TimeUnit.NANOSECONDS);
         } catch (Exception ex) {
            JPG_CACHE_WRITE_FAIL.update(System.nanoTime() - writeStartTime, TimeUnit.NANOSECONDS);
            log.info("failed to write recording preview to cache:", ex);
         }
      }

      JPG_CACHE_MISS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
      return result;
   }

   public byte[] getResponseFromStorage(UUID recordingId) throws Exception {
      VideoStreamingSession session;
      try {
         session = dao.session(recordingId);
         JPG_REQUEST_DOES_EXIST.inc();
      } catch (Exception ex) {
         if(ex.getMessage().contains("does not exist")) {
            JPG_REQUEST_DOESNT_EXIST.inc();
            return null;
         }
         throw ex;
      }

      if(session == null) {
         JPG_REQUEST_DOESNT_EXIST.inc();
         return null;
      }

     VideoRecording rec = session.getRecording();
      if(rec.iframes.isEmpty()) {
         JPG_REQUEST_NOIFRAME.inc();

         log.debug("recording {} has no iframes, returning null", rec.recordingId);
         return null;
      }

      VideoStorageSession sess = videoStorage.create(rec);
      VideoIFrame iframe = rec.iframes.get(0);
      byte[] buf = new byte[(int)iframe.byteLength];
      sess.read(buf, iframe.byteOffset, iframe.byteLength, 0);
      log.debug("posting to the snapshot server an rec {} iframe of length: {}", rec.recordingId, buf.length);
      return postToSnapshotServer(buf);
   }

   private byte[] postToSnapshotServer(byte[] buf) throws Exception {
      HttpPost post = new HttpPost(snaphostUrl);
      RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(SNAPSHOT_TIMEOUT)
            .setSocketTimeout(SNAPSHOT_TIMEOUT)
            .build();
      post.setHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, "application/octet-stream");
      ByteArrayEntity entity = new ByteArrayEntity(buf, 0, buf.length);
      post.setEntity(entity);
      post.setConfig(config);

      // TODO:  pooling
      CloseableHttpClient client = HttpClients.createDefault();

      try {
         log.debug("sending post to snapshot server to convert iframe to a jpg");
         org.apache.http.HttpResponse response = client.execute(post);
         EntityUtils.consume(entity);
         if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            JPG_REQUEST_TRANSCODE_RESPONSE.inc();
            throw new Exception("Failed to transcode iframe to jpeg, transcoder returned : " + response.getStatusLine().getStatusCode());
         }

         HttpEntity resEntity = response.getEntity();
         ByteBuf resBuffer = Unpooled.wrappedBuffer(EntityUtils.toByteArray(resEntity));
         EntityUtils.consume(resEntity);
         log.debug("got response from snapshot server of length {}", resBuffer.readableBytes());

         byte[] data = new byte[resBuffer.readableBytes()];
         resBuffer.getBytes(resBuffer.readerIndex(), data);
         return data;
      } catch(Exception e) {
         JPG_REQUEST_TRANSCODE_ERROR.inc();
         log.error("failed to convert iframe to snapshot", e);
         throw e;
      } finally {
         client.close();
      }
   }
}

