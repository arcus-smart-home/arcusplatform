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

import java.util.Map;

import com.iris.Utils;


/**
 * Metadata about a {@link Capability}.  The name
 * is what should be used for documentation, DSL,
 * and end-user display purposes.  The namespace
 * is used for attributes, commands, and events which
 * are associated with the capability.
 * 
 * @deprecated use com.iris.capability.definition.CapabilityDefinition instead
 */
public class CapabilityDefinition {
   private final String name;
	private final String namespace;
   private final String enhances;
	private final String description;
	private final Map<String, AttributeDefinition> attributes;
	private final Map<String, CommandDefinition> commands;
	private final Map<String, EventDefinition> events;

	public CapabilityDefinition(
			String name,
			String namespace,
         String enhances,
			String description,
         Map<String, AttributeDefinition> attributes,
         Map<String, CommandDefinition> commands,
         Map<String, EventDefinition> events
   ) {
	   this.name = name;
	   this.namespace = namespace;
	   this.enhances = enhances;
	   this.description = description;
	   this.attributes = Utils.unmodifiableCopy(attributes);
	   this.commands = Utils.unmodifiableCopy(commands);
	   this.events = Utils.unmodifiableCopy(events);
   }

	public String getCapabilityName() {
	   return name;
	}

	public String getNamespace() {
		return namespace;
	}

   public String getEnhances() {
      return enhances;
   }

	public String getDescription() {
		return description;
	}

	public Map<String, AttributeDefinition> getAttributes() {
		return attributes;
	}
	
	public Map<String, CommandDefinition> getCommands() {
		return commands;
	}

	public Map<String, EventDefinition> getEvents() {
		return events;
	}

   @Override
   public String toString() {
      return "CapabilityDefinition [name=" + name + ", namespace=" + namespace
            + ", description=" + description + ", enhances=" + enhances
            + ", attributes=" + attributes + ", commands=" + commands
            + ", events=" + events + "]";
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
            + ((attributes == null) ? 0 : attributes.hashCode());
      result = prime * result + ((commands == null) ? 0 : commands.hashCode());
      result = prime * result
            + ((description == null) ? 0 : description.hashCode());
      result = prime * result + ((enhances == null) ? 0 : enhances.hashCode());
      result = prime * result + ((events == null) ? 0 : events.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result
            + ((namespace == null) ? 0 : namespace.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      CapabilityDefinition other = (CapabilityDefinition) obj;
      if (attributes == null) {
         if (other.attributes != null) return false;
      }
      else if (!attributes.equals(other.attributes)) return false;
      if (commands == null) {
         if (other.commands != null) return false;
      }
      else if (!commands.equals(other.commands)) return false;
      if (description == null) {
         if (other.description != null) return false;
      }
      else if (!description.equals(other.description)) return false;
      if (enhances == null) {
         if (other.enhances != null) return false;
      }
      else if (!enhances.equals(other.enhances)) return false;
      if (events == null) {
         if (other.events != null) return false;
      }
      else if (!events.equals(other.events)) return false;
      if (name == null) {
         if (other.name != null) return false;
      }
      else if (!name.equals(other.name)) return false;
      if (namespace == null) {
         if (other.namespace != null) return false;
      }
      else if (!namespace.equals(other.namespace)) return false;
      return true;
   }

}

