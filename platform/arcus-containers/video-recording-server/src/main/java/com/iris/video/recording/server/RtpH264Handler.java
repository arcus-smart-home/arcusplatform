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

import java.io.IOException;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.iris.media.H264Factory;
import com.iris.media.IrisRtspSdp;
import com.iris.media.RtspPushHeaders;
import com.iris.video.VideoSessionRegistry;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

public final class RtpH264Handler extends MessageToMessageDecoder<RtpPacket> {
   private static final Logger log = LoggerFactory.getLogger(RtpH264Handler.class);
   public static final int PAYLOAD_H264_96 = 96;
   public static final int PAYLOAD_H264_99 = 99;

   public static final int MASK_FZR  = 0x80;
   public static final int MASK_NRI  = 0x03;
   public static final int MASK_TYPE = 0x1F;

   public static final int SHIFT_NRI = 5;
   public static final int SHIFT_TYPE = 0;

   public static final int MASK_FU_SRT = 0x80;
   public static final int MASK_FU_END = 0x40;
   public static final int MASK_FU_RSV = 0x20;
   public static final int MASK_FU_TYP = 0x1F;

   private final H264Factory factory;

   @Nullable
   private IrisRtspSdp sdp;

   @Nullable
   private RtspPushHeaders hdrs;

   @Nullable
   private H264Factory.VideoSession session;

   private VideoSessionRegistry registry;

   private int channel;
   private int curSeq = Integer.MIN_VALUE;

   public RtpH264Handler(H264Factory factory, VideoSessionRegistry registry) {
      this.factory = factory;
      this.registry = registry;
      this.session = null;
   }

   @Override
   public void userEventTriggered(@Nullable ChannelHandlerContext ctx, @Nullable Object evt) throws Exception {
      if (evt instanceof RtspChannel) {
         channel = ((RtspChannel)evt).getChannel();
         log.trace("channel now: {}", channel);
      } else if (evt instanceof IrisRtspSdp) {
         sdp = (IrisRtspSdp)evt;
      } else if (evt instanceof RtspPushHeaders) {
         hdrs = (RtspPushHeaders)evt;
      }

      if (session == null && sdp != null && hdrs != null) {
         session = factory.createVideoSession(ctx, hdrs);
         session.setSdp(sdp);
      }

      if (ctx != null) {
         ctx.fireUserEventTriggered(evt);
      }
   }

   @Override
   protected void decode(@Nullable ChannelHandlerContext ctx, @Nullable RtpPacket rtp, @Nullable List<Object> out) throws Exception {
      Preconditions.checkNotNull(ctx);
      Preconditions.checkNotNull(rtp);
      Preconditions.checkNotNull(out);

      // Sercomm (and some DLink models) use format 96, DLinks uses format 99
      if ((rtp.getPayloadType() != PAYLOAD_H264_96) && (rtp.getPayloadType() != PAYLOAD_H264_99)) {
         out.add(rtp);
         return;
      }

      int nextSeq = rtp.getSequenceNumber();
      if (curSeq != Integer.MIN_VALUE) {
         if (nextSeq <= curSeq && nextSeq != 0 && curSeq != 65535) {
            log.warn("OUT OF ORDER RTP PACKET: cur={}, next={}", curSeq, nextSeq);
         }
      }

      ByteBuf msg = rtp.getPayload();
      try {
         int nalUnitHeader = msg.readUnsignedByte();
         if ((nalUnitHeader & MASK_FZR) != 0) {
            log.warn("nal unit header corrupt: force zero bit set");
         }

         int type = (nalUnitHeader >> SHIFT_TYPE) & MASK_TYPE;
         switch (type) {
         case 28: decodeFU(ctx, rtp, msg, out, nalUnitHeader, false); break;
         case 29: decodeFU(ctx, rtp, msg, out, nalUnitHeader, true); break;
         default:
            if (1 <= type && type <= 23) {
               decodeSingle(ctx, rtp, msg, out, nalUnitHeader);
            } else {
               log.warn("received unknown rtp.h264 packet: nalu={}, msg={}", nalUnitHeader);
            }
            break;
         }
      } finally {
         curSeq = nextSeq;
         msg.release();
      }
   }

   private void startSession() {
   }

   private void decodeSingle(ChannelHandlerContext ctx, RtpPacket rtp, ByteBuf msg, List<Object> out, int nalUnitHeader) throws Exception {
      if (session == null) {
         log.warn("empty session, closing current recording session");
         ctx.close();
         return;
      }

      session.h264(msg.nioBuffer(), rtp.getTimestamp(), nalUnitHeader);

      if (log.isTraceEnabled()) {
         log.trace("received rtp.h264 single nal unit packet: nalu={}, type={}, ts={}", nalUnitHeader, rtp.getTimestamp());
      }
   }

   private void decodeFU(ChannelHandlerContext ctx, RtpPacket rtp, ByteBuf msg, List<Object> out, int nalUnitHeader, boolean hasDon) throws Exception {
      if (session == null) {
         log.warn("empty session, closing current recording session");
         ctx.close();
         return;
      }

      int fuHeader = msg.readUnsignedByte();
      if (hasDon) {
         msg.readUnsignedByte();
      }

      boolean srt = (fuHeader & MASK_FU_SRT) != 0;
      boolean end = (fuHeader & MASK_FU_END) != 0;
      boolean rsv = (fuHeader & MASK_FU_RSV) != 0;
      int typ = (fuHeader & MASK_FU_TYP);

      int fnalUnitHeader = (nalUnitHeader & ~0x1F) | typ;
      if (srt) {
         session.startH264(msg.nioBuffer(), rtp.getTimestamp(), fnalUnitHeader);
      } else if (end) {
         session.finishH264(msg.nioBuffer(), rtp.getTimestamp(), fnalUnitHeader);
      } else {
         session.appendH264(msg.nioBuffer(), rtp.getTimestamp(), fnalUnitHeader);
      }

      if (log.isTraceEnabled()) {
         log.trace("received rtp.h264 fu-{} packet: nalu={}, start={}, end={}, reserved={}, type={}, ts={}", hasDon ? "b" : "a", nalUnitHeader, srt, end, rsv, typ, rtp.getTimestamp());
      }
   }

   @Override
   public void channelInactive(@Nullable ChannelHandlerContext ctx) throws Exception {
      try {
         if (session != null) {
            session.close();
         }
      } catch (IOException ex) {
         // ignore
      } finally {
         registry.remove(ctx);
      }

      super.channelInactive(ctx);
   }
}

