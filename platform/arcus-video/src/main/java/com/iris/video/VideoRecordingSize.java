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

import java.util.UUID;

public class VideoRecordingSize {
	public static final long SIZE_UNKNOWN = -1;
	
	private final UUID recordingId;
	private final long size;
	private final boolean isFavorite;
	
	public VideoRecordingSize(UUID recordingId, long size, boolean isFavorite) {
		this.recordingId = recordingId;
		this.size = size;
		this.isFavorite = isFavorite;
	}
	
	public VideoRecordingSize(UUID recordingId, boolean isFavorite) {
		this(recordingId, 0, isFavorite);
	}

	public UUID getRecordingId() {
		return recordingId;
	}

	public long getSize() {
		return size;
	}
	
	public boolean isCompletedRecording() {
		if(SIZE_UNKNOWN == size) {
			throw new IllegalAccessError("Can not call isCompletedRecording becasuse size is not retrieved");
		}
		return size > 0;
	}

	@Override
	public String toString() {
		return "VideoRecordingSize [recordingId=" + recordingId + ", size=" + size + ", favorite="+isFavorite + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((recordingId == null) ? 0 : recordingId.hashCode());
		result = prime * result + (int) (size ^ (size >>> 32));
		result = prime * result + (isFavorite ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VideoRecordingSize other = (VideoRecordingSize) obj;
		if (recordingId == null) {
			if (other.recordingId != null)
				return false;
		}else if (!recordingId.equals(other.recordingId)) {
			return false;
		}else if (size != other.size) {
			return false;
		}else if (isFavorite != other.isFavorite) {         
         return false;
      }
		return true;
	}

	public boolean isFavorite() {
		return isFavorite;
	}
	
}

