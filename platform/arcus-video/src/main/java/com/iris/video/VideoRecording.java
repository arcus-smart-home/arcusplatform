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
package com.iris.video;

import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

public class VideoRecording {
   public final UUID recordingId;
   public final UUID cameraId;
   public final UUID accountId;
   public final UUID placeId;
   public final String storage;

   @Nullable
   public final UUID personId;

   public final int width;
   public final int height;
   public final int bandwidth;
   public final double framerate;

   public double duration;
   public long size;

   @Nullable
   public final VideoCodec videoCodec;
   @Nullable
   public final AudioCodec audioCodec;

   public final List<VideoIFrame> iframes;
   
   public final long expiration;	

   VideoRecording(
      UUID recordingId,
      UUID cameraId,
      UUID accountId,
      UUID placeId,
      long expiration,
      @Nullable UUID personId,
      String storage,
      int width,
      int height,
      int bandwidth,
      double framerate,
      double duration,
      long size,
      @Nullable VideoCodec videoCodec,
      @Nullable AudioCodec audioCodec,
      List<VideoIFrame> iframes
   ) {
      this.recordingId = recordingId;
      this.cameraId = cameraId;
      this.accountId = accountId;
      this.placeId = placeId;
      this.personId = personId;
      this.storage = storage;

      this.width = width;
      this.height = height;
      this.bandwidth = bandwidth;
      this.framerate = framerate;
      this.duration = duration;
      this.size = size;
      this.videoCodec = videoCodec;
      this.audioCodec = audioCodec;
      this.iframes = iframes;
      this.expiration = expiration;
   }

   public boolean isRecordingFinished() {
      return duration >= 0.0;
   }
}

