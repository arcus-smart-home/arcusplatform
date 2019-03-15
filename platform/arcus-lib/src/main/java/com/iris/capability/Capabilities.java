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
package com.iris.capability;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.iris.capability.builder.Builders;
import com.iris.capability.builder.CapabilityDefinitionBuilder;
import com.iris.capability.builder.ReflectiveCapabilityBuilder;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.capability.Capability;

/**
 *
 */
public class Capabilities {
	private Capabilities() { }

	public static ReflectiveCapabilityBuilder implement(CapabilityDefinition definition) {
		return Builders.newReflectiveCapabilityBuilder(definition);
	}

	public static CapabilityDefinitionBuilder define() {
		return new CapabilityDefinitionBuilder();
	}

	public static String getNamespace(CapabilityDefinition definition) {
		return definition != null ? definition.getNamespace() : null;
	}

   public static String getNamespace(Capability capability) {
      return capability != null ? capability.getName() : null;
   }

	public static String getName(Capability capability) {
		return capability != null ? capability.getName() : null;
	}

	public static String getNamespace(String name) {
		Preconditions.checkNotNull(name, "name may not be null");
		String [] parts = StringUtils.split(name, ':');
		if(parts.length != 2) {
			throw new IllegalArgumentException("name is not namespaced");
		}
		return parts[0];
	}

	public static boolean isNamespaced(String name) {
	   return name.indexOf(':') > 0;
   }

	public static String namespace(String namespace, String name) {
		Preconditions.checkNotNull(namespace, "namespace may not be null");
		Preconditions.checkNotNull(name, "name may not be null");
		Preconditions.checkArgument(!StringUtils.contains(name, ':'), "name may not contain ':'");
	   return namespace + ':' + name;
   }

}

