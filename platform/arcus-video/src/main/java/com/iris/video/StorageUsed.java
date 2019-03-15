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

import org.eclipse.jdt.annotation.Nullable;

public class StorageUsed {
	private final long usedBytes;
	private final long timestamp;
	
	public StorageUsed(long usedBytes, long timestamp) {
		this.usedBytes = usedBytes;
		this.timestamp = timestamp;
	}

	public StorageUsed(long usedBytes, @Nullable Date timestamp) {
		this.usedBytes = usedBytes;
		this.timestamp = timestamp == null ? -1 : timestamp.getTime();
	}

	public long getUsedBytes() {
		return usedBytes;
	}

	public long getTimestamp() {
		return timestamp;
	}
	
	@Nullable
	public Date getLastModified() {
		return timestamp > -1 ? new Date(timestamp) : null;
	}

	@Override
	public String toString() {
		return "UsedQuota [usedBytes=" + usedBytes + ", timestamp=" + timestamp + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		result = prime * result + (int) (usedBytes ^ (usedBytes >>> 32));
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
		StorageUsed other = (StorageUsed) obj;
		if (timestamp != other.timestamp)
			return false;
		if (usedBytes != other.usedBytes)
			return false;
		return true;
	}
	
}

