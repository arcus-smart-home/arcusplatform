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

import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.iris.media.IrisRtspSdp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

public final class RtspInterleavedHandler extends ByteToMessageDecoder {
   private static final Logger log = LoggerFactory.getLogger(RtspInterleavedHandler.class);

   private static final int MAGIC = 0x24;

   private int channel = -1;

   @Override
   protected void decode(@Nullable ChannelHandlerContext ctx, @Nullable ByteBuf msg, @Nullable List<Object> out) throws Exception {
      Preconditions.checkNotNull(ctx);
      Preconditions.checkNotNull(msg);
      Preconditions.checkNotNull(out);

      if (msg.readableBytes() < 4) {
         return;
      }

      int mgc = msg.getUnsignedByte(msg.readerIndex());
      if (mgc != MAGIC) {
         throw new CorruptedFrameException("invalid rtsp interleaved packet: invalid magic byte: 0x" + Integer.toHexString(mgc));
      }

      int len = msg.getUnsignedShort(msg.readerIndex() + 2);
      if (msg.readableBytes() < (len + 4)) {
         return;
      }

      int ch = msg.getUnsignedByte(msg.readerIndex() + 1);
      if (ch != channel) {
         channel = ch;
         ctx.fireUserEventTriggered(new RtspChannel(channel));
      }

      msg.skipBytes(4);
      out.add(msg.readSlice(len).retain());
   }

   @Override
   public void userEventTriggered(@Nullable ChannelHandlerContext ctx, @Nullable Object evt) throws Exception {
      if (evt instanceof IrisRtspSdp) {
         IrisRtspSdp sdp = (IrisRtspSdp)evt;
         log.trace("interleaved sdp: {}", sdp);
      }

      ctx.fireUserEventTriggered(evt);
   }
}

