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

import static com.iris.video.VideoMetrics.RECORDING_SESSION_RECORDING;
import static com.iris.video.VideoMetrics.RECORDING_SESSION_RULE;
import static com.iris.video.VideoMetrics.RECORDING_SESSION_STREAM;
import static com.iris.video.recording.RecordingMetrics.RECORDING_SESSION_BAD_ENCODING;
import static com.iris.video.recording.RecordingMetrics.RECORDING_SESSION_NOBW;
import static com.iris.video.recording.RecordingMetrics.RECORDING_SESSION_NOFR;
import static com.iris.video.recording.RecordingMetrics.RECORDING_SESSION_NORES;
import static com.iris.video.recording.RecordingMetrics.RECORDING_SESSION_NOVIDEO;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.media.IrisRtspSdp;
import com.iris.messages.address.Address;
import com.iris.video.AudioCodec;
import com.iris.video.VideoCodec;
import com.iris.video.VideoMetadata;
import com.iris.video.VideoSessionRegistry;
import com.iris.video.cql.VideoRecordingManager;
import com.iris.video.storage.VideoStorageSession;

import io.netty.channel.ChannelHandlerContext;

public abstract class RecordingSession implements AutoCloseable {

   private static final Logger log = LoggerFactory.getLogger(RecordingSession.class);

   private static final String H264 = "H264";

   private final VideoSessionRegistry registry;
   @Nullable private final ChannelHandlerContext ctx;
   private final VideoStorageSession storage;
   private final VideoRecordingManager recordingManager;
   private final RecordingEventPublisher eventPublisher;
   private final boolean stream;
   private final double precapture;
   private final boolean completeOnClose;
   private volatile boolean closed = false;

   public RecordingSession(
      VideoSessionRegistry registry,
      @Nullable ChannelHandlerContext ctx,
      VideoStorageSession storage,
      VideoRecordingManager recordingMgr,
      RecordingEventPublisher eventPublisher,
      boolean stream,
      double precapture,
      boolean completeOnClose
   ) {
      this.registry = registry;
      this.ctx = ctx;
      this.storage = storage;
      this.recordingManager = recordingMgr;
      this.eventPublisher = eventPublisher;
      this.stream = stream;
      this.precapture = precapture;
      this.completeOnClose = completeOnClose;

      if (stream) {
         RECORDING_SESSION_STREAM.inc();
      } else {
         RECORDING_SESSION_RECORDING.inc();
      }

      UUID perId = storage.getPersonId();
      if (perId != null && perId.getMostSignificantBits() == 0) {
         RECORDING_SESSION_RULE.inc();
      }
      log.info("starting recording session: recid={}, camid={}, plcid={}, perid={}", storage.getRecordingId(), storage.getCameraId(), storage.getPlaceId(), storage.getPersonId());
   }

   public final VideoStorageSession storage() {
      return storage;
   }

   public final VideoRecordingManager getRecordingManager() {
      return recordingManager;
   }

   public final RecordingEventPublisher eventPublisher() {
      return eventPublisher;
   }

   public final boolean stream() {
      return stream;
   }

   public final double precapture() {
      return precapture;
   }

   public final UUID accountId() {
      return storage().getAccountId();
   }

   public final UUID placeId() {
      return storage().getPlaceId();
   }
   
   public final long ttlInSeconds() {
   	return storage.getRecordingTtlInSeconds();
   	
   }

   public final UUID cameraId() {
      return storage().getCameraId();
   }

   public final UUID recordingId() {
      return storage().getRecordingId();
   }

   @Nullable
   public final UUID personId() {
      return storage().getPersonId();
   }

   @Nullable
   public final Address actor() {
      return storage().getActor();
   }

   public final void setSdp(IrisRtspSdp sdp) throws IOException {
      onSdp(sdp);
      IrisRtspSdp.Media video = null;
      IrisRtspSdp.Media audio = null;
      for (IrisRtspSdp.Media media : sdp.getMedia()) {
         if(media.isVideo()) {
            video = media;
         } else {
            audio = media;
         }
      }

      if (video == null) {
         RECORDING_SESSION_NOVIDEO.inc();
         throw new IOException("stream must include video component");
      }

      if (!H264.equals(video.getEncodingName())) {
         RECORDING_SESSION_BAD_ENCODING.inc();
         throw new IOException("stream must be in H.264 format");
      }

      int width = 0;
      int height = 0;
      if (video.hasResolution()) {
         width = video.getWidth();
         height = video.getHeight();
      } else {
         RECORDING_SESSION_NORES.inc();
      }

      double framerate = 0.0;
      if (video.hasFrameRate()) {
         framerate = video.getFrameRate();
      } else {
         RECORDING_SESSION_NOFR.inc();
      }

      int bandwidth = 0;
      if (video.hasBandwidth()) {
         bandwidth = video.getBandwith() * 1000;
      } else {
         RECORDING_SESSION_NOBW.inc();
      }

      VideoCodec vcodec = extractVideoCodec(video);
      AudioCodec acodec = extractAudioCodec(audio);

      setMetadata(width, height, bandwidth, framerate, vcodec, acodec);
   }

   /* hook method concrete classes may implement if they have special code required to execute when the sdp is set */
   protected void onSdp(IrisRtspSdp sdp) {
   }

   public VideoMetadata setMetadata(int width, int height, int bandwidth, double framerate, VideoCodec vcodec, AudioCodec acodec) throws IOException {
      try {
         VideoMetadata md = getRecordingManager().storeMetadata(storage(), width, height, bandwidth, framerate, precapture, stream, vcodec, acodec);
         if(ctx != null) {
            registry.put(recordingId(), ctx);
         }
         eventPublisher().sendAdded(md);
         return md;
      } catch (Exception ex) {
         throw new IOException(ex);
      }
   }

   public final void onIFrame(long tsIn90KHz, long frameByteOffset, long frameByteSize) throws Exception {
      double tsInSeconds = tsIn90KHz / 90000.0;
      onIFrameTsInSeconds(tsInSeconds, frameByteOffset, frameByteSize);
   }

   public final void onIFrameTsInSeconds(double tsInSeconds, long frameByteOffset, long frameByteSize) throws Exception {
   	recordingManager.storeIFrame(storage, tsInSeconds, frameByteOffset, frameByteSize);
   }

   public final boolean closed() {
      return closed;
   }

   private static VideoCodec extractVideoCodec(IrisRtspSdp.Media m) {
      return extractFmt(m, "profile-level-id")
         .map(VideoCodec::fromSdpProfile)
         .orElse(VideoCodec.H264_BASELINE_3_1);
   }

   private static AudioCodec extractAudioCodec(IrisRtspSdp.Media m) {
      if(m == null) {
         return AudioCodec.NONE;
      }
      return extractFmt(m, "mode")
         .map((s) -> AudioCodec.fromSdp(m.getEncodingName(), s))
         .orElse(AudioCodec.fromSdp(m.getEncodingName(), null));
   }

   private static Optional<String> extractFmt(IrisRtspSdp.Media m, String p) {
      return m.getFormatParameters().stream()
         .filter(s -> StringUtils.containsIgnoreCase(s, p))
         .findFirst()
         .map(RecordingSession::parseFmtString);
   }

   private static String parseFmtString(String s) {
      String[] parts = s.split("=");
      if(parts.length != 2) {
         log.trace("parameter format did not have enough parts, default values will be used");
         return null;
      }
      String val = parts[1];
      if(val.endsWith(";")) {
         val = val.substring(0, val.length() - 1);
      }
      return val;
   }

   @Override
   public void close() {
      if(!closed) {
         closed = true;
         registry.remove(recordingId());
         doClose();
         // some sessions we don't want to automatically send the complete on close because they may still be
         // flushing buffers that impact the duration and byte count and should manually complete the recording
         if(completeOnClose) {
            complete();
         }
      }
   }

   public final void complete() {
      try {
         Optional<Date> purgedAt = recordingManager.storeDurationAndSize(storage, durationInSeconds(), byteCount(), stream);
         if(purgedAt.isPresent()) {
            eventPublisher.sendValueChange(storage.getPlaceId(), storage.getRecordingId(), durationInSeconds(), byteCount(), purgedAt.get());
         } else {
            eventPublisher.sendValueChange(storage.getPlaceId(), storage.getRecordingId(), durationInSeconds(), byteCount());
         }
      } catch (Exception ex) {
         log.warn("{} could not store video duration: {}", recordingId(), ex.getMessage(), ex);
      }
   }

   protected abstract void doClose();
   protected abstract double durationInSeconds();
   protected abstract long byteCount();
}

