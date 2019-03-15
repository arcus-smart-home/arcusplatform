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
package com.iris.capability.definition;

/**
 * @author Trip, Terry Trippany
 */
public class ErrorCodeDefinition extends Definition {

	private String code;
	private String codeId;

	/**
	 * This constructor is not meant top be called directly. Rather it should be
	 * invoked via <code>Definitions#ErrorCodeDefinitionBuilder
	 * 
	 * @param name
	 *            The name of the error code definition.
	 * @param description
	 *            Description included in the error code
	 * @param code
	 *            The code that will uniquely define this error code with
	 *            respect to the method that contains it
	 * @param codeId
	 *            The code that will be used as the code identifier for
	 *            references to static strings that will be generated in
	 *            capability files for the given code.
	 */
	ErrorCodeDefinition(String name, String description, String code, String codeId) {
		super(name, description);
		this.code = code;
		this.codeId = codeId;
	}

	public String getCode() {
		return code;
	}

	public String getCodeId() {
		return codeId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/*
	 * This version of equals is used for set operations. Two
	 * ErrorCodeDefinition's are considered equal for discrimination in the set
	 * if they have the same name, which is used as an identifier.
	 * 
	 * Thus this implementation of equals is in the context of an Entity key
	 * implementation and not object state equality.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ErrorCodeDefinition other = (ErrorCodeDefinition) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ErrorCodeDefinition [code=" + code + ", codeId=" + codeId + ", name=" + name + ", description="
				+ description + "]";
	}
}

