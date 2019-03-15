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

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.media.IrisRtspSdp;
import com.iris.media.RtspPushHeaders;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;

public final class RtspPushHandler extends ChannelDuplexHandler {
   private static final Logger log = LoggerFactory.getLogger(RtspPushHandler.class);
   private static final byte LF = (byte)0x0A;
   private static final byte NL = (byte)0x0D;
   private static final int MAX_HEADER_SIZE = 4*1024;

   private static final byte[] REQUIRED_INITIAL_LINE = new byte[] {
      (byte)'R',
      (byte)'T',
      (byte)'S',
      (byte)'P',
      (byte)'/',
   };

   private static enum State {
      HEADERS_0,
      HEADERS_1,
      HEADERS_2,
      DATA
   }

   private final ByteBuf headerData = Unpooled.buffer();
   private State state = State.HEADERS_0;

   @Override
   public void channelRead(@Nullable ChannelHandlerContext ctx, @Nullable Object message) throws Exception {
      if (ctx == null) {
         return;
      }

      if (state != State.DATA) {
         ByteBuf msg = (ByteBuf)message;

         try {
            if (msg == null || !handleHeaders(ctx,msg)) {
               return;
            }

            state = State.DATA;
            if (msg.readableBytes() > 0) {
               super.channelRead(ctx, msg);
            }
         } finally {
            if (msg != null) {
               //msg.release();
            }
         }
      }

      headerData.release();
      ctx.pipeline().remove(this);
   }

   private boolean handleHeaders(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
      // Run all of the new bytes through the state machine to see if we
      // are at the end of the headers.
      int idx = msg.forEachByte(new ByteBufProcessor() {
         @Override
         public boolean process(byte value) {
            if (value == LF) {
               if (state == State.HEADERS_0) {
                  state = State.HEADERS_1;
               } else if (state == State.HEADERS_1 || state == State.HEADERS_2) {
                  state = State.DATA;
               }
            } else if (state == State.HEADERS_1 && value == NL) {
               state = State.HEADERS_2;
            } else {
               state = State.HEADERS_0;
            }

            return (state != State.DATA);
         }
      });

      // Make sure the initial header line looks valid. We do this early so we can
      // terminate the connection quickly if invalid bytes are arriving.
      int testLength = headerData.readableBytes();
      if (testLength > REQUIRED_INITIAL_LINE.length) {
         testLength = REQUIRED_INITIAL_LINE.length;
      }

      for (int i = 0; i < testLength; ++i) {
         if (headerData.getByte(i + headerData.readerIndex()) != REQUIRED_INITIAL_LINE[i]) {
            throw new CorruptedFrameException("invalid initial header line");
         }
      }

      // If we have not reached the end of the data then either wait for more data
      // to arrive or terminate the connection if the header is too large.
      if (idx < 0) {
         headerData.writeBytes(msg);
         if (headerData.readableBytes() > MAX_HEADER_SIZE) {
            throw new CorruptedFrameException("header too large");
         }

         return false;
      }

      // We have fully read the header data so parse the content and pass it up
      // the handler stack.
      Map<String,String> headers = new HashMap<>();
      try {
         headerData.writeBytes(msg, idx - msg.readerIndex() + 1);

         int length = headerData.readableBytes();
         String hdr = headerData.toString(StandardCharsets.UTF_8);
         log.trace("RTSP HEADERS: {}", hdr);
         try (BufferedReader rd = new BufferedReader(new StringReader(hdr))) {
            String next = rd.readLine();
            while (next != null) {
               int hidx = next.indexOf(':');
               if (hidx >= 0) {
                  String key = next.substring(0, hidx).trim().toLowerCase();
                  String value = next.substring(hidx+1).trim();
                  headers.put(key,value);
               }

               next = rd.readLine();
            }
         }
      } catch (Exception ex) {
         log.warn("could not parse rtsp push headers: {}", ex.getMessage(), ex);
      }

      ctx.fireUserEventTriggered(new RtspPushHeaders(headers));
      if (headers.containsKey("x-sdp")) {
         try {
            String encoded = headers.get("x-sdp");
            byte[] decodedArray = Base64.decodeBase64(encoded);
            String decoded = new String(decodedArray, StandardCharsets.UTF_8);
            log.trace("raw sdp: {}", decoded);

            IrisRtspSdp sdp = IrisRtspSdp.parse(decoded);
            log.trace("parsed sdp: {}", sdp);

            ctx.fireUserEventTriggered(sdp);
         } catch (Exception ex) {
            log.warn("could not parse sdp: {}", ex.getMessage(), ex);
         }
      }

      headerData.capacity(0);
      return true;
   }
}

