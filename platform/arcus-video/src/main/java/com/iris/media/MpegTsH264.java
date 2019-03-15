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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.protoc.runtime.ProtocUtil;

////////////////////////////////////////////////////////////////////////////////
// MPEG-TS File Format:
//
//    FR #  PID   TYPE
//    1     0000  program association table
//    2     0042  program map table
//    3     0045  PES-stream video packet (with start, with PCR)
//    4     0045  PES-stream video packet
//    ...
//    8     0045  PES-stream video packet (with PCR)
//    ...
//    11    0045  PES-stream video packet (with padding)
//    12    0000  program association table
//    13    0042  program map table
//    14    0045  PES-stream video packet (with start, with PCR)
//    ...
//
// MPEG-TS Packet Frame Format (must be exactly 188 bytes):
//    sync byte                  8-bits   The values 0x47
//    transport error indicator  1-bit    Informs the stream processor
//                                        that the packet contains errors.
//    payload start indicator    1-bit    Set to indicate the start of a
//                                        PES data or PSI payload.
//    transport priority         1-bit    Set to indicate a higher
//                                        priority.
//    packet identifier          13-bits  The packet identifier.
//    scrambling control         2-bit    The scrambling control used
//                                        by the stream.
//    adaptation field exists    1-bit    Set if an adaptation field is
//                                        present in the packet.
//    contains payload           1-bit    Set if a payload field is
//                                        present in the packet.
//    continuity counter         4-bits   Sequence number used only
//                                        when a payload is present.
//                                        Incremented each time a
//                                        given PID has a payload.
//    adaptation field           var      An adaptation field formatted
//                                        according to the adaptation
//                                        field format.
//    payload data               var      The payload.
//
// Adaptation Field Format:
//    adaptation field length    8-bits   Number of bytes in the
//                                        adaptation field immediately
//                                        following this byte.
//    discontinuity indicator    1-bit    Set to 1 if the current ts
//                                        packet is in a discontinuity
//                                        state.
//    random access indicator    1-bit    Set to 1 if the PES packet in
//                                        this TS packet starts a video
//                                        or audio sequence.
//    es priority indicator      1-bit    Set to 1 if this elementary
//                                        stream is higher priority.
//    pcr flag                   1-bit    Set to 1 if this adaptation
//                                        field contains a PCR field.
//    opcr flag                  1-bit    Set to 1 if this adaptation
//                                        field contains a OPCR field.
//    splicing point flag        1-bit    Set to 1 if this adaptation
//                                        field contains a a splice
//                                        count field.
//    transport private flag     1-bit    Set to 1 if this adaptation
//                                        field contains private data.
//    adpatation field ext flag  1-bit    Set to 1 if this adaptation
//                                        field contains an extension.
//    pcr                        48-bits  Program clock reference stored
//                                        as 33-bit base, 6-bit pad,
//                                        and 9-bit extension.
//    opcr                       48-bits  Original clock reference
//                                        stored as 33-bit base, 6-bit
//                                        pad, and 9-bit extension.
//    splice countdown           8-bit    Inidicates how many TS packets
//                                        from this one a splicing point
//                                        occurs (may be negative).
//    stuffing bytes             var      Stuffing bytes used to pad
//                                        out to "length" bytes.
//
// Program Association Table:
//    pointer field              8-bits   Set to 0x00 for PAT.
//    table id                   8-bits   Set to 0x00 for PAT.
//    section syntax indicator   1-bit    Set to 1 for PAT.
//    zero                       1-bit    Set to 0.
//    reserved                   2-bit    Set to 11.
//    section length             12-bits  Number of bytes in the packet
//                                        beyond this field.
//    transport stream id        16-bits  The transport stream identifier
//                                        (0x22DC in example).
//    reserved                   2-bits   Set to 11.
//    version number             5-bits   Incremented each time that PAT
//                                        changes (set to 5 in example).
//    current next indicator     1-bit    Set to 1 for PAT.
//    section number             8-bits   The section number of this
//                                        current PAT packet.
//    last section number        8-bits   The section number of the
//                                        final packet of the PAT.
//
//    1..N sections
//       program number          16-bits  Program number assigned
//                                        by user starting at 1.
//       reserved                3-bits   Set to 111.
//       network or PMT PID      13-bits  Set to the PID of the network
//                                        or PMT identifier.
//
//    Footer
//       crc 32                  32-bits  CRC-32 checksum for the packet.
//
// Program Map Table:
//    pointer field              8-bits   Set to 0x00 for PMT.
//    table id                   8-bits   Set to 0x02 for PMT.
//    section syntax indicator   1-bit    Set to 1 for PMT.
//    zero                       1-bit    Set to 0.
//    reserved                   2-bit    Set to 11.
//    section length             12-bits  Number of bytes in the packet
//                                        beyond this field.
//    program number             16-bits  The program number.
//    reserved                   2-bits   Set to 11.
//    version number             5-bits   Incremented each time the PMT
//                                        changes ().
//    current next indicator     1-bit    Set to 1 for PMT.
//    section number             8-bits   The section number of this
//                                        current PMT packet.
//    last section number        8-bits   The section number of the
//                                        final packet of the PMT.
//    reserved                   3-bits   Set to 111.
//    pcr pid                    13-bits  The PID of the packet
//                                        that contains PCR
//    reserved                   4-bits   Set to 1111.
//    program info length        12-bits
//    program info descriptor    var
//
//    1..N sections
//       stream type             8-bits   Set to 0x1B for H.264
//       reserved                3-bits   Set to 111.
//       elementary PID          13-bits
//       reserved                4-bits   Set to 1111.
//       es info length          12-bits
//       descriptor              var
//
//    Footer
//       crc 32                  32-bits  CRC-32 checksum for the packet.
////////////////////////////////////////////////////////////////////////////////
public class MpegTsH264 implements H264Factory.VideoSession {
   private static final Logger log = LoggerFactory.getLogger(MpegTsH264.class);

   public static int PAT_LENGTH = 188;
   public static int PMT_LENGTH = 188;

   public static int MASK_SYNC_BYTE                    = 0xFF000000;
   public static int MASK_TRANSPORT_ERROR_INDICATOR    = 0x00800000;
   public static int MASK_PAYLOAD_UNIT_START_INDICATOR = 0x00400000;
   public static int MASK_TRANSPORT_PRIORITY           = 0x00200000;
   public static int MASK_PACKET_IDENTIFIER            = 0x001FFF00;
   public static int MASK_SCRAMBLING_CONTROL           = 0x000000C0;
   public static int MASK_ADAPTION_FIELD_EXISTS        = 0x00000020;
   public static int MASK_CONTAINS_PAYLOAD             = 0x00000010;
   public static int MASK_CONTINUITY_COUNTER           = 0x0000000F;

   public static final int SYNC_BYTE = 0x47;
   public static final int SCRAMBLING_CONTROL_CLEAR    = 0x00;
   public static final int SCRAMBLING_CONTROL_RSVD     = 0x01;
   public static final int SCRAMBLING_CONTROL_EVEN     = 0x02;
   public static final int SCRAMBLING_CONTROL_ODD      = 0x03;

   public static final int PID_PAT             = 0x0000;
   public static final int PID_CAT             = 0x0001;
   public static final int PID_TS_DESC         = 0x0002;
   public static final int PID_IPMP_CTRL_INFO  = 0x0003;
   public static final int PID_RSV_START       = 0x0004;
   public static final int PID_RSV_END         = 0x000F;
   public static final int PID_DVB_META_START  = 0x0010;
   public static final int PID_DVB_META_END    = 0x001F;
   public static final int PID_ASSIGNED_START  = 0x0020;
   public static final int PID_ASSIGNED_END    = 0x1FFA;
   public static final int PID_ATSC_MGT_META   = 0x1FFB;
   public static final int PID_ASSIGNED2_START = 0x1FFC;
   public static final int PID_ASSIGNED2_END   = 0x1FFE;
   public static final int PID_NULL            = 0x1FFF;

   public static final int PID_SPTS_PMT        = 0x0030;
   public static final int PID_SPTS_VIDEO      = 0x0031;
   public static final int PID_SPTS_AC3_AUDIO  = 0x0034;
   public static final int PID_SPTS_EIT0       = 0x1770;
   public static final int PID_SPTS_EIT1       = 0x1771;
   public static final int PID_SPTS_EIT2       = 0x1772;
   public static final int PID_SPTS_EIT3       = 0x1773;
   public static final int PID_SPTS_PSIP_BASE  = 0x1FFB;

   public static final int TID_MPEG_PAT        = 0x00;
   public static final int TID_MPEG_CAT        = 0x01;
   public static final int TID_MPEG_PMT        = 0x02;
   public static final int TID_MPEG_TS_DESC    = 0x03;

   private static final ByteBuffer PAT;
   private static final ByteBuffer PMT;

   static {
      ByteBuffer pat = ByteBuffer.allocate(184);
      ByteBuffer pmt = ByteBuffer.allocate(184);

      pat.putInt(0xa600ffff);  pat.putInt(0xffffffff); pat.putInt(0xffffffff); pat.putInt(0xffffffff);
      pat.putInt(0xffffffff);  pat.putInt(0xffffffff); pat.putInt(0xffffffff); pat.putInt(0xffffffff);
      pat.putInt(0xffffffff);  pat.putInt(0xffffffff); pat.putInt(0xffffffff); pat.putInt(0xffffffff);
      pat.putInt(0xffffffff);  pat.putInt(0xffffffff); pat.putInt(0xffffffff); pat.putInt(0xffffffff);
      pat.putInt(0xffffffff);  pat.putInt(0xffffffff); pat.putInt(0xffffffff); pat.putInt(0xffffffff);
      pat.putInt(0xffffffff);  pat.putInt(0xffffffff); pat.putInt(0xffffffff); pat.putInt(0xffffffff);
      pat.putInt(0xffffffff);  pat.putInt(0xffffffff); pat.putInt(0xffffffff); pat.putInt(0xffffffff);
      pat.putInt(0xffffffff);  pat.putInt(0xffffffff); pat.putInt(0xffffffff); pat.putInt(0xffffffff);
      pat.putInt(0xffffffff);  pat.putInt(0xffffffff); pat.putInt(0xffffffff); pat.putInt(0xffffffff);
      pat.putInt(0xffffffff);  pat.putInt(0xffffffff); pat.putInt(0xffffffff); pat.putInt(0xffffffff);
      pat.putInt(0xffffffff);  pat.putInt(0xffffff00); pat.putInt(0x00b00d22); pat.putInt(0xdccb0000);
      pat.putInt(0x0001e042);  pat.putInt(0xb56a386c);
      pat.flip();


      pmt.putInt(0x9b00ffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff);
      pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff);
      pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff);
      pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff);
      pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff);
      pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff);
      pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff);
      pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff);
      pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff);
      pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0xffffffff); pmt.putInt(0x0002b018);
      pmt.putInt(0x0001c100); pmt.putInt(0x00e045f0); pmt.putInt(0x001be045); pmt.putInt(0xf0060a04);
      pmt.putInt(0x00000000); pmt.putInt(0xb1c56edb);
      pmt.flip();

      PAT = pat.asReadOnlyBuffer();
      PMT = pmt.asReadOnlyBuffer();
   }

   protected final DataOutputStream output;
   protected final WritableByteChannel channel;

   private int h264Counter;
   private int patCounter;
   private int pmtCounter;
   private ByteBuffer spsPpsPrefix = ByteBuffer.allocate(0);

   @Nullable
   protected IrisRtspSdp sdp;

   @Nullable
   protected IrisRtspSdp.Media h264;

   @Nullable
   protected H264SpsInfo.Info h264Info;

   protected UUID recId;

   protected MpegTsH264(DataOutputStream output, UUID recId) {
      this.output = output;
      this.channel = Channels.newChannel(output);
      this.recId = recId;
      h264Counter = 0;
   }

   @Override
   public void setSdp(IrisRtspSdp sdp) throws IOException {
      this.sdp = sdp;

      for (IrisRtspSdp.Media media : this.sdp.getMedia()) {
         if (!"video".equals(media.getType())) {
            continue;
         }

         if ("h264".equalsIgnoreCase(media.getEncodingName())) {
            this.h264 = media;
            break;
         }
      }

      sdp(sdp, this.h264);
   }

   @Override
   public UUID getRecordingId() {
      return recId;
   }

   /////////////////////////////////////////////////////////////////////////////
   // H.264 frame output
   /////////////////////////////////////////////////////////////////////////////

   private void sdp(IrisRtspSdp sdp, @Nullable IrisRtspSdp.Media media) throws IOException {
      if (media == null) {
         return;
      }

      List<String> fmtp = media.getFormatParameters();
      for (String param : fmtp) {
         if (param == null || param.isEmpty()) {
            continue;
         }

         if (param.startsWith("sprop-parameter-sets=")) {
            sprops(sdp, media, param.substring("sprop-parameter-sets=".length()));
            continue;
         }
      }
   }

   private void sprops(IrisRtspSdp sdp, IrisRtspSdp.Media media, String sprops) throws IOException {
      log.trace("sprops: {}", sprops);
      byte[] sps = null;
      byte[] pps = null;

      String spsEnc = sprops;
      String ppsEnc = "";

      int idx = sprops.indexOf(',');
      if (idx >= 0) {
         log.trace("sps/pps idx: {}", idx);
         spsEnc = sprops.substring(0, idx);
         ppsEnc = sprops.substring(idx+1);
      }

      if (!spsEnc.isEmpty()) {
         log.trace("decoding sps: {}", spsEnc);

         try {
            sps = Base64.decodeBase64(spsEnc);
         } catch (Exception ex) {
            log.warn("failed to base64 decode sps: {}", ex.getMessage(), ex);
         }
      }

      if (!ppsEnc.isEmpty()) {
         log.trace("decoding pps: {}", ppsEnc);

         try {
            pps = Base64.decodeBase64(ppsEnc);
         } catch (Exception ex) {
            log.warn("failed to base64 decode sps: {}", ex.getMessage(), ex);
         }
      }

      int size = 6 + (sps != null ? sps.length + 4 : 0) + (pps != null ? pps.length + 4 : 0);
      spsPpsPrefix = ByteBuffer.allocate(size);

      if (sps != null || pps != null) {
         spsPpsPrefix.putInt(0x00000001);
         spsPpsPrefix.putShort((short)0x09E0);
      }

      if (sps != null) {
         try {
            h264Info = H264SpsInfo.parseSpsInfo(sps);
            media.setResolution(h264Info.getWidth(), h264Info.getHeight());
         } catch (Throwable th) {
            // ignore
         }

         spsPpsPrefix.putInt(0x00000001);
         spsPpsPrefix.put(sps);
      }

      if (pps != null) {
         spsPpsPrefix.putInt(0x00000001);
         spsPpsPrefix.put(pps);
      }

      spsPpsPrefix.flip();
      if (log.isTraceEnabled()) {
         log.trace("sps/pps prefix: {}", ProtocUtil.toHexString(spsPpsPrefix));
      }
   }

   private void pat() throws IOException {
      output.writeInt(0x47400030 | (patCounter++ & 0xF));
      rawpacket(PAT.duplicate());
   }

   private void pmt() throws IOException {
      output.writeInt(0x47404230 | (pmtCounter++ & 0xF));
      rawpacket(PMT.duplicate());
   }

   public boolean isIFrame(int naluHeader) {
      return (naluHeader & 0x1F) == 5;
   }

   @Override
   public void h264(ByteBuffer data, long ts, int naluHeader) throws IOException {
      boolean iframe = isIFrame(naluHeader);
      if (iframe) {
         pat();
         pmt();
      }

      doH264(data, true, true, ts, naluHeader);
   }

   @Override
   public void startH264(ByteBuffer data, long ts, int naluHeader) throws IOException {
      boolean iframe = isIFrame(naluHeader);
      if (iframe) {
         pat();
         pmt();
      }

      doH264(data, true, false, ts, naluHeader);
   }

   @Override
   public void appendH264(ByteBuffer data, long ts, int naluHeader) throws IOException {
      doH264(data, false, false, ts, naluHeader);
   }

   @Override
   public void finishH264(ByteBuffer data, long ts, int naluHeader) throws IOException {
      doH264(data, false, true, ts, naluHeader);
   }

   private void doH264(ByteBuffer data, boolean start, boolean finish, long ts, int naluHeader) throws IOException {
      if (start) {
         int size = data.remaining();
         size = (size > (184-spsPpsPrefix.remaining()-32)) ? (184-spsPpsPrefix.remaining()-32) : size;

         header(true, 0x45, h264Counter++, ts, size + spsPpsPrefix.remaining() + 24);
         pes(ts + 67500, ts + 67500, 0);

         payload(spsPpsPrefix.duplicate(), spsPpsPrefix.remaining());
         output.writeInt(0x00000001);
         output.writeByte(naluHeader & 0xFF);

         payload(data, size);
      }

      while (data.hasRemaining()) {
         int size = data.remaining();
         if (size > 184) size = 184;
         else if (size == 183) size = 182;

         packet(data, false, 0x45, h264Counter++, size);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Packet formatting methods
   /////////////////////////////////////////////////////////////////////////////

   private void adaptation(long ts, int size) throws IOException {
      long pcr = (ts << 15);

      output.writeShort(((size-1) << 8) | 0x10);
      output.writeShort((short)(pcr >> 32));
      output.writeInt((int)pcr);
      for (int i = 0; i < (size-8); ++i) {
         output.writeByte(0xFF);
      }
   }

   private void padding(int size) throws IOException {
      output.writeShort((size-1) << 8);
      for (int i = 0; i < (size-2); ++i) {
         output.writeByte(0xFF);
      }
   }

   private void pes(long pts, long dts, int size) throws IOException {
      output.writeInt(0x000001E0);
      output.writeInt(((size & 0xFFFF) << 16) | 0x80C0);
      output.writeByte(0x0A);

      long pts_32_30 = (pts >> 29) & 0x7;
      long pts_29_15 = (pts >> 15) & 0x7FFF;
      long pts_14_0  = pts & 0x7FFF;
      long ptsv = 0x3100010001L | (pts_32_30 << 33L) | (pts_29_15 << 17L) | (pts_14_0 << 1L);

      long dts_32_30 = (dts >> 29) & 0x7;
      long dts_29_15 = (dts >> 15) & 0x7FFF;
      long dts_14_0  = dts & 0x7FFF;
      long dtsv = 0x1100010001L | (dts_32_30 << 33L) | (dts_29_15 << 17L) | (dts_14_0 << 1L);

      output.writeByte((int)(ptsv >> 32));
      output.writeInt((int)ptsv);

      output.writeByte((int)(dtsv >> 32));
      output.writeInt((int)dtsv);
   }

   private void header(boolean start, int pid, int counter, long ts, int size) throws IOException {
      int padding = 184 - size;
      if (padding <= 7) {
         throw new RuntimeException("improperly formatted payload handed to MpegTsH264.packet(): " + padding + ", " + size);
      }

      int header = 0x47000000 | ((pid & 0x1FFF) << 8) | (counter & 0x0F);
      if (start) header |= 0x00400000;
      if (padding > 0) header |= 0x00000020;
      if (size > 0) header |= 0x00000010;

      output.writeInt(header);
      if (padding > 0) {
         adaptation(ts, padding);
      }
   }

   private void header(boolean start, int pid, int counter, int size) throws IOException {
      int padding = 184 - size;
      if (padding < 0 || padding == 1) {
         throw new RuntimeException("improperly formatted payload handed to MpegTsH264.packet(): " + padding + ", " + size);
      }

      int header = 0x47000000 | ((pid & 0x1FFF) << 8) | (counter & 0x0F);
      if (start) header |= 0x00400000;
      if (padding > 0) header |= 0x00000020;
      if (size > 0) header |= 0x00000010;

      output.writeInt(header);
      if (padding > 0) {
         padding(padding);
      }
   }

   private void packet(ByteBuffer data, boolean start, int pid, int counter, int size) throws IOException {
      header(start, pid, counter, size);
      payload(data, size);
   }

   private void payload(ByteBuffer data, int size) throws IOException {
      ByteBuffer slice = (ByteBuffer)data.duplicate().limit(data.position() + size);
      data.position(data.position()+size);
      channel.write(slice);
   }

   private void rawpacket(ByteBuffer data) throws IOException {
      payload(data, data.remaining());
   }

   /////////////////////////////////////////////////////////////////////////////
   // MpegTs initialization and finalization
   /////////////////////////////////////////////////////////////////////////////

   public static MpegTsH264 create(String path, UUID recId) throws IOException {
      return create(new File(path), recId);
   }

   public static MpegTsH264 create(File path, UUID recId) throws IOException {
      return create(new BufferedOutputStream(new FileOutputStream(path)), recId);
   }

   public static MpegTsH264 create(OutputStream output, UUID recId) throws IOException {
      return new MpegTsH264(new DataOutputStream(output), recId);
   }

   @Override
   public void close() throws IOException {
      output.close();
   }
}

