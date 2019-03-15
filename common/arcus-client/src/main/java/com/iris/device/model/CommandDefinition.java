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
 * @deprecated use com.iris.capability.definition.MethodDefinition instead
 */
public class CommandDefinition {
	private final String name;
	private final String namespace;
	private final String command;
	private final String description;
	private final Map<String, AttributeDefinition> returnParameters;
	private final Map<String, AttributeDefinition> inputArguments;

	public CommandDefinition(String namespace, String command, String description, Map<String, AttributeDefinition> returnParameters, Map<String, AttributeDefinition> input) {
		this.name = Utils.namespace(namespace, command);
		this.namespace = namespace;
		this.command = command;
		this.description = description;
		this.returnParameters = Utils.unmodifiableCopy(returnParameters);
		this.inputArguments = Utils.unmodifiableCopy(input);
	}

	public String getName() {
		return name;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getCommand() {
		return command;
	}

	public String getDescription() {
		return description;
	}

	public Map<String, AttributeDefinition> getReturnParameters() {
	   return returnParameters;
	}

   public Map<String, AttributeDefinition> getInputArguments() {
		return inputArguments;
	}

//	public void validate(DeviceCommand command) throws ValidationException {
//		if(!name.equals(command.getCommand())) {
//			throw new ValidationException("Incompatible commands, expected [" + this.command + "] but was [" + command.getCommand() + "]");
//		}
//		Attributes.validate(inputArguments.values(), command.getAttributes());
//	}
//
	public CommandDefinitionBuilder builder() {
		return new CommandDefinitionBuilder(name);
	}

	@Override
   public String toString() {
      return "CommandDefinition [name=" + name + ", description=" + description
            + ", returnParameters=" + returnParameters + ", inputArguments="
            + inputArguments + "]";
   }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((command == null) ? 0 : command.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result
				+ ((inputArguments == null) ? 0 : inputArguments.hashCode());
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
		CommandDefinition other = (CommandDefinition) obj;
		if (command == null) {
			if (other.command != null)
				return false;
		} else if (!command.equals(other.command))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (inputArguments == null) {
			if (other.inputArguments != null)
				return false;
		} else if (!inputArguments.equals(other.inputArguments))
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

   public class CommandDefinitionBuilder {
		private String commandName;
		private Map<String,Object> attributes = new HashMap<>();

		CommandDefinitionBuilder(String commandName) {
			this.commandName = commandName;
		}

      public <V> CommandDefinitionBuilder add(AttributeKey<V> key, V value) {
         AttributeDefinition definition = inputArguments.get(key.getName());
         if(definition == null) {
            // TODO temporarilly disabled this validation
//            throw new IllegalArgumentException("Unexpected attribute [" + key + "] for command [" + CommandDefinition.this + "]");
         }
         else if(!definition.getKey().equals(key)) {
				throw new IllegalArgumentException("Unexpected attribute [" + key + "] for command [" + CommandDefinition.this + "]");
			}
			this.attributes.put(key.getName(), value);
			return this;
		}

		public <V> CommandDefinitionBuilder add(AttributeValue<V> value) {
			return add(value.getKey(), value.getValue());
		}

		public MessageBody create() {
		   return MessageBody.buildMessage(commandName, attributes);
		}

	}
}

