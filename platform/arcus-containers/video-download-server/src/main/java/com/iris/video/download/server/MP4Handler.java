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
package com.iris.video.download.server;

import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_CLIENT_CLOSED_CHANNEL;
import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_EXP_NULL;
import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_FAIL;
import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_ID_BAD;
import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_REJECTED;
import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_REQUEST_DOES_EXIST;
import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_SIG_NULL;
import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_SLOW_CLIENT_WAIT;
import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_START_FAIL;
import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_SUCCESS;
import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_URL_BAD;
import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_UUID_BAD;
import static com.iris.video.download.server.VideoDownloadMetrics.DOWNLOAD_VALIDATION_FAILED;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.spec.SecretKeySpec;

import com.iris.media.MP4Convert;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.video.VideoRecording;
import com.iris.video.VideoUtil;
import com.iris.video.download.server.dao.VideoDownloadDao;
import com.iris.video.download.server.dao.VideoDownloadSession;
import com.iris.video.recording.VideoRecordingFileName;
import com.iris.video.storage.VideoStorage;
import com.iris.video.storage.VideoStorageSession;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;

public class MP4Handler extends SimpleChannelInboundHandler<FullHttpRequest> {
   private static final Logger log = LoggerFactory.getLogger(MP4Handler.class);

   private static final int UUID_START = "/mp4/".length();
   private static final int UUID_END = UUID_START + 36;

   private final ExecutorService executor;
   private final VideoDownloadDao videoDownloadDao;
   private final DeviceDAO deviceDAO;
   private final PlaceDAO placeDAO;
   private final VideoStorage videoStorage;
   private final VideoDownloadServerConfig config;
   private final SecretKeySpec secret;

   private final long maxBlockTime;
   private final AtomicReference<ChunkedOutputStream> outputStream;

   public MP4Handler(
      ExecutorService executor,
      VideoDownloadServerConfig config,
      VideoDownloadDao dao,
      VideoStorage videoStorage,
      DeviceDAO deviceDAO,
      PlaceDAO placeDAO
   ) {
      this.executor = executor;
      this.videoDownloadDao = dao;
      this.videoStorage = videoStorage;
      this.config = config;
      this.secret = config.getDownloadSecretAsSpec();
      this.maxBlockTime = TimeUnit.NANOSECONDS.convert(config.getMaxWriteBufferBlockTime(), TimeUnit.MILLISECONDS);
      this.outputStream = new AtomicReference<>();
      this.deviceDAO = deviceDAO;
      this.placeDAO = placeDAO;
   }

   @Override
   public void channelRead0(@Nullable ChannelHandlerContext ctx, @Nullable FullHttpRequest request) throws Exception {
      if (ctx == null) {
         log.error("channel context should not be null");
         return;
      }

      if (request == null) {
         sendErrorResponse(ctx,HttpResponseStatus.INTERNAL_SERVER_ERROR);
         return;
      }

      try {
         QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
         UUID id = getRecordingId(decoder, UUID_START, UUID_END, ".mp4");
         if (id == null || !validateRequest(request, id, decoder)) {
            sendErrorResponse(ctx,HttpResponseStatus.BAD_REQUEST);
            return;
         }

         stream(id, ctx, request);
      } catch (Exception ex) {
         sendErrorResponse(ctx,HttpResponseStatus.BAD_REQUEST);
      }
   }
  
   public void stream(UUID recordingId, ChannelHandlerContext ctx, FullHttpRequest request) {
      VideoDownloadSession session;
      try {
         session = videoDownloadDao.session(recordingId);
         DOWNLOAD_REQUEST_DOES_EXIST.inc();

         final VideoRecording rec = session.getRecording();
         if (!rec.isRecordingFinished()) {
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND);
            return;
         }

         final VideoStorageSession sess = videoStorage.create(rec);
         executor.submit(() -> {
            long startTime = System.nanoTime();
            boolean started = false;
            try (InputStream is = sess.input();
                 ChunkedOutputStream os = new ChunkedOutputStream(ctx,config.getChunkSize())) {
               outputStream.set(os);

               DefaultHttpResponse rsp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
               HttpHeaders.setTransferEncodingChunked(rsp);
               rsp.headers().set(HttpHeaders.Names.CONTENT_TYPE, "video/mp4");

               VideoRecordingFileName vrfn = new VideoRecordingFileName(rec.recordingId, rec.cameraId, rec.placeId, deviceDAO, placeDAO);
               String name = vrfn.getByDeviceOrDefault();

               rsp.headers().set("Content-Disposition", "attachment; filename=\"" + name + "\"");

               started = true;
               ctx.write(rsp);

               MP4Convert.convert(is, os);
               DOWNLOAD_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            } catch (ClosedChannelException ex) {
               log.debug("terminating conversion process abnormally: channel closed");
               DOWNLOAD_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
               DOWNLOAD_CLIENT_CLOSED_CHANNEL.inc();
            } catch(Exception ex) {
               DOWNLOAD_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
               log.debug("terminating conversion process abnormally:", ex);
               if (!started) {
                  sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
               } else {
                  log.trace("closing video download socket:", ex);
                  ctx.close();
               }
            } finally {
               outputStream.set(null);
            }

            return null;
         });
      } catch (RejectedExecutionException ex) {
         sendErrorResponse(ctx,HttpResponseStatus.SERVICE_UNAVAILABLE);
         DOWNLOAD_REJECTED.inc();
      } catch (Exception ex) {
         DOWNLOAD_START_FAIL.inc();
         HttpResponseStatus status = ex.getMessage().contains("does not exist") 
                                       ? HttpResponseStatus.NOT_FOUND 
                                       : HttpResponseStatus.INTERNAL_SERVER_ERROR;
         sendErrorResponse(ctx, status);
      }
   }

   private static void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {
      FullHttpResponse errResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", StandardCharsets.UTF_8));
      errResponse.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
      errResponse.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
      ctx.writeAndFlush(errResponse).addListener(ChannelFutureListener.CLOSE);
   }

   protected boolean validateRequest(FullHttpRequest request, UUID recordingId, QueryStringDecoder decoder) throws Exception {
      List<String> sigs = decoder.parameters().get("sig");
      if (sigs == null || sigs.size() != 1) {
         DOWNLOAD_SIG_NULL.inc();
         log.debug("failed validation because sigs is null or size is not 1: {}", recordingId);
         return false;
      }

      List<String> exps = decoder.parameters().get("exp");
      if (exps == null || exps.size() != 1) {
         DOWNLOAD_EXP_NULL.inc();
         log.debug("failed validation because exp is null or size is not 1: {}", recordingId);
         return false;
      }

      String sigRaw = sigs.get(0);
      if (sigRaw == null) {
         DOWNLOAD_SIG_NULL.inc();
         log.debug("failed validation because sigRaw is null: {}", recordingId);
         return false;
      }

      String sig = sigRaw.trim();
      if (sig.isEmpty()) {
         DOWNLOAD_SIG_NULL.inc();
         log.debug("failed validation because sigRaw is empty: {}", recordingId);
         return false;
      }

      String exp = exps.get(0);
      if (!VideoUtil.verifyAccessSignature(secret, recordingId, exp, sig)) {
         DOWNLOAD_VALIDATION_FAILED.inc();
         log.debug("failed validation because VideoUtil.verifyAccessSignature failed: {}", recordingId);
         return false;
      }

      return true;
   }

   @Nullable
   protected UUID getRecordingId(QueryStringDecoder uri, int start, int end, @Nullable String finl) {
      try {
         String path = uri.path();
         String recording = path.substring(start, end);

         UUID uuid = UUID.fromString(recording);
         if (uuid.version() != 1) {
            DOWNLOAD_UUID_BAD.inc();
            log.debug("failed to retreive recording id because uuid wasn't version 1: {}", uri);
            return null;
         }

         if (finl != null) {
            String fin = path.substring(end);
            if (!finl.equals(fin)) {
               DOWNLOAD_URL_BAD.inc();
               log.debug("failed to retreive recording id because url was wrong: {}", uri);
               return null;
            }
         }

         return uuid;
      } catch (Exception ex) {
         DOWNLOAD_ID_BAD.inc();
         return null;
      }
   }

   @Override
   public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
      ChunkedOutputStream os = outputStream.get();
      if (os != null) {
         os.writabilityChanged();
      }

      super.channelWritabilityChanged(ctx);
   }

   @Override
   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      ChunkedOutputStream os = outputStream.get();
      if (os != null) {
         os.writabilityChanged();
      }

      super.channelInactive(ctx);
   }

   private class ChunkedOutputStream extends OutputStream {
      final ByteBuf buf;
      final ChannelHandlerContext ctx;

      public ChunkedOutputStream(ChannelHandlerContext ctx, int chunksize) {
         Preconditions.checkArgument(chunksize >= 1, "invalid chunksize");

         this.buf = Unpooled.buffer(chunksize, chunksize);
         this.ctx = ctx;
      }

      @Override
      public void write(int b) throws IOException {
         if (buf.maxWritableBytes() < 1) {
            flush();
         }

         buf.writeByte(b);
      }

      @Override
      public void close() throws IOException {
         doFlush(false);
         ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);
      }

      @Override
      public void write(@Nullable byte[] b, int off, int len) throws IOException {
         int rem = len;
         int offset = off;

         int max = buf.maxWritableBytes();
         while (max < rem) {
            buf.writeBytes(b, offset, max);
            offset = offset + max;
            rem = rem - max;
            flush();

            max = buf.maxWritableBytes();
         }

         if (rem > 0) {
            buf.writeBytes(b, offset, rem);
         }
      }

      @Override
      public void flush() throws IOException {
         doFlush(true);
      }
      
      private void doFlush(boolean allowWaiting) throws IOException {
         Channel ch = ctx.channel();
         if (ch == null || !ch.isActive()) {
            throw new ClosedChannelException();
         }

         if (allowWaiting) {
            blockUntilReady();
         }

         ctx.writeAndFlush(new DefaultHttpContent(buf.copy()));
         buf.clear();
      }

      private void blockUntilReady() throws IOException {
         boolean waitRequired = false;
         long startTime = System.nanoTime();
         while (true) {
            Channel ch = ctx.channel();
            if (ch == null || !ch.isActive()) {
               throw new ClosedChannelException();
            }

            if (ch.isWritable()) {
               if (waitRequired) {
                  long waited = System.nanoTime() - startTime;
                  DOWNLOAD_SLOW_CLIENT_WAIT.update(waited, TimeUnit.NANOSECONDS);

                  log.trace("waited for {} ns", waited);
               }

               return;
            }

            long remaining = 5000000000L;
            if (maxBlockTime > 0) {
               long elapsed = System.nanoTime() - startTime;
               
               remaining = maxBlockTime - elapsed;
               if (remaining <= 0) {
                  throw new IOException("timeout");
               }
            }

            synchronized (this) {
               try {
                  waitRequired = true;
                  this.wait(remaining / 1000000L, (int)(remaining % 1000000L));
               } catch (InterruptedException ex) {
                  throw new IOException("interrupted", ex);
               }
            }

         }
      }

      private void writabilityChanged() throws IOException {
         synchronized (this) {
            this.notifyAll();
         }
      }
   }
}

