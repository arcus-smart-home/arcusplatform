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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.util.IrisUUID;
import com.iris.video.cql.VideoConstants;

public class VideoMetadata {

   private UUID accountId;
   private UUID placeId;
   private UUID cameraId;
   private UUID recordingId;
   private UUID personId;
   private String name;
   private String loc;
   private int width;
   private int height;
   private int bandwidth;
   private double framerate;
   private double precapture;
   private boolean stream;
   private Set<String> tags;
   private double duration;
   private long size;
   private Date deletionTime;
   private int deletionPartition =  VideoConstants.DELETION_PARTITION_UNKNOWN;
   private VideoCodec videoCodec = VideoCodec.H264_BASELINE_3_1;
   private AudioCodec audioCodec = AudioCodec.NONE;
   private long expiration;
   private boolean isDeleted = false;
   // TODO images?

   public UUID getAccountId() {
      return accountId;
   }

   public void setAccountId(UUID accountId) {
      this.accountId = accountId;
   }

   public UUID getPlaceId() {
      return placeId;
   }

   public void setPlaceId(UUID placeId) {
      this.placeId = placeId;
   }

   public UUID getCameraId() {
      return cameraId;
   }

   public void setCameraId(UUID cameraId) {
      this.cameraId = cameraId;
   }

   public UUID getRecordingId() {
      return recordingId;
   }

   public void setRecordingId(UUID recordingId) {
      this.recordingId = recordingId;
   }

   public UUID getPersonId() {
      return personId;
   }

   public void setPersonId(UUID personId) {
      this.personId = personId;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getLoc() {
      return loc;
   }

   public void setLoc(String loc) {
      this.loc = loc;
   }

   public int getWidth() {
      return width;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public int getHeight() {
      return height;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public int getBandwidth() {
      return bandwidth;
   }

   public void setBandwidth(int bandwidth) {
      this.bandwidth = bandwidth;
   }

   public double getFramerate() {
      return framerate;
   }

   public void setFramerate(double framerate) {
      this.framerate = framerate;
   }

   public double getPrecapture() {
      return precapture;
   }

   public void setPrecapture(double precapture) {
      this.precapture = precapture;
   }

   public boolean isStream() {
      return stream;
   }

   public void setStream(boolean stream) {
      this.stream = stream;
   }

   public void addTag(String tag) {
      if(tags == null) {
         tags = new HashSet<>(4);
      }
      tags.add(tag);
   }

   public Set<String> getTags() {
      return tags == null ? ImmutableSet.of() : tags;
   }

   public void setTags(Set<String> tags) {
      this.tags = tags;
   }

   public boolean isDeleted() {
      return isDeleted;
   }


   public Date getDeletionTime() {
      return deletionTime;
   }

   public void setDeletionTime(Date deletionTime) {
      this.deletionTime = deletionTime;      
   }

   public int getDeletionPartition() {
      return deletionPartition;
   }

   public void setDeletionPartition(int deletionPartition) {
      this.deletionPartition = deletionPartition;
   }

   public boolean isInProgress() {
      return duration <= 0.0;
   }

   public double getDuration() {
      return duration;
   }

   public void setDuration(double duration) {
      this.duration = duration;
   }

   public long getSize() {
      return size;
   }

   public void setSize(long size) {
      this.size = size;
   }

   public VideoCodec getVideoCodec() {
      return videoCodec;
   }

   public void setVideoCodec(VideoCodec videoCodec) {
      this.videoCodec = videoCodec;
   }

   public AudioCodec getAudioCodec() {
      return audioCodec;
   }

   public void setAudioCodec(AudioCodec audioCodec) {
      this.audioCodec = audioCodec;
   }

   public Map<String, Object> toMap() {
      Map<String, Object> out = new HashMap<>(32);

      UUID recId = getRecordingId();
      out.put(Capability.ATTR_ID, recId);
      out.put(Capability.ATTR_ADDRESS, Address.platformService(recId, RecordingCapability.NAMESPACE).getRepresentation());
      out.put(Capability.ATTR_TYPE, RecordingCapability.NAMESPACE);
      out.put(Capability.ATTR_CAPS, ImmutableSet.of(Capability.NAMESPACE, RecordingCapability.NAMESPACE));
      out.put(Capability.ATTR_TAGS, getTags());
      out.put(Capability.ATTR_IMAGES, Collections.emptyMap());
      out.put(Capability.ATTR_INSTANCES, Collections.emptyMap());

      out.put(RecordingCapability.ATTR_TYPE, isStream() ? RecordingCapability.TYPE_STREAM : RecordingCapability.TYPE_RECORDING);
      out.put(RecordingCapability.ATTR_DELETED, isDeleted());
      out.put(RecordingCapability.ATTR_DELETETIME, getDeletionTime() != null? getDeletionTime(): VideoConstants.DELETE_TIME_SENTINEL);
      out.put(RecordingCapability.ATTR_TIMESTAMP, IrisUUID.timeof(recId));
      out.put(RecordingCapability.ATTR_PLACEID, getPlaceId());

      out.put(RecordingCapability.ATTR_ACCOUNTID, getAccountId());
      out.put(RecordingCapability.ATTR_BANDWIDTH, getBandwidth());
      out.put(RecordingCapability.ATTR_CAMERAID, getCameraId());
      out.put(RecordingCapability.ATTR_DURATION, getDuration());
      out.put(RecordingCapability.ATTR_FRAMERATE, getFramerate());
      out.put(RecordingCapability.ATTR_HEIGHT, getHeight());
      out.put(RecordingCapability.ATTR_NAME, getName());
      out.put(RecordingCapability.ATTR_PERSONID, getPersonId());
      out.put(RecordingCapability.ATTR_PRECAPTURE, getPrecapture());
      out.put(RecordingCapability.ATTR_SIZE, getSize());
      out.put(RecordingCapability.ATTR_WIDTH, getWidth());
      out.put(RecordingCapability.ATTR_COMPLETED, !isInProgress());

      if(videoCodec != null) {
         out.put(RecordingCapability.ATTR_VIDEOCODEC, videoCodec.displayString());
      }
      if(audioCodec != null) {
         out.put(RecordingCapability.ATTR_AUDIOCODEC, audioCodec.displayString());
      }
      return out;
   }

   @Override
   public String toString() {
      return "VideoMetadata{" +
         "accountId=" + accountId +
         ", placeId=" + placeId +
         ", cameraId=" + cameraId +
         ", recordingId=" + recordingId +
         ", personId=" + personId +
         ", name='" + name + '\'' +
         ", loc='" + loc + '\'' +
         ", width=" + width +
         ", height=" + height +
         ", bandwidth=" + bandwidth +
         ", framerate=" + framerate +
         ", precapture=" + precapture +
         ", stream=" + stream +
         ", tags=" + tags +
         ", duration=" + duration +
         ", size=" + size +
         ", deletionTime=" + deletionTime +
         ", deletionPartition=" + deletionPartition +
         ", videoCodec=" + videoCodec +
         ", audioCodec=" + audioCodec +
         ", expiration=" + expiration +
         '}';
   }

   @Override
   public boolean equals(Object o) {
      if(this == o) return true;
      if(o == null || getClass() != o.getClass()) return false;
      VideoMetadata that = (VideoMetadata) o;
      return width == that.width &&
         height == that.height &&
         bandwidth == that.bandwidth &&
         Double.compare(that.framerate, framerate) == 0 &&
         Double.compare(that.precapture, precapture) == 0 &&
         stream == that.stream &&
         Double.compare(that.duration, duration) == 0 &&
         size == that.size &&
         deletionPartition == that.deletionPartition &&
         Objects.equals(accountId, that.accountId) &&
         Objects.equals(placeId, that.placeId) &&
         Objects.equals(cameraId, that.cameraId) &&
         Objects.equals(recordingId, that.recordingId) &&
         Objects.equals(personId, that.personId) &&
         Objects.equals(name, that.name) &&
         Objects.equals(loc, that.loc) &&
         Objects.equals(tags, that.tags) &&
         Objects.equals(deletionTime, that.deletionTime) &&
         videoCodec == that.videoCodec &&
         audioCodec == that.audioCodec ;
   }

   @Override
   public int hashCode() {
      return Objects.hash(accountId, placeId, cameraId, recordingId, personId, name, loc, width, height, bandwidth, framerate, precapture, stream, tags, duration, size, deletionTime, deletionPartition, videoCodec, audioCodec);
   }

	public long getExpiration() {
		return expiration;
	}

	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}

	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}
	
	public boolean isFavorite() {
		if(this.tags != null) {
			return this.tags.contains(VideoConstants.TAG_FAVORITE);
		}
		return false;
	}

	
}

