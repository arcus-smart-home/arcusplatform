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

import org.apache.commons.lang.StringUtils;

public enum VideoCodec {
   H264_BASELINE_3_0("42001e", "H.264 Baseline Profile level 3.0"),
   H264_BASELINE_3_1("42001f", "H.264 Baseline Profile level 3.1"),
   H264_MAIN_3_0("4d001e", "H.264 Main Profile level 3.0"),
   H264_MAIN_3_1("4d001f", "H.264 Main Profile level 3.1"),
   H264_MAIN_4_0("4d0028", "H.264 Main Profile level 4.0"),
   H264_MAIN_4_1("4d0029", "H.264 Main Profile level 4.1"),
   H264_HIGH_3_1("64001f", "H.264 High Profile level 3.1"),
   H264_HIGH_4_0("640028", "H.264 High Profile level 4.0"),
   H264_HIGH_4_1("640029", "H.264 High Profile level 4.1");

   private final String profileId;
   private final String playlistCodec;
   private final String displayString;

   VideoCodec(String profileId, String displayString) {
      this.profileId = profileId;
      this.playlistCodec = "avc1." + profileId.toLowerCase();
      this.displayString = displayString;
   }

   public static VideoCodec fromSdpProfile(String profile) {
      for(VideoCodec codec : VideoCodec.values()) {
         if(StringUtils.equalsIgnoreCase(codec.profileId, profile)) {
            return codec;
         }
      }
      return H264_BASELINE_3_1;
   }

   public String playlistCodec() {
      return playlistCodec;
   }

   public String displayString() {
      return displayString;
   }
}

