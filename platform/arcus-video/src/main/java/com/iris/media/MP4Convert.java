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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class MP4Convert {
   private static final Logger log = LoggerFactory.getLogger(MP4Convert.class);

   public static int PACKET_SIZE                   = 188;

   public static int MASK_SYNC_BYTE                = 0xFF000000;
   public static int SYNC_BYTE                     = 0x47000000;

   public static int MASK_TRANSPORT_ERROR_INDICATOR    = 0x00800000;
   public static int MASK_PAYLOAD_UNIT_START_INDICATOR = 0x00400000;
   public static int MASK_TRANSPORT_PRIORITY           = 0x00200000;
   public static int MASK_PACKET_IDENTIFIER            = 0x001FFF00;
   public static int MASK_SCRAMBLING_CONTROL           = 0x000000C0;
   public static int MASK_ADAPTION_FIELD_EXISTS        = 0x00000020;
   public static int MASK_CONTAINS_PAYLOAD             = 0x00000010;
   public static int MASK_CONTINUITY_COUNTER           = 0x0000000F;
   public static int PACKET_IDENTIFIER_SHIFT           = 8;

   public static int MASK_RAI          = 0x40;
   public static int MASK_PCR          = 0x10;
   public static int MASK_OPCR         = 0x08;

   public static final int PID_PAT     = 0x0000;
   public static final int PID_PMT     = 0x0042;
   public static final int PID_H264    = 0x0045;

   public static final int ATOM_FTYP   = 0x66747970;
   public static final int ATOM_MOOV   = 0x6D6F6F76;
   public static final int ATOM_MVHD   = 0x6d766864;
   public static final int ATOM_TRAK   = 0x7472616b;
   public static final int ATOM_TKHD   = 0x746b6864;
   public static final int ATOM_MDIA   = 0x6d646961;
   public static final int ATOM_MDHD   = 0x6d646864;
   public static final int ATOM_HDLR   = 0x68646c72;
   public static final int ATOM_MINF   = 0x6d696e66;
   public static final int ATOM_VMHD   = 0x766d6864;
   public static final int ATOM_STBL   = 0x7374626c;
   public static final int ATOM_STSD   = 0x73747364;
   public static final int ATOM_STTS   = 0x73747473;
   public static final int ATOM_STSS   = 0x73747373;
   public static final int ATOM_STCO   = 0x7374636f;
   public static final int ATOM_STSZ   = 0x7374737a;
   public static final int ATOM_STSC   = 0x73747363;
   public static final int ATOM_DINF   = 0x64696e66;
   public static final int ATOM_DREF   = 0x64726566;
   public static final int ATOM_URL    = 0x75726c20;
   public static final int ATOM_MDAT   = 0x6d646174;
   public static final int ATOM_FREE   = 0x66726565;
   public static final int ATOM_SKIP   = 0x736b6970;
   public static final int ATOM_WIDE   = 0x77696465;
   public static final int ATOM_VIDE   = 0x76696465;
   public static final int ATOM_AVC1   = 0x61766331;
   public static final int ATOM_AVCC   = 0x61766343;
   public static final int ATOM_PRFL   = 0x7072666c;

   public static final int BRAND_MP41_MAJOR  = 0x6d703431;
   public static final int BRAND_MP41_MINOR  = 0x00000000;
   public static final int BRAND_MP42_MAJOR  = 0x6d703432;
   public static final int BRAND_MP42_MINOR  = 0x00000000;
   public static final int BRAND_ISOM_MAJOR  = 0x69736f6d;
   public static final int BRAND_ISOM_MINOR  = 0x00000200;
   public static final int BRAND_AVC1_MAJOR  = 0x61766331;
   public static final int BRAND_AVC1_MINOR  = 0x00000000;

   private MP4Convert() {
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   public static void convert(InputStream input, OutputStream output) throws IOException {
      convert(Channels.newChannel(input), Channels.newChannel(output));
   }

   public static void convert(ReadableByteChannel input, WritableByteChannel output) throws IOException {
      ByteBuffer buffer = null;
      ByteBuffer frame = null;
      State state = new State();

      try {
         buffer = ByteBuffer.allocateDirect(188).order(ByteOrder.BIG_ENDIAN);
         frame = ByteBuffer.allocateDirect(1024*1024).order(ByteOrder.BIG_ENDIAN);
         buffer.flip();

         outputHeader(state, output);
         while (true) {
            int size = buffer.remaining();
            if (size < PACKET_SIZE) {
               if (!fill(buffer, input)) {
                  if (buffer.hasRemaining()) {
                     log.warn("discarding {} bytes at end of stream", buffer.remaining());
                  }

                  outputFooter(state, output);
                  return;
               }
            }

            next(packet(buffer), frame, state, output);
            buffer.position(buffer.position() + PACKET_SIZE);
         }
      } finally {

         if (buffer != null) {
            dispose(buffer);
         }

         if (frame != null) {
            dispose(frame);
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   private static void next(ByteBuffer packet, ByteBuffer frame, State state, WritableByteChannel output) throws IOException {
      int header = packet.getInt();
      if ((header & MASK_SYNC_BYTE) != SYNC_BYTE) {
         throw new IOException("invalid packet: sync byte not found (header=0x" + Integer.toHexString(header) + ")");
      }

      int pid = ((header & MASK_PACKET_IDENTIFIER) >> PACKET_IDENTIFIER_SHIFT);
      if (pid == PID_PAT || pid == PID_PMT) {
         log.trace("skipping packet identifier: {}", pid);
         return;
      }

      if ((header & MASK_PAYLOAD_UNIT_START_INDICATOR) != 0) {
         frame.flip();
         if (frame.hasRemaining()) {
            outputFrame(frame, state, output);
         }
         frame.compact();
      }

      if ((header & MASK_ADAPTION_FIELD_EXISTS) != 0) {
         adaptation(packet, state);
      }

      frame.put(packet);
   }

   private static void adaptation(ByteBuffer packet, State state) throws IOException {
      int length = packet.get() & 0xFF;
      int header = packet.get() & 0xFF;

      if ((header & MASK_PCR) != 0) {
         long pcrh = packet.getInt() & 0xFFFFFFFFL;
         long pcrl = packet.getShort() & 0xFFFFL;
         state.pcr = (pcrh << 1) | (pcrl >> 15);
         length -= 6;
      }

      packet.position(packet.position() + length - 1);
   }

   private static boolean fill(ByteBuffer buffer, ReadableByteChannel input) throws IOException {
      buffer.compact();
      while (true) {
         int read = input.read(buffer);
         int size = buffer.position();

         if (size >= PACKET_SIZE) {
            buffer.flip();
            return true;
         }

         if (read < 0) {
            buffer.flip();
            return false;
         }
      }
   }

   private static ByteBuffer packet(ByteBuffer buffer) throws IOException {
      return ((ByteBuffer)(buffer.duplicate().limit(buffer.position() + PACKET_SIZE))).slice();
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   private static ByteBuffer spspps(ByteBuffer frame, State state) throws IOException {
      int zeros = 0;
      int sps = -1;
      int pps = -1;
      int spse = -1;
      int ppse = -1;
      int spss = -1;
      int ppss = -1;
      int sync = 0;

      for (int i = frame.position(), e = frame.limit(); i < e; ++i) {
         if (sps >= 0 && pps >= 0 && spse >= 0 && ppse >= 0) {
            break;
         }

         int next = frame.get(i) & 0xFF;
         switch (next) {
         case 0:
            zeros++;
            break;

         case 1:
            sync = (zeros == 2) ? 3 :
                   (zeros >= 3) ? 4 :
                   0;
            zeros = 0;
            break;

         default:
            if (sync != 0) {
               int loc = i - sync;
               if (sps >= 0 && spse < 0) {
                  spse = loc;
               } else if (pps >= 0 && ppse < 0) {
                  ppse = loc;
               }

               if (sps < 0 && (next & 0x1F) == 0x07) {
                  sps = loc;
                  spss = sync;
               } else if (pps < 0 && (next & 0x1F) == 0x08) {
                  pps = loc;
                  ppss = sync;
               }
            }
            zeros = 0;
            sync = 0;
            break;
         }
      }

      if (!(sps >= 0 && spse > sps && pps >= 0 && ppse > pps)) {
         return frame;
      }

      if (state.sps == null) {
         int spsl = spse - sps;
         state.sps = ByteBuffer.allocate(spsl);
         state.sps.put((ByteBuffer)frame.duplicate().position(sps+spss).limit(spse));
         state.sps.flip();

         ByteBuffer spsinfoBuf = ((ByteBuffer)frame.duplicate().position(sps + spss).limit(spse));
         byte[] spsinfo = new byte[spsinfoBuf.remaining()];
         spsinfoBuf.get(spsinfo);

         state.info = H264SpsInfo.parseSpsInfo(spsinfo);
      }

      if (state.pps == null) {
         int ppsl = ppse - pps;
         state.pps = ByteBuffer.allocate(ppsl);
         state.pps.put((ByteBuffer)frame.duplicate().position(pps+ppss).limit(ppse));
         state.pps.flip();
      }

      return ((ByteBuffer)frame.duplicate().position(ppse + 4));
   }

   private static void outputHeader(State state, WritableByteChannel output) throws IOException {
      ByteBuffer ftyp = ByteBuffer.allocate(28).order(ByteOrder.BIG_ENDIAN);
      ftyp.putInt(28);
      ftyp.putInt(ATOM_FTYP);
      ftyp.putInt(BRAND_MP42_MAJOR);
      ftyp.putInt(BRAND_MP42_MINOR);
      ftyp.putInt(BRAND_MP42_MAJOR);
      ftyp.putInt(BRAND_ISOM_MAJOR);
      ftyp.putInt(BRAND_AVC1_MAJOR);
      ftyp.flip();

      ByteBuffer free = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
      free.putInt(8);
      free.putInt(ATOM_FREE);
      free.flip();

      state.fileOffset = ftyp.remaining() + free.remaining();
      output.write(ftyp);
      output.write(free);
   }

   private static void outputFrame(ByteBuffer frame, State state, WritableByteChannel output) throws IOException {
      ByteBuffer esframe = spspps(frame, state);
      log.trace("{} byte frame (pcr={}) (pes frame is {} bytes)", esframe.remaining(), state.pcr/90000.0, frame.remaining());

      ByteBuffer mdat = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN);
      mdat.putInt(12+esframe.remaining());
      mdat.putInt(ATOM_MDAT);
      mdat.putInt(esframe.remaining()); // NAL Unit Length
      mdat.flip();

      state.chunks.writeInt(state.chunkNum);
      state.chunks.writeInt(1);
      state.chunks.writeInt(1);
      state.chunkNum++;

      state.times.writeInt(1);
      state.times.writeInt((state.lastPcr < 0) ? (int)(2*90000*state.info.ticksPerTimeScale/state.info.timeScale) : (int)(state.pcr - state.lastPcr));
      state.lastPcr = state.pcr;

      state.sizes.writeInt(esframe.remaining() + 4);

      if ((esframe.get(esframe.position()) & 0x1F) == 0x05) {
         state.sync.writeInt(state.sampleNum);
      }

      state.samples.writeInt((int)(state.fileOffset + mdat.remaining() - 4));
      state.sampleNum++;

      state.fileOffset += mdat.remaining() + esframe.remaining();

      output.write(mdat);
      output.write(esframe);
      frame.position(frame.limit());
   }

   private static void outputFooter(State state, WritableByteChannel output) throws IOException {
      if (state.info == null || state.sps == null || state.pps == null) {
         throw new IOException("no sps/pps information found in mpeg-ts");
      }

      int width = state.info.width;
      int height = state.info.height;
      int tbase = 90000;
      long endpcr = state.pcr + (2*tbase*state.info.ticksPerTimeScale/state.info.timeScale);

      int avcc_size = 19 + state.sps.remaining() + state.pps.remaining();
      ByteBuffer avcc = ByteBuffer.allocate(avcc_size).order(ByteOrder.BIG_ENDIAN);
      avcc.putInt(avcc_size);             // Byte Size
      avcc.putInt(ATOM_AVCC);             // Sample Description Atom
      avcc.put((byte)1);                  // AVCC version
      avcc.put((byte)state.info.profileIdc); // AVCC profile indication
      avcc.put((byte)state.info.constraintsFlags); // AVCC profile compatibility
      avcc.put((byte)state.info.levelIdc);   // AVCC level indication
      avcc.put((byte)3);                  // AVCC length size
      avcc.put((byte)1);                  // AVCC num sps
      avcc.putShort((short)state.sps.remaining());
      avcc.put(state.sps.duplicate());
      avcc.put((byte)1);                  // AVCC num pps
      avcc.putShort((short)state.pps.remaining());
      avcc.put(state.pps.duplicate());
      avcc.flip();

      int avc1_size = 86 + avcc_size;
      ByteBuffer avc1 = ByteBuffer.allocate(86).order(ByteOrder.BIG_ENDIAN);
      avc1.putInt(avc1_size);             // Byte Size
      avc1.putInt(ATOM_AVC1);             // Sample Description Atom
      avc1.putInt(0x00000000);            // Reserved
      avc1.putShort((short)0);            // Reserved
      avc1.putShort((short)1);            // Data Reference Index
      avc1.putShort((short)0);            // Reserved
      avc1.putInt(0);                     // Reserved
      avc1.putInt(0);                     // Reserved
      avc1.putInt(0);                     // Reserved
      avc1.putShort((short)0);            // Reserved
      avc1.putShort((short)width);        // Width
      avc1.putShort((short)height);       // Height
      avc1.putInt(0x00480000);            // Horizontal Resolution: 72 dpi
      avc1.putInt(0x00480000);            // Vertical Resolution: 72 dpi
      avc1.putInt(0);                     // Reserved
      avc1.putShort((short)1);            // Frame Count
      avc1.putLong(0x0449524953000000L);  // Compressor Name
      avc1.putLong(0L);
      avc1.putLong(0L);
      avc1.putLong(0L);
      avc1.putShort((short)0x0018);       // Depth: color, no alpha
      avc1.putShort((short)0xFFFF);       // Reserved
      avc1.flip();

      state.samplesBaos.close();
      ByteBuffer samples = ByteBuffer.wrap(state.samplesBaos.toByteArray());

      state.sizesBaos.close();
      ByteBuffer sizes = ByteBuffer.wrap(state.sizesBaos.toByteArray());

      state.timesBaos.close();
      ByteBuffer times = ByteBuffer.wrap(state.timesBaos.toByteArray());

      state.chunksBaos.close();
      ByteBuffer chunks = ByteBuffer.wrap(state.chunksBaos.toByteArray());

      state.syncBaos.close();
      ByteBuffer sync = ByteBuffer.wrap(state.syncBaos.toByteArray());

      int stsc_size = 16 + chunks.remaining();
      ByteBuffer stsc = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
      stsc.putInt(stsc_size);             // Byte Size
      stsc.putInt(ATOM_STSC);             // Sample Description Atom
      stsc.putInt(0x00000000);            // Version and Flags
      stsc.putInt(chunks.remaining()/12); // Number of samples
      stsc.flip();

      int stss_size = 16 + sync.remaining();
      ByteBuffer stss = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
      stss.putInt(stss_size);             // Byte Size
      stss.putInt(ATOM_STSS);             // Sample Description Atom
      stss.putInt(0x00000000);            // Version and Flags
      stss.putInt(sync.remaining()/4);    // Number of samples
      stss.flip();

      int stts_size = 16 + times.remaining();
      ByteBuffer stts = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
      stts.putInt(stts_size);             // Byte Size
      stts.putInt(ATOM_STTS);             // Sample Description Atom
      stts.putInt(0x00000000);            // Version and Flags
      stts.putInt(times.remaining()/8);   // Number of samples
      stts.flip();

      int stsz_size = 20 + sizes.remaining();
      ByteBuffer stsz = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
      stsz.putInt(stsz_size);             // Byte Size
      stsz.putInt(ATOM_STSZ);             // Sample Description Atom
      stsz.putInt(0x00000000);            // Version and Flags
      stsz.putInt(0);                     // Default sample size
      stsz.putInt(sizes.remaining()/4);   // Number of samples
      stsz.flip();

      int stco_size = 16 + samples.remaining();
      ByteBuffer stco = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
      stco.putInt(stco_size);             // Byte Size
      stco.putInt(ATOM_STCO);             // Sample Description Atom
      stco.putInt(0x00000000);            // Version and Flags
      stco.putInt(samples.remaining()/4); // Entry Count
      stco.flip();

      int stsd_size = 16 + avc1_size;
      ByteBuffer stsd = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
      stsd.putInt(stsd_size);             // Byte Size
      stsd.putInt(ATOM_STSD);             // Sample Description Atom
      stsd.putInt(0x00000000);            // Version and Flags
      stsd.putInt(1);                     // Entry Count
      stsd.flip();

      int stbl_size = 8 + stsd_size + stts_size + stss_size + stsc_size + stsz_size + stco_size;
      ByteBuffer stbl = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
      stbl.putInt(stbl_size);             // Byte Size
      stbl.putInt(ATOM_STBL);             // Sample Table Atom
      stbl.flip();

      ByteBuffer url = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN);
      url.putInt(12);                     // Byte Size
      url.putInt(ATOM_URL);               // Data Reference Atom
      url.putInt(0x00000001);             // Version and Flags: same file
      url.flip();

      int dref_size = 16 + url.remaining();
      ByteBuffer dref = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN);
      dref.putInt(dref_size);             // Byte Size
      dref.putInt(ATOM_DREF);             // Data Reference Atom
      dref.putInt(0x00000000);            // Version and Flags
      dref.putInt(0x00000001);            // Entry Count
      dref.flip();

      int dinf_size = 8 + dref_size;
      ByteBuffer dinf = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
      dinf.putInt(dinf_size);             // Byte Size
      dinf.putInt(ATOM_DINF);             // Data Information Atom
      dinf.flip();

      ByteBuffer vmhd = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
      vmhd.putInt(20);                    // Byte Size
      vmhd.putInt(ATOM_VMHD);             // Video Media Header Atom
      vmhd.putInt(0x00000001);            // Version and Flags: version=0, flags=1
      vmhd.putInt(0x00000000);
      vmhd.putInt(0x00000000);
      vmhd.flip();

      int minf_size = 8 + vmhd.remaining() + dinf_size + stbl_size;
      ByteBuffer minf = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
      minf.putInt(minf_size);             // Byte Size
      minf.putInt(ATOM_MINF);             // Media Information Atom
      minf.flip();

      ByteBuffer hdlr = ByteBuffer.allocate(45).order(ByteOrder.BIG_ENDIAN);
      hdlr.putInt(45);                    // Byte Size
      hdlr.putInt(ATOM_HDLR);             // Handler Reference Atom
      hdlr.putInt(0x00000000);            // Version and Flags: version=0
      hdlr.putInt(0);                     // Predefined
      hdlr.putInt(ATOM_VIDE);             // Handler Type
      hdlr.putInt(0);                     // Reserved
      hdlr.putInt(0);                     // Reserved
      hdlr.putInt(0);                     // Reserved
      hdlr.putInt(0x00000000);            // Name
      hdlr.putInt(0x00000000);
      hdlr.putInt(0x00000000);
      hdlr.put((byte)0);
      hdlr.flip();

      ByteBuffer mdhd = ByteBuffer.allocate(32).order(ByteOrder.BIG_ENDIAN);
      mdhd.putInt(32);                    // Byte Size
      mdhd.putInt(ATOM_MDHD);             // Media Header Atom
      mdhd.putInt(0x00000000);            // Version and Flags: version=1
      mdhd.putInt(0);                     // Creation Time
      mdhd.putInt(0);                     // Modification Time
      mdhd.putInt(tbase);                 // Time Scale is 90 KHz for H.264
      mdhd.putInt((int)endpcr);           // Duration in time scale units
      mdhd.putShort((short)0x55C4);       // Language
      mdhd.putShort((short)0x0000);       // Reserved
      mdhd.flip();

      int mdia_size = 8 + mdhd.remaining() + hdlr.remaining() + minf_size;
      ByteBuffer mdia = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
      mdia.putInt(mdia_size);
      mdia.putInt(ATOM_MDIA);
      mdia.flip();

      ByteBuffer tkhd = ByteBuffer.allocate(92).order(ByteOrder.BIG_ENDIAN);
      tkhd.putInt(92);                    // Byte Size
      tkhd.putInt(ATOM_TKHD);             // Track Header Atom
      tkhd.putInt(0x00000007);            // Version and Flags: enabled, movie, preview
      tkhd.putInt(0);                     // Creation Time
      tkhd.putInt(0);                     // Modification Time
      tkhd.putInt(0x01);                  // Track ID 1
      tkhd.putInt(0);                     // Reserved
      tkhd.putInt((int)endpcr);           // Duration in time scale units
      tkhd.putLong(0x0000000000000000L);  // Reserved
      tkhd.putInt(0);                     // Layer and Group: normal layer, no group
      tkhd.putInt(0);                     // Volume: no volume (video only)
      tkhd.putInt(0x00010000);            // Predefined transformation matrix
      tkhd.putInt(0x00000000);
      tkhd.putInt(0x00000000);
      tkhd.putInt(0x00000000);
      tkhd.putInt(0x00010000);
      tkhd.putInt(0x00000000);
      tkhd.putInt(0x00000000);
      tkhd.putInt(0x00000000);
      tkhd.putInt(0x40000000);
      tkhd.putInt(width<<16);             // Presentation width
      tkhd.putInt(height<<16);            // Presentation height
      tkhd.flip();

      int trak_size = 8 + tkhd.remaining() + mdia_size;
      ByteBuffer trak = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
      trak.putInt(trak_size);
      trak.putInt(ATOM_TRAK);
      trak.flip();

      ByteBuffer mvhd = ByteBuffer.allocate(108).order(ByteOrder.BIG_ENDIAN);
      mvhd.putInt(108);                   // Byte Size
      mvhd.putInt(ATOM_MVHD);             // Movie Header ATOM
      mvhd.putInt(0x00000000);            // Version and Flags
      mvhd.putInt(0);                     // Creation Time
      mvhd.putInt(0);                     // Modification Time
      mvhd.putInt(tbase);                 // Time Scale is 90 KHz for H.264
      mvhd.putInt((int)endpcr);           // Duration in time scale units
      mvhd.putInt(0x00010000);            // Rate: normal
      mvhd.putShort((short)0x0100);       // Volume: normal
      mvhd.putShort((short)0);            // Reserved
      mvhd.putLong(0);                    // Reserved
      mvhd.putInt(0x00010000);            // Predefined transformation matrix
      mvhd.putInt(0x00000000);
      mvhd.putInt(0x00000000);
      mvhd.putInt(0x00000000);
      mvhd.putInt(0x00010000);
      mvhd.putInt(0x00000000);
      mvhd.putInt(0x00000000);
      mvhd.putInt(0x00000000);
      mvhd.putInt(0x40000000);
      mvhd.putInt(0x00000000);            // Predefined values
      mvhd.putInt(0x00000000);
      mvhd.putInt(0x00000000);
      mvhd.putInt(0x00000000);
      mvhd.putInt(0x00000000);
      mvhd.putInt(0x00000000);
      mvhd.putInt(0x00000002);            // Next Track ID: 2
      mvhd.flip();

      int moov_size = 8 + mvhd.remaining() + trak_size;
      ByteBuffer moov = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
      moov.putInt(moov_size);
      moov.putInt(ATOM_MOOV);
      moov.flip();

      output.write(moov);
      output.write(mvhd);
      output.write(trak);
      output.write(tkhd);
      output.write(mdia);
      output.write(mdhd);
      output.write(hdlr);
      output.write(minf);
      output.write(vmhd);
      output.write(dinf);
      output.write(dref);
      output.write(url);
      output.write(stbl);
      output.write(stsd);
      output.write(avc1);
      output.write(avcc);
      output.write(stts);
      output.write(times);
      output.write(stss);
      output.write(sync);
      output.write(stsc);
      output.write(chunks);
      output.write(stsz);
      output.write(sizes);
      output.write(stco);
      output.write(samples);
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   private static String hex(ByteBuffer buffer) {
      StringBuilder bld = new StringBuilder();

      int pos = buffer.position();
      while (pos < buffer.limit()) {
         if (pos != buffer.position()) {
            bld.append(',');
         }

         int next = buffer.get(pos) & 0xFF;
         if (next < 16) {
            bld.append('0');
         }

         bld.append(Integer.toHexString(next));
         pos++;
      }

      return bld.toString();
   }

   private static void dispose(Buffer buf) {
      if (!buf.isDirect()) {
         return;
      }

      try {
         Buffer buffer = buf;
         Class<?> bufferClass = buffer.getClass();
         if(!"java.nio.DirectByteBuffer".equals(bufferClass.getName())) {
            Field attField = bufferClass.getDeclaredField("att");
            attField.setAccessible(true);

            buffer = (Buffer)attField.get(buffer);
            bufferClass = buffer.getClass();
         }

         Method cleanerMethod = bufferClass.getMethod("cleaner");
         cleanerMethod.setAccessible(true);

         Object cleaner = cleanerMethod.invoke(buffer);
         Method cleanMethod = cleaner.getClass().getMethod("clean");

         cleanMethod.setAccessible(true);
         cleanMethod.invoke(cleaner);
      } catch(Exception e) {
         throw new RuntimeException("could not dispose of direct buffer", e);
      }
   }

   public static void main(String[] args) throws IOException {
      try (InputStream input = new BufferedInputStream(new FileInputStream(args[0]));
           OutputStream output = new BufferedOutputStream(new FileOutputStream(args[1]))) {
         convert(input, output);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   private static final class State {
      long pcr;
      ByteBuffer sps = null;
      ByteBuffer pps = null;
      H264SpsInfo.Info info = null;

      long fileOffset;
      int sampleNum = 1;
      ByteArrayOutputStream samplesBaos = new ByteArrayOutputStream();
      DataOutput samples = new DataOutputStream(samplesBaos);

      ByteArrayOutputStream sizesBaos = new ByteArrayOutputStream();
      DataOutput sizes = new DataOutputStream(sizesBaos);

      long lastPcr = -1;
      ByteArrayOutputStream timesBaos = new ByteArrayOutputStream();
      DataOutput times = new DataOutputStream(timesBaos);

      int chunkNum = 1;
      ByteArrayOutputStream chunksBaos = new ByteArrayOutputStream();
      DataOutput chunks = new DataOutputStream(chunksBaos);

      ByteArrayOutputStream syncBaos = new ByteArrayOutputStream();
      DataOutput sync = new DataOutputStream(syncBaos);
   }
}

