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
package com.iris.video;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableList;

public enum AudioCodec {
   NONE("", "None", ImmutableList.of(), ""),
   AAC_LC("mp4a.40.2", "AAC-LC", ImmutableList.of("MPEG4-GENERIC", "MP4A-LATM"), "AAC-hbr"),
   MP3("mp4a.40.34", "MP3", ImmutableList.of("MPA"), ""),
   G711("", "", ImmutableList.of("PCMU"), "");

   private final String playlistCodec;
   private final String displayString;
   private final List<String> sdpEncNames;
   private final String sdpMode;

   AudioCodec(String playlistCodec, String displayString, List<String> sdpEncNames, String sdpMode) {
      this.playlistCodec = playlistCodec;
      this.displayString = displayString;
      this.sdpEncNames = sdpEncNames;
      this.sdpMode = sdpMode;
   }

   public static AudioCodec fromSdp(String encName, String mode) {
      final String enc = encName == null ? "" : encName;
      mode = mode == null ? "" : mode;

      for(AudioCodec codec : AudioCodec.values()) {
         if(StringUtils.equalsIgnoreCase(codec.sdpMode, mode) &&
            codec.sdpEncNames.stream().anyMatch((s) -> StringUtils.equalsIgnoreCase(s, enc))) {

            // we transcode everyting to aac so if the sdp is indicating audio but not aac then return aac because
            // its been transcoded and this must be the audio codec that the streaming server will use
            if(codec != AAC_LC) {
               return AAC_LC;
            }

            return codec;
         }
      }
      return NONE;
   }

   public String playlistCodec() {
      return playlistCodec;
   }

   public String displayString() {
      return displayString;
   }

   public List<String> sdpEncNames() {
      return sdpEncNames;
   }

   public String sdpMode() {
      return sdpMode;
   }
}

