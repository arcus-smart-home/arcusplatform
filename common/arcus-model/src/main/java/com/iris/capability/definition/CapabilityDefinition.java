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
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class CapabilityDefinition extends ObjectDefinition implements MergeableDefinition<CapabilityDefinition> {
   private final String enhances;
   private final List<AttributeDefinition> attributes;
   private final Set<ErrorCodeDefinition> errorEventExceptions;

   CapabilityDefinition(
         String name,
         String description,
         String namespace,
         String version,
         List<MethodDefinition> methods,
         List<EventDefinition> events,
         String enhances,
         List<AttributeDefinition> attributes,
         Set<ErrorCodeDefinition> errorEventExceptions
   ) {
      super(
            name,
            description,
            namespace,
            version,
            methods,
            events
      );
      this.enhances = enhances;
      this.attributes = Collections.unmodifiableList(attributes);
      this.errorEventExceptions = ImmutableSet.copyOf(errorEventExceptions); 
   }
   
   public String getEnhances() {
      return enhances;
   }

   public List<AttributeDefinition> getAttributes() {
      return attributes;
   }

   public Set<ErrorCodeDefinition> getErrorEventExceptions() {
	   return errorEventExceptions;
   }

   @Override
   public CapabilityDefinition merge(CapabilityDefinition toMerge) {
      return new CapabilityDefinition(
         name,
         description,
         namespace,
         version,
         MergeUtils.mergeMethods(name, methods, toMerge.methods),
         MergeUtils.mergeEvents(name, events, toMerge.events),
         enhances,
         MergeUtils.mergeAttributes(name, attributes, toMerge.attributes),
         MergeUtils.mergeErrors(name, errorEventExceptions, toMerge.errorEventExceptions)
      );
   }

   @Override
	public String toString() {
		return "CapabilityDefinition [enhances=" + enhances + ", attributes=" + attributes + ", errorEventExceptions="
				+ errorEventExceptions + ", namespace=" + namespace + ", version=" + version + ", methods=" + methods
				+ ", events=" + events + ", name=" + name + ", description=" + description + "]";
	}

}

