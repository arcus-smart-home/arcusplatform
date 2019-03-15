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
package com.iris.video.recording.server;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.commons.io.output.CountingOutputStream;

import com.iris.media.MpegTsH264;

public abstract class MpegTsH264Adapter extends MpegTsH264 implements Closeable {
   private static final long TS_START_OF_STREAM = Long.MIN_VALUE;
   private static final long TS_END_OF_STREAM = Long.MAX_VALUE;

   protected final CountingOutputStream mpegts;

   private long previousIFrameTs = TS_START_OF_STREAM;
   private long previousIFrameStart;
   private long previousIFrameEnd;

   private long previousFrameTs = TS_START_OF_STREAM;
   private long previousFrameStart;
   private long previousFrameEnd;

   private long fragmentedFrameStart;

   private int lastNalu;
   private long lastTs;
   private long lastBytes;

   private long lastFlushTs = Long.MIN_VALUE;
   private long flushFrequencyIn90KHz;

   protected MpegTsH264Adapter(CountingOutputStream mpegts, double flushTimeInS, UUID recId) throws IOException {
      super(new DataOutputStream(mpegts), recId);
      this.mpegts = mpegts;

      this.flushFrequencyIn90KHz = (long)(flushTimeInS * 90000);
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public void h264(ByteBuffer data, long ts, int naluHeader) throws IOException {
      long fs = mpegts.getByteCount();
      super.h264(data, ts, naluHeader);
      long fe = mpegts.getByteCount();

      handleFrame(naluHeader, ts, fs, fe);
      lastNalu = naluHeader;
      lastTs = ts;
      lastBytes = mpegts.getByteCount();

      checkFlush();
   }

   @Override
   public void startH264(ByteBuffer data, long ts, int naluHeader) throws IOException {
      fragmentedFrameStart = mpegts.getByteCount();
      super.startH264(data, ts, naluHeader);

      lastNalu = naluHeader;
      lastTs = ts;
      lastBytes = mpegts.getByteCount();
   }

   @Override
   public void appendH264(ByteBuffer data, long ts, int naluHeader) throws IOException {
      super.appendH264(data, ts, naluHeader);

      lastNalu = naluHeader;
      lastTs = ts;
      lastBytes = mpegts.getByteCount();
   }

   @Override
   public void finishH264(ByteBuffer data, long ts, int naluHeader) throws IOException {
      super.finishH264(data, ts, naluHeader);
      long fe = mpegts.getByteCount();

      handleFrame(naluHeader, ts, fragmentedFrameStart, fe);
      lastNalu = naluHeader;
      lastTs = ts;
      lastBytes = mpegts.getByteCount();

      checkFlush();
   }

   private void checkFlush() throws IOException {
      if (lastFlushTs == Long.MIN_VALUE) {
         lastFlushTs = lastTs;
         return;
      }

      long timeSinceLastFlush = lastTs - lastFlushTs;
      if (timeSinceLastFlush >= flushFrequencyIn90KHz) {
         lastFlushTs = lastTs;
         mpegts.flush();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   protected abstract void handleFrameData(long tsIn90KHz, long lengthIn90KHz, long frameByteOffset, long frameByteSize, boolean isIFrame) throws Exception;

   private void handleFrame(int naluHeader, long ts, long frameStart, long frameEnd) throws IOException {
      boolean iframe = isIFrame(naluHeader);
      if (iframe) {
         handleIFrame(ts, frameStart, frameEnd);
      }

      long outputTs = previousFrameTs;
      long outputStart = previousFrameStart;
      long outputEnd = previousFrameEnd;

      previousFrameTs = ts;
      previousFrameStart = frameStart;
      previousFrameEnd = frameEnd;

      if (outputTs == TS_START_OF_STREAM) {
         // no previous frame to flush
         return;
      }

      long length = ts - outputTs;
      long size = outputEnd - outputStart;

      try {
         handleFrameData(outputTs, length, outputStart, size, iframe);
      } catch (IOException ex) {
         throw ex;
      } catch (Exception ex) {
         throw new IOException(ex);
      }
   }

   private void finishFrame(int naluHeader, long lastTs) throws IOException {
      handleFrame(naluHeader, lastTs, 0, 0);
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   protected abstract void handleIFrameData(long tsIn90KHz, long lengthTillNextIn90KHz, long frameByteOffset, long frameByteSize) throws Exception;

   private void handleIFrame(long ts, long frameStart, long frameEnd) throws IOException {
      if(previousIFrameTs == TS_END_OF_STREAM) {
         // the stream has been closed, ignore additional frames
         return;
      }
   
      long outputTs = previousIFrameTs;
      long outputStart = previousIFrameStart;
      long outputEnd = previousIFrameEnd;

      previousIFrameTs = ts;
      previousIFrameStart = frameStart;
      previousIFrameEnd = frameEnd;

      if (outputTs == TS_START_OF_STREAM) {
         // no previous iframe to flush
         return;
      }

      long lengthTillNext = ts - outputTs;
      long size = outputEnd - outputStart;

      try {
         handleIFrameData(outputTs, lengthTillNext, outputStart, size);
      } catch (IOException ex) {
         throw ex;
      } catch (Exception ex) {
         throw new IOException(ex);
      }
   }

   private void finishIFrame(long lastTs) throws IOException {
      handleIFrame(lastTs, 0, 0);
      // don't process anymore i-frames
      previousIFrameTs = TS_END_OF_STREAM;
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   public long getByteSize() {
      return lastBytes;
   }

   public float getDurationInSeconds() {
      if (h264Info != null) {
         long frameTsLength = 90000 * h264Info.getTicksPerTimeScale() / h264Info.getTimeScale();
         return (lastTs + frameTsLength) / 90000.0f;
      } else {
         return lastTs / 90000.0f;
      }
   }

   public float getBandwidthEstimateInBytesPerSecond() {
      return getByteSize() / getDurationInSeconds();
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public void close() throws IOException {
      finishFrame(lastNalu, lastTs);
      finishIFrame(lastTs);

      super.close();
   }
}

