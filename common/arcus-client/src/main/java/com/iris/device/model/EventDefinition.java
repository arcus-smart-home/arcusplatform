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
package com.iris.device.model;

import java.util.HashMap;
import java.util.Map;

import com.iris.Utils;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeValue;
import com.iris.messages.MessageBody;

/**
 * @deprecated use com.iris.capability.definition.EventDefinition instead
 */
public class EventDefinition {
	private final String name;
	private final String namespace;
	private final String event;
	private final Map<String, AttributeDefinition> attributes;

	public EventDefinition(String namespace, String event, Map<String, AttributeDefinition> attributes) {
		this.name = namespace + ":" + event;
		this.namespace = namespace;
		this.event = event;
		this.attributes = Utils.unmodifiableCopy(attributes);
   }

	public String getName() {
		return name;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getEvent() {
		return event;
	}

	public Map<String, AttributeDefinition> getAttributes() {
		return attributes;
	}

	public EventBuilder builder() {
		return new EventBuilder(getEvent());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((event == null) ? 0 : event.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((namespace == null) ? 0 : namespace.hashCode());
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
		EventDefinition other = (EventDefinition) obj;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (event == null) {
			if (other.event != null)
				return false;
		} else if (!event.equals(other.event))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (namespace == null) {
			if (other.namespace != null)
				return false;
		} else if (!namespace.equals(other.namespace))
			return false;
		return true;
	}

	public static class EventBuilder {
		private String event;
		private Map<String,Object> attributes = null;

		EventBuilder(String event) {
			this.event = event;
		}

		Map<String,Object> attributes() {
		   if(attributes == null) {
		      attributes = new HashMap<>();
		   }
		   return attributes;
		}

		public <V> EventBuilder addAttribute(AttributeKey<V> key, V value) {
		   // TODO validate this is a legal attribute?
			attributes().put(key.getName(), value);
			return this;
		}

		public <V> EventBuilder addAttribute(AttributeValue<V> value) {
			return addAttribute(value.getKey(), value.getValue());
		}

		public MessageBody create() {
		   return MessageBody.buildMessage(event, attributes);
		}

	}

}

