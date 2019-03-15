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
package com.iris.video.recording;

import static com.iris.video.recording.RecordingMetrics.RECORDING_LATENCY_FUTURE;
import static com.iris.video.recording.RecordingMetrics.RECORDING_LATENCY_SUCCESS;
import static com.iris.video.recording.RecordingMetrics.RECORDING_LATENCY_TIMEOUT;
import static com.iris.video.recording.RecordingMetrics.RECORDING_SESSION_CREATE_AUTH;
import static com.iris.video.recording.RecordingMetrics.RECORDING_SESSION_CREATE_FAIL;
import static com.iris.video.recording.RecordingMetrics.RECORDING_SESSION_CREATE_INVALID;
import static com.iris.video.recording.RecordingMetrics.RECORDING_SESSION_CREATE_SUCCESS;
import static com.iris.video.recording.RecordingMetrics.RECORDING_SESSION_CREATE_TIMEOUT;
import static com.iris.video.recording.RecordingMetrics.RECORDING_SESSION_CREATE_VALIDATION;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.iris.core.dao.PlaceDAO;
import com.iris.media.RtspPushHeaders;
import com.iris.util.IrisUUID;
import com.iris.video.VideoSessionRegistry;
import com.iris.video.VideoUtil;
import com.iris.video.cql.VideoRecordingManager;
import com.iris.video.storage.VideoStorage;
import com.iris.video.storage.VideoStorageSession;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

public abstract class RecordingSessionFactory<T extends RecordingSession> {

   private static final Logger log = LoggerFactory.getLogger(RecordingSessionFactory.class);

   private final PlaceDAO placeDAO;
   private final VideoRecordingManager dao;
   private final VideoSessionRegistry registry;
   private final VideoStorage videoStorage;
   private final RecordingEventPublisher eventPublisher;
   private final SecretKeySpec secret;
   private final long sessionTimeoutInMs;
   private final VideoTtlResolver ttlResolver;

   private final LoadingCache<UUID, UUID> accountCache = CacheBuilder
      .newBuilder()
      .recordStats()
      .build(new CacheLoader<UUID, UUID>() {
         @Override
         public UUID load(UUID key) {
            return placeDAO.getAccountById(key);
         }
      });

   public RecordingSessionFactory(
      PlaceDAO placeDAO,
      VideoRecordingManager dao,
      VideoSessionRegistry registry,
      VideoStorage videoStorage,
      RecordingEventPublisher eventPublisher,
      byte[] recordingSecret,
      long sessionTimeoutSecs,
      VideoTtlResolver ttlResolver
   ) {
      this.placeDAO = placeDAO;
      this.dao = dao;
      this.registry = registry;
      this.videoStorage = videoStorage;
      this.eventPublisher = eventPublisher;
      this.ttlResolver = ttlResolver;
      this.secret = new SecretKeySpec(recordingSecret, "HmacSHA256");
      this.sessionTimeoutInMs = TimeUnit.MILLISECONDS.convert(sessionTimeoutSecs, TimeUnit.SECONDS);
   }

   public final T createSession(@Nullable ChannelHandlerContext ctx, RtspPushHeaders hdrs) throws IOException {
      try {
         ByteBuf session = parse(hdrs);
         UUID cameraId = nextUUID(session);
         UUID placeId = nextUUID(session);
         UUID personId = nextUUID(session);
         UUID recordingId = nextUUID(session);

         UUID accountId = accountCache.get(placeId);
         verifySignature(secret, cameraId, accountId, placeId, personId, recordingId, session);

         if (!IrisUUID.isNil(recordingId)) {
            long ts = IrisUUID.timeof(recordingId);
            long elapsed = System.currentTimeMillis() - ts;
            if (elapsed < 0) {
               RECORDING_SESSION_CREATE_INVALID.inc();
               RECORDING_LATENCY_FUTURE.update(elapsed, TimeUnit.MILLISECONDS);
               throw new Exception("video session invalid, closing: timestamp is in the future");
            }

            if (sessionTimeoutInMs > 0 && elapsed >= sessionTimeoutInMs) {
               RECORDING_SESSION_CREATE_TIMEOUT.inc();
               RECORDING_LATENCY_TIMEOUT.update(elapsed, TimeUnit.MILLISECONDS);
               throw new Exception("video session invalid, closing: session timed out");
            }

            RECORDING_LATENCY_SUCCESS.update(elapsed, TimeUnit.MILLISECONDS);
         } else {
            recordingId = IrisUUID.timeUUID();
         }

         if (IrisUUID.isNil(personId)) {
            personId = null;
         }

         boolean stream = VideoUtil.isStreamUUID(recordingId);

         double precapture = 0;
         try {
            precapture = Double.valueOf(hdrs.getHeaders().getOrDefault("x-precapture", "0"));
         } catch (Exception ex) {
            // ignore
         }

         log.info("creating new recording session: camera={}, account={}, place={}, person={}, recording={}", cameraId, accountId, placeId, personId, recordingId);
         VideoStorageSession storage = videoStorage.create(recordingId, cameraId, accountId, placeId, personId, ttlResolver.resolveTtlInSeconds(placeId, stream));
         T result = create(registry, ctx, dao, eventPublisher, storage, precapture, stream);
         RECORDING_SESSION_CREATE_SUCCESS.inc();
         return result;
      } catch (IOException ex) {
         RECORDING_SESSION_CREATE_FAIL.inc();
         throw ex;
      } catch (Exception ex) {
         RECORDING_SESSION_CREATE_FAIL.inc();
         throw new IOException(ex);
      }
   }

   public final T createSession(
      @Nullable ChannelHandlerContext ctx,
      UUID recId,
      UUID camId,
      UUID acctId,
      UUID placeId,
      UUID personId,
      double precapture,
      boolean stream,
      long ttlInSeconds
   ) throws Exception {
      log.info("creating new recording session: camera={}, account={}, place={}, person={}, recording={}", camId, acctId, placeId, personId, recId);
      VideoStorageSession storage = videoStorage.create(recId, camId, acctId, placeId, personId, ttlInSeconds);
      T result = create(registry, ctx, dao, eventPublisher, storage, precapture, stream);
      RECORDING_SESSION_CREATE_SUCCESS.inc();
      return result;
   }

   protected abstract T create(
      VideoSessionRegistry registry,
      @Nullable ChannelHandlerContext ctx,
      VideoRecordingManager dao,
      RecordingEventPublisher eventPublisher,
      VideoStorageSession storage,
      double precapture,
      boolean stream
   );

   private static UUID nextUUID(ByteBuf buffer) {
      return new UUID(buffer.readLong(), buffer.readLong());
   }

   private static void verifySignature(SecretKeySpec secret, UUID cameraId, UUID accountId, UUID placeId, UUID personId, UUID recordingId, ByteBuf sig) throws Exception {
      StringBuilder message = new StringBuilder(180);
      message.append(cameraId);
      message.append(accountId);
      message.append(placeId);
      message.append(personId);
      message.append(recordingId);

      Mac hmac = Mac.getInstance("HmacSHA256");
      hmac.init(secret);
      byte[] result = hmac.doFinal(message.toString().getBytes(StandardCharsets.UTF_8));

      ByteBuf computed = Unpooled.wrappedBuffer(result, 0, 16);
      if (!ByteBufUtil.equals(sig,computed)) {
         RECORDING_SESSION_CREATE_VALIDATION.inc();
         throw new Exception("signature validation failed");
      }
   }

   private static ByteBuf parse(RtspPushHeaders hdrs) throws Exception {
      String auth = hdrs.getHeaders().get("authorization");
      if (auth == null || auth.length() < "basic ".length()) {
         RECORDING_SESSION_CREATE_AUTH.inc();
         throw new Exception("could not find recording session tokens, terminating session");
      }

      String type = auth.substring(0, "basic ".length());
      if (!"basic ".equalsIgnoreCase(type)) {
         RECORDING_SESSION_CREATE_AUTH.inc();
         throw new Exception("recording session tokens in invalid format, terminating session");
      }

      String raw = auth.substring("basic ".length());
      String base64 = new String(Base64.getDecoder().decode(raw), StandardCharsets.UTF_8);

      int idx = base64.indexOf(':');
      if (idx <= 0) {
         RECORDING_SESSION_CREATE_AUTH.inc();
         throw new Exception("recording session has missing or invalid token data, terminating session");
      }

      String infoRaw1 = base64.substring(0,idx);
      String infoRaw2 = base64.substring(idx+1);

      byte[] info1 = Base64.getUrlDecoder().decode(infoRaw1);
      if (info1 == null || info1.length != 32) {
         RECORDING_SESSION_CREATE_AUTH.inc();
         throw new Exception("recording session has invalid token data, terminating session");
      }

      byte[] info2 = Base64.getUrlDecoder().decode(infoRaw2);
      if (info2 == null || info2.length != 48) {
         RECORDING_SESSION_CREATE_AUTH.inc();
         throw new Exception("recording session has invalid token data, terminating session");
      }

      ByteBuf buffer = Unpooled.buffer(info1.length + info2.length);
      buffer.writeBytes(info1);
      buffer.writeBytes(info2);

      return buffer;
   }
}

