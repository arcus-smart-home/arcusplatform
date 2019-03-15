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

import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H264SpsInfo {
   private static final Logger log = LoggerFactory.getLogger(H264SpsInfo.class);

   private H264SpsInfo() {
   }

   private static ByteBuffer decodenal(ByteBuffer input) throws IOException {
      int zeros = 0;
      ByteBuffer result = ByteBuffer.allocate(input.remaining());
      for (int i = input.position(), e = input.limit(); i < e; ++i) {
         int next = input.get(i) & 0xFF;
         if (zeros == 2 && next == 3) {
            zeros = 0;
            continue;
         }

         zeros = (next == 0) ? (zeros + 1) : 0;
         result.put((byte)next);
      }

      result.flip();
      return result;
   }

   private static byte[] decodenal(byte[] sps) throws IOException {
      return decodenal(ByteBuffer.wrap(sps)).array();
   }

   public static Info parseSpsInfo(byte[] spsNal) throws IOException {
      byte[] sps = decodenal(spsNal);
      H264SpsDecoder decoder = new H264SpsDecoder(sps);
      Info info = new Info();

      int nalu = decoder.u8();
      log.trace("sps nalu: {}", nalu);

      int profileIdc = decoder.u8();
      log.trace("sps profile idc: {}", profileIdc);
      info.profileIdc = profileIdc;

      int constraintFlags = decoder.u8();
      log.trace("sps constraints: {}", constraintFlags);
      info.constraintsFlags = constraintFlags;

      int levelIdc = decoder.u8();
      log.trace("sps levelIdc: {}", levelIdc);
      info.levelIdc = levelIdc;

      int spsId = decoder.ue();
      log.trace("sps id: {}", spsId);
      info.spsId = spsId;

      if (profileIdc == 44 || profileIdc == 83 || profileIdc == 86 || profileIdc == 100 ||
          profileIdc == 110 || profileIdc == 118 || profileIdc == 122 || profileIdc == 244) {
         int chromaFormatIdc = decoder.ue();
         log.trace("sps chroma format idc: {}", chromaFormatIdc);

         if (chromaFormatIdc == 3) {
            int separateColorPlane = decoder.u1();
            log.trace("sps separate color plane: {}", separateColorPlane);
         }

         int bitDepthLuma = decoder.ue();
         log.trace("sps luma bit depth: {}", bitDepthLuma);

         int bitDepthChroma = decoder.ue();
         log.trace("sps chroma bit depth: {}", bitDepthChroma);

         int qpPrimeTransformation = decoder.u1();
         log.trace("sps qp prime transformation: {}", qpPrimeTransformation);

         int seqScalingMatrix = decoder.u1();
         log.trace("sps scaling matrix: {}", seqScalingMatrix);

         if (seqScalingMatrix != 0) {
            int len = (chromaFormatIdc != 3) ? 8 : 12;
            for (int i = 0; i < len; ++i) {
               int seqScalingListFlag = decoder.u1();
               if (seqScalingListFlag != 0) {
                  int listSize;
                  if (i < 6) {
                     listSize = 16;
                  } else {
                     listSize = 64;
                  }

                  int lastScale = 8;
                  int nextScale = 8;
                  for (int j = 0; j < listSize; ++j) {
                     if (nextScale != 0) {
                        int deltaScale = decoder.se();
                        nextScale = (lastScale + deltaScale + 256) % 256;
                     }

                     int slj = (nextScale == 0) ? lastScale : nextScale;
                     lastScale = slj;
                  }
               }
            }
         }
      }

      int log2MaxFrameNum = decoder.ue();
      log.trace("sps log max frame num: {}", log2MaxFrameNum);

      int picOrderCntType = decoder.ue();
      log.trace("sps pic order cnt type: {}", picOrderCntType);

      if (picOrderCntType == 0) {
         int log2MaxPicOrderCnt = decoder.ue();
         log.trace("sps log max pic order cnt: {}", log2MaxPicOrderCnt);
      } else if (picOrderCntType == 1) {
         int deltaPicOrderAlwaysZero = decoder.u1();
         log.trace("sps delta pic order always zero: {}", deltaPicOrderAlwaysZero);

         int offsetForNonRefPic = decoder.se();
         log.trace("sps offset for non-ref pic: {}", offsetForNonRefPic);

         int offsetForTopToBottom = decoder.se();
         log.trace("sps offset for top to bottom: {}", offsetForTopToBottom);

         int numRefFramesInPicOrder = decoder.ue();
         log.trace("sps num ref frames in pic order: {}", numRefFramesInPicOrder);

         for (int i = 0; i < numRefFramesInPicOrder; ++i) {
            decoder.se();
         }
      }

      int maxNumRefFrames = decoder.ue();
      log.trace("sps max num ref frames: {}", maxNumRefFrames);

      int gapsInFrameNumAllowed = decoder.u1();
      log.trace("sps gaps in frame num: {}", gapsInFrameNumAllowed);

      int picWidth = decoder.ue();
      log.trace("sps pic width: {}", picWidth);

      int picHeight = decoder.ue();
      log.trace("sps pic height: {}", picHeight);

      int frameMbsOnly = decoder.u1();
      log.trace("sps frame mbs only: {}", frameMbsOnly);

      if (frameMbsOnly == 0) {
         int mbAdaptiveFrame = decoder.u1();
         log.trace("sps mb adaptive frame: {}", mbAdaptiveFrame);
      }

      int directInference = decoder.u1();
      log.trace("sps direct inference: {}", directInference);

      int frameCropping = decoder.u1();
      log.trace("sps frame cropping: {}", frameCropping);

      int frameCropLeft = 0;
      int frameCropRight = 0;
      int frameCropTop = 0;
      int frameCropBottom = 0;
      if (frameCropping != 0) {
         frameCropLeft = decoder.ue();
         frameCropRight = decoder.ue();
         frameCropTop = decoder.ue();
         frameCropBottom = decoder.ue();
      }

      log.trace("sps frame cropping: left={}, right={}, top={}, bottom={}", frameCropLeft, frameCropRight, frameCropTop, frameCropBottom);

      int vuiPresent = decoder.u1();
      log.trace("sps vui present: {}", vuiPresent);
      if (vuiPresent != 0) {
         int aspectRatioPresent = decoder.u1();
         log.trace("aspect ratio present: {}", aspectRatioPresent);

         if (aspectRatioPresent != 0) {
            int aspectRatioIdc = decoder.u8();
            log.trace("aspect ratio idc: {}", aspectRatioIdc);

            if (aspectRatioIdc == 255) {
               int sarWidth = decoder.u16();
               int sarHeight = decoder.u16();
               log.trace("sar width: {}", sarWidth);
               log.trace("sar height: {}", sarHeight);
            }
         }

         int overScanPresent = decoder.u1();
         log.trace("overscan info present: {}", overScanPresent);

         if (overScanPresent != 0) {
            int overscanAppropriate = decoder.u1();
            log.trace("overscan appropriate flag: {}", overscanAppropriate);
         }

         int videoSignalPresent = decoder.u1();
         log.trace("video signal type present: {}", videoSignalPresent);

         if (videoSignalPresent != 0) {
            int videoFormat = decoder.u3();
            log.trace("video format: {}", videoFormat);

            int videoFullRange = decoder.u1();
            log.trace("video full range: {}", videoFullRange);

            int colorDescPresent = decoder.u1();
            log.trace("color description present: {}", colorDescPresent);

            if (colorDescPresent != 0) {
               int colorPrimaries = decoder.u8();
               int transferChars = decoder.u8();
               int matrixCoeff = decoder.u8();

               log.trace("color primaries: {}", colorPrimaries);
               log.trace("transfer characteristics: {}", transferChars);
               log.trace("matrix coefficients: {}", matrixCoeff);
            }
         }

         int chromaLocInfoPresent = decoder.u1();
         log.trace("chroma loc info present: " + chromaLocInfoPresent);

         if (chromaLocInfoPresent != 0) {
            int chromaSampleLocTop = decoder.ue();
            log.trace("chroma sample loc type top: {}", chromaSampleLocTop);

            int chromaSampleLocBot = decoder.ue();
            log.trace("chroma sample loc type bot: {}", chromaSampleLocBot);
         }

         int timingInfoPresent = decoder.u1();
         log.trace("timing info present: {}", timingInfoPresent);

         if (timingInfoPresent != 0) {
            int numUnitInTicks = decoder.u32();
            log.trace("num units in ticks: {}", numUnitInTicks);
            int timeScale = decoder.u32();
            log.trace("time scale: {}", timeScale);
            int fixedFrameRate = decoder.u1();
            log.trace("fixed frame rate: {}", fixedFrameRate);

            info.ticksPerTimeScale = numUnitInTicks;
            info.timeScale = timeScale;
         }

         int nalHrdPresent = decoder.u1();
         log.trace("nal hrd parameters present: {}", nalHrdPresent);

         if (nalHrdPresent != 0) {
            int cpbCnt = decoder.ue();
            log.trace("cpb cnt: {}", cpbCnt);

            int bitrateScale = decoder.u4();
            log.trace("bit rate scale: {}", bitrateScale);

            int cpbSize = decoder.u4();
            log.trace("cpb size scale: {}", cpbSize);

            for (int i = 0; i <= cpbCnt; ++i) {
               decoder.ue();
               decoder.ue();
               decoder.u1();
            }

            int initialCpb = decoder.u5();
            log.trace("initial cpb removal delay: {}", initialCpb);

            int cpbRemoval = decoder.u5();
            log.trace("cpb removal delay: {}", cpbRemoval);

            int dpbOutput = decoder.u5();
            log.trace("dpb output delay: {}", dpbOutput);

            int timeOffset = decoder.u5();
            log.trace("time offset length: {}", timeOffset);
         }

         int vclHrdPresent = decoder.u1();
         log.trace("vlc hrd parameters present: {}", vclHrdPresent);

         if (vclHrdPresent != 0) {
            int cpbCnt = decoder.ue();
            log.trace("cpb cnt: {}", cpbCnt);

            int bitrateScale = decoder.u4();
            log.trace("bit rate scale: {}", bitrateScale);

            int cpbSize = decoder.u4();
            log.trace("cpb size scale: {}", cpbSize);

            for (int i = 0; i < cpbCnt; ++i) {
               decoder.ue();
               decoder.ue();
               decoder.u1();
            }

            int initialCpb = decoder.u5();
            log.trace("initial cpb removal delay: {}", initialCpb);

            int cpbRemoval = decoder.u5();
            log.trace("cpb removal delay: {}", cpbRemoval);

            int dpbOutput = decoder.u5();
            log.trace("dpb output delay: {}", dpbOutput);

            int timeOffset = decoder.u5();
            log.trace("time offset length: {}", timeOffset);
         }

         if (nalHrdPresent != 0 || vclHrdPresent != 0) {
            int lowDelay = decoder.u1();
            log.trace("low delay hrd: {}", lowDelay);
         }

         int picStructPresent = decoder.u1();
         log.trace("pic struct present: {}", picStructPresent);

         int bitRestrict = decoder.u1();
         log.trace("bitstream restriction: {}", bitRestrict);

         if (bitRestrict != 0) {
            int motionVectors = decoder.u1();
            log.trace("motion vectors over pic: {}", motionVectors);

            int maxBytesPerPicDenom = decoder.ue();
            log.trace("max bytes per pic denom: {}", maxBytesPerPicDenom);

            int maxBitsPerMbDenom = decoder.ue();
            log.trace("max bits per mb denom: {}", maxBitsPerMbDenom);

            int log2MaxMvLengthHoriz = decoder.ue();
            log.trace("log2 max mv length horizontal: {}", log2MaxMvLengthHoriz);

            int log2MaxMvLengthVert = decoder.ue();
            log.trace("log2 max mv length vertical: {}", log2MaxMvLengthVert);

            int maxNumReorder = decoder.ue();
            log.trace("max num reorder frames: {}", maxNumReorder);

            int maxDecFrameBuf = decoder.ue();
            log.trace("max dec frame buffering: {}", maxDecFrameBuf);
         }
      }

      int stopBit = decoder.u1();
      log.trace("sps stopBit: {}", stopBit);

      info.width = ((picWidth + 1) * 16) - frameCropRight*2 - frameCropLeft*2;
      info.height = ((2 - frameMbsOnly) * (picHeight + 1) * 16) - (frameCropTop*2) - (frameCropBottom*2);

      return info;
   }

   public static final class Info {
      int width;
      int height;
      int profileIdc;
      int levelIdc;
      int constraintsFlags;
      int spsId;

      int ticksPerTimeScale;
      int timeScale;

      public int getWidth() {
         return width;
      }

      public int getHeight() {
         return height;
      }

      public int getProfileIdc() {
         return profileIdc;
      }

      public int getLevelIdc() {
         return levelIdc;
      }

      public int getConstraintsFlags() {
         return constraintsFlags;
      }

      public int getSpsId() {
         return spsId;
      }

      public int getTicksPerTimeScale() {
         return ticksPerTimeScale;
      }

      public int getTimeScale() {
         return timeScale;
      }
   }
}

