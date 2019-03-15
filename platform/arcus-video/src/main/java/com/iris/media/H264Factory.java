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
package com.iris.media;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import io.netty.channel.ChannelHandlerContext;

public interface H264Factory {
   VideoSession createVideoSession(ChannelHandlerContext ctx, RtspPushHeaders hdrs) throws IOException;

   public static interface VideoSession extends Closeable {
      void setSdp(IrisRtspSdp sdp) throws IOException;

      UUID getRecordingId();
      void h264(ByteBuffer data, long ts, int naluHeader) throws IOException;
      void startH264(ByteBuffer data, long ts, int naluHeader) throws IOException;
      void appendH264(ByteBuffer data, long ts, int naluHeader) throws IOException;
      void finishH264(ByteBuffer data, long ts, int naluHeader) throws IOException;
   }
}


