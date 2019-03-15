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
package com.iris.platform.history;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableSet;

public class ActivityEvent {
	private UUID placeId;
	private Date timestamp;
	private Set<String> activeDevices = ImmutableSet.of();
	private Set<String> inactiveDevices = ImmutableSet.of();
	
	public UUID getPlaceId() {
		return placeId;
	}
	
	public void setPlaceId(UUID placeId) {
		this.placeId = placeId;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	public Set<String> getActiveDevices() {
		return activeDevices;
	}
	
	public void setActiveDevices(Set<String> activeDevices) {
		this.activeDevices = activeDevices;
	}
	
	public Set<String> getInactivateDevices() {
		return inactiveDevices;
	}
	
	public void setInactiveDevices(Set<String> inactiveDevices) {
		this.inactiveDevices = inactiveDevices;
	}

	@Override
	public String toString() {
		return "ActivityEvent [placeId=" + placeId + ", timestamp=" + timestamp + ", activeDevices=" + activeDevices
				+ ", inactiveDevices=" + inactiveDevices + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((activeDevices == null) ? 0 : activeDevices.hashCode());
		result = prime * result + ((inactiveDevices == null) ? 0 : inactiveDevices.hashCode());
		result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
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
		ActivityEvent other = (ActivityEvent) obj;
		if (activeDevices == null) {
			if (other.activeDevices != null)
				return false;
		} else if (!activeDevices.equals(other.activeDevices))
			return false;
		if (inactiveDevices == null) {
			if (other.inactiveDevices != null)
				return false;
		} else if (!inactiveDevices.equals(other.inactiveDevices))
			return false;
		if (placeId == null) {
			if (other.placeId != null)
				return false;
		} else if (!placeId.equals(other.placeId))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		return true;
	}

}

