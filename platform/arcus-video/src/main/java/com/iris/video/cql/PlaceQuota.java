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

import com.google.common.base.Objects;

public class PlaceQuota {
	public enum Unit {
		Bytes,
		Number
	}
	private final long used;
	private final long quota;
	private final long usedTimestamp;
	private final Unit unit;
	
	public PlaceQuota(long used, long usedTimestamp, long quota, Unit unit) {
		this.used = used;
		this.usedTimestamp = usedTimestamp;
		this.quota = quota;
		this.unit = unit;
	}
	
	public long getUsed() {
		return used;
	}
	
	public long getQuota() {
		return quota;
	}
	
	public boolean isUnderQuota() {
		return used < quota;
	}

	@Override
	public String toString() {
		return "PlaceQuota [used=" + used + ", quota=" + quota + ", unit=" + unit + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (quota ^ (quota >>> 32));
		result = prime * result + (int) (used ^ (used >>> 32));
		result = prime * result + (int) (usedTimestamp ^ (usedTimestamp >>> 32));
		result = prime * result + (int) (unit!=null ? unit.hashCode() : 0);
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
		PlaceQuota other = (PlaceQuota) obj;
		if (quota != other.quota)
			return false;
		if (used != other.used)
			return false;
		if (usedTimestamp != other.usedTimestamp)
			return false;
		if(Objects.equal(unit, other.unit))
			return false;

		return true;
	}

	public Unit getUnit() {
		return unit;
	}

	public long getUsedTimestamp() {
		return usedTimestamp;
	}
	
}

