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
import java.util.UUID;

public class PlacePurgeRecord {
	
	public enum PurgeMode {
		PINNED,	//purge only pinned videos
		ALL	//purge all videos
	};
		
	private final UUID placeId;
	private final PurgeMode mode;
	private final Date deleteTime;
	
	public PlacePurgeRecord(UUID placeId, Date deleteTime, PurgeMode mode) {
		this.placeId = placeId;
		this.deleteTime = deleteTime;
		this.mode = mode;		
	}

	public UUID getPlaceId() {
		return placeId;
	}

	public PurgeMode getMode() {
		return mode;
	}

	public Date getDeleteTime() {
		return deleteTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
      int result = super.hashCode();
      result = prime * result
            + ((placeId == null) ? 0 : placeId.hashCode());
      result = prime * result
            + ((mode == null) ? 0 : mode.hashCode());
      result = prime * result
            + ((deleteTime == null) ? 0 : deleteTime.hashCode());
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
      PlacePurgeRecord other = (PlacePurgeRecord) obj;
      if (placeId == null) {
         if (other.placeId != null)
            return false;
      } else if (!placeId.equals(other.placeId))
         return false;
      if (mode == null) {
         if (other.mode != null)
            return false;
      } else if (!mode.equals(other.mode))
         return false;
      if (deleteTime == null) {
         if (other.deleteTime != null)
            return false;
      } else if (!deleteTime.equals(other.deleteTime))
         return false;
      
      return true;
	}

	@Override
	public String toString() {
		return "PlacePurgeRecord [placeId=" + placeId + 
				"mode=" + mode +
				"deleteTime=" + deleteTime +"]";
	}
	
	
}

