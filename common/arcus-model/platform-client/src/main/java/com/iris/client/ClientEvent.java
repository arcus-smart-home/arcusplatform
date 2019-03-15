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
/**
 * 
 */
package com.iris.client;

import java.util.HashMap;
import java.util.Map;

import com.iris.client.event.Event;

/**
 * 
 * We could further this to return more specialized events for the client to listen to
 * while using this for a "I want all events" type of listener.
 * 
 */
public class ClientEvent implements Event {
	private String type;
	private String sourceAddress;
	private Map<String, Object> attributes;

	public ClientEvent(String type, String sourceAddress) {
	   this(type, sourceAddress, null);
	}

	public ClientEvent(String type, String sourceAddress, Map<String, Object> attributes) {
		this.type = type;
		this.sourceAddress = sourceAddress;
		this.attributes = attributes != null ? attributes : new HashMap<String, Object>();
	}

	public String getType() {
		return type;
	}

	public String getSourceAddress() {
	   return sourceAddress;
	}
	
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public Object getAttribute(String attribute) {
		return attributes.get(attribute);
	}

	@Override
	public String toString() {
		return "ClientEvent [type=" + type + ", source=" + sourceAddress + ", attributes=" + attributes + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attributes == null) ? 0 : attributes.hashCode());
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
		ClientEvent other = (ClientEvent) obj;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
}

