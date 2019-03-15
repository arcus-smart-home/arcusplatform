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

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.iris.platform.PagedQuery;

public class VideoQuery extends PagedQuery {

   private UUID placeId;
   private boolean listDeleted = false;
   private boolean listInProgress = true;
   private VideoType recordingType = VideoType.ANY;
   private Date earliest;
   private Date latest;
   private Set<String> cameras = ImmutableSet.of();
   private Set<String> tags = ImmutableSet.of();
   
   public VideoType getRecordingType() {
      return recordingType;
   }
    
   public void setRecordingType(@Nullable VideoType type) {
      this.recordingType = type == null ? VideoType.RECORDING : type;
   }
    
   public void setRecordingType(@Nullable String type) {
      if(StringUtils.isEmpty(type)) {
         this.recordingType = VideoType.ANY;
      }
      else {
         this.recordingType = VideoType.valueOf(type);
      }
   }
    
   public boolean isListDeleted() {
      return listDeleted;
   }

   public void setListDeleted(@Nullable Boolean listDeleted) {
      this.listDeleted = listDeleted == null ? false : listDeleted;
   }

   public boolean isListInProgress() {
      return listInProgress;
   }

   public void setListInProgress(boolean listInProgress) {
      this.listInProgress = listInProgress;
   }

   public @Nullable Date getEarliest() {
      return earliest;
   }

   public void setEarliest(@Nullable Date earliest) {
      this.earliest = earliest;
   }

   public @Nullable Date getLatest() {
      return latest;
   }

   public void setLatest(@Nullable Date latest) {
      this.latest = latest;
   }

   public Set<String> getCameras() {
      return cameras;
   }

   public void setCameras(@Nullable Set<String> cameras) {
      this.cameras = cameras == null ? ImmutableSet.of() : ImmutableSet.copyOf(cameras);
   }

   public Set<String> getTags() {
      return tags;
   }

   public void setTags(@Nullable Set<String> tags) {
      this.tags = tags == null ? ImmutableSet.of() : ImmutableSet.copyOf(tags);
   }

   public UUID getPlaceId() {
      return placeId;
   }

   public void setPlaceId(UUID placeId) {
      this.placeId = placeId;
   }

   @Override
   public void setToken(String token) {
      super.setToken(token);
   }

   @Override
   public String toString() {
      return "VideoQuery [placeId=" + placeId + ", listDeleted=" + listDeleted + ", listInProgress=" + listInProgress
            + ", recordingType=" + recordingType + ", earliest=" + earliest + ", latest=" + latest + ", cameras="
            + cameras + ", tags=" + tags + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((cameras == null) ? 0 : cameras.hashCode());
      result = prime * result + ((earliest == null) ? 0 : earliest.hashCode());
      result = prime * result + ((latest == null) ? 0 : latest.hashCode());
      result = prime * result + (listDeleted ? 1231 : 1237);
      result = prime * result + (listInProgress ? 1231 : 1237);
      result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
      result = prime * result + ((recordingType == null) ? 0 : recordingType.hashCode());
      result = prime * result + ((tags == null) ? 0 : tags.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (!super.equals(obj))
         return false;
      if (getClass() != obj.getClass())
         return false;
      VideoQuery other = (VideoQuery) obj;
      if (cameras == null) {
         if (other.cameras != null)
            return false;
      } else if (!cameras.equals(other.cameras))
         return false;
      if (earliest == null) {
         if (other.earliest != null)
            return false;
      } else if (!earliest.equals(other.earliest))
         return false;
      if (latest == null) {
         if (other.latest != null)
            return false;
      } else if (!latest.equals(other.latest))
         return false;
      if (listDeleted != other.listDeleted)
         return false;
      if (listInProgress != other.listInProgress)
         return false;
      if (placeId == null) {
         if (other.placeId != null)
            return false;
      } else if (!placeId.equals(other.placeId))
         return false;
      if (recordingType != other.recordingType)
         return false;
      if (tags == null) {
         if (other.tags != null)
            return false;
      } else if (!tags.equals(other.tags))
         return false;
      return true;
   }
   
}

