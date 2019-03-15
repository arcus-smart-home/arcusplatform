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
package com.iris.messages.model;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageConstants;
import com.iris.messages.services.PlatformConstants;

public class MobileDevice implements Copyable<MobileDevice> {

	private UUID personId;
	private int deviceIndex;
	private Date associated;
	private String osType;
	private String osVersion;
	private String formFactor;
	private String phoneNumber;
	private String deviceIdentifier;
	private String deviceModel;
	private String deviceVendor;
	private String resolution;
	private String notificationToken;
	private double lastLatitude;
	private double lastLongitude;
	private Date lastLocationTime;
	private String name;
	private String appVersion;

	public String getType() {
		return PlatformConstants.SERVICE_MOBILEDEVICES;
	}

	public String getAddress() {
		return MessageConstants.SERVICE + ":" + getType() + ":" + getId();
	}

	public Set<String> getCaps() {
		return ImmutableSet.of("base", getType());
	}

	public String getId() {
		return personId + "." + deviceIndex;
	}

	public UUID getPersonId() {
		return personId;
	}

	public void setPersonId(UUID personId) {
		this.personId = personId;
	}

	public int getDeviceIndex() {
		return deviceIndex;
	}

	public void setDeviceIndex(int deviceIndex) {
		this.deviceIndex = deviceIndex;
	}

	public Date getAssociated() {
		return associated;
	}

	public void setAssociated(Date associated) {
		this.associated = associated;
	}

	public String getOsType() {
		return osType;
	}

	public void setOsType(String osType) {
		this.osType = osType;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}

	public String getFormFactor() {
		return formFactor;
	}

	public void setFormFactor(String formFactor) {
		this.formFactor = formFactor;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getDeviceIdentifier() {
		return deviceIdentifier;
	}

	public void setDeviceIdentifier(String deviceIdentifier) {
		this.deviceIdentifier = deviceIdentifier;
	}

	public String getDeviceModel() {
		return deviceModel;
	}

	public void setDeviceModel(String deviceModel) {
		this.deviceModel = deviceModel;
	}

	public String getDeviceVendor() {
		return deviceVendor;
	}

	public void setDeviceVendor(String deviceVendor) {
		this.deviceVendor = deviceVendor;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}

	public String getNotificationToken() {
		return notificationToken;
	}

	public void setNotificationToken(String notificationToken) {
		this.notificationToken = notificationToken;
	}

	public double getLastLatitude() {
		return lastLatitude;
	}

	public void setLastLatitude(double lastLatitude) {
		this.lastLatitude = lastLatitude;
	}

	public double getLastLongitude() {
		return lastLongitude;
	}

	public void setLastLongitude(double lastLongitude) {
		this.lastLongitude = lastLongitude;
	}

	public Date getLastLocationTime() {
		return lastLocationTime;
	}

	public void setLastLocationTime(Date lastLocationTime) {
		this.lastLocationTime = lastLocationTime;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((appVersion == null) ? 0 : appVersion.hashCode());
		result = prime * result + ((associated == null) ? 0 : associated.hashCode());
		result = prime * result + ((deviceIdentifier == null) ? 0 : deviceIdentifier.hashCode());
		result = prime * result + deviceIndex;
		result = prime * result + ((deviceModel == null) ? 0 : deviceModel.hashCode());
		result = prime * result + ((deviceVendor == null) ? 0 : deviceVendor.hashCode());
		result = prime * result + ((formFactor == null) ? 0 : formFactor.hashCode());
		long temp;
		temp = Double.doubleToLongBits(lastLatitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((lastLocationTime == null) ? 0 : lastLocationTime.hashCode());
		temp = Double.doubleToLongBits(lastLongitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((notificationToken == null) ? 0 : notificationToken.hashCode());
		result = prime * result + ((osType == null) ? 0 : osType.hashCode());
		result = prime * result + ((osVersion == null) ? 0 : osVersion.hashCode());
		result = prime * result + ((personId == null) ? 0 : personId.hashCode());
		result = prime * result + ((phoneNumber == null) ? 0 : phoneNumber.hashCode());
		result = prime * result + ((resolution == null) ? 0 : resolution.hashCode());
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
		MobileDevice other = (MobileDevice) obj;
		if (appVersion == null) {
			if (other.appVersion != null)
				return false;
		} else if (!appVersion.equals(other.appVersion))
			return false;
		if (associated == null) {
			if (other.associated != null)
				return false;
		} else if (!associated.equals(other.associated))
			return false;
		if (deviceIdentifier == null) {
			if (other.deviceIdentifier != null)
				return false;
		} else if (!deviceIdentifier.equals(other.deviceIdentifier))
			return false;
		if (deviceIndex != other.deviceIndex)
			return false;
		if (deviceModel == null) {
			if (other.deviceModel != null)
				return false;
		} else if (!deviceModel.equals(other.deviceModel))
			return false;
		if (deviceVendor == null) {
			if (other.deviceVendor != null)
				return false;
		} else if (!deviceVendor.equals(other.deviceVendor))
			return false;
		if (formFactor == null) {
			if (other.formFactor != null)
				return false;
		} else if (!formFactor.equals(other.formFactor))
			return false;
		if (Double.doubleToLongBits(lastLatitude) != Double.doubleToLongBits(other.lastLatitude))
			return false;
		if (lastLocationTime == null) {
			if (other.lastLocationTime != null)
				return false;
		} else if (!lastLocationTime.equals(other.lastLocationTime))
			return false;
		if (Double.doubleToLongBits(lastLongitude) != Double.doubleToLongBits(other.lastLongitude))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (notificationToken == null) {
			if (other.notificationToken != null)
				return false;
		} else if (!notificationToken.equals(other.notificationToken))
			return false;
		if (osType == null) {
			if (other.osType != null)
				return false;
		} else if (!osType.equals(other.osType))
			return false;
		if (osVersion == null) {
			if (other.osVersion != null)
				return false;
		} else if (!osVersion.equals(other.osVersion))
			return false;
		if (personId == null) {
			if (other.personId != null)
				return false;
		} else if (!personId.equals(other.personId))
			return false;
		if (phoneNumber == null) {
			if (other.phoneNumber != null)
				return false;
		} else if (!phoneNumber.equals(other.phoneNumber))
			return false;
		if (resolution == null) {
			if (other.resolution != null)
				return false;
		} else if (!resolution.equals(other.resolution))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MobileDevice [personId=" + personId + ", deviceIndex=" + deviceIndex + ", associated=" + associated
				+ ", osType=" + osType + ", osVersion=" + osVersion + ", formFactor=" + formFactor + ", phoneNumber="
				+ phoneNumber + ", deviceIdentifier=" + deviceIdentifier + ", deviceModel=" + deviceModel
				+ ", deviceVendor=" + deviceVendor + ", resolution=" + resolution + ", notificationToken="
				+ notificationToken + ", lastLatitude=" + lastLatitude + ", lastLongitude=" + lastLongitude
				+ ", lastLocationTime=" + lastLocationTime + ", name=" + name + ", appVersion=" + appVersion + "]";
	}

	@Override
	public MobileDevice copy() {
		try {
			return (MobileDevice) clone();
		} catch (CloneNotSupportedException cnse) {
			throw new RuntimeException(cnse);
		}
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		MobileDevice md = (MobileDevice) super.clone();
		md.associated = associated != null ? (Date) associated.clone() : null;
		md.lastLocationTime = lastLocationTime != null ? (Date) lastLocationTime.clone() : null;
		return md;
	}
}

