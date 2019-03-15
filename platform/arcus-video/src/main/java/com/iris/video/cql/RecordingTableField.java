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
package com.iris.video.cql;


public enum RecordingTableField {
      // headers -- stored at TS -inf
      STORAGE(0L),
      CAMERA(1L),
      PLACE(2L),
      PERSON(3L),
      ACCOUNT(4L),
      EXPIRATION(5L),
      
      WIDTH(100L),
      HEIGHT(101L),
      BANDWIDTH(102L),
      FRAMERATE(103L),
		VIDEO_CODEC(104L),
		AUDIO_CODEC(105L),


      // trailers -- stored at TS +inf
      DURATION(200L),
      SIZE(201L)
      ;
   
      private final long bo;
      RecordingTableField(long bo) {
         this.bo = bo;
      }
      
      public long bo() {
         return bo;
      }
      
      public double ts() {
         if(this == DURATION || this == RecordingTableField.SIZE) {
            return VideoConstants.REC_TS_END;
         }
         else {
            return VideoConstants.REC_TS_START;
         }
      }
}

