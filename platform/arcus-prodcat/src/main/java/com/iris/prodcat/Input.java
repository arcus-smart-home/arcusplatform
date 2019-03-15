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
package com.iris.prodcat;

public class Input {
	public enum InputType { HIDDEN, TEXT };
	
	private InputType type;
	private String name;
	private String label;
	private String value;
	private Integer maxlen;
	private Integer minlen;
	private Boolean required;
	
	public InputType getType() {
		return type;
	}
	public void setType(InputType type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public Integer getMaxlen() {
		return maxlen;
	}
	public void setMaxlen(Integer maxlen) {
		this.maxlen = maxlen;
	}
	public Boolean getRequired() {
		return required;
	}
	public void setRequired(Boolean required) {
		this.required = required;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	
	public Integer getMinlen() {
		return minlen;
	}
	public void setMinlen(Integer minlen) {
		this.minlen = minlen;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((maxlen == null) ? 0 : maxlen.hashCode());
		result = prime * result + ((minlen == null) ? 0 : minlen.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((required == null) ? 0 : required.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		Input other = (Input) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (maxlen == null) {
			if (other.maxlen != null)
				return false;
		} else if (!maxlen.equals(other.maxlen))
			return false;
		if (minlen == null) {
			if (other.minlen != null)
				return false;
		} else if (!minlen.equals(other.minlen))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (required == null) {
			if (other.required != null)
				return false;
		} else if (!required.equals(other.required))
			return false;
		if (type != other.type)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "Input [type=" + type + ", name=" + name + ", label=" + label
				+ ", value=" + value + ", maxlen=" + maxlen + ", minlen=" + minlen + ", required="
				+ required + "]";
	}
	
	
	
	
	
}

