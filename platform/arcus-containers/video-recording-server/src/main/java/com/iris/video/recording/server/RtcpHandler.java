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
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

public final class RtcpHandler extends MessageToMessageDecoder<RtpPacket> {
   private static final Logger log = LoggerFactory.getLogger(RtpH264Handler.class);

   public static final int PAYLOAD_RTCP = 72;

   @Nullable
   private IrisRtspSdp sdp;
   private int channel;

   @Override
   protected void decode(@Nullable ChannelHandlerContext ctx, @Nullable RtpPacket msg, @Nullable List<Object> out) throws Exception {
      Preconditions.checkNotNull(ctx);
      Preconditions.checkNotNull(msg);
      Preconditions.checkNotNull(out);

      // TODO: this really needs to examine the SDP information
      if (msg.getPayloadType() != PAYLOAD_RTCP) {
         out.add(msg);
         return;
      }

      ByteBuf payload = msg.getPayload();
      if (log.isTraceEnabled()) {
         log.trace("rtcp payload: {}", ByteBufUtil.hexDump(payload));
      }

      payload.release();
   }

   @Override
   public void userEventTriggered(@Nullable ChannelHandlerContext ctx, @Nullable Object evt) throws Exception {
      if (evt instanceof RtspChannel) {
         channel = ((RtspChannel)evt).getChannel();
      } else if (evt instanceof IrisRtspSdp) {
         sdp = (IrisRtspSdp)evt;
      }

      ctx.fireUserEventTriggered(evt);
   }
}

