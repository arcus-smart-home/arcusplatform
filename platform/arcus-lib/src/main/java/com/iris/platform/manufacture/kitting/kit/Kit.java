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
package com.iris.platform.manufacture.kitting.kit;

import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class Kit {
	private final String hubid;
	private final String type;
	private final List<KitDevice> devices;
		
	public Kit(String hubid, String type, List<KitDevice> devices) {
		this.hubid = hubid;
		this.type = type;
		this.devices = devices;
	}
	
	public static class Builder {
		private String hubid;
		private String type;
		private List<KitDevice> devices;

		Builder() {	
			devices = new ArrayList<>();
		}
		
		public Builder withHubId(String hub) {
			this.hubid= hub;
			return this;
		}
		
		public Builder withType(String type) {
			this.type = type;
			return this;
		}
		
		public Builder withDevice(KitDevice device) {
			devices.add(device);
			return this;
		}
		
		public Builder withDevices(List<KitDevice> devices) {
			this.devices = devices;
			return this;
		}
		
		public Kit build() {
			return new Kit(hubid,type,devices);
		}
	}
	
	public static Builder builder() {
		return new Builder();
	}

	public String getHubId() {
		return hubid;
	}

	public String getType() {
		return type;
	}

	public List<KitDevice> getDevices() {
		return devices;
	}

	public List<KitDevice> getSortedDevices() {
		List<KitDevice> sortedDevices = new ArrayList<>(devices);
		sortedDevices.sort(comparing(KitDevice::getType));
		return sortedDevices;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((devices == null) ? 0 : devices.hashCode());
		result = prime * result + ((hubid == null) ? 0 : hubid.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		Kit other = (Kit) obj;
		if (devices == null) {
			if (other.devices != null)
				return false;
		} else if (!devices.equals(other.devices))
			return false;
		if (hubid == null) {
			if (other.hubid != null)
				return false;
		} else if (!hubid.equals(other.hubid))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "Kit [hubid=" + hubid + ", type=" + type + ", devices=" + devices + "]";
	}
	
	public final static class Error {
		public static final Error OK = new Error(0,"Kit Ok");
		public static final Error KIT_IS_NULL = new Error(1,"Kit is null");
		public static final Error HUBID_NOT_FOUND = new Error(2,"No hub id was found");
		public static final Error HUBID_INVALID = new Error(3, "Hub id not of proper format XXX-####");
		public static final Error TYPE_NOT_FOUND = new Error(4, "No kit type information found");
		public static final Error TYPE_INVALID = new Error(5, "Not a valid kit type");
		public static final Error DEVICES_NOT_FOUND = new Error(6, "No devices where found in the kit.");
		public static final Error DEVICES_COUNT_INCORRECT = new Error(7, "Devices do not match those required in the kit type.");
		public static final Error DEVICE_INVALID = new Error(8, "1 or more devices are not of the proper format.");

		protected int id;
		protected String message;
		
		public Error(Error code, String message) {
			this.id = code.id;
			this.message = message;
		}
		
		private Error(int id, String message) {
			this.id = id;
			this.message = message;
		}
		
		public int  getId() {
			return id;
		}
		
		public String getMessage() {
			return message;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
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
			Error other = (Error) obj;
			if (id != other.id)
				return false;
			return true;
		}
		
	}

	public Error isValid() {
		if (StringUtils.isBlank(hubid)) return Error.HUBID_NOT_FOUND;
		if (StringUtils.isBlank(type)) return Error.TYPE_NOT_FOUND;
		if (devices == null) return Error.DEVICES_NOT_FOUND;
		if (devices != null) {
			String message = "";
			for (KitDevice device: devices) {
				KitDevice.Error code = device.isValid();
				if (code != KitDevice.Error.OK) {
					message += code.getMessage() + "\n";
				}
			}
			if (!StringUtils.isBlank(message)) {
				return new Error(Error.DEVICE_INVALID,message);
			}
		}
		return Error.OK;
	}	
}

