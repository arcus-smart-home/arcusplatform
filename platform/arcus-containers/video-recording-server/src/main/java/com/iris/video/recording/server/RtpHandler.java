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

public final class RtpHandler extends ByteToMessageDecoder {
   private static final Logger log = LoggerFactory.getLogger(RtpHandler.class);

   @Override
   protected void decode(@Nullable ChannelHandlerContext ctx, @Nullable ByteBuf msg, @Nullable List<Object> out) throws Exception {
      Preconditions.checkNotNull(ctx);
      Preconditions.checkNotNull(msg);
      Preconditions.checkNotNull(out);

      if (!msg.isReadable()) {
         return;
      }

      int header = msg.readInt();
      long timestamp = ((long)msg.readInt()) & 0xFFFFFFFFL;
      int ssrc = msg.readInt();

      int cc = getCsrcCount(header);
      ByteBuf csrc = RtpPacket.EMPTY;
      if (cc > 0) {
         csrc = msg.readSlice(4*cc).copy();
      }

      boolean ext = getExtension(header);
      int extProfileSpecific = 0;
      int extLength = 0;
      ByteBuf extension = RtpPacket.EMPTY;
      if (ext) {
         extProfileSpecific = msg.readShort() & 0xFFFF;
         extLength = msg.readShort() & 0xFFFF;
         extension = msg.readSlice(extLength).copy();
      }

      int len = msg.readableBytes();
      boolean padding = getPadding(header);
      if (padding) {
         int padBytes = msg.getByte(msg.readerIndex() + len - 1) & 0xFF;
         len -= padBytes;
      }

      ByteBuf data = msg.readSlice(len).copy();
      out.add(new RtpPacket(header, timestamp, ssrc, csrc, data, extProfileSpecific, extension));
   }

   private static int getCsrcCount(int header) {
      return (header >> RtpPacket.SHIFT_CSRCCNT) & RtpPacket.MASK_CSRCCNT;
   }

   private static boolean getPadding(int header) {
      return (header & RtpPacket.MASK_PADDING) != 0;
   }

   private static boolean getExtension(int header) {
      return (header & RtpPacket.MASK_EXTNSN) != 0;
   }

   @Override
   public void userEventTriggered(@Nullable ChannelHandlerContext ctx, @Nullable Object evt) throws Exception {
      if (evt instanceof RtspChannel) {
         int channel = ((RtspChannel)evt).getChannel();
         log.trace("channel now: {}", channel);
      } else if (evt instanceof IrisRtspSdp) {
         IrisRtspSdp sdp = (IrisRtspSdp)evt;
         log.trace("rtp sdp: {}", sdp);
      }

      ctx.fireUserEventTriggered(evt);
   }
}

