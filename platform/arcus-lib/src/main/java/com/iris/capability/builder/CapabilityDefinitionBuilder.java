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
package com.iris.capability.builder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.iris.Utils;
import com.iris.capability.attribute.Attributes;
import com.iris.capability.attribute.ChainedAttributeDefinitionBuilder;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;
import com.iris.device.model.CommandDefinition;
import com.iris.device.model.EventDefinition;
import com.iris.messages.capability.Capability;

/**
 *
 */
public class CapabilityDefinitionBuilder {
   private String name;
	private String namespace;
	private String enhances = Capability.NAME;
	private String description;
	private Map<String, AttributeDefinition> attributes =
			new LinkedHashMap<>();
	private Map<String, CommandDefinition> commands =
			new LinkedHashMap<>();
	private Map<String, EventDefinition> events =
			new LinkedHashMap<>();

	public CapabilityDefinitionBuilder withName(String name) {
	   this.name = name;
	   return this;
	}

	public CapabilityDefinitionBuilder withNamespace(String namespace) {
	   this.namespace = namespace;
	   return this;
	}

   public CapabilityDefinitionBuilder withEnhances(String enhances) {
      this.enhances = enhances;
      return this;
   }

	public CapabilityDefinitionBuilder withDescription(String description) {
		this.description = description;
		return this;
	}

	public CapabilityDefinitionBuilder addAttribute(AttributeDefinition attribute) {
		String key = attribute.getName();
      checkNamespace(key);
		checkNotDefined(attributes, key, "attribute");
		attributes.put(key, attribute);
		return this;
	}

	public ChainedAttributeDefinitionBuilder<CapabilityDefinitionBuilder> buildAttribute(String attributeName, Class<?> type) {
		return buildAttribute(AttributeKey.create(Utils.namespace(namespace, attributeName), type));
	}

	public ChainedAttributeDefinitionBuilder<CapabilityDefinitionBuilder> buildAttribute(AttributeKey<?> key) {
      checkNamespace(key.getName());
	   return Attributes
					.build(key)
					.chain((attribute) -> this.addAttribute(attribute));
	}

   public CapabilityDefinitionBuilder addCommand(CommandDefinition command) {
		String key = command.getCommand();
		checkNotDefined(commands, key, "command");
		commands.put(key, command);
		return this;
	}

	public CapabilityCommandBuilder buildCommand(String command) {
		Preconditions.checkNotNull(command, "command");
		return new CapabilityCommandBuilder(command);
	}

	public CapabilityDefinitionBuilder addEvent(EventDefinition event) {
		String key = event.getEvent();
		checkNotDefined(events, key, "event");
		events.put(key, event);
		return this;
	}

	public CapabilityEventBuilder buildEvent(String event) {
		Preconditions.checkNotNull(event, "event");
		return new CapabilityEventBuilder(event);
	}

	public CapabilityDefinition create() {
	   Utils.assertNotEmpty(name, "Must specify a name");
	   Utils.assertNotEmpty(namespace, "Must specify a namespace");
		return new CapabilityDefinition(name, namespace, enhances, description, attributes, commands, events);
	}

	public <T> AttributeKey<T> createKey(String attribute, Class<T> simpleType) {
	   return AttributeKey.create(Utils.namespace(namespace, attribute), simpleType);
	}

   public <T> AttributeKey<Set<T>> createSetKey(String attribute, Class<T> containedType) {
      return AttributeKey.createSetOf(Utils.namespace(namespace, attribute), containedType);
   }

   public <T> AttributeKey<List<T>> createListKey(String attribute, Class<T> containedType) {
      return AttributeKey.createListOf(Utils.namespace(namespace, attribute), containedType);
   }

   public <T> AttributeKey<Map<String, T>> createMapKey(String attribute, Class<T> containedType) {
      return AttributeKey.createMapOf(Utils.namespace(namespace, attribute), containedType);
   }

   private void checkNamespace(String keyName) {
      String namespace = Utils.getNamespace(keyName);
      Utils.assertTrue(this.namespace.equals(namespace), "Invalid attribute name '" + keyName + "' not in the '" + namespace + "' namespace");
   }

	private void checkNotDefined(Map<String, ?> beans,  String key, String type) {
		if(beans.containsKey(key)) {
			throw new IllegalStateException("Tried to add " + type + " named [" + key + "] twice");
		}
   }

	public class CapabilityCommandBuilder {
		private CommandDefinitionBuilder delegate;

		CapabilityCommandBuilder(String command) {
			this.delegate = new CommandDefinitionBuilder(namespace, command);
		}

		public CapabilityCommandBuilder addInputArgument(AttributeDefinition attribute) {
	      delegate.addInputArgument(attribute);
	      return this;
      }

		public CapabilityCommandBuilder withDescription(String description) {
	      delegate.withDescription(description);
	      return this;
      }

		public CapabilityCommandBuilder addReturnParameter(AttributeDefinition attribute) {
		   delegate.addReturnParameter(attribute);
		   return this;
		}

		public ChainedAttributeDefinitionBuilder<CapabilityCommandBuilder> buildReturnParameter(String name, Class<?> type) {
		   return Attributes.build(name, type).chain((attribute) -> this.addReturnParameter(attribute));
		}

		public ChainedAttributeDefinitionBuilder<CapabilityCommandBuilder> buildReturnParameter(AttributeKey<?> key) {
		   return Attributes.build(key).chain((attribute) -> this.addReturnParameter(attribute));
		}

		public ChainedAttributeDefinitionBuilder<CapabilityCommandBuilder> buildInputArgument(String name, Class<?> type) {
			return Attributes.build(name, type).chain((attribute) -> this.addInputArgument(attribute));
      }

		public ChainedAttributeDefinitionBuilder<CapabilityCommandBuilder> buildInputArgument(AttributeKey<?> key) {
			return Attributes.build(key).chain((attribute) -> this.addInputArgument(attribute));
      }

		public CapabilityDefinitionBuilder add() {
			return CapabilityDefinitionBuilder.this.addCommand(this.delegate.create());
		}

		public CommandDefinition addAndGet() {
			CommandDefinition command = this.delegate.create();
			CapabilityDefinitionBuilder.this.addCommand(command);
			return command;
		}
	}

	public class CapabilityEventBuilder {
		private String event;
		private Map<String, AttributeDefinition> attributes =
				new LinkedHashMap<>();

		CapabilityEventBuilder(String event) {
			this.event = event;
		}

		public CapabilityEventBuilder addAttribute(AttributeDefinition attribute) {
	      String key = attribute.getName();
	      checkNotDefined(attributes, key, event + " attribute");
			return this;
      }

		public ChainedAttributeDefinitionBuilder<CapabilityEventBuilder> buildAttribute(String name, Class<?> type) {
	      return Attributes.build(name, type).chain((attribute) -> this.addAttribute(attribute));
      }

		public ChainedAttributeDefinitionBuilder<CapabilityEventBuilder> buildAttribute(AttributeKey<?> key) {
	      return Attributes.build(key).chain((attribute) -> this.addAttribute(attribute));
      }

		public CapabilityDefinitionBuilder add() {
			return CapabilityDefinitionBuilder.this.addEvent(this.create());
		}

		public EventDefinition addAndGet() {
			EventDefinition event = this.create();
			CapabilityDefinitionBuilder.this.addEvent(event);
			return event;
		}

		private EventDefinition create() {
			return new EventDefinition(namespace, event, attributes);
		}
	}

}

