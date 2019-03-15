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
package com.iris.platform.history.appender;

import java.util.UUID;

import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.RuleCapability;

public class MessageContext {
	
	long timestamp;
	UUID placeId;
	String placeName;
   Address subjectAddress;
   Address actorAddress;
   String actorName;
   String methodName;
   
   UUID deviceId;
   String deviceName;
   String hubId;
   String hubName;
   private ObjectNameCache cache;
   
   
   public MessageContext(long timestamp, UUID placeId, String placeName, Address subjectAddress, Address actorAddress, String actorName, String methodName, ObjectNameCache cache) {
   	this.timestamp = timestamp;
   	this.placeId = placeId;
   	this.placeName = placeName;
   	this.subjectAddress = subjectAddress;
   	this.actorAddress = actorAddress;
   	this.actorName = actorName;
   	this.methodName = methodName;
   	this.cache = cache;
   }

	public long getTimestamp() {
		return timestamp;
	}

	public UUID getPlaceId() {
		return placeId;
	}
	
	public String getPlaceName() {
		return placeName;
	}

	public Address getSubjectAddress() {
		return subjectAddress;
	}

	public Address getActorAddress() {
		return actorAddress;
	}
	
	public String getActorName() {
		return actorName;
	}

	public String getMethodName() {
		return methodName;
	}

	public UUID getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(UUID deviceId) {
		this.deviceId = deviceId;
	}
	
	public String getHubId() {
		return hubId;
	}
	
	public void setHubId(String hubId) {
		this.hubId = hubId;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getHubName() {
		return hubName;
	}

	public void setHubName(String hubName) {
		this.hubName = hubName;
	}

	public boolean actorIsPerson() {
		if (actorAddress == null) return false;
		return (actorAddress.getGroup().equals(PersonCapability.NAMESPACE));
	}
	
	public boolean actorIsRule() {
		if (actorAddress == null) return false;
		return (actorAddress.getGroup().equals(RuleCapability.NAMESPACE));
	}
	
	public boolean subjectIsDevice() {
		return ((String)subjectAddress.getGroup()).equals(DeviceCapability.NAMESPACE);
	}
	
	public boolean subjectIsSubsystem() {
		return ((String)subjectAddress.getGroup()).startsWith("sub");
	}
	
	public boolean subjectIsHub() {
		return subjectAddress.isHubAddress();
	}
	
	public String findName(Address address) {
		return cache.getName(address);
	}
	
   
}

