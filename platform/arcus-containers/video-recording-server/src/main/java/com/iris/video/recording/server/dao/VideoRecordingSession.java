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
package com.iris.video.recording.server.dao;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.commons.io.output.CountingOutputStream;

import com.iris.media.H264Factory;
import com.iris.media.IrisRtspSdp;
import com.iris.video.VideoSessionRegistry;
import com.iris.video.cql.VideoRecordingManager;
import com.iris.video.recording.RecordingEventPublisher;
import com.iris.video.recording.RecordingSession;
import com.iris.video.recording.server.MpegTsH264Adapter;
import com.iris.video.storage.VideoStorageSession;

import io.netty.channel.ChannelHandlerContext;

public class VideoRecordingSession extends RecordingSession implements H264Factory.VideoSession  {

   private final MpegTsH264Adapter adapter;
   private final VideoStorageSession storage;
   private final CountingOutputStream mpegts;

   VideoRecordingSession(
      VideoSessionRegistry registry,
      ChannelHandlerContext ctx,
      VideoRecordingManager dao,
      RecordingEventPublisher eventPublisher,
      VideoStorageSession storage,
      double precapture,
      boolean stream,
      CountingOutputStream mpegts,
      double flushTimeInS
   ) throws IOException {
      super(registry, ctx, storage, dao, eventPublisher, stream, precapture, true);

      this.adapter = new Adapter(mpegts, flushTimeInS, storage.getRecordingId());
      this.storage = storage;
      this.mpegts = mpegts;
   }

   @Override
   public UUID getRecordingId() {
      return storage.getRecordingId();
   }

   @Override
   public void h264(ByteBuffer data, long ts, int naluHeader) throws IOException {
      adapter.h264(data, ts, naluHeader);
   }

   @Override
   public void startH264(ByteBuffer data, long ts, int naluHeader) throws IOException {
      adapter.startH264(data, ts, naluHeader);
   }

   @Override
   public void appendH264(ByteBuffer data, long ts, int naluHeader) throws IOException {
      adapter.appendH264(data, ts, naluHeader);
   }

   @Override
   public void finishH264(ByteBuffer data, long ts, int naluHeader) throws IOException {
      adapter.finishH264(data, ts, naluHeader);
   }

   @Override
   protected void onSdp(IrisRtspSdp sdp) {
      try {
         adapter.setSdp(sdp);
      } catch(IOException ioe) {
         throw new RuntimeException(ioe);
      }
   }

   @Override
   protected void doClose() {
      try {
         adapter.close();
      } catch(IOException ioe) {
         throw new RuntimeException(ioe);
      }
   }

   @Override
   protected double durationInSeconds() {
      return adapter.getDurationInSeconds();
   }

   @Override
   protected long byteCount() {
      return mpegts.getByteCount();
   }

   private class Adapter extends MpegTsH264Adapter {

      Adapter(CountingOutputStream mpegts, double flushTimeInS, UUID recId) throws IOException {
         super(mpegts, flushTimeInS, recId);
      }

      @Override
      protected void handleFrameData(long tsIn90KHz, long lengthIn90KHz, long frameByteOffset, long frameByteSize, boolean isIFrame) throws Exception {

      }

      @Override
      protected void handleIFrameData(long tsIn90KHz, long lengthTillNextIn90KHz, long frameByteOffset, long frameByteSize) throws Exception {
         onIFrame(tsIn90KHz, frameByteOffset, frameByteSize);
      }
   }
}

