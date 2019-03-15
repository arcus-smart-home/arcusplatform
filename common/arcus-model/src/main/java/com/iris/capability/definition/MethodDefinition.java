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

import java.util.Collections;
import java.util.List;

public class MethodDefinition extends ParameterizedDefinition {
	private final boolean restful;
	
	private final List<ParameterDefinition> returnValues;
	private final List<ErrorCodeDefinition> errorCodes;

	protected MethodDefinition(String name, String description, boolean restful, List<ParameterDefinition> parameters, List<ParameterDefinition> returnValues, List<ErrorCodeDefinition> errorCodes) {
		super(name, description, parameters);
		this.returnValues = Collections.unmodifiableList(returnValues);
		this.restful = restful;
		this.errorCodes = Collections.unmodifiableList(errorCodes);
	}

	public List<ParameterDefinition> getReturnValues() {
		return returnValues;
	}

	public boolean isRestful() {
		return restful;
	}

	public List<ErrorCodeDefinition> getErrorCodes() {
		return errorCodes;
	}

	@Override
	public String toString() {
		return "MethodDefinition [restful=" + restful + ", returnValues=" + returnValues + ", errorCodes=" + errorCodes
				+ ", parameters=" + parameters + ", name=" + name + ", description=" + description + "]";
	}

}

