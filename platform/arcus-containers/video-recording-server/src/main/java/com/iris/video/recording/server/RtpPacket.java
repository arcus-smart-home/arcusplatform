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

import com.iris.protoc.runtime.ProtocUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class RtpPacket {
   public static final ByteBuf EMPTY = Unpooled.unmodifiableBuffer(Unpooled.buffer(0,0));

   public static final int MASK_VERSION = 0x00000003;
   public static final int MASK_PADDING = 0x20000000;
   public static final int MASK_EXTNSN  = 0x10000000;
   public static final int MASK_CSRCCNT = 0x0000000F;
   public static final int MASK_MARKER  = 0x00800000;
   public static final int MASK_PTYPE   = 0x0000007F;
   public static final int MASK_SEQNUM  = 0x0000FFFF;

   public static final int SHIFT_VERSION = 30;
   public static final int SHIFT_PADDING = 29;
   public static final int SHIFT_EXTNSN  = 28;
   public static final int SHIFT_CSRCCNT = 24;
   public static final int SHIFT_MARKER  = 23;
   public static final int SHIFT_PTYPE   = 16;
   public static final int SHIFT_SEQNUM  = 0;

   private final int header;
   private final long timestamp;
   private final int ssrc;
   private final ByteBuf csrc;
   private final ByteBuf data;
   private final int extSpec;
   private final ByteBuf ext;

   public RtpPacket(int header, long timestamp, int ssrc, ByteBuf csrc, ByteBuf data, int extSpec, ByteBuf ext) {
      this.header = header;
      this.timestamp = timestamp;
      this.ssrc = ssrc;
      this.csrc = csrc;
      this.data = data;
      this.extSpec = extSpec;
      this.ext = ext;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Accessor methods
   /////////////////////////////////////////////////////////////////////////////

   public int getVersion() {
      return (header >> SHIFT_VERSION) & MASK_VERSION;
   }

   public boolean hasPadding() {
      return (header & MASK_PADDING) != 0;
   }

   public boolean hasExtension() {
      return (header & MASK_EXTNSN) != 0;
   }

   public int getCsrcCount() {
      return (header >> SHIFT_CSRCCNT) & MASK_CSRCCNT;
   }

   public boolean hasMarker() {
      return (header & MASK_MARKER) != 0;
   }

   public int getPayloadType() {
      return (header >> SHIFT_PTYPE) & MASK_PTYPE;
   }

   public int getSequenceNumber() {
      return (header >> SHIFT_SEQNUM) & MASK_SEQNUM;
   }

   public long getTimestamp() {
      return timestamp;
   }

   public int getSsrc() {
      return ssrc;
   }

   public ByteBuf getCsrc() {
      return csrc;
   }

   public int getExtensionSpecific() {
      return extSpec;
   }

   public ByteBuf getExtension() {
      return ext;
   }

   public ByteBuf getPayload() {
      return data;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Printing support
   /////////////////////////////////////////////////////////////////////////////

   @Override
   public String toString() {
      return "rtp [" +
         "version=" + getVersion() +
         ",padding=" + hasPadding() +
         ",extension=" + hasExtension() +
         ",csrccnt=" + getCsrcCount() +
         ",marker=" + hasMarker() +
         ",payloadtype=" + getPayloadType() +
         ",seqnum=" + getSequenceNumber() +
         ",timestamp=" + getTimestamp() +
         ",ssrc=" + getSsrc() +
         ",csrc=" + ProtocUtil.toHexString(getCsrc()) +
         ",extspec=" + getExtensionSpecific() +
         ",ext=" + ProtocUtil.toHexString(getExtension()) +
         ",payload=" + ProtocUtil.toHexString(getPayload()) +
         "]";
   }
}

